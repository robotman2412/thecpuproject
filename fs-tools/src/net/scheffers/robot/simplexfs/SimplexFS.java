package net.scheffers.robot.simplexfs;

import jutils.IOUtils;
import jutils.database.BytePool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SimplexFS {
	
	public static boolean doSendWarnings = true;
	
	/**
	 * Makes a SimplexFS with the given parameters.
	 * Between 16 and 65535 sectors of 256 bytes.
	 * @param out the thing to write to
	 * @param volumeName the volume name (optional)
	 * @param numSectors the number of sectors in total
	 * @param mediaType the media type (default 0x00)
	 * @param identifier the identifier (any)
	 * @param include directory to recursively include as root (optional)
	 * @throws IOException when reading or writing fails
	 * @throws IllegalArgumentException when the number of sectors is out of range
	 * @throws IllegalArgumentException when volume name is too long
	 * @throws Exception if there is not enough space to include the requested files
	 */
	public static void makeSimplexFS(OutputStream out, String volumeName, int numSectors, byte mediaType, int identifier, File include) throws Exception {
		// Sanity check.
		if (numSectors > 65535) {
			throw new IllegalArgumentException("SimplexFS cannot hold more than 65535 sectors!");
		}
		if (numSectors < 5) {
			throw new IllegalArgumentException("SimplexFS cannot hold less than 5 sectors!");
		}
		else if (numSectors < 16) {
			warn("Less then 16 sectors is highly discouraged!");
		}
		if (volumeName == null || volumeName.length() == 0) {
			volumeName = "nameless drive";
		}
		else
		{
			boolean gotNull = false;
			boolean gotCtrl = false;
			boolean gotNonAscii = false;
			
			for (char c : volumeName.toCharArray()) {
				if (c == 0) {
					gotNull = true;
				} else if (c < 0x20) {
					gotCtrl = true;
				} else if (c > 0x7f) {
					gotNonAscii = true;
				}
			}
			
			if (gotNull) warn("Got null character in volume name!");
			if (gotCtrl) warn("Got ascii control character in volume name!");
			if (gotNonAscii) warn("Got non-ascii character in volume name!");
			if (gotNonAscii || gotCtrl || gotNull) {
				warn("For volume name \"" + escVoln(volumeName) + "\"");
			}
			if (volumeName.getBytes(StandardCharsets.US_ASCII).length > 24) {
				throw new IllegalArgumentException("Volume name cannot be longer than 24 ascii bytes!");
			}
		}
		
		int fatSectorSize = (numSectors + 127) / 128;
		int rootDirStart = 2 + fatSectorSize * 2;
		int rootDirLen = 0;
		
		byte[][] sectors = new byte[numSectors][];
		
		byte[] header = new byte[256];
		
		// Header magic.
		header[0] = (byte) 0xfe;
		header[1] = (byte) 0xca;
		header[2] = (byte) 0x01;
		header[3] = (byte) 0x32;
		header[4] = (byte) 0x94;
		// Number of sectors.
		header[5] = (byte) (numSectors & 0xff);
		header[6] = (byte) (numSectors >>> 8);
		// Number of FAT entries.
		header[7] = (byte) (numSectors & 0xff);
		header[8] = (byte) (numSectors >>> 8);
		// Number of sectors FAT uses.
		header[9] = (byte) (fatSectorSize & 0xff);
		header[10] = (byte) (fatSectorSize >>> 8);
		// Filesystem version (v1.0).
		header[13] = (byte) 1;
		header[14] = (byte) 0;
		// Media type.
		header[15] = mediaType;
		// Identifier.
		header[16] = (byte) ((identifier >>> 24) & 0xff);
		header[17] = (byte) ((identifier >>> 16) & 0xff);
		header[18] = (byte) ((identifier >>> 8) & 0xff);
		header[19] = (byte) (identifier & 0xff);
		// Volume name.
		byte[] volName = volumeName.getBytes(StandardCharsets.US_ASCII);
		System.arraycopy(volName, 0, header, 20, volName.length);
		
		// Now, we need to make the allocation tables before we can continue.
		byte[] fat = new byte[fatSectorSize * 256];
		// Mark out the reserved sectors.
		for (int i = 0; i < rootDirStart; i++) {
			fat[i * 2] = (byte) 0xff;
			fat[i * 2 + 1] = (byte) 0xff;
		}
		
		if (include == null) {
			// Create dummy root.
			fat[rootDirStart * 2] = (byte) 0xff;
			fat[rootDirStart * 2 + 1] = (byte) 0xff;
			// Funnily enough, completely null is a valid empty directory.
		}
		else
		{
			// Time for some recursion.
			InsertedFile rootStart = insertFile(include, header, fat, sectors);
			rootDirStart = rootStart.startingBlock;
			rootDirLen = rootStart.fileSize;
		}
		
		// Root directory sector.
		header[11] = (byte) (rootDirStart & 0xff);
		header[12] = (byte) (rootDirStart >>> 8);
		
		// Root directory size.
		header[44] = (byte) (rootDirLen & 0xff);
		header[45] = (byte) ((rootDirLen >>> 8) & 0xff);
		header[46] = (byte) ((rootDirLen >>> 16) & 0xff);
		
		// Calculate FAT checksum.
		byte fatCksumLo = 0;
		byte fatCksumHi = 0;
		for (int i = 0; i < numSectors; i++) {
			fatCksumLo ^= fat[i * 2];
			fatCksumHi ^= fat[i * 2 + 1];
		}
		
		// Fat checksum.
		header[252] = fatCksumLo;
		header[253] = fatCksumHi;
		
		// Calculate header checksum.
		byte headCksumLo = 0;
		byte headCksumHi = 0;
		for (int i = 0; i < 254; i += 2) {
			headCksumLo ^= header[i];
			headCksumHi ^= header[i + 1];
		}
		
		// Header checksum.
		header[254] = headCksumLo;
		header[255] = headCksumHi;
		
		// Copy everything to sector array.
		sectors[0] = header;
		sectors[1] = header;
		for (int i = 0; i < fatSectorSize; i++) {
			sectors[i + 2] = new byte[256];
			sectors[i + 2 + fatSectorSize] = sectors[i + 2];
			System.arraycopy(fat, i * 256, sectors[i + 2], 0, 256);
		}
		
		// Flush sector array.
		byte[] nullArr = new byte[256];
		for (int i = 0; i < sectors.length; i++) {
			if (sectors[i] == null) {
				out.write(nullArr);
			}
			else
			{
				out.write(sectors[i]);
			}
		}
		
		// Finish up.
		out.flush();
		out.close();
		
	}
	
	/**
	 * Unpacks a SimplexFS to the host OS.
	 * @param in the SimplexFS image input
	 * @param out the output directory
	 * @param metaOut where to put metadata about the filesystem (useful but optional)
	 * @throws IOException when reading fails   
	 */
	public static void unpackSimplexFS(InputStream in, File out, OutputStream metaOut) throws IOException {
		if (metaOut == null) {
			// Make a void OutputStream.
			metaOut = new OutputStream() {
				@Override
				public void write(int b) throws IOException {
					
				}
			};
		}
		
		if (in.available() < 256 * 5) {
			throw new IOException("The input image (" + in.available() + " bytes) is smaller than the minimum size (1280 bytes)!");
		}
		// Read primary header.
		byte[] header0 = new byte[256];
		int read = in.read(header0);
		if (read != 256) throw new IOException("What?!? I want 256 bytes, not " + read + "!");
		// Read backup header.
		byte[] header1 = new byte[256];
		read = in.read(header1);
		if (read != 256) throw new IOException("What?!? I want 256 bytes, not " + read + "!");
		// Header magic.
		boolean magic0 = true;
		if (header0[0] != (byte) 0xfe) magic0 = false;
		if (header0[1] != (byte) 0xca) magic0 = false;
		if (header0[2] != (byte) 0x01) magic0 = false;
		if (header0[3] != (byte) 0x32) magic0 = false;
		if (header0[4] != (byte) 0x94) magic0 = false;
		boolean magic1 = true;
		if (header1[0] != (byte) 0xfe) magic1 = false;
		if (header1[1] != (byte) 0xca) magic1 = false;
		if (header1[2] != (byte) 0x01) magic1 = false;
		if (header1[3] != (byte) 0x32) magic1 = false;
		if (header1[4] != (byte) 0x94) magic1 = false;
		if (!magic0) {
			throw new IOException("Header has an invalid magic value!");
		}
		if (!magic1) {
			throw new IOException("Backup header has an invalid magic value!");
		}
		byte chkSumLo0 = 0;
		byte chkSumHi0 = 0;
		byte chkSumLo1 = 0;
		byte chkSumHi1 = 0;
		for (int i = 0; i < 254; i += 2) {
			chkSumLo0 ^= header0[i];
			chkSumHi0 ^= header0[i + 1];
			chkSumLo1 ^= header1[i];
			chkSumHi1 ^= header1[i + 1];
		}
		boolean checksum0 = chkSumLo0 == header0[254] && chkSumHi0 == header0[255];
		boolean checksum1 = chkSumLo1 == header1[254] && chkSumHi1 == header1[255];
		if (!checksum0) {
			throw new IOException("Header has an invalid checksum!");
		}
		if (!checksum1) {
			throw new IOException("Backup header has an invalid checksum!");
		}
		
		// TODO: validate FATs
		
		// Initial things are good, now load more crap.
		
		// Number of physical sectors.
		int numSect = (header0[5] & 0xff) | ((header0[6] << 8) & 0xff00);
		// Number of FAT entries.
		int numFatEntries = (header0[7] & 0xff) | ((header0[8] << 8) & 0xff00);
		// Number of sectors reserved per FAT.
		int numFatSectors = (header0[9] & 0xff) | ((header0[10] << 8) & 0xff00);
		// Root directory starting sector.
		int rootStart = (header0[11] & 0xff) | ((header0[12] << 8) & 0xff00);
		// SimplexFS version.
		int majorVer = (header0[13] & 0xff);
		int minorVer = (header0[14] & 0xff);
		// Media type.
		byte mediaType = header0[15];
		// Media ID.
		int identifier = (header0[16] & 0xff) | ((header0[17] << 8) & 0xff00) | ((header0[18] << 16) & 0xff0000) | ((header0[19] << 24) & 0xff000000);
		// Volume name.
		int bufLen = 0;
		for (; bufLen < 24 && header0[bufLen + 20] != 0; bufLen++);
		String volumeName = new String(header0, 20, bufLen, StandardCharsets.US_ASCII);
		// Root directory size.
		int rootSize = header0[44] | ((header0[45] << 8) & 0xff00) | ((header0[46] << 16) & 0xff0000);
		// TODO: fat checksum
		
		// Now, read the FATs.
		byte[] fat0 = new byte[numFatSectors * 256];
		read = in.read(fat0);
		if (read != fat0.length) throw new IOException("What?!? I want " + fat0.length + " bytes, not" + read + "!");
		byte[] fat1 = new byte[numFatSectors * 256];
		read = in.read(fat1);
		if (read != fat1.length) throw new IOException("What?!? I want " + fat1.length + " bytes, not" + read + "!");
		
		// Now, read the remaining sectors.
		byte[][] sectors = new byte[numSect][256];
		for (int i = 2 + numFatSectors * 2; i < numSect; i++) {
			read = in.read(sectors[i]);
			if (read < 256) {
				warn("The image is too small; some data may have been lost!");
				break;
			}
		}
		
		extractFile(rootStart, rootSize, 0x41ff, header0, fat0, sectors, out, metaOut);
	}
	
	public static void extractFile(int block, int size, int flags, byte[] header, byte[] fat, byte[][] sectors, File output, OutputStream metaOutput) throws IOException {
		byte[] file = getRawFile(block, size, fat, sectors);
		if ((flags & 0x4000) > 0) {
			// Extract directory.
			int numEntries = (file[0] & 0xff) | (file[1] << 8);
			for (int i = 0; i < numEntries; i++) {
				int feflags = (file[2 + i * 16] & 0xff) | ((file[3 + i * 16] << 8) & 0xff00);
				int feUid = (file[4 + i * 16] & 0xff) | ((file[5 + i * 16] << 8) & 0xff00);
				int feSect = (file[6 + i * 16] & 0xff) | ((file[7 + i * 16] << 8) & 0xff00);
				int feLen = (file[8 + i * 16] & 0xff) | ((file[9 + i * 16] << 8) & 0xff00) | ((file[10 + i * 16] << 16) & 0xff0000);
				int feChksum = (file[11 + i * 16] & 0xff) | ((file[12 + i * 16] << 8) & 0xff00);
				int feStrp = (file[13 + i * 16] & 0xff) | ((file[14 + i * 16]) & 0xff00);
				feStrp += 4 + numEntries * 16;
				int feEnd;
				for (feEnd = feStrp; feEnd < file.length; feEnd ++) {
					if (file[feEnd] == 0) break;
				}
				String fileName = new String(file, feStrp, feEnd - feStrp, StandardCharsets.US_ASCII);
				String newPath = output.getAbsolutePath();
				if (!newPath.endsWith("/") && !newPath.endsWith("\\")) newPath += "/";
				newPath += fileName;
				extractFile(feSect, feLen, feflags, header, fat, sectors, new File(newPath), metaOutput);
			}
		}
		else
		{
			// Extract file.
			IOUtils.saveBytes(output, file);
		}
	}
	
	public static byte[] getRawFile(int block, int size, byte[] fat, byte[][] sectors) throws IOException {
		BytePool pool = new BytePool(256);
		List<Integer> chain = new ArrayList<>(10);
		while (true) {
			int chainContent = (fat[block * 2] & 0xff) | ((fat[block * 2 + 1] << 8) & 0xff00);
			if (chainContent == 0x0000) {
				throw new IOException("Chain " + descChain(chain) + " ends in free sector and not end-of-chain!");
			}
			else if (chain.contains(chainContent)) {
				throw new IOException("Chain " + descChain(chain) + " loops back on itself!");
			}
			chain.add(block);
			if (sectors[block] == null) {
				pool.addBytes(new byte[256]);
			}
			else
			{
				pool.addBytes(sectors[block]);
			}
			if (chainContent == 0xffff) {
				break;
			}
			block = chainContent;
		}
		if (chain.size() > (size + 255) / 256) {
			// Warn that chain is too long.
			warn("Chain " + descChain(chain) + " is longer than expected length of " + (size + 255) / 256 + "!");
		}
		else
		{
			// Set correct length for content.
			pool.bufferUsedLength = size;
		}
		return pool.copyToArray();
	}
	
	public static String descChain(List<Integer> chain) {
		StringBuilder out = new StringBuilder();
		out.append('[').append(String.format("0x%04x", chain.get(0)));
		for (int i = 1; i < chain.size(); i++) {
			out.append(String.format(" 0x%04x", chain.get(i)));
		}
		out.append(']');
		return out.toString();
	}
	
	/**
	 * Inserts a file or directory directory from the host system into SimplexFS.
	 * Does not recalculate any checksums.
	 * @param include file to inlude
	 * @param header filesystem header
	 * @param fat fat to use and allocate to
	 * @param sectors an array of sectors, full size even in excluding header and fat
	 * @return the starting sector of the new file or directory
	 * @throws Exception if the filesystem is too full to add this file or directory
	 */
	public static InsertedFile insertFile(File include, byte[] header, byte[] fat, byte[][] sectors) throws Exception {
		byte[] data;
		if (include.isDirectory()) {
			data = includeDirectory(include, header, fat, sectors);
		}
		else
		{
			data = IOUtils.readBytes(include);
		}
		InsertedFile ins = new InsertedFile();
		ins.fileSize = data.length;
		int numBlocks = (data.length + 255) / 256;
		List<Integer> chain = new ArrayList<>(numBlocks);
		int tableIndex = 4; // It's impossible to have a free block before this.
		for (; tableIndex < sectors.length && chain.size() < numBlocks; tableIndex ++) {
			if (fat[tableIndex * 2] == 0 && fat[tableIndex * 2 + 1] == 0) {
				chain.add(tableIndex);
			}
		}
		if (chain.size() < numBlocks) {
			throw new Exception("Out of space!");
		}
		for (int i = 0; i < chain.size() - 1; i++) {
			fat[chain.get(i) * 2] = (byte) (chain.get(i + 1) & 0xff);
			fat[chain.get(i) * 2 + 1] = (byte) (chain.get(i + 1) >>> 8);
			if (sectors[chain.get(i)] == null) {
				sectors[chain.get(i)] = new byte[256];
			}
			System.arraycopy(data, i * 256, sectors[chain.get(i)], 0, 256);
		}
		fat[chain.get(chain.size() - 1) * 2] = (byte) 0xff;
		fat[chain.get(chain.size() - 1) * 2 + 1] = (byte) 0xff;
		if (sectors[chain.get(chain.size() - 1)] == null) {
			sectors[chain.get(chain.size() - 1)] = new byte[256];
		}
		System.arraycopy(data, chain.size() * 256 - 256, sectors[chain.get(chain.size() - 1)], 0, data.length & 0xff);
		ins.startingBlock = chain.get(0);
		return ins;
	}
	
	/**
	 * Directory part of {@link SimplexFS#insertFile(File, byte[], byte[], byte[][])}
	 */
	protected static byte[] includeDirectory(File include, byte[] header, byte[] fat, byte[][] sectors) throws Exception {
		File[] sub = include.listFiles();
		int stringLen = 0;
		byte[] data = new byte[32 + sub.length * 32];
		int fileIndex = 32;
		data[0] = (byte) (sub.length & 0xff);
		data[1] = (byte) (sub.length >>> 8);
		for (File f : sub) {
			//                            dir      file
			//                          0q040644 0q000644
			int flags = f.isDirectory() ? 0x41a4 : 0x01a4;
			data[fileIndex] = (byte) (flags & 0xff);
			data[fileIndex + 1] = (byte) (flags >>> 8);
			// File owner.
			data[fileIndex + 2] = (byte) 0x00;
			data[fileIndex + 3] = (byte) 0x00;
			InsertedFile ins = insertFile(f, header, fat, sectors);
			// File starting block.
			data[fileIndex + 4] = (byte) (ins.startingBlock & 0xff);
			data[fileIndex + 5] = (byte) (ins.startingBlock >>> 8);
			// File length.
			data[fileIndex + 6] = (byte) (ins.fileSize & 0xff);
			data[fileIndex + 7] = (byte) (ins.fileSize >>> 8);
			data[fileIndex + 8] = (byte) (ins.fileSize >>> 16);
			// Checksum of content.
			data[fileIndex + 9] = ins.checkSumLo;
			data[fileIndex + 10] = ins.checkSumHi;
			// Filename.
			byte[] str = f.getName().getBytes(StandardCharsets.US_ASCII);
			if (str.length > 16) {
				throw new Exception("Filename '" + f.getName() + "' too long!\nPath is '" + f.getAbsolutePath() + "'");
			}
			System.arraycopy(str, 0, data, fileIndex + 16, str.length);
			fileIndex += 32;
		}
		return data;
	}
	
	public static String escVoln(String volumeName) {
		StringBuilder out = new StringBuilder();
		for (char c : volumeName.toCharArray()) {
			if (c == 0) {
				out.append("\\0");
			}
			else if (c < 0x20) {
				out.append(String.format("\\x%02x", (byte) c));
			}
			else if (Character.isWhitespace(c) && c != ' ') {
				if (c > 0xff) {
					out.append(String.format("\\u%04x", (int) c));
				}
				else
				{
					out.append(String.format("\\x%02x", (byte) c));
				}
			}
			else
			{
				out.append(c);
			}
		}
		return out.toString();
	}
	
	public static void warn(String message) {
		if (doSendWarnings) {
			String[] lines = message.split("\\r\\n|\\n|\\r");
			System.err.println("[SimplexFS] [WARN] " + lines[0]);
			for (int i = 1; i < lines.length; i++) {
				System.err.println("                   " + lines[0]);
			}
		}
	}
	
}
