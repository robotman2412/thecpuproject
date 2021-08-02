package isgen;

import jutils.gui.Button;
import processing.core.PApplet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
public class ISGUI_R3 extends PApplet {
	
	public static void main(String[] args) {
		PApplet.main(ISGUI_R3.class.getName());
	}
	
	public void settings() {
		size(600, 200);
	}
	
	Button selectSrc;
	Button selectAsmg;
	Button selectOut;
	Button compile;
	String srcPath = "";
	String asmgPath = "";
	String outPath = "";
	String argCont = "";
	
	float progress = 0.5f;
	String progText = "Sample text.";
	boolean progEnabled = false;
	
	String error0 = "";
	String error1 = "";
	
	/*
	 * Todos:
	 */
	
	ISGUI_R3 gui;
	
	public void setup() {
		String[] s = loadStrings("data/paths.txt");
		if (s != null && s.length == 3) {
			srcPath = s[0];
			asmgPath = s[1];
			outPath = s[2];
			selAsmg(new File(asmgPath));
		}
		gui = this;
		selectSrc = new Button(this, 100, 5, 50, 20, "select", false, new Runnable() {
			public void run() {
				selectInput("select control signal file...", "selSrc", null, gui);
			}
		});
		selectAsmg = new Button(this, 100, 35, 50, 20, "select", false, new Runnable() {
			public void run() {
				selectInput("select assembly guide file...", "selAsmg", null, gui);
			}
		});
		selectOut = new Button(this, 100, 65, 50, 20, "select", false, new Runnable() {
			public void run() {
				selectOutput("select output name...", "selOut", null, gui);
			}
		});
		compile = new Button(this, 5, 95, 60, 20, "compile", false, new Runnable() {
			public void run() {
				if (loadStrings(asmgPath) == null) {
					error0 = "missing assembly guide";
					return;
				}
				if (loadStrings(srcPath) == null) {
					error0 = "missing control signals";
					return;
				}
				compile();
				String[] s = {
						srcPath,
						asmgPath,
						outPath
				};
				saveStrings("data/paths.txt", s);
			}
		});
		surface.setTitle("ISGEN GR8CPU Rev3 is deprecated, use ISGEN GR8CPU Rev3.1 for respective CPU");
		frameRate(15);
	}

	public void draw() {
		background(255);
		textAlign(CORNER);
		fill(0);
		text("Controls file", 5, 20);
		text("Assembly guide", 5, 50);
		text("Output file", 5, 80);
		text(srcPath, 155, 20);
		text(asmgPath, 155, 50);
		text(outPath, 155, 80);
		selectSrc.render();
		selectAsmg.render();
		selectOut.render();
		compile.render();
		filh(0xff0000);
		textAlign(CORNER);
		text(error0, 5, 135);
		text(error1, 5, 155);
		if (progEnabled) {
			fill(232);
			stroke(0);
			strokeWeight(1);
			rect(0, height - 40, width - 1, 39);
			noStroke();
			filh(0x00df00);
			rect(1, height - 39, ((float)width - 2f) * progress, 38);
			textAlign(CENTER);
			fill(0);
			text(progText, width / 2, height - 15);
		}
	}
	
	public void mousePressed() {
		
	}
	
	public void filh(int col) {
		fill(coloh(col));
	}
	
	public int coloh(int col) {
		return color((col >> 16) & 0xff, (col >> 8) & 0xff, col & 0xff);
	}
	
	public void strokh(int col) {
		fill(coloh(col));
	}

	boolean exists(String path) {
		File f = new File(path);
		return f.exists();
	}

	public void selSrc(File selected) {
		if (selected != null && selected.exists()) {
			srcPath = selected.getAbsolutePath();
			mousePressed = false;
		}
	}

	public void selAsmg(File selected) {
		if (selected != null && selected.exists()) {
			progress = 0;
			asmgPath = selected.getAbsolutePath();
			mousePressed = false;
			progEnabled = false;
		}
	}
	
	public static String formatKib(int bytes) {
		if (bytes > 1073741824) {
			return (bytes + 512) / 1024 + "Gib";
		}
		if (bytes > 1048576) {
			return (bytes + 512) / 1024 + "Mib";
		}
		if (bytes > 1024) {
			return (bytes + 512) / 1024 + "Kib";
		}
		return bytes + "";
	}
	
	public static String formatKB(int bytes) {
		if (bytes > 1000000000) {
			return (bytes + 512) / 1024 + "GB";
		}
		if (bytes > 1000000) {
			return (bytes + 512) / 1024 + "MB";
		}
		if (bytes > 1000) {
			return (bytes + 512) / 1024 + "KB";
		}
		return bytes + "";
	}

	public void selOut(File selected) {
		if (selected != null) {
			outPath = selected.getAbsolutePath();
			mousePressed = false;
		}
	}

	void compile() {
		new Thread(new Runnable() {
			public void run() {
				error0 = "";
				error1 = "";
				try {
					compileISr3(loadStrings(srcPath), loadStrings(asmgPath));
				} catch(CompilerError e) {
					error0 = "Syntax error in line " + e.instruction;
					error1 = e.message;
				}
			}
		}).start();
	}
	
