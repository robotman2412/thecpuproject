package assembler;

import jutils.gui.DropdownElement;
import jutils.gui.SidedRunnable;

import processing.serial.Serial;

public class FlasherThread extends Thread {
	
	public long refreshTime = 2500000;
	
	public String selectedPort;
	public Serial serial;
	public long lastRefreshtime;
	
	public static Assembler asm;
	
	byte[] buffer = new byte[65536];
	int bufferLength = 0;
	int expectedLength = 3;
	boolean readingCommand = true;
	byte command;
	
	byte[] dataToFlash;

	public boolean mustChangePort;

	public int sysType;
	public int sysVersion;

	public int flashedUntil;
	public int lastFlashedStart;
	public boolean readyToFlash;
	public int lastFlashedLength;
	public String msg = "";
	
	public static final byte CMD_SYS_TYPE = 0x01;
	public static final byte SYS_TYPE_MEMFLASHER = 0x02;
	public static final byte SYS_TYPE_ROMFLASHER = 0x03;

	public static final byte CMD_ERROR = 0x00;
	public static final byte CMD_FLASH = 0x02;
	public static final byte CMD_READ = 0x03;
	public static final byte CMD_REP_MEM = 0x04;
	public static final byte CMD_PART_FLASH = 0x05;
	public static final byte CMD_FLASH_READY = 0x06;
	public static final byte CMD_FLASH_PROGRESS = 0x07;
	public static final byte CMD_DEBUG = 0x08;

	public static final byte BUFFER_OVERFLOW = 1;
	
	public FlasherThread() {
		asm.portDropdown.onChange = new SidedRunnable() {
			@Override
			public void pre() {
			}
			@Override
			public void post() {
				mustChangePort = true;
			}
		};
		refresh();
		selectedPort = null;
		setPriority(MIN_PRIORITY);
		setName("Flasher Thread");
	}
	
	public void changePort() {
		if (serial != null) {
			serial.stop();
			serial = null;
		}
		selectedPort = asm.portDropdown.elements[asm.portDropdown.selectedIndex].value;
		if (selectedPort == null) {
			return;
		}
		try {
			serial = new Serial(asm, selectedPort, 115200);
		} catch(Exception e) {
			System.err.println("Could not open " + selectedPort + "!");
			selectedPort = null;
			e.printStackTrace();
			return;
		}
		readingCommand = true;
		expectedLength = 3;
		bufferLength = 0;
		sysType = -1;
		sysVersion = -1;
	}
	
	public void run() {
		while (true) {
			try {
				synchronized (this) {
					this.wait(50);
				}
			} catch (InterruptedException e) {
				//e.printStackTrace();
			}
			if (mustChangePort) {
				System.out.println("Changing port...");
				changePort();
				mustChangePort = false;
				if (selectedPort != null) {
					System.out.println("Connected to port!");
				}
			}
			if (selectedPort != null && serial.available() > 0) {
				int available = serial.available();
				int readable = Math.min(expectedLength - bufferLength, available);
				for (int i = 0; i < readable; i++) {
					buffer[bufferLength + i] = (byte) serial.read();
				}
				bufferLength += readable;
				if (bufferLength == expectedLength) {
					if (readingCommand) {
						command = buffer[0];
						expectedLength = (buffer[1] << 8) | buffer[2];
						if (expectedLength == 0) {
							processCommand();
							expectedLength = 3;
							bufferLength = 0;
							readingCommand = false;
						}
						bufferLength = 0;
					}
					else
					{
						processCommand();
						expectedLength = 3;
						bufferLength = 0;
					}
					readingCommand = !readingCommand;
				}
			}
			if (dataToFlash != null && selectedPort != null) {
				if (sysType == SYS_TYPE_MEMFLASHER) {
					if (dataToFlash.length > 256) {
						System.err.println(asm.error0 = "Cannot flash: too much data!");
						asm.error1 = "";
						dataToFlash = null;
					}
					else
					{
						flashSimple();
					}
				}
				else if (sysType == SYS_TYPE_ROMFLASHER) {
					if (dataToFlash.length <= 1024) {
						flashSimple();
					}
					else if (readyToFlash) {
						flashPart();
					}
				}
			}
			if (System.nanoTime() > lastRefreshtime + refreshTime) {
				refresh();
			}
		}
	}

