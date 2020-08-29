package net.scheffers.robot.cpucontroldebug;

import jutils.database.BytePool;
import processing.core.PApplet;
import processing.serial.Serial;

public class CPUControlDebug extends PApplet {
	
	public static String comPort = "COM9";
	public static Serial serial;
	
	public static void main(String[] args) {
		PApplet.main(CPUControlDebug.class.getName());
	}
	
	@Override
	public void settings() {
		size(500, 500);
	}
	
	byte[] cpuMemory;
	byte regA;
	byte regB;
	byte regC;
	byte regADR;
	byte regPC;
	byte regIR;
	
	@Override
	public void setup() {
		serial = new Serial(this, "COM9", 115200);
		pool = new BytePool();
		reading = false;
		cpuMemory = new byte[256];
		new Thread(() -> {
			Object lock = 0;
			while (true) {
				if (serial.available() > 0) {
					handleSerial();
				}
				try {
					synchronized (lock) {
						lock.wait(10);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	@Override
	public void draw() {
		background(255);
		fill(0);
		textAlign(CORNER);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				text(hex(cpuMemory[y * 16 + x], 2), x * 20 + 20, y * 20 + 60);
			}
		}
		text(hex(regA, 2), 20, 40);
		text(hex(regB, 2), 40, 40);
		text(hex(regC, 2), 60, 40);
		text(hex(regPC, 2), 80, 40);
		text(hex(regADR, 2), 100, 40);
		text(hex(regIR, 2), 120, 40);
		text("A", 20, 20);
		text("B", 40, 20);
		text("C", 60, 20);
		text("PC", 80, 20);
		text("AR", 100, 20);
		text("IR", 120, 20);
	}
	
	boolean reading;
	BytePool pool;
	public void handleSerial() {
		while (serial.available() > 0) {
			while (!reading) {
				if (serial.available() < 1) {
					return;
				} else if (serial.read() == 2) {
					reading = true;
					pool.clear();
				}
			}
			while (reading) {
				if (serial.available() < 1) {
					return;
				}
				else
				{
					int read = serial.read();
					if (read == 3) {
						reading = false;
						handleMsg(new String(pool.copyToArray()));
					}
					else
					{
						pool.addBytes((byte) read);
					}
				}
			}
		}
	}
	
	public void handleMsg(String msg) {
		int indexial = msg.indexOf(':');
		if (indexial == -1) {
			return;
		}
		String type = msg.substring(0, indexial);
		String payload = msg.substring(indexial + 1);
		switch (type.toLowerCase()) {
			case("regs"):
				regsUpdate(payload);
				break;
			case("write_mem"):
				memUpdate(payload);
				break;
			case("write_d"):
				regDUpdate(payload);
				break;
		}
	}
	
	protected void regsUpdate(String payload) {
		if (!payload.matches("[0-9A-Fa-f]{12}")) {
			return;
		}
		regA = (byte) unhex(payload.substring(0, 2));
		regB = (byte) unhex(payload.substring(2, 4));
		regC = (byte) unhex(payload.substring(4, 6));
		regADR = (byte) unhex(payload.substring(6, 8));
		regIR = (byte) unhex(payload.substring(8, 10));
		regPC = (byte) unhex(payload.substring(10, 12));
	}
	
	protected void memUpdate(String payload) {
		if (!payload.matches("[0-9A-Fa-f]{4}")) {
			return;
		}
		int adr = unhex(payload.substring(0, 2));
		byte val = (byte) unhex(payload.substring(2, 4));
		System.out.println(payload);
		cpuMemory[adr] = val;
	}
	
	protected void regDUpdate(String payload) {
		if (!payload.matches("[0-9A-Fa-f]{2}")) {
			return;
		}
		System.out.write(unhex(payload));
	}
	
}
