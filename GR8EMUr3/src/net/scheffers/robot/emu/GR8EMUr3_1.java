package net.scheffers.robot.emu;

import jutils.database.BytePool;
import jutils.gui.style.TextureButtonStyle;
import jutils.guiv2.Button;
import jutils.guiv2.GUIScreen;
import jutils.guiv2.TextureButton;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class GR8EMUr3_1 extends PApplet implements GR8EMUConstants {
	
	public static final int EXC_ERR = -1;
	public static final int EXC_NORM = 0;
	public static final int EXC_HALT = 1;
	public static final int EXC_BRK = 2;
	public static final int EXC_OVERFLOW = 3;
	public static final int EXC_NOINSN = 4;
	public static final int EXC_WAIT_STEP = 5;
	public static final int EXC_RESET = 6;
	
	public static void main(String[] args) {
		PApplet.main(GR8EMUr3_1.class.getName());
	}
	
	@Override
	public void settings() {
		size(601, 601);
	}
	
	public EmuThread emulator;
	public static PFont font12;
	public static PFont font48;
	public static GR8EMUr3_1 inst;
	
	public GUIScreen screen;
	public Register8Bit regA, regB, regX, regD, regIR, bus;
	public ArithmeticLogicUnit alu;
	public Register16Bit regPC, regAR, stackPtr, adrBus;
	
	public Memory memory, rom;
	public ControlUnit controlUnit;
	
	public PImage reset, resetHover, resetPressed, resetDisabled;
	public PImage play, playHover, playPressed, playDisabled;
	public PImage pause, pauseHover, pausePressed, pauseDisabled;
	public PImage cycle, cycleHover, cyclePressed, cycleDisabled;
	public PImage instruction, instructionHover, instructionPressed, instructionDisabled;
	
	public TextureButton resetButton, playButton, pauseButton, cycleButton, instructionButton;
	
	public byte[][] ttyBuffer;
	public int ttyWidth, ttyHeight, ttyCursorPos;
	
//	public byte regA, regB, regX, regD, regIR, bus, stage, mode;
//	public short regPC, regAR, stackPtr, adrBus, alo;
//	
//	public boolean flagCout, flagZero;
//	public byte[] rom;
//	public byte[] ram;
//	public short[] breakpoints;
//	public int[] isa = defaultISA;
	
	@Override
	public void setup() {
		font12 = loadFont("font12.vlw");
		font48 = loadFont("font48.vlw");
		
		emulator = new EmuThread();
		emulator.setHertz(1000000);
		emulator.doTick = false;
		emulator.reset();
		emulator.start();
		
		//region loading
		reset =					loadImage("reset.png");
		resetHover =			loadImage("reset_hover.png");
		resetPressed =			loadImage("reset_pressed.png");
		resetDisabled =			loadImage("reset_disabled.png");
		
		play =					loadImage("play.png");
		playHover =				loadImage("play_hover.png");
		playPressed =			loadImage("play_pressed.png");
		playDisabled =			loadImage("play_disabled.png");
		
		pause =					loadImage("pause.png");
		pauseHover =			loadImage("pause_hover.png");
		pausePressed =			loadImage("pause_pressed.png");
		pauseDisabled =			loadImage("pause_disabled.png");
		
		cycle =					loadImage("cycle.png");
		cycleHover =			loadImage("cycle_hover.png");
		cyclePressed =			loadImage("cycle_pressed.png");
		cycleDisabled =			loadImage("cycle_disabled.png");
		
		instruction =			loadImage("instruction.png");
		instructionHover =		loadImage("instruction_hover.png");
		instructionPressed =	loadImage("instruction_pressed.png");
		instructionDisabled =	loadImage("instruction_disabled.png");
		//endregion loading
		
		//region tty
		ttyWidth = 40;
		ttyHeight = 30;
		ttyBuffer = new byte[ttyWidth][ttyHeight];
		ttyCursorPos = 0;
		//endregion tty
		
		screen = new GUIScreen(this);
		
		//region control
		resetButton = new TextureButton(this, 2 * thingyWidth + 5, 5, 40, 40, false, new TextureButtonStyle(
				reset, resetHover, resetPressed, resetDisabled
		), ()->emulator.reset());
		screen.add(resetButton);
		
		pauseButton = new TextureButton(this, 2 * thingyWidth + 50, 5, 40, 40, false, new TextureButtonStyle(
				pause, pauseHover, pausePressed, pauseDisabled
		), ()->emulator.doTick=false);
		screen.add(pauseButton);
		
		playButton = new TextureButton(this, 2 * thingyWidth + 95, 5, 40, 40, false, new TextureButtonStyle(
				play, playHover, playPressed, playDisabled
		), ()->emulator.doTick=true);
		screen.add(playButton);
		
		cycleButton = new TextureButton(this, 2 * thingyWidth + 140, 5, 40, 40, false, new TextureButtonStyle(
				cycle, cycleHover, cyclePressed, cycleDisabled
		), ()->emulator.forceTick++);
		screen.add(cycleButton);
		
		instructionButton = new TextureButton(this, 2 * thingyWidth + 185, 5, 40, 40, false, new TextureButtonStyle(
				instruction, instructionHover, instructionPressed, instructionDisabled
		));
		screen.add(instructionButton);
		//endregion control
		
		//region speed
		screen.add(new Button(this, 2 * thingyWidth + 5, 50, 60, 20, "10 MHz", false, ()->emulator.setHertz(10000000)));
		screen.add(new Button(this, 2 * thingyWidth + 5, 75, 60, 20, "1 MHz", false, ()->emulator.setHertz(1000000)));
		screen.add(new Button(this, 2 * thingyWidth + 5, 100, 60, 20, "100 KHz", false, ()->emulator.setHertz(100000)));
		screen.add(new Button(this, 2 * thingyWidth + 5, 125, 60, 20, "10 KHz", false, ()->emulator.setHertz(10000)));
		screen.add(new Button(this, 2 * thingyWidth + 5, 150, 60, 20, "1 KHz", false, ()->emulator.setHertz(1000)));
		
		screen.add(new Button(this, 2 * thingyWidth + 70, 50, 60, 20, "100 Hz", false, ()->emulator.setHertz(100)));
		screen.add(new Button(this, 2 * thingyWidth + 70, 75, 60, 20, "10 Hz", false, ()->emulator.setHertz(10)));
		screen.add(new Button(this, 2 * thingyWidth + 70, 100, 60, 20, "5 Hz", false, ()->emulator.setHertz(5)));
		screen.add(new Button(this, 2 * thingyWidth + 70, 125, 60, 20, "2 Hz", false, ()->emulator.setHertz(2)));
		screen.add(new Button(this, 2 * thingyWidth + 70, 150, 60, 20, "1 Hz", false, ()->emulator.setHertz(1)));
		//endregion speed
		
		//region modules
		bus = new Register8Bit(this, 1 * thingyWidth, 0 * thingyHeight, "data bus");
		bus.valueSupplier = ()->(int)emulator.instance.bus;
		screen.add(bus);
		
		regA = new Register8Bit(this, 1 * thingyWidth, 1 * thingyHeight, "A register");
		regA.valueSupplier = ()->(int)emulator.instance.regA;
		regA.valueUpdater = (v)-> emulator.instance.regA=(byte)(int)v;
		screen.add(regA);
		
		regB = new Register8Bit(this, 1 * thingyWidth, 2 * thingyHeight, "B register");
		regB.valueSupplier = ()->(int)emulator.instance.regB;
		regB.valueUpdater = (v)-> emulator.instance.regB=(byte)(int)v;
		screen.add(regB);
		
		alu = new ArithmeticLogicUnit(this, 1 * thingyWidth, 3 * thingyHeight, "ALU");
		alu.valueSupplier = ()->(int)emulator.instance.alo;
		screen.add(alu);
		
		regX = new Register8Bit(this, 1 * thingyWidth, 5 * thingyHeight, "X register");
		regX.valueSupplier = ()->(int)emulator.instance.regX;
		regX.valueUpdater = (v)-> emulator.instance.regX=(byte)(int)v;
		screen.add(regX);
		
		regD = new Register8Bit(this, 1 * thingyWidth, 6 * thingyHeight, "D register");
		regD.valueSupplier = ()->(int)emulator.instance.regD;
		regD.valueUpdater = (v)-> emulator.instance.regD=(byte)(int)v;
		screen.add(regD);
		
		regIR = new Register8Bit(this, 1 * thingyWidth, 7 * thingyHeight, "instruction register");
		regIR.valueSupplier = ()->(int)emulator.instance.regIR;
		regIR.valueUpdater = (v)-> emulator.instance.regIR=(byte)(int)v;
		screen.add(regIR);
		
		adrBus = new Register16Bit(this, 0 * thingyWidth, 0 * thingyHeight, "address bus");
		adrBus.valueSupplier = ()->(int)emulator.instance.adrBus;
		screen.add(adrBus);
		
		memory = new Memory(this, 0 * thingyWidth, 1 * thingyHeight, "memory");
		memory.addressSource = emulator.instance::getAddress;
		memory.cpu = emulator.instance;
		screen.add(memory);
		
		regPC = new Register16Bit(this, 0 * thingyWidth, 5 * thingyHeight, "program counter");
		regPC.valueSupplier = ()->(int)emulator.instance.regPC;
		regPC.valueUpdater = (v)-> emulator.instance.regPC=(short)(int)v;
		screen.add(regPC);
		
		regAR = new Register16Bit(this, 0 * thingyWidth, 6 * thingyHeight, "address register");
		regAR.valueSupplier = ()->(int)emulator.instance.regAR;
		regAR.valueUpdater = (v)-> emulator.instance.regAR=(short)(int)v;
		screen.add(regAR);
		
		stackPtr = new Register16Bit(this, 0 * thingyWidth, 7 * thingyHeight, "stack pointer");
		stackPtr.valueSupplier = ()->(int)emulator.instance.stackPtr;
		stackPtr.valueUpdater = (v)-> emulator.instance.stackPtr=(short)(int)v;
		screen.add(stackPtr);
		
		controlUnit = new ControlUnit(this, 2 * thingyWidth, 5 * thingyHeight, emulator);
		screen.add(controlUnit);
		//endregion modules
		
		surface.setResizable(true);
	}
	
	@Override
	public void draw() {
		background(255);
		
		pauseButton.enabled = emulator.doTick;
		playButton.enabled = !emulator.doTick;
		cycleButton.enabled = !emulator.doTick;
		instructionButton.enabled = !emulator.doTick;
		
		textFont(font12, 12);
		textAlign(CORNER);
		text(getSpeeds(), 2f * thingyWidth + 20, 2.5f * thingyHeight);
		
		screen.render();
	}
	
	public String getSpeeds() {
		double hertz = emulator.currentHertz;
		double target = emulator.tickTimes * 1000000000d / emulator.nanoWait;
		if (target >= 1000000) {
			hertz = Math.round(hertz / 100000d) / 10d;
			return hertz + " MHz";
		} else if (target >= 1000) {
			hertz = Math.round(hertz / 100d) / 10d;
			return hertz + " KHz";
		} else {
			hertz = Math.round(hertz * 10d) / 10d;
			return hertz + " Hz";
		}
	}
	
	//region IO
	public void loadTheThingy(File selected) {
		if (selected != null) {
			String paff = selected.getAbsolutePath();
			if (paff.endsWith(".hex")) {
				emulator.doTick = false;
				byte[] rom = null;
				try {
					rom = decodeLHF(selected);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (rom != null) {
					emulator.instance.rom = rom;
				}
			}
		}
	}
	
	public byte[] decodeLHF(File file) throws IOException {
		FileInputStream in = new FileInputStream(file);
		BytePool out = new BytePool();
		String hexes = "0123456789ABCDEF";
		while (in.available() > 0) {
			int read = in.read();
			if (read == '\n') {
				break;
			}
		}
		int stuff = 0;
		while (in.available() > 0) {
			int read = in.read();
			if (read == ' ') {
				if ((stuff & 0xffffff00) != 0) {
					throw new IOException("Invalid LHF format.");
				}
				out.addBytes((byte) stuff);
				stuff = 0;
			}
			else
			{
				stuff <<= 4;
				int indexial = hexes.indexOf((char) read);
				if (indexial == -1) {
					out.addBytes((byte) stuff);
					break;
				}
				stuff |= indexial;
			}
		}
		in.close();
		return out.copyToArray();
	}
	//endregion IO
	
	//region UI
	@Override
	public void mousePressed() {
		screen.mousePressed();
	}
	
	@Override
	public void mouseReleased() {
		screen.mouseReleased();
	}
	
	public boolean ctrlPressed;
	
	@Override
	public void keyPressed() {
		screen.keyPressed();
		if (keyCode == CONTROL) {
			ctrlPressed = true;
		}
		if (ctrlPressed && key == 15) {
			selectInput("Open ROM or assembly file...", "loadTheThingy");
			emulator.doTick = false;
		}
	}
	
	@Override
	public void keyReleased() {
		screen.keyReleased();
		if (keyCode == CONTROL) {
			ctrlPressed = false;
		}
	}
	//endregion UI
	
	@Override
	public void exit() {
		if (key == ESC) {
			return; // Stupid feature.
		}
		emulator.cont = false;
		try {
			emulator.join(2500);
		} catch (InterruptedException e) {
			System.err.println("Interruped waiting for emulator.");
		}
		super.exit();
	}
	
	public static class EmuThread extends Thread {
		
		public static final int EXC_ERR = -1;
		public static final int EXC_NORM = 0;
		public static final int EXC_HALT = 1;
		public static final int EXC_BRK = 2;
		public static final int EXC_OVERFLOW = 3;
		public static final int EXC_NOINSN = 4;
		public static final int EXC_WAIT_STEP = 5;
		public static final int EXC_RESET = 6;
		
		public GR8CPURev3_1 instance;
		
		protected int tickTimes;
		protected long nanoWait;
		
		public volatile boolean cont;
		public volatile boolean doTick;
		public volatile int forceTick;
		public volatile double currentHertz;
		
		public EmuThread() {
			instance = new GR8CPURev3_1();
		}
		
		/**
		 * Sets the wait time based on hertz.
		 * Set to -1 to stop.
		 * Set to 0 for fast as possible.
		 *
		 * @param hertz the target frequency in hertz
		 */
		public void setHertz(double hertz) {
			int leFactor = 3;
			if (hertz == 0) {
				nanoWait = 0;
				tickTimes = 10000;
				return;
			} else if (hertz < 0) {
				nanoWait = 20000000;
				tickTimes = 0;
				doTick = false;
				return;
			} else if (hertz >= 10000000) {
				hertz /= 100000 * leFactor;
				tickTimes = 100000 * leFactor;
			} else if (hertz >= 1000000) {
				hertz /= 10000 * leFactor;
				tickTimes = 10000 * leFactor;
			} else if (hertz >= 100000) {
				hertz /= 1000 * leFactor;
				tickTimes = 1000 * leFactor;
			} else if (hertz >= 10000) {
				hertz /= 100 * leFactor;
				tickTimes = 100 * leFactor;
			} else if (hertz >= 1000) {
				hertz /= 10 * leFactor;
				tickTimes = 10 * leFactor;
			} else {
				tickTimes = 1;
			}
			nanoWait = (long) (1000000000d / hertz);
		}
		
		/**
		 * Sets the wait time.
		 * Set to -1 to stop.
		 * Set to 0 for fast as possible.
		 *
		 * @param nanoWait the target wait time
		 */
		public void setNanoWait(long nanoWait) {
			this.nanoWait = nanoWait;
			tickTimes = 1;
		}
		
		@Override
		public void run() {
			cont = true;
			doTick = false;
			long nano0;
			long nano1 = System.nanoTime();
			long nano2;
			int times;
			setPriority(MAX_PRIORITY);
			while (cont) {
				if (forceTick > 0) {
					times = forceTick;
					forceTick = 0;
				} else if (!doTick) {
					times = 0;
				} else {
					times = tickTimes;
				}
				nano2 = nano1;
				nano0 = System.nanoTime();
				int res = instance.tick(times);
				nano1 = (System.nanoTime() + 5000) / 10000 * 10000;
				currentHertz = 1000000000d / (nano1 - nano2) * tickTimes;
				long wait = Math.max(0, nanoWait - nano1 + nano0);
				if (wait < 1) {
					wait = 1;
				}
				long waitMs = wait / 1000000;
				if (!doTick) {
					waitMs = 20;
					wait = 0;
				}
				try {
					synchronized (this) {
						wait(waitMs, (int) (wait % 1000000));
					}
				} catch (InterruptedException e) {
					cont = false;
				}
				if (res != 0 && res != EXC_HALT && res != EXC_BRK) {
					doTick = false;
					System.err.println("Emulator stopped with code " + res);
				}
				synchronized (this) {
					notifyAll();
				}
			}
		}
		
		public void reset() {
			if (doTick) {
				doTick = false;
				synchronized (this) {
					try {
						wait(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			instance.reset();
		}
	}
	
}