	private void flashSimple() {
		System.out.println("Flashing...");
		serial.write(CMD_FLASH);
		serial.write((dataToFlash.length >> 8) & 0xff);
		serial.write(dataToFlash.length & 0xff);
		serial.write(dataToFlash);
		dataToFlash = null;
		readyToFlash = false;
	}

	private void flashPart() {
		serial.write(CMD_PART_FLASH);
		int len = Math.min(dataToFlash.length - flashedUntil, 1000);
		System.out.println("Flashing part (" + flashedUntil + " -> " + (flashedUntil + len) + ")...");
		int len2 = len + 2;
		serial.write((len2 >> 8) & 0xff);
		serial.write(len2 & 0xff);
		lastFlashedStart = flashedUntil;
		serial.write((flashedUntil >> 8) & 0xff);
		serial.write(flashedUntil & 0xff);
		for (int i = 0; i < len; i++) {
			serial.write(dataToFlash[i + flashedUntil]);
		}
		if (len == dataToFlash.length - flashedUntil) {
			dataToFlash = null;
		}
		flashedUntil += len;
		readyToFlash = false;
	}
	
	public void processCommand() {
		switch(command) {
			case(CMD_FLASH_PROGRESS):
				int prog = (Byte.toUnsignedInt(buffer[0]) << 8) | Byte.toUnsignedInt(buffer[1]);
				asm.progEnabled = true;
				asm.progress = (prog + lastFlashedStart) / (float) lastFlashedLength;
				asm.progText = "Flashing: " + Math.round(asm.progress * 100f) + "%";
				break;
			case(CMD_FLASH_READY):
				System.out.println("Flashing operation ready.");
				if (dataToFlash == null) {
					asm.progress = 1;
					asm.progText = msg;
				}
				readyToFlash = true;
				break;
			case(CMD_SYS_TYPE):
				System.out.println("Flasher ready.");
				if (bufferLength != 2) {
					System.err.println("Error: Flasher metadata must be exactly 2 bytes!");
					return;
				}
				if (buffer[0] == SYS_TYPE_MEMFLASHER) {
					System.out.println("System type: GR8CPU Rev2 Memory flasher.");
				}
				else if (buffer[0] == SYS_TYPE_ROMFLASHER) {
					System.out.println("System type: 16-bit ROM flasher.");
				}
				else
				{
					System.err.println(String.format("Error: Flasher system type 0x%02x not supported!", buffer[0]));
				}
				readyToFlash = true;
				sysVersion = Byte.toUnsignedInt(buffer[1]);
				sysType = buffer[0];
				System.out.println("System version: " + sysVersion);
				break;
			case(CMD_REP_MEM):
				System.out.println("Dump length: " + bufferLength);
				byte[] buf = new byte[bufferLength];
				System.arraycopy(buffer, 0, buf, 0, bufferLength);
				asm.reportMem(buf);
				break;
			case(CMD_DEBUG):
				System.out.printf("%02x ", buffer[0]);
				System.out.flush();
				break;
		}
	}

	public void refresh() {
		refreshTime = System.nanoTime();
		String[] ports = Serial.list();
		DropdownElement[] elements = new DropdownElement[ports.length + 1];
		elements[0] = new DropdownElement(null, "No port");
		for (int i = 0; i < ports.length; i++) {
			elements[i + 1] = new DropdownElement(ports[i], ports[i]);
		}
		asm.portDropdown.elements = elements;
		asm.portDropdown.selectedIndex = asm.portDropdown.selectedIndex % asm.portDropdown.elements.length;
	}
	
	public void flash(byte[] data, String msg) {
		System.out.println("Flashing " + data.length + " bytes...");
		this.msg = msg;
		flashedUntil = lastFlashedStart = 0;
		dataToFlash = data;
		lastFlashedLength = data.length;
	}

	public void dumpMemory() {
		if (serial != null) {
			serial.write(CMD_READ);
			serial.write(0);
			serial.write(1);
			serial.write(0);
		}
	}
	
}
