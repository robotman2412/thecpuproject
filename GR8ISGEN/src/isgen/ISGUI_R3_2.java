package isgen;

import jutils.IOUtils;
import jutils.gui.Button;
import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ISGUI_R3_2 extends PApplet {
	
	public static void main(String[] args) {
		PApplet.main(ISGUI_R3_2.class.getName());
	}
	
	public void settings() {
		size(600, 200);
	}
	
	Button selectSrc;
	Button selectAsmg;
	Button selectRegs;
	Button selectOut;
	Button compile;
	String srcPath = "";
	String asmgPath = "";
	String regsPath = "";
	String outPath = "";
	String argCont = "";
	
	float progress = 0.5f;
	String progText = "Sample text.";
	boolean progEnabled = false;
	
	String error0 = "";
	String error1 = "";
	
	ISGUI_R3_2 gui;
	
	public void setup() {
		String[] s = loadStrings("data/isgenr3_2_paths.txt");
		if (s != null && s.length == 4) {
			srcPath = s[0];
			asmgPath = s[1];
			regsPath = s[2];
			outPath = s[3];
			selAsmg(new File(asmgPath));
		}
		gui = this;
		selectSrc = new Button(this, 100, 5, 50, 20, "select", false, () -> {
			selectInput("select control signal file...", "selSrc", null, gui);
		});
		selectAsmg = new Button(this, 100, 35, 50, 20, "select", false, () -> {
			selectInput("select assembly guide file...", "selAsmg", null, gui);
		});
		selectRegs = new Button(this, 100, 65, 50, 20, "select", false, () -> {
			selectInput("select register map file...", "selRegs", null, gui);
		});
		selectOut = new Button(this, 100, 95, 50, 20, "select", false, () -> {
			selectOutput("select output name...", "selOut", null, gui);
		});
		compile = new Button(this, 5, 125, 60, 20, "compile", false, () -> {
			if (loadStrings(asmgPath) == null) {
				error0 = "missing assembly guide";
				return;
			}
			if (loadStrings(srcPath) == null) {
				error0 = "missing control signals";
				return;
			}
			compile();
			String[] s1 = {
					srcPath,
					asmgPath,
					regsPath,
					outPath
			};
			saveStrings("data/isgenr3_2_paths.txt", s1);
		});
		surface.setTitle("IS generator for GR8CPU Rev3.2");
		frameRate(15);
	}
	
	public void draw() {
		background(255);
		textAlign(CORNER);
		fill(0);
		text("Controls file", 5, 20);
		text("Assembly guide", 5, 50);
		text("Registers file", 5, 80);
		text("Output file", 5, 110);
		text(srcPath, 155, 20);
		text(asmgPath, 155, 50);
		text(regsPath, 155, 80);
		text(outPath, 155, 110);
		selectSrc.render();
		selectAsmg.render();
		selectRegs.render();
		selectOut.render();
		compile.render();
		filh(0xff0000);
		textAlign(CORNER);
		text(error0, 75, 135);
		text(error1, 75, 155);
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
			progress = 0;
			srcPath = selected.getAbsolutePath();
			mousePressed = false;
			progEnabled = false;
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
	
	public void selRegs(File selected) {
		if (selected != null && selected.exists()) {
			progress = 0;
			regsPath = selected.getAbsolutePath();
			mousePressed = false;
			progEnabled = false;
		}
	}
	
	public void selOut(File selected) {
		if (selected != null) {
			progress = 0;
			outPath = selected.getAbsolutePath();
			mousePressed = false;
			progEnabled = false;
		}
	}
	
	public static String formatKib(int bytes) {
		if (bytes > 1073741824) {
			return (bytes + 536870912) / 1073741824 + "Gib";
		}
		if (bytes > 1048576) {
			return (bytes + 524288) / 1048576 + "Mib";
		}
		if (bytes > 1024) {
			return (bytes + 512) / 1024 + "Kib";
		}
		return bytes + "";
	}
	
	public static String formatKB(int bytes) {
		if (bytes > 1000000000) {
			return (bytes + 500000000) / 1000000000 + "GB";
		}
		if (bytes > 1000000) {
			return (bytes + 500000) / 1000000 + "MB";
		}
		if (bytes > 1000) {
			return (bytes + 500) / 1000 + "KB";
		}
		return bytes + "";
	}
	
	void compile() {
		new Thread(() -> {
			error0 = "";
			error1 = "";
			try {
				compileISr3_2(loadStrings(srcPath), new String(IOUtils.readBytes(asmgPath)), loadStrings(regsPath));
			} catch(CompilerError e) {
				int insn = e.instruction;
				error0 = insn > 256 ? String.format("Syntax error in loader 0x%02x", insn & 0xff) : String.format("Syntax error in instruction 0x%02x", insn);
				error1 = e.message;
			} catch (IOException e) {
				error0 = "IO exception occurred";
				error1 = e.getMessage();
			}
		}).start();
	}
	
	public void compileISr3_2(String[] CTRLs, String ASMG, String[] REGs) throws CompilerError {
		int[] IS = new int[4096];
		boolean[] used = new boolean[256];
		Map<String, Integer> ctrlMap = new HashMap<>();
		ctrlMap.put("", 0);
		for (int i = 0; i < CTRLs.length; i++) {
			if (CTRLs[i].indexOf("-") == 0) {
				continue;
			}
			ctrlMap.put(CTRLs[i].substring(0, CTRLs[i].indexOf(' ')), 1 << i);
			println(CTRLs[i].substring(0, CTRLs[i].indexOf(' ')), i);
		}
		parseREGSr3_2(REGs, ctrlMap);
		progEnabled = true;
		progText = "Compiling instruction set...";
		
		JSONObject asmgRoot = JSONObject.parse(ASMG);
		JSONArray asmgInsn = asmgRoot.getJSONArray("instructions");
		JSONArray asmgLoad = asmgRoot.getJSONArray("load");
		boolean[] insnUsed = new boolean[256];
		boolean[] loaderUsed = new boolean[256];
		for (int i = 0; i < asmgInsn.size(); i++) {
			JSONObject insn = asmgInsn.getJSONObject(i);
			int num = unhex(insn.getString("hex"));
			JSONArray controlsRaw = insn.getJSONArray("controls");
			String[] controls = new String[controlsRaw.size()];
			for (int x = 0; x < controlsRaw.size(); x++) {
				controls[x] = controlsRaw.getString(x);
			}
			if (num > 0x7F) {
				throw new CompilerError(num, "Instructions may not exceed 0x7f!");
			}
			else if (insnUsed[num]) {
				throw new CompilerError(num, String.format("Instruction 0x%02x redefined!", num));
			}
			insnUsed[num] = true;
			int[] sig = decINSTr3_2(num, ctrlMap, controls);
			print(String.format("insn 0x%02x:", num));
			for (int x = 0; x < 16; x++) {
				IS[(num << 4) | x] = sig[x];
				print(String.format(" %08x", sig[x]));
			}
			println("\n");
		}
		for (int i = 0; i < asmgLoad.size(); i++) {
			JSONObject insn = asmgLoad.getJSONObject(i);
			int num = unhex(insn.getString("hex"));
			JSONArray controlsRaw = insn.getJSONArray("controls");
			String[] controls = new String[controlsRaw.size()];
			for (int x = 0; x < controlsRaw.size(); x++) {
				controls[x] = controlsRaw.getString(x);
			}
			if (num > 0x03) {
				throw new CompilerError(num | 0x100, "Loaders may not exceed 0x03!");
			}
			else if (loaderUsed[num]) {
				throw new CompilerError(num | 0x100, String.format("Loader 0x%02x redefined!", num));
			}
			loaderUsed[num] = true;
			int[] sig = decINSTr3_2(num | 0x100, ctrlMap, controls);
			print(String.format("ld 0x%02x:", num));
			for (int x = 0; x < 16; x++) {
				IS[(num << 4) | x | (1 << 11)] = sig[x];
				print(String.format(" %08x", sig[x]));
			}
			println("\n");
		}
		
		progText = "Saving instruction set...";
		saveAll(IS);
		progText = "Done!";
	}
	
	public void parseREGSr3_2(String[] regs, Map<String, Integer> ctrlMap) {
		Map<String, Integer> regsMap = new HashMap<>();
		Map<String, String> prefixMap = new HashMap<>();
		
		// Get the stupid metadata.
		for (int i = 0; i < regs.length; i++) {
			String reg = regs[i].trim();
			int indexial = reg.indexOf('#');
			if (indexial == 0) {
				continue;
			}
			reg = reg.substring(0, indexial >= 0 ? indexial : reg.length());
			String[] split = reg.trim().split("\\s+");
			if (split.length < 3 || !split[0].equals("meta")) {
				continue;
			}
			
			String applies = split[1];
			
			prefixMap.put(applies, applies + "_"); // Lazy coding :D
		}
		
		// Map shit to other shit.
		for (int i = 0; i < regs.length; i++) {
			String reg = regs[i].trim();
			int indexial = reg.indexOf('#');
			if (indexial == 0) {
				continue;
			}
			reg = reg.substring(0, indexial >= 0 ? indexial : reg.length());
			String[] split = reg.trim().split("\\s+");
			if (split.length < 3 || split[0].equals("meta")) {
				continue;
			}
			
			String prefix = prefixMap.get(split[0]);
			String applies = split[2];
			int num = Integer.parseInt(split[1], 16);
			
			int ctrls = 0;
			for (int x = 0; x < 8; x++) {
				if ((num & 1) > 0) {
					ctrls |= ctrlMap.get(prefix + x);
				}
				num >>= 1;
			}
			
			ctrlMap.put(applies, ctrls);
		}
	}
	
	public int[] decINSTr3_2(int insn, Map<String, Integer> ctrlMap, String[] instCtrls) throws CompilerError {
		if (instCtrls.length > 16) {
			throw new CompilerError(insn, "Instruction has too many stages!");
		}
		int[] inst = new int[16];
		for (int i = 0; i < instCtrls.length; i++) {
			String[] split = instCtrls[i].trim().split("\\s+");
			for (String s : split) {
				if (ctrlMap.containsKey(s)) {
					if ((inst[i] & ctrlMap.get(s)) > 0) {
						throw new CompilerError(insn, "Control signal \"" + s + "\" interferes with other control signals!");
					}
					inst[i] |= ctrlMap.get(s);
				}
				else
				{
					throw new CompilerError(insn, "Unregistered control signal \"" + s  +"\"!");
				}
			}
		}
		return inst;
	}
	
	public void saveAll(int[] IS) {
		saveHEX(IS);
		saveVerilog(IS);
		saveC(IS);
	}
	
	public void saveC(int[] IS) {
		StringBuilder sl = new StringBuilder("{");
		for (int i = 0; i < IS.length; i++) {
			if (i % 16 == 0 && i < IS.length - 1) {
				sl.append('\n');
			}
			sl.append(String.format("0x%08x, ", IS[i]));
		}
		sl.append("\n};");
		saveBytes(outPath + "_c_java.txt", sl.toString().getBytes(StandardCharsets.US_ASCII));
	}
	
	public void saveHEX(int[] IS) {
		StringBuilder sl = new StringBuilder();
		for (int value : IS) {
			sl.append(String.format("%08x ", value));//hex((int)(IS[i] & 0xffffffff), 8) + " ";
		}
		saveStrings(outPath + "_logisim.lhf", new String[] {"v2.0 raw", sl.toString()});
	}
	
	public void saveVerilog(int[] IS) {
		StringBuilder s = new StringBuilder();
		for (int value : IS) {
			s.append(String.format("\n%08x", value));
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
