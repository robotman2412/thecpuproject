package emur3;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jutils.IOUtils;
import jutils.JUtils;
import jutils.guiv2.GUI;
import processing.core.PApplet;

public class GR8EMU extends PApplet {
	
	public static boolean cont = true;
	public static final long delayNanos = 12500;
	
	public static int[] screenBuf;
	
	public static volatile RunMode CPUMode;
	public static volatile RunMode GPUMode;
	
	public static void main(String[] args) {
		JUtils.getArgs(args);
		System.out.println("Running GPU @ 20KHz and CPU @ 4MHz");
		//long nextNanos = System.nanoTime();
		boolean doGPU = true;
		try {
			initCPU();
			initGPU();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		if (!JUtils.getArg("NOGUI").equals("true")) {
			PApplet.main("emur3.GR8EMU");
			System.out.println("Started GR8EMUr3 GUI");
		}
		RAMLastRead = -1;
		RAMLastWritten = -1;
		CPUMode = RunMode.STOP;
		while (cont) {
			//while (System.nanoTime() < nextNanos);
			//nextNanos += delayNanos;
			if (!CPUMode.equals(RunMode.STOP)) {
				cycleCPU();
				if (CPUMode.equals(RunMode.ONCE)) {
					CPUMode = RunMode.STOP;
				}
			}
			if (doGPU) {
				cycleGPU();
			}
			if (resetSim) {
				resetSim = false;
				try {
					initCPU();
					initGPU();
					CPUMode = RunMode.STOP;
					RAMLastRead = -1;
					RAMLastWritten = -1;
					System.out.println("Simulation reset.");
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
	}
	
	public static Map<String, Boolean> ctrls;
	
	//Debug
	public static int RAMLastRead;
	public static int RAMLastWritten;
	
	//Registers
	public static byte A;
	public static byte B;
	public static byte C;
	public static byte D;
	public static byte X;
	public static byte Y;
	public static byte IR;
	public static int ISOP;
	public static int ISTAGE;
	public static int ISTAGEPRE;
	
	public static byte flags;
	public static byte errPointer;
	public static byte intrPointer;
	public static int stackPointer;
	public static int adrReg;
	public static int PC;
	
	//Memory
	public static byte[] RAM;
	public static byte[] ROM;
	public static byte[] VRAM;
	public static byte VRAMPage;
	
	public static byte readMem(int address) {
		RAMLastRead = address;
		if (address >= 0xff00) {
			return VRAM[address - 0xff00 + VRAMPage << 8];
		}
		if (address == 0xfeff) {
			return VRAMPage;
		}
		//check additional stuffs
		return RAM[address];
	}
	
	public static void writeMem(int address, byte data) {
		RAMLastWritten = address;
		if (address >= 0xff00) {
			int VRAdr = (address & 0xff) + (Byte.toUnsignedInt(VRAMPage) << 8);
			VRAM[VRAdr] = data;
		}
		else if (address == 0xfeff) {
			VRAMPage = data;
		} //check additional stuffs
		else
		{
			RAM[address] = data;
		}
	}
	
	public static Map<Byte, String[]> IS;
	public static String[] ctrlList;
	
	public static void initCPU() throws IOException {
		ctrls = new HashMap<String, Boolean>();
		A = 0;
		B = 0;
		C = 0;
		D = 0;
		X = 0;
		Y = 0;
		IR = 0;
		ISOP = 0;
		ISTAGE = 0;
		ISTAGEPRE = 0;
		PC = 0;
		stackPointer = 0;
		errPointer = 0;
		intrPointer = 0;
		adrReg = 0;
		RAM = new byte[65536];
		VRAM = new byte[65536];
		ctrls.put("IRI", false);
		ctrls.put("ITR", false);
		String ASMGPath = "D:/logisim projects/GR8CPU Rev3/IS/ASMG.txt";
		String CTRLSPath = "D:/logisim projects/GR8CPU Rev3/IS/CTRL.txt";
		ctrlList = IOUtils.readStrings(CTRLSPath);
		for (int i = 0; i < ctrlList.length; i++) {
			ctrls.put(ctrlList[i] = ctrlList[i].split(" ")[0].toUpperCase(), false);
		}
		String[] asmgRaw = IOUtils.readStrings(ASMGPath);
		IS = new HashMap<Byte, String[]>();
		for (String s : asmgRaw) {
			if (s.charAt(0) != '@') {
				s = s.replaceAll("[\t ]+", " ");
				String[] split = s.split(" ");
				byte inst = (byte)Integer.parseInt(split[0], 16);
				split = split[split.length - 1].split(";");
				IS.put(inst, split);
			}
		}
		try {
			ROM = IOUtils.readBytes("D:/logisim projects/GR8CPU Rev3/OS/ROM.dat");
		} catch(IOException e) {
			e.printStackTrace();
			System.out.println("Error loading ROM!");
			ROM = new byte[0];
		}
		for (int i = 0; i < ROM.length; i++) {
			RAM[i] = ROM[i];
		}
		CPUMode = RunMode.CONTINUE;
	}
	
	public static void rstCTRLS() {
		for (Entry<String, Boolean> ctrl : ctrls.entrySet()) {
			ctrl.setValue(false);
		}
	}

	public static void outBus() {
		if (ctrls.get("ROA")) {
			bus |= A;
		}
		if (ctrls.get("ROB")) {
			bus |= B;
		}
		if (ctrls.get("ROC")) {
			bus |= C;
		}
		if (ctrls.get("ROD")) {
			bus |= D;
		}
		if (ctrls.get("ROX")) {
			bus |= X;
		}
		if (ctrls.get("ROY")) {
			bus |= Y;
		}
		if (ctrls.get("STPO")) {
			adrBus |= stackPointer;
		}
		else if (ctrls.get("ARO")) {
			adrBus |= adrReg;
		}
		else
		{
			adrBus |= PC;
		}
		if (ctrls.get("STPOB")) {
			if (ctrls.get("INHIGH")) {
				bus |= (byte)(stackPointer >> 8 & 0xff);
			}
			else
			{
				bus |= stackPointer & 0xff;
			}
		}
		if (ctrls.get("COB")) {
			if (ctrls.get("INHIGH")) {
				bus |= (byte)(PC >> 8 & 0xff);
			}
			else
			{
				bus |= PC & 0xff;
			}
		}
		else if (ctrls.get("PTRO")) {
			if (ctrls.get("PTRI")) {
				if (ctrls.get("PTRS")) {
					adrBus |= errPointer;
				}
				else
				{
					adrBus |= intrPointer;
				}
			}
			else
			{
				if (ctrls.get("PTRS")) {
					bus |= errPointer;
				}
				else
				{
					bus |= intrPointer;
				}
			}
		}
		if (ctrls.get("ILD")) {
			int ofs = 0;
			if (ctrls.get("OFSX")) {
				ofs = X;
			}
			if (ctrls.get("OFSY")) {
				ofs = Y;
			}
			if (ctrls.get("ADRSUB")) {
				ofs = -ofs;
			}
			bus |= readMem((adrBus + ofs) & 0xffff);
		}
		boolean carry = false;
		boolean zero = false;
		byte result = 0;
		byte mA = (byte)(Byte.toUnsignedInt(A) ^ (ctrls.get("AIA") ? 0xff : 0));
		byte mB = (byte)(Byte.toUnsignedInt(B) ^ (ctrls.get("AIB") ? 0xff : 0));
		if (ctrls.get("ADC")) {
			if (ctrls.get("ADC")) {
				result = (byte)(mA | mB);
			}
			else
			{
				result = (byte)(mA ^ mB);
			}
		}
		else
		{
			int mRes = Byte.toUnsignedInt(mA) + Byte.toUnsignedInt(mB) + (ctrls.get("ACR") ? 1 : 0);
			if (mRes > 255) {
				carry = true;
			}
			result = (byte)(mRes & 0xff);
		}
		zero = result == 0;
		if (ctrls.get("ALO")) {
			bus |= result;
		}
		if (ctrls.get("FRI")) {
			flags = 0;
			if (carry) {
				flags |= 1;
			}
			if (zero) {
				flags |= 2;
			}
		}
		else if (ctrls.get("ADA") && ctrls.get("ADC")) {
			bus |= flags;
		}
	}
	
	public static void inBus() {
		if (ctrls.get("IRI")) {
			IR = bus;
			//System.out.println(String.format("Loaded inst: %02x", IR));
			if (IR == 0 && stopOnNop) {
				CPUMode = RunMode.STOP;
			}
		}
		if (ctrls.get("RIA")) {
			A = bus;
		}
		if (ctrls.get("RIB")) {
			B = bus;
		}
		if (ctrls.get("RIC")) {
			C = bus;
			System.out.println(String.format("C: %02x", C));
		}
		if (ctrls.get("RID")) {
			D = bus;
			System.out.print((char)D);
		}
		if (ctrls.get("RIX")) {
			if (ctrls.get("CTX")) {
				X --;
			}
			else {
				X = bus;
			}
		}
		else if (ctrls.get("CTX")) {
			X ++;
		}
		if (ctrls.get("RIY")) {
			if (ctrls.get("CTY")) {
				Y --;
			}
			else {
				Y = bus;
			}
		}
		else if (ctrls.get("CTY")) {
			Y ++;
		}
		if (ctrls.get("CIN")) {
			PC = (PC + 1) & 0xffff;
		}
		if (ctrls.get("STPI")) {
			if (ctrls.get("STPINC")) {
				stackPointer = (stackPointer - 1) & 0xffff;
			}
			else if (ctrls.get("INHIGH")) {
				stackPointer = stackPointer & 0xff | (bus << 8);
			}
			else
			{
				stackPointer = stackPointer & 0xff00 | bus;
			}
		}
		else if (ctrls.get("STPINC")) {
			stackPointer = (stackPointer + 1) & 0xffff;
		}
		if (ctrls.get("JMP")) {
			PC = adrBus;
		}
		else if (ctrls.get("JBC")) {
			boolean carry = (flags & 1) > 0;
			boolean zero = (flags & 2) > 0;
			boolean res = false;
			if (ctrls.get("OPTN0")) {
				if (ctrls.get("OPTN1")) {
					res = carry;
				}
				else
				{
					res = carry && !zero;
				}
			}
			else if (ctrls.get("OPTN1")) {
				res = !(carry || zero);
			}
			else
			{
				res = zero;
			}
			if (res ^ ctrls.get("OPTN2")) {
				PC = adrBus;
			}
		}
		if (ctrls.get("ISA")) {
			if (ctrls.get("INHIGH")) {
				adrReg = adrReg & 0xff | ((bus << 8) & 0xff00);
			}
			else
			{
				adrReg = adrReg & 0xff00 | (bus & 0xff);
			}
		}
		if (ctrls.get("IST")) {
			int ofs = 0;
			if (ctrls.get("OFSX")) {
				ofs = X;
			}
			if (ctrls.get("OFSY")) {
				ofs = Y;
			}
			if (ctrls.get("ADRSUB")) {
				ofs = -ofs;
			}
			writeMem((adrBus + ofs) & 0xffff, bus);
		}
	}
	
	public static String jamCondition;
	public static String jamCode;
	public static boolean jam;
	
	static String cccc;
	
	public static void doCU() {
		if (ctrls.get("STR")) {
			ISTAGE = 0;
			//check for interrupts
			ISOP = 0;
			B = 0;
			//System.out.println("Instruction end");
		}
		rstCTRLS();
		if (ISOP == 0) { //load inst normal
			ctrls.put("ILD", true);
			ctrls.put("CIN", true);
			ctrls.put("IRI", true);
			ctrls.put("ITR", true);
		}
		else if (ISOP == 1) { //load inst err
			
		}
		else if (ISOP == 2) { //load inst interrupt
			
		}
		else //exec inst
		{
			if (IS.containsKey(IR)) {
				try {
					for (String s : IS.get(IR)[ISTAGE].split(":")) {
						ctrls.put(s.toUpperCase(), true);
					}
				} catch(ArrayIndexOutOfBoundsException e) {
					jamCondition = "Instruction set fault: Instruction without end";
					jamCode = "IS_NO_END";
					jam = true;
				}
			}
			else
			{
				jamCondition = "Program fault: Nonexistant instruction run";
				jamCode = "PROG_INVALID_INST";
				jam = true;
			}
		}
		if (ctrls.get("ITR")) {
			ISTAGE = 0;
			ISOP = 3;
			ctrls.put("ITR", false);
		}
		else
		{
			ISTAGE ++;
		}
		cccc = "";
		for (Entry<String, Boolean> ctrl : ctrls.entrySet()) {
			if (ctrl.getValue()) {
				cccc += ctrl.getKey() + " ";
			}
		}
	}
	
	public static byte bus = 0;
	public static int adrBus = 0;
	
	public static boolean isHalted;
	
	public static void cycleCPU() {
		bus = 0;
		adrBus = 0;
		doCU();
		outBus();
		inBus();
		if (ctrls.get("HLT")) {
			CPUMode = RunMode.STOP;
		}
		if (jam && !pJam) {
			CPUMode = RunMode.STOP;
			PApplet.runSketch(new String[] {"GR8EMUJamHandler"}, new GR8EMUJamHandler());
		}
		pJam = jam;
	}
	
	public static int gX;
	public static int gY;
	
	public static void initGPU() {
		screenBuf = new int[200 * 150];
		GPUMode = RunMode.CONTINUE;
		gX = 0;
		gY = 0;
	}
	
	public static void cycleGPU() {
		screenBuf[gX + gY * 200] = gCol(VRAM[gX + gY * 256]);
		gX ++;
		if (gX >= 200) {
			gY ++;
			gX -= 200;
			if (gY >= 150) {
				gY -= 150;
			}
		}
	}
	
	public void settings() {
		size(800, 600);
	}
	
	public void setup() {
		surface.setTitle("GR8CPU Rev3 emulator");
	}
	
	static boolean pJam;
	
	public static boolean resetSim;
	public static boolean stopOnNop;
	
	public void draw() {
		noStroke();
		for (int y = 0; y < 150; y++) {
			for (int x = 0; x < 200; x++) {
				fill(screenBuf[x + y * 200]);
				rect(x * 4, y * 4, 4, 4);
			}
		}
	}
	
	public static int gCol(byte col) {
		int[] rs = {
			0,
			85,
			170,
			255
		};
		int[] cs = {
			0,
			36,
			72,
			109,
			145,
			182,
			218,
			255
		};
		int r = col & 0x3;
		int g = (col >> 2) & 0x7;
		int b = (col >> 5) & 0x7;
		return GUI.color(rs[r], cs[g], cs[b]);
	}

	public static final int COMMAND = 157;
	public boolean CTRL;
	public boolean CMD;
	
	public void keyPressed() {
		if (keyCode == CONTROL) {
			CTRL = true;
		}
		else if (keyCode == COMMAND) {
			CMD = true;
		}
		if ((CTRL && key == 4) || (CMD && key == 'd')) {
			PApplet.runSketch(new String[] {"GR8EMUDebugger"}, new GR8EMUDebugger());
		}
		else if ((CTRL && key == 10) || (CMD && key == 'j')) {
			jam = true;
			jamCode = "MANUAL_JAM";
			jamCondition = "Manual CPU jam";
		}
		//println((int)key);
	}
	
	public void keyReleased() {
		if (keyCode == CONTROL) {
			CTRL = false;
		}
		else if (keyCode == COMMAND) {
			CMD = false;
		}
	}
}
