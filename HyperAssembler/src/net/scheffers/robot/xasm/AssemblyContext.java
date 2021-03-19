package net.scheffers.robot.xasm;

public class AssemblyContext {
	
	/** The offset at which the buffer is to start in memory. */
	protected long bufferOffset;
	/** The current address of the output in real memory address stuffs. */
	protected long currentAddress;
	/** The output buffer, word of. */
	protected long[] outputBuffer;
	/** The length the output buffer that is used. */
	protected int outputLength;
	
	public AssemblyContext() {
		outputBuffer = new long[512];
		bufferOffset = 0;
		currentAddress = 0;
		outputLength = 0;
	}
	
	public void pad(int nWords, long value) {
		
	}
	
	public void seek(long address) {
		currentAddress = address;
	}
	
	public int getOutputLength() {
		return outputLength;
	}
	
}
