package net.scheffers.robot.emu;

import jutils.database.BytePool;
import jutils.gui.style.TextureButtonStyle;
import jutils.guiv2.*;
import net.scheffers.robot.emu.modules.*;
import net.scheffers.robot.hyperasm.AssemblerCore;
import net.scheffers.robot.hyperasm.Pass2Out;
import net.scheffers.robot.hyperasm.isa.InstructionSet;
import org.json.JSONObject;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Random;

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
	
	public GR8EMUr3_1() {
		emulator = new EmuThread();
	}
	
	@Override
	public void settings() {
		size(921, 601);
	}
	
	public Debugger debugger;
	public final EmuThread emulator;
	public static PFont font12;
	public static PFont font48;
	public static GR8EMUr3_1 inst;
	
	//region GUI
	public GUIScreen screen;
	public Register8Bit regA, regB, regX, regY, regIR, bus;
	public ArithmeticLogicUnit alu;
	public RegisterFlags regF;
	public Register16Bit regPC, regAR, stackPtr, adrBus;
	public Register16Bit regIRQ, regNMI;
	//endregion GUI
	
	//region speed
	public int speedMultiplier = 1;
	public int speed = 1;
	
	public Button speedMulMHz, speedMulKHz, speedMulHz;
	public Button speed100, speed10, speed5, speed1;
	public Button speedMulDisabled, speedDisabled;
	//endregion speed
	
	//region settings
	public GUIScreen settingScreen;
	
	/**
	 * Select file mapped to virtual drive.
	 */
	public Button selDriveFile;
	/**
	 * File mapped to virtual drive.
	 */
	public String driveFile;
	/**
	 * File mapped to virtual drive.
	 */
	public Text dispDriveFile;
	/**
	 * File mapped to virtual drive.
	 */
	public Button ejectDrive;
	/** Whether the image is read-only. */
	public CheckBox readOnlyImage;
	
	/**
	 * Memory mapped IO address inputs.
	 */
	public TextInput mmioTTY, mmioKeyboard, mmioDriveAddr, mmioDrivePort;
	/**
	 * Memory mapped IO addresses.
	 */
	public int mmioAdrTTY, mmioAdrKeyboard, mmioAdrDriveAddr, mmioAdrDrivePort;
	/**
	 * Memory mapped IO default addresses.
	 */
	public static final int mmioDefTTY = 0xfefd, mmioDefKeyboard = 0xfefc, mmioDefDriveAddr = 0xfed0, mmioDefDrivePort = 0xfed4;
	//endregion settings
	
	/**
	 * Memory available to the CPU.
	 */
	public Memory memory, rom;
	public ControlUnit controlUnit;
	
	//region images
	/**
	 * Images for RESET button.
	 */
	public PImage reset, resetHover, resetPressed, resetDisabled;
	/**
	 * Images for play button.
	 */
	public PImage play, playHover, playPressed, playDisabled;
	/**
	 * Images for pause button.
	 */
	public PImage pause, pauseHover, pausePressed, pauseDisabled;
	/**
	 * Images for cycle button.
	 */
	public PImage cycle, cycleHover, cyclePressed, cycleDisabled;
	/**
	 * Images for step over button.
	 */
	public PImage stepOver, stepOverHover, stepOverPressed, stepOverDisabled;
	/**
	 * Images for step in button.
	 */
	public PImage stepIn, stepInHover, stepInPressed, stepInDisabled;
	/**
	 * Images for step out button.
	 */
	public PImage stepOut, stepOutHover, stepOutPressed, stepOutDisabled;
	/**
	 * Images for step out button.
	 */
	public PImage debug, debugHover, debugPressed, debugDisabled;
	/**
	 * Icon images.
	 */
	public PImage[] iconImages;
	public PImage iconImage;
	//endregion images
	
	/**
	 * CPU control buttons.
	 */
	public TextureButton resetButton, playButton, pauseButton, cycleButton, debugButton;
	
	/**
	 * Keyboard module.
	 */
	public Keyboardonator keyboard;
	
	/**
	 * Character buffer for the TTY.
	 */
	public char[][] ttyBuffer;
	/**
	 * Input buffer not yet consumed by the CPU.
	 */
	public byte[] ttyInputBuffer;
	/**
	 * TTY variables.
	 */
	public int ttyWidth, ttyHeight, ttyCursorPos, ttyInputLen, ttyInputPos;
	
	/**
	 * Whether or not the settings thingy is open.
	 */
	public boolean settings;
	
	//region resources
	public static PImage loadJarImage(String resource) {
		try {
			InputStream stream = GR8EMUr3_1.class.getClassLoader().getResourceAsStream("emu_resources/" + resource);
			if (stream == null) {
				throw new FileNotFoundException(resource);
			}
			BufferedImage image = ImageIO.read(stream);
			stream.close();
			return new PImage(image);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static PFont loadJarFont(String resource) {
		try {
			InputStream stream = GR8EMUr3_1.class.getClassLoader().getResourceAsStream("emu_resources/" + resource);
			if (stream == null) {
				throw new FileNotFoundException(resource);
			}
			return new PFont(stream);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte[] loadJarBytes(String resource) {
		try {
			InputStream stream = GR8EMUr3_1.class.getClassLoader().getResourceAsStream("emu_resources/" + resource);
			if (stream == null) {
				throw new FileNotFoundException(resource);
			}
			byte[] heck = new byte[stream.available()];
			int n = stream.read(heck);
			return heck;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	//endregion resources
	
	@Override
	@SuppressWarnings("all")
	public void setup() {
		inst = this;
		
		emulator.setHertz(1000000);
		emulator.doTick = false;
		emulator.reset();
		
		//region loading
		font12 = loadJarFont("font12.vlw");
		font48 = loadJarFont("font48.vlw");
		
		JSONObject isaRaw = new JSONObject(new String(loadJarBytes("isa.json")));
		verboseInstructionSet = new InstructionSet(isaRaw, true);
		standardInstructionSet = new InstructionSet(isaRaw);
		
		reset = loadJarImage("reset.png");
		resetHover = loadJarImage("reset_hover.png");
		resetPressed = loadJarImage("reset_pressed.png");
		resetDisabled = loadJarImage("reset_disabled.png");
		
		play = loadJarImage("play.png");
		playHover = loadJarImage("play_hover.png");
		playPressed = loadJarImage("play_pressed.png");
		playDisabled = loadJarImage("play_disabled.png");
		
		pause = loadJarImage("pause.png");
		pauseHover = loadJarImage("pause_hover.png");
		pausePressed = loadJarImage("pause_pressed.png");
		pauseDisabled = loadJarImage("pause_disabled.png");
		
		cycle = loadJarImage("cycle.png");
		cycleHover = loadJarImage("cycle_hover.png");
		cyclePressed = loadJarImage("cycle_pressed.png");
		cycleDisabled = loadJarImage("cycle_disabled.png");
		
		stepOver = loadJarImage("over.png");
		stepOverHover = loadJarImage("over_hover.png");
		stepOverPressed = loadJarImage("over_pressed.png");
		stepOverDisabled = loadJarImage("over_disabled.png");
		
		stepIn = loadJarImage("in.png");
		stepInHover = loadJarImage("in_hover.png");
		stepInPressed = loadJarImage("in_pressed.png");
		stepInDisabled = loadJarImage("in_disabled.png");
		
		stepOut = loadJarImage("out.png");
		stepOutHover = loadJarImage("out_hover.png");
		stepOutPressed = loadJarImage("out_pressed.png");
		stepOutDisabled = loadJarImage("out_disabled.png");
		
		debug = loadJarImage("debug.png");
		debugHover = loadJarImage("debug_hover.png");
		debugPressed = loadJarImage("debug_pressed.png");
		debugDisabled = loadJarImage("debug_disabled.png");
		
		iconImages = new PImage[] {
			loadJarImage("icon_small_purple.png"),
			loadJarImage("icon_small_blue.png"),
			loadJarImage("icon_small_red.png")
		};
		
		//endregion loading
		
		//region tty
		ttyWidth = 40;
		ttyHeight = 30;
		ttyBuffer = new char[ttyHeight][ttyWidth];
		ttyInputBuffer = new byte[ttyWidth];
		ttyCursorPos = 0;
		ttyInputPos = 0;
		ttyInputLen = 0;
		//endregion tty
		
		screen = new GUIScreen(this);
		
		//region control
		resetButton = new TextureButton(this, 2 * thingyWidth + 5, 5, 40, 40, false, new TextureButtonStyle(
				reset, resetHover, resetPressed, resetDisabled
		), () -> {
			emulator.reset();
			for (int y = 0; y < ttyHeight; y++) {
				for (int x = 0; x < ttyWidth; x++) {
					ttyBuffer[y][x] = 0;
				}
			}
			ttyInputBuffer = new byte[ttyInputBuffer.length];
			ttyInputLen = 0;
			ttyCursorPos = 0;
		});
		screen.add(resetButton);
		
		pauseButton = new TextureButton(this, 2 * thingyWidth + 45, 5, 40, 40, false, new TextureButtonStyle(
				pause, pauseHover, pausePressed, pauseDisabled
		), () -> emulator.doTick = false);
		screen.add(pauseButton);
		
		playButton = new TextureButton(this, 2 * thingyWidth + 85, 5, 40, 40, false, new TextureButtonStyle(
				play, playHover, playPressed, playDisabled
		), () -> emulator.doTick = true);
		screen.add(playButton);
		
		cycleButton = new TextureButton(this, 2 * thingyWidth + 125, 5, 40, 40, false, new TextureButtonStyle(
				cycle, cycleHover, cyclePressed, cycleDisabled
		), () -> emulator.forceTick++);
		screen.add(cycleButton);
		
		debugButton = new TextureButton(this, 2 * thingyWidth + 165, 5, 40, 40, false, new TextureButtonStyle(
				debug, debugHover, debugPressed, debugDisabled
		), () -> debugger.show());
		screen.add(debugButton);
		
		keyboard = new Keyboardonator(this);
		screen.add(keyboard);
		//endregion control
		
		//region speed
		screen.add(new Button(this, 2 * thingyWidth + 10, 50, 60, 20, "10 MHz", false, () -> emulator.setHertz(10000000)));
		screen.add(new Button(this, 2 * thingyWidth + 10, 80, 60, 20, "1 MHz", false, () -> emulator.setHertz(1000000)));
		screen.add(new Button(this, 2 * thingyWidth + 10, 110, 60, 20, "100 KHz", false, () -> emulator.setHertz(100000)));
		screen.add(new Button(this, 2 * thingyWidth + 10, 140, 60, 20, "10 KHz", false, () -> emulator.setHertz(10000)));
		screen.add(new Button(this, 2 * thingyWidth + 10, 170, 60, 20, "1 KHz", false, () -> emulator.setHertz(1000)));
		
		screen.add(new Button(this, 2 * thingyWidth + 80, 50, 60, 20, "100 Hz", false, () -> emulator.setHertz(100)));
		screen.add(new Button(this, 2 * thingyWidth + 80, 80, 60, 20, "10 Hz", false, () -> emulator.setHertz(10)));
		screen.add(new Button(this, 2 * thingyWidth + 80, 110, 60, 20, "5 Hz", false, () -> emulator.setHertz(5)));
		screen.add(new Button(this, 2 * thingyWidth + 80, 140, 60, 20, "2 Hz", false, () -> emulator.setHertz(2)));
		screen.add(new Button(this, 2 * thingyWidth + 80, 170, 60, 20, "1 Hz", false, () -> emulator.setHertz(1)));
		
		speedMulDisabled = speedMulMHz;
		speedDisabled = speed1;
		//endregion speed
		
		//region modules
		bus = new Register8Bit(this, 1 * thingyWidth, 0 * thingyHeight, "data bus");
		bus.valueSupplier = () -> (int) emulator.instance.bus;
		screen.add(bus);
		
		regA = new Register8Bit(this, 1 * thingyWidth, 1 * thingyHeight, "A register");
		regA.valueSupplier = () -> (int) emulator.instance.regA;
		regA.valueUpdater = (v) -> emulator.instance.regA = (byte) (int) v;
		screen.add(regA);
		
		regB = new Register8Bit(this, 1 * thingyWidth, 2 * thingyHeight, "B register");
		regB.valueSupplier = () -> (int) emulator.instance.regB;
		regB.valueUpdater = (v) -> emulator.instance.regB = (byte) (int) v;
		screen.add(regB);
		
		alu = new ArithmeticLogicUnit(this, 1 * thingyWidth, 3 * thingyHeight, "ALU");
		alu.valueSupplier = () -> (int) emulator.instance.alo;
		screen.add(alu);
		
		regF = new RegisterFlags(this, 1 * thingyWidth, 4 * thingyHeight, "flag register");
		screen.add(regF);
		
		regX = new Register8Bit(this, 1 * thingyWidth, 5 * thingyHeight, "X register");
		regX.valueSupplier = () -> (int) emulator.instance.regX;
		regX.valueUpdater = (v) -> emulator.instance.regX = (byte) (int) v;
		screen.add(regX);
		
		regY = new Register8Bit(this, 1 * thingyWidth, 6 * thingyHeight, "Y register");
		regY.valueSupplier = () -> (int) emulator.instance.regY;
		regY.valueUpdater = (v) -> emulator.instance.regY = (byte) (int) v;
		screen.add(regY);
		
		regIR = new Register8Bit(this, 1 * thingyWidth, 7 * thingyHeight, "instruction register");
		regIR.valueSupplier = () -> (int) emulator.instance.regIR;
		regIR.valueUpdater = (v) -> emulator.instance.regIR = (byte) (int) v;
		screen.add(regIR);
		
		adrBus = new Register16Bit(this, 0 * thingyWidth, 0 * thingyHeight, "address bus");
		adrBus.valueSupplier = () -> (int) emulator.instance.adrBus;
		regIR.valueUpdater = (v) -> emulator.instance.adrBus = (short) (int) v;
		screen.add(adrBus);
		
		memory = new Memory(this, 0 * thingyWidth, 1 * thingyHeight, "memory");
		memory.addressSource = emulator.instance::getAddress;
		memory.cpu = emulator.instance;
		screen.add(memory);
		
		regPC = new Register16Bit(this, 0 * thingyWidth, 5 * thingyHeight, "program counter");
		regPC.valueSupplier = () -> (int) emulator.instance.regPC;
		regPC.valueUpdater = (v) -> emulator.instance.regPC = (short) (int) v;
		screen.add(regPC);
		
		regAR = new Register16Bit(this, 0 * thingyWidth, 6 * thingyHeight, "address register");
		regAR.valueSupplier = () -> (int) emulator.instance.regAR;
		regAR.valueUpdater = (v) -> emulator.instance.regAR = (short) (int) v;
		screen.add(regAR);
		
		stackPtr = new Register16Bit(this, 0 * thingyWidth, 7 * thingyHeight, "stack pointer");
		stackPtr.valueSupplier = () -> (int) emulator.instance.stackPtr;
		stackPtr.valueUpdater = (v) -> emulator.instance.stackPtr = (short) (int) v;
		screen.add(stackPtr);
		
		regIRQ = new Register16Bit(this, 2 * thingyWidth, 3 * thingyHeight, "IRQ register");
		regIRQ.valueSupplier = () -> (int) emulator.instance.regIRQ;
		regIRQ.valueUpdater = (v) -> emulator.instance.regIRQ = (short) (int) v;
		screen.add(regIRQ);
		
		regNMI = new Register16Bit(this, 2 * thingyWidth, 4 * thingyHeight, "NMI register");
		regNMI.valueSupplier = () -> (int) emulator.instance.regNMI;
		regNMI.valueUpdater = (v) -> emulator.instance.regNMI = (short) (int) v;
		screen.add(regNMI);
		
		controlUnit = new ControlUnit(this, 2 * thingyWidth, 5 * thingyHeight, emulator);
		screen.add(controlUnit);
		//endregion modules
		
		//region settings
		selDriveFile = new Button(this, 2 * thingyWidth + 150, 50, 100, 20, "select disk", false,
				() -> selectInput("Seleft drive file...", "selectDrive")
		);
		screen.add(selDriveFile);
		
		dispDriveFile = new Text(this, 2 * thingyWidth + 260, 67, "none selected");
		screen.add(dispDriveFile);
		
		ejectDrive = new Button(this, 2 * thingyWidth + 150, 80, 100, 20, "eject disk", false, this::ejectDrive);
		screen.add(ejectDrive);
		
		readOnlyImage = new CheckBox(this, 2 * thingyWidth + 260, 80, 20, 20, ()->{
			emulator.instance.doWriteVolume = !readOnlyImage.value;
		});
		readOnlyImage.value = true;
		screen.add(readOnlyImage);
		screen.add(new Text(this, 2 * thingyWidth + 285, 95, "read-only"));
		//endregion settings
		
		if (GR8CPURev3_1.nativeLoadSuccess) {
			emulator.start();
		}
		debugger = new Debugger();
		
		//region icon selection
		Random random = new Random();
		for (int i = 0; i < iconImages.length; i++) {
			int a = random.nextInt(iconImages.length);
			int b = random.nextInt(iconImages.length - 1);
			if (b == a) b ++;
			PImage temp = iconImages[a];
			iconImages[a] = iconImages[b];
			iconImages[b] = temp;
		}
		iconImage = iconImages[0];
		debugger.iconImage = iconImages[1];
		surface.setIcon(iconImage);
		//endregion icon selection
		
		runSketch(new String[]{"Debugger"}, debugger);
		surface.setResizable(true);
	}
	
	@Override
	public void draw() {
		background(255);
		
		if (!GR8CPURev3_1.nativeLoadSuccess) {
			surface.setSize(670, 130);
			fill(0);
			textFont(font48, 48);
			text("library load failed", 10, 48);
			textFont(font12, 12);
			text("The emulator library could not be loaded,\nand the emulator cannot function as a result.", 10, 68);
			Throwable errorCause = GR8CPURev3_1.errorCause;
			if (errorCause != null) {
				text("Caused by: " + errorCause.getMessage(), 10, 110);
				if (errorCause.getCause() != errorCause && errorCause.getCause() != null) {
					text("Caused by: " + errorCause.getCause().getMessage(), 10, 124);
				}
			} else {
				text("Unknown cause.", 10, 110);
			}
			return;
		}
		
		if (settings) {
			settingScreen.render();
		} else {
			// Button enable conditions.
			pauseButton.enabled = emulator.doTick;
			playButton.enabled = !emulator.doTick;
			cycleButton.enabled = !emulator.doTick;
			
			// Draw the measured frequency.
			textFont(font12, 12);
			textAlign(CORNER);
			fill(0);
			strokeWeight(1);
			stroke(0xff7f7f7f);
			text(getSpeeds(), 2 * thingyWidth + 15, 207);
			
			// Draw the TTY.
			rect(-ttyWidth * 7 + width - 22, -ttyHeight * 13 + height - 42, ttyWidth * 7 + 4, ttyHeight * 13 + 4);
			fill(0xff00ff00);
			for (int y = 0; y < ttyHeight; y++) {
				for (int x = 0; x < ttyWidth; x++) {
					if (ttyBuffer[y][x] != 0) {
						text(ttyBuffer[y][x], (x - ttyWidth) * 7 + width - 20, (y - ttyHeight) * 13 + height - 29);
					}
				}
			}
			
			// Draw the GUI elements.
			screen.render();
		}
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
	
	public void ttyWrite(byte val) {
		if (val == (byte) '\n') {
			ttyNewline();
			return;
		} else if (val == (byte) '\r') {
			ttyCursorPos = 0;
			return;
		} else if (val == (byte) '\b') if (ttyCursorPos > 0) {
			ttyCursorPos--;
			ttyBuffer[ttyHeight - 1][ttyCursorPos] = (char) 0;
			return;
		}
		if (ttyCursorPos == ttyWidth) {
			ttyNewline();
		}
		ttyBuffer[ttyHeight - 1][ttyCursorPos] = (char) val;
		ttyCursorPos++;
	}
	
	public void ttyNewline() {
		for (int i = 0; i < ttyHeight - 1; i++) {
			ttyBuffer[i] = ttyBuffer[i + 1];
		}
		ttyBuffer[ttyHeight - 1] = new char[ttyWidth];
		ttyCursorPos = 0;
	}
	
	public void ttyType(byte typed) {
		if (ttyInputLen < ttyInputBuffer.length) {
			ttyInputBuffer[(ttyInputPos + ttyInputLen) % ttyInputBuffer.length] = typed;
			ttyInputLen++;
		}
	}
	
	public byte ttyRead(boolean notouchy) {
		if (ttyInputLen > 0) {
			byte ret = ttyInputBuffer[ttyInputPos];
			if (!notouchy) {
				ttyInputPos++;
				ttyInputLen--;
				ttyInputPos %= ttyInputBuffer.length;
			}
			return ret;
		} else {
			return 0;
		}
	}
	
	//region IO
	public File selectedASM;
	public InstructionSet verboseInstructionSet;
	public InstructionSet standardInstructionSet;
	public Pass2Out assemblyData;
	
	public void loadTheThingy(File selected) {
		if (selected != null) {
			String paff = selected.getAbsolutePath();
			if (paff.endsWith(".hex")) {
				assemblyData = null;
				selectedASM = null;
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
			} else if (paff.endsWith(".asm") || paff.endsWith(".S")) {
				selectedASM = selected;
				try {
					loadASMFile();
				} catch (IOException e) {
					e.printStackTrace();
					selectedASM = null;
				}
			}
		}
	}
	
	public void loadASMFile() throws IOException {
		assemblyData = null;
		Pass2Out verbose = AssemblerCore.simpleFullAssemble(selectedASM.getAbsolutePath(), verboseInstructionSet);
		if (verbose.errors.size() == 0) {
			assemblyData = verbose;
		} else {
			Pass2Out nonverbose = AssemblerCore.simpleFullAssemble(selectedASM.getAbsolutePath(), standardInstructionSet);
			if (nonverbose.errors.size() == 0) {
				assemblyData = nonverbose;
			}
		}
		if (assemblyData == null) {
			emulator.instance.rom = new byte[0];
		} else {
			emulator.instance.rom = assemblyData.getBytes();
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
			} else {
				int indexial = hexes.indexOf((char) read);
				if (indexial == -1) {
					out.addBytes((byte) stuff);
					break;
				}
				stuff <<= 4;
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
		if (settings) {
			settingScreen.mousePressed();
		} else {
			screen.mousePressed();
		}
	}
	
	@Override
	public void mouseReleased() {
		if (settings) {
			settingScreen.mouseReleased();
		} else {
			screen.mouseReleased();
		}
	}
	
	public boolean ctrlPressed;
	
	@Override
	public void keyPressed() {
		if (settings) {
			settingScreen.keyPressed();
		} else {
			screen.keyPressed();
			if (ctrlPressed && key == 15) {
				selectInput("Open ROM or assembly file...", "loadTheThingy");
				emulator.doTick = false;
			}
		}
		if (keyCode == CONTROL) {
			ctrlPressed = true;
		}
	}
	
	@Override
	public void keyReleased() {
		if (settings) {
			settingScreen.keyReleased();
		} else {
			screen.keyReleased();
		}
		if (keyCode == CONTROL) {
			ctrlPressed = false;
		}
	}
	//endregion UI
	
	public void ejectDrive() {
		emulator.instance.setVolume(null, true);
		dispDriveFile.text = "none selected";
	}
	
	public void selectDrive(File selected) {
		if (selected != null) {
			driveFile = selected.getAbsolutePath();
			emulator.instance.setVolume(selected, true);
			mousePressed = false;
			dispDriveFile.text = selected.getName();
		}
	}
	
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
		public static final int EXC_TCON = 7;
		
		public static final String[] exitCodes = {
				"Normal",
				"CPU halted",
				"Breakpoint hit",
				"Stack overflow",
				"No instruction"
		};
		
		public GR8CPURev3_1 instance;
		
		protected int tickTimes;
		protected long nanoWait;
		
		public volatile boolean cont;
		public volatile boolean doTick;
		public volatile int forceTick;
		public volatile double currentHertz;
		public volatile int tickMode;
		
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
				int res;
				if (tickMode != 0) {
					times = tickTimes;
					res = instance.tick(times, tickMode);
					if (res == 7) {
						res = 0;
					} else {
						tickMode = 0;
					}
				} else {
					res = instance.tick(times, 0);
				}
				nano1 = (System.nanoTime() + 5000) / 10000 * 10000;
				currentHertz = 1000000000d / (nano1 - nano2) * tickTimes;
				long wait = Math.max(0, nanoWait - nano1 + nano0);
				if (wait < 1) {
					wait = 1;
				}
				long waitMs = wait / 1000000;
				if (!doTick && tickMode == 0) {
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
				if (res != 0) {
					doTick = false;
					if (res < 0 || res >= exitCodes.length) {
						System.err.println("Emulator stopped with code " + res);
					} else {
						System.err.println("Emulator stopped: " + exitCodes[res]);
					}
				}
				synchronized (this) {
					notifyAll();
				}
			}
		}
		
		public void stepOver() {
			tickMode = GR8CPURev3_1.TICK_STEP_OVER;
		}
		
		public void stepIn() {
			tickMode = GR8CPURev3_1.TICK_STEP_IN;
		}
		
		public void stepOut() {
			tickMode = GR8CPURev3_1.TICK_STEP_OUT;
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
