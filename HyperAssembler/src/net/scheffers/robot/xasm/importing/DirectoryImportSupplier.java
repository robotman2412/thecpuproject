package net.scheffers.robot.xasm.importing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DirectoryImportSupplier implements ImportSupplier {
	
	public String dir;
	
	public DirectoryImportSupplier(String dir) {
		if (!dir.endsWith("/") && !dir.endsWith("/")) {
			dir += '/';
		}
		this.dir = dir;
	}
	
	public DirectoryImportSupplier(File dirFile) {
		this(dirFile.getAbsolutePath());
	}
	
	public File resolveFile(String path) {
		File abs = new File(path);
		if (abs.exists()) {
			return abs;
		}
		File rel = new File(dir + path);
		if (rel.exists()) {
			return rel;
		}
		return null;
	}
	
	@Override
	public boolean isFileAvailable(String name) {
		return resolveFile(name) != null;
	}
	
	@Override
	public String getStringFile(String name) throws IOException {
		return new String(Files.readAllBytes(resolveFile(name).toPath()));
	}
	
	@Override
	public byte[] getByteFile(String name) {
		return new byte[0];
	}
	
	@Override
	public long[] getBinaryFile(String name, int bits, boolean isLittleEndian) {
		return new long[0];
	}
	
	public void setDir(String dir) {
		if (!dir.endsWith("/") && !dir.endsWith("/")) {
			dir += '/';
		}
		this.dir = dir;
	}
}
