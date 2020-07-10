package net.scheffers.robot.hyperasm.importing;

public class NopImportSupplier implements ImportSupplier {
	
	@Override
	public boolean isFileAvailable(String name) {
		return false;
	}
	
	@Override
	public String getStringFile(String name) {
		return null;
	}
	
	@Override
	public byte[] getByteFile(String name) {
		return new byte[0];
	}
	
	@Override
	public long[] getBinaryFile(String name, int bits, boolean isLittleEndian) {
		return new long[0];
	}
	
}
