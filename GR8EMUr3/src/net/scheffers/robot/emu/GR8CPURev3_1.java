package net.scheffers.robot.emu;

public class GR8CPURev3_1 {
	
	public static final boolean nativeLoadSuccess;
	
	static {
		boolean a = false;
		try {
			System.loadLibrary("GR8CPUr3_1");
			a = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		nativeLoadSuccess = a;
	}
	
	public byte regA, regB, regX, regIR;
	public short regPC, regAR, stackPtr;
	
	public boolean flagCout, flagZero;
	
	/** True when the CPU is waiting for controller or keyboard input. */
	public boolean isWaiting;
	
	public void writeD(byte regD) {
		System.out.write(regD);
	}
	
	public byte readD() {
		return 0;
	}
	
	public void tick(int tickTimes) {
		if (nativeLoadSuccess) {
			nativeTick(tickTimes);
			return;
		}
		//TODO: slower pure java impl
	}
	
	protected native void nativeTick(int tickTimes);
	
}