	public void compileISr3(String[] CTRLs, String[] ASMG) throws CompilerError {
		long[] IS = new long[2048];
		boolean[] used = new boolean[256];
		Map<String, Integer> ctrlMap = new HashMap<>();
		for (int i = 0; i < CTRLs.length; i++) {
			if (CTRLs[i].indexOf("-") == 0) {
				continue;
			}
			ctrlMap.put(CTRLs[i].substring(0, CTRLs[i].indexOf(' ')), i);
			println(CTRLs[i].substring(0, CTRLs[i].indexOf(' ')), i);
		}
		progEnabled = true;
		progText = "Compiling instruction set...";

		float lines = ASMG.length;
		for (int i = 0; i < ASMG.length; i++) {
			progress = 1f / lines * (i + 1);
			String s = collapse(ASMG[i]);
			if (s.indexOf('@') != 0) {
				println(s);
				int instc = unhex(s.substring(0, s.indexOf(' ')));
				if (used[instc]) {
					throw new CompilerError(i, String.format("Instruction %02x is redefined!", instc));
				}
				used[instc] = true;
				long[] inst = decINSTr3(i, ctrlMap, s.substring(s.lastIndexOf('\"') + 2));
				print(String.format("0x%02x:", instc));
				for (int l = 0; l < 8; l++) {
					IS[instc | (l << 8)] = inst[l];
					print(String.format(" %016x", inst[l]));
				}
				println("\n");
			}
		}
		progText = "Saving instruction set...";
		saveAll(IS);
		progText = "Done!";
	}
	
	public long[] decINSTr3(int line, Map<String, Integer> ctrlMap, String instCtrls) throws CompilerError {
		String[] split = instCtrls.split(";");
		if (split.length > 8) {
			throw new CompilerError(line, "Instruction has too many stages!");
		}
		long[] inst = new long[8];
		for (int i = 0; i < split.length; i++) {
			String[] mSplit = split[i].split(":");
			for (String s : mSplit) {
				if (ctrlMap.containsKey(s)) {
					inst[i] |= 1l << ctrlMap.get(s);
				}
				else
				{
					throw new CompilerError(line, "Unregistered control signal \"" + s  +"\"!");
				}
			}
		}
		return inst;
	}
	
	public void saveAll(long[] IS) {
		saveHEX(IS);
		saveVerilog(IS);
	}
	
	public void saveHEX(long[] IS) {
		String sl = "";
		String sh = "";
		for (int i = 0; i < IS.length; i++) {
			sl += String.format("%08x ", IS[i] & 0xffffffff);//hex((int)(IS[i] & 0xffffffff), 8) + " ";
			sh += String.format("%04x ", IS[i] >> 32);//hex((int)((IS[i] >> 32) & 0xffff), 4) + " ";
		}
		saveStrings(outPath + "_logisim_low.lhf", new String[] {"v2.0 raw", sl});
		saveStrings(outPath + "_logisim_high.lhf", new String[] {"v2.0 raw", sh});
	}
	
	public void saveVerilog(long[] IS) {
		String s = "";
		for (int i = 0; i < IS.length; i++) {
			s += String.format("\n%012x", IS[i]);
		}
		String[] file = s.substring(1).split("\n");
		saveStrings(outPath + "_verilog.data", file);
	}
	
	public static String collapse(String raw) {
		String s = "";
		boolean pIndent = false;
		for (char c : raw.toCharArray()) {
			if (c == ' ' || c == '\t') {
				if (!pIndent) {
					pIndent = true;
					s += ' ';
				}
			}
			else
			{
				pIndent = false;
				s += c;
			}
		}
		return s;
	}
	
	public String string(Integer[] array) {
		char[] chars = new char[array.length];
		for (int i = 0; i < array.length; i++) {
			chars[i] = (char)(int)array[i];
		}
		return new String(chars);
	}
	
	Integer[] unescString(String raw) {
		List<Integer> chars = new ArrayList<Integer>();
		for (int y = 0; y < raw.length(); y++) {
			char c = raw.charAt(y);
			if (c == '\\') {
				switch(raw.charAt(y + 1)) {
					case('b'):
						c = '\b';
						chars.add((int)c);
						break;
					case('t'):
						c = '\t';
						chars.add((int)c);
						break;
					case('n'):
						c = '\n';
						chars.add((int)c);
						break;
					case('f'):
						c = '\f';
						chars.add((int)c);
						break;
					case('r'):
						c = '\r';
						chars.add((int)c);
						break;
					case('0'):
						chars.add(0);
						break;
					default:
						chars.add((int)raw.charAt(y + 1));
						break;
				}
				y ++;
			}
			else
			{
				chars.add((int)c);
			}
		}
		return chars.toArray(new Integer[0]);
	}
	
	boolean isEmpty(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c != ' ' && c != '\t') {
				return false;
			}
		}
		return true;
	}

	void printArray(String[] s) {
		for (int i = 0; i < s.length; i++) {
			if (s[i] == null) {
				print("null");
			} else
			{
				print("\"" + s[i] + "\"");
			}
			if (i < s.length - 1) {
				print(", ");
			}
		}
		println();
	}

	Object[] append(Object[] array, Object obj) {
		array = (Object[]) expand(array, array.length + 1);
		array[array.length - 1] = obj;
		return array;
	}
	
	String[] getSubArray(String[] raw, int start, int end) {
		String[] ar1 = new String[end - start];
		for (int i = 0; i < ar1.length; i++) {
			ar1[i] = raw[i + start];
		}
		return ar1;
	}
	
	String[] getSubArray(String[] raw, int start) {
		return getSubArray(raw, start, raw.length);
	}
	
}
