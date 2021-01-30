package net.scheffers.robot.simplexfs;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class LogisimBullshitOutputStream extends OutputStream {
	
	public static final char[] hexChars = "0123456789abcdef".toCharArray();
	
	protected final OutputStream stream;
	protected boolean first;
	
	public LogisimBullshitOutputStream(OutputStream stream) throws IOException {
		this.stream = stream;
		head();
	}
	
	public LogisimBullshitOutputStream(File output) throws IOException {
		this.stream = new FileOutputStream(output);
		head();
	}
	
	protected void head() throws IOException {
		first = true;
		stream.write("v1.0 raw\n".getBytes(StandardCharsets.US_ASCII));
	}
	
	@Override
	public void write(int b) throws IOException {
		if (first) {
			first = false;
		}
		else
		{
			stream.write(' ');
		}
		stream.write(hexChars[(b >> 4) & 0xf]);
		stream.write(hexChars[b & 0xf]);
	}
	
}
