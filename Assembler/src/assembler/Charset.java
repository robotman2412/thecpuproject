package assembler;

import jutils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Charset {
	
	public static void addAscii(Map<Character, Integer> charMap) {
		for (int i = 0; i < 128; i++) {
			charMap.put((char) i, i);
		}
	}
	
	public int numBits;
	public boolean isLittleEndian;
	public Map<Character, Integer> charMap;
	
	public Charset(int numBits, boolean isLittleEndian) {
		this.numBits = numBits;
		this.isLittleEndian = isLittleEndian;
		charMap = new HashMap<>();
	}
	
	public Charset(File file) throws IOException {
		String raw = new String(IOUtils.readBytes(file));
		int offset = 0;
		charMap = new HashMap<>();
		while (raw.length() > 0) {
			int eol;
			int nextl;
			//stupid newlines
			//and charsets, for that matter
			{
				int a = raw.indexOf('\r');
				int b = raw.indexOf('\n');
				if (b != -1 && b < a) {
					eol = b;
					nextl = b + 1;
				}
				else if (a != -1 && b == a + 1) {
					eol = a;
					nextl = b + 1;
				}
				else if (a == -1) {
					eol = b;
					nextl = b + 1;
				}
				else
				{
					eol = a;
					nextl = a + 1;
				}
			}
			String line = raw.substring(0, eol);
			if (line.startsWith("bits=")) {
				numBits = Integer.parseInt(line.substring(5));
			}
			else if (line.equals("endian=litte")) {
				isLittleEndian = true;
			}
			else if (line.equals("endian=big")) {
				isLittleEndian = false;
			}
			else if (line.startsWith("offset=")) {
				offset = Integer.parseInt(line.substring(7));
			}
			else if (line.equals("exteds-ascii")) {
				addAscii(charMap);
			}
			else if (line.startsWith("start-ordered ")) {
				int indexial = line.indexOf(':');
				int numToRead = Integer.parseInt(line.substring(14, indexial));
				String toRead = raw.substring(indexial + 1, indexial + 1 + numToRead);
				nextl = indexial + 1 + numToRead;
				for (int i = 0; i < numToRead; i++) {
					charMap.put(toRead.charAt(i), offset + i);
				}
			}
			raw = raw.substring(nextl);
		}
	}
	
	public int translate(char in) {
		return charMap.getOrDefault(in, (int) in);
	}
	
}
