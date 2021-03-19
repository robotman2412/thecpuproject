package net.scheffers.robot.xasm.importing;

import java.io.IOException;

public interface ImportSupplier {
	
	boolean isFileAvailable(String name);
	String getStringFile(String name) throws IOException;
	byte[] getByteFile(String name) throws IOException;
	long[] getBinaryFile(String name, int bits, boolean isLittleEndian) throws IOException;
	
}
