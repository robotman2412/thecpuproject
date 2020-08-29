package net.scheffers.robot.emu;

import processing.core.PApplet;

public class GR8EMUr3_1 extends PApplet {
	
	public static void main(String[] args) {
		PApplet.main(GR8CPURev3_1.class.getName());
	}
	
	@Override
	public void settings() {
		size(500, 500);
	}
	
	public EmuThread emulator;
	
	@Override
	public void setup() {
		emulator = new EmuThread();
		emulator.setHertz(1000);
		emulator.start();
	}
	
	@Override
	public void draw() {
		
	}
	
	@Override
	public void exit() {
		emulator.cont = false;
		try {
			emulator.join(2500);
		} catch (InterruptedException e) {
			System.err.println("Interruped waiting for emulator.");
		}
		super.exit();
	}
	
	public static class EmuThread extends Thread {
		
		public GR8CPURev3_1 instance;
		
		protected int tickTimes;
		protected long nanoWait;
		
		public volatile boolean cont;
		
		/**
		 * Sets the wait time based on hertz.
		 * Set to -1 to stop.
		 * Set to 0 for fast as possible.
		 * @param hertz the target frequency in hertz
		 */
		public void setHertz(double hertz) {
			if (hertz == 0) {
				nanoWait = 0;
				return;
			}
			else if (hertz < 0) {
				nanoWait = -1;
				return;
			}
			else if (hertz > 10000000) {
				hertz /= 100000;
				tickTimes = 100000;
			}
			else if (hertz > 10000) {
				hertz /= 100;
				tickTimes = 100;
			}
			else
			{
				tickTimes = 1;
			}
			nanoWait = (long) (1000000000 / hertz);
		}
		
		/**
		 * Sets the wait time.
		 * Set to -1 to stop.
		 * Set to 0 for fast as possible.
		 * @param nanoWait the target wait time
		 */
		public void setNanoWait(long nanoWait) {
			this.nanoWait = nanoWait;
			tickTimes = 1;
		}
		
		@Override
		public void run() {
			cont = true;
			while (cont) {
				long nano0 = System.nanoTime();
				instance.tick(tickTimes);
				long nano1 = System.nanoTime();
				long wait = Math.max(0, nanoWait - nano1 + nano0);
				long waitMs = wait / 1000;
				try {
					synchronized (this) {
						wait(waitMs, (int) (wait % 1000));
					}
				} catch (InterruptedException e) {
					cont = false;
				}
			}
		}
		
	}
	
}
