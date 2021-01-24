package net.scheffers.robot.emu.mmio;

public class MMIOSockets {
	
	public static int MMIO_START = 0xfec0;
	
	public static int activeSocket = 0;
	public static MMIOSocket[] sockets = new MMIOSocket[256];
	
	public int zFlags;
	public int zLocalPort;
	public int zLocalAddress;
	public int zRemotePort;
	public int zRemoteAddress;
	
	public static void write(int address, int value) {
		if (address < MMIO_START || address > MMIO_START + 0xf) {
			return;
		}
		value &= 0xff;
		address -= MMIO_START;
		
		if (address == 0x0) {
			if (sockets[value] != null) {
				activeSocket = value;
			} else {
				activeSocket = 0;
			}
			return;
		}
		
		if (activeSocket == 0) {
			//if (address)
		}
		
	}
	
}
