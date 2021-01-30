package net.scheffers.robot.simplexfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LogisimBullshitInputStream extends InputStream {
	
	protected final InputStream stream;
	
	public LogisimBullshitInputStream(InputStream stream) throws IOException {
		this.stream = stream;
		check();
	}
	
	public LogisimBullshitInputStream(File input) throws IOException {
		this.stream = new FileInputStream(input);
		check();
	}
	
	protected void check() throws IOException {
		byte[] buffer = new byte[30];
		int i;
		for (i = 0; i < 30; i++) {
			int read = stream.read();
			if (read < 0) {
				throw new IOException("Incorrect logisim hex file header!");
			}
			else if (read == '\n') {
				String res = new String(buffer, 0, i);
				if (res.matches("v[0-9]+\\.[0-9]+ raw")) {
					return;
				}
				throw new IOException("Incorrect logisim hex file header!");
			}
			buffer[i] = (byte) read;
		}
		throw new IOException("Incorrect logisim hex file header!");
	}
	
	@Override
	public int available() throws IOException {
		if (stream.markSupported()) {
			stream.mark(Integer.MAX_VALUE);
			int num = 0;
			while (read() != -1) num ++;
			stream.reset();
			return num;
		}
		return stream.available() / 3;
	}
	
	@Override
	public int read() throws IOException {
		int out = 0;
		for (int i = 0; i < 2; i++) {
			int read = stream.read();
			if (read == -1 && i == 0) return -1;
			if (read == ' ' && i != 0) break;
			else if (read == ' ') i --;
			out <<= 4;
			if (read >= '0' && read <= '9') {
				out |= read - '0';
			}
			else if (read >= 'a' && read <= 'f') {
				out |= read - 'a' + 0xA;
			}
			else if (read >= 'A' && read <= 'F') {
				out |= read - 'A' + 0xA;
			}
			else if (read != ' ') {
				throw new IOException("Invalid hexadecimal format!");
			}
		}
		return out;
	}
	
}
