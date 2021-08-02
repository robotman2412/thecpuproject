package assembler;

import assembler.customs.TorcherCompressor;
import jdk.nashorn.internal.runtime.regexp.joni.exception.SyntaxException;
import jutils.gui.Button;
import jutils.gui.Dropdown;
import jutils.gui.DropdownElement;
import processing.core.PApplet;

import java.io.File;
import java.util.*;

public class Assembler extends PApplet {

    public static void main(String[] args) {
        PApplet.main(Assembler.class.getName());
    }

    public void settings() {
        size(600, 200);
    }


    public boolean doFlasher = true;
    public boolean showAsmg = true;
    public Button selectSrc;
    public Button selectAsmg;
    public Button selectOut;
    public Button compile;
    public Button openDump;
    public Button flash;
    public Button flashAFile;
    public Button dumpMem;
    public String srcPath = "";
    public String asmgPath = "";
    public String outPath = "";
    public InstructionSet IS;

    public FlasherThread flasher;

    public float progress = 0.5f;
    public String progText = "Sample text.";
    public boolean progEnabled = false;
    public boolean flashOnComplete = false;
    public boolean saveMemDump = false;

    public Dropdown portDropdown;

    public String error0 = "";
    public String error1 = "";

    /*
     * Todos:
     */

    public static Assembler asm;

    public void setup() {
        Expression.asm = this;
        ExpressionPart.asm = this;
        ImportedFile.asm = this;
        FlasherThread.asm = this;
        String[] s = loadStrings("data/paths.txt");
        if (s != null && s.length == 3) {
            srcPath = s[0];
            if (showAsmg) {
                asmgPath = s[1];
            }
            outPath = s[2];
            selAsmg(new File(asmgPath));
        }
        selectSrc = new Button(this, 100, 5, 50, 20, "select", false, new Runnable() {
            public void run() {
                selectInput("select source code file...", "selSrc", null, asm);
            }
        });
        asm = this;
        if (showAsmg) {
            selectAsmg = new Button(this, 100, 35, 50, 20, "select", false, new Runnable() {
                public void run() {
                    selectInput("select assembly guide file...", "selAsmg", null, asm);
                }
            });
        }
        selectOut = new Button(this, 100, 65, 50, 20, "select", false, new Runnable() {
            public void run() {
                selectOutput("select output name...", "selOut", null, asm);
            }
        });
        compile = new Button(this, 5, 95, 60, 20, "compile", false, new Runnable() {
            public void run() {
                if (loadStrings(asmgPath) == null) {
                    error0 = "missing assembly guide";
                    return;
                }
                if (loadStrings(srcPath) == null) {
                    error0 = "missing source code";
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
        openDump = new Button(this, compile.x + compile.width + 5, 95, 100, 20, "open dump file", false, new Runnable() {
            public void run() {
                launch(outPath + ".dump");
            }
        });
        if (doFlasher) {
            flash = new Button(this, openDump.x + openDump.width + 5, 95, 110, 20, "compile and flash", false, new Runnable() {
                public void run() {
                    flashOnComplete = true;
                    compile.onPress.run(null);
                }
            });
            portDropdown = new Dropdown(this, flash.x + flash.width + 5, 95, 100, 20, new DropdownElement[]{new DropdownElement("No ports", "")});
            flasher = new FlasherThread();
            flasher.start();
            dumpMem = new Button(this, portDropdown.x + portDropdown.width + 5, 95, 90, 20, "dump memory", false, new Runnable() {
                public void run() {
                    saveMemDump = true;
                    flasher.dumpMemory();
                }
            });
            flashAFile = new Button(this, dumpMem.x + dumpMem.width + 5, 95, 90, 20, "flash file", false, new Runnable() {
                public void run() {
                    selectInput("select file to flash...", "flashAFile", null, asm);
                }
            });
        }
		/*if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				println(args[i]);
				if (args[i].equals("-i") && i < args.length - 1) {
					if (true) {//exists(args[i] + 1)) {
						println("input: " + (args[i] + 1));
						argCont = "input: " + (args[i] + 1);
					}
				}
			}
		}*/
		/*String[] tkn = tokenise("$ff + ($10 - $50) * $02");
		tkn = getSubArray(tkn, 2);
		Expression.asm = this;
		Expression e = new Expression(tkn);
		try {
			e.prep(null, 0);
			println("Expression:");
			e.printInfo(1);
			println("Result: " + e.resolve(null, 0));
		} catch (CompilerError e1) {
			System.err.println(e1.message);
			e1.printStackTrace();
		}*/
        frameRate(15);
    }

    public void draw() {
        background(255);
        textAlign(CORNER);
        fill(0);
        text("Source file", 5, 20);
        text("Assembly guide", 5, 50);
        text("Output file", 5, 80);
        text(srcPath, 155, 20);
        text(asmgPath, 155, 50);
        text(outPath, 155, 80);
        selectSrc.render();
        if (showAsmg) {
            selectAsmg.render();
        }
        selectOut.render();
        compile.render();
        openDump.render();
        if (doFlasher) {
            flash.render();
            dumpMem.render();
            flashAFile.render();
        }
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
            rect(1, height - 39, ((float) width - 2f) * progress, 38);
            textAlign(CENTER);
            fill(0);
            text(progText, width / 2, height - 15);
        }
        if (doFlasher) {
            portDropdown.render();
        }
    }

    public void mousePressed() {
        if (doFlasher) {
            portDropdown.clicked();
        }
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

    public boolean exists(String path) {
        File f = new File(path);
        return f.exists();
    }

    public void selSrc(File selected) {
        if (selected != null && selected.exists()) {
            srcPath = selected.getAbsolutePath();
            outPath = selected.getParent() + File.separator + selected.getName().substring(0, selected.getName().lastIndexOf(".")) + ".hex";
            mousePressed = false;
        }
    }

    public void selAsmg(File selected) {
        if (selected != null && selected.exists()) {
            progEnabled = true;
            progText = "Parsing guide...";
            progress = 0;
            asmgPath = selected.getAbsolutePath();
            IS = new InstructionSet();
            String[] s = loadStrings(selected);
            List<AsmNumberFormat> numberFormats = new ArrayList<>();
            for (int i = 0; i < s.length; i++) {
                if (s[i].charAt(0) == '@') {
                    try {
                        String op = s[i].substring(1, s[i].indexOf(':'));
                        String args = s[i].substring(s[i].indexOf(':') + 1);
                        switch (op) {
                            default:
                                throw new SyntaxException("Unknown property \"" + op + "\"");
                            case ("NumberFormat"):
                                String[] split = args.split(",");
                                if (split.length == 2) {
                                    AsmNumberFormat n = new AsmNumberFormat();
                                    n.prefix = split[0];
                                    n.base = Integer.parseUnsignedInt(split[1]);
                                    numberFormats.add(n);
                                }
                            case ("Relative"):
                                assert (args.matches("([0-9]+)(,(LittleEndian|BigEndian)((\\+([0-9]+))?)?(,(Signed|Unsigned))?)"));
                                WordFormat w = new WordFormat(IS.wordBits, args.substring(args.indexOf('=') + 1));
                                IS.special.put(args.substring(0, args.indexOf('=')), w);
                                w.isRelative = true;
                                break;
                            case ("InOpcode"):
                                IS.special.put(args.substring(0, args.indexOf('=')), new WordFormat(IS.wordBits, args.substring(args.indexOf('=') + 1)));
                                break;
                            case ("Special"):
                                assert (args.matches("([0-9]+)(,(LittleEndian|BigEndian)((\\+([0-9]+))?)?(,(Signed|Unsigned))?)"));
                                IS.special.put(args.substring(0, args.indexOf('=')), new WordFormat(IS.wordBits, args.substring(args.indexOf('=') + 1)));
                                break;
                            case ("Word"):
                                IS.wordBits = Integer.parseUnsignedInt(args);
                                break;
                            case ("Inst"):
                                IS.instWords = Integer.parseUnsignedInt(args);
                                break;
                            case ("Memory"):
                                IS.addressingBits = Integer.parseUnsignedInt(args) * IS.wordBits;
                                break;
                            case ("MemoryBits"):
                                IS.addressingBits = Integer.parseUnsignedInt(args);
                                break;
                            case ("Nolabels"):
                            case ("Nolabel"):
                                IS.noLabelStrings = args.split(" ");
                                break;
                            case ("NolabelChars"):
                            case ("NolabelChar"):
                                IS.noLabelChars = args.toCharArray();
                                break;
                            case ("ISGEN"):
                                //ignored, as this is only useful to the ISGEN
                                break;
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        if (e instanceof CompilerError) {
                            error0 = "Syntax error in line " + (i + 1);
                            error1 = e.getMessage();
                        } else if (e instanceof Exception) {
                            error0 = "Exception in line " + (i + 1);
                            error1 = e.getMessage();
                        }
                    }
                } else {
                    try {
                        IS.addInstruction(s[i]);
                        progress = ((float) i) / s.length;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        if (e instanceof CompilerError) {
                            error0 = "Syntax error in line " + (i + 1);
                            error1 = e.getMessage();
                            e.printStackTrace();
                        } else if (e instanceof Exception) {
                            error0 = "Exception in line " + (i + 1);
                            error1 = e.getMessage();
                            e.printStackTrace();
                        }
                    }
                }
            }
            mousePressed = false;
            progEnabled = false;
            IS.numberFormats = numberFormats.toArray(new AsmNumberFormat[0]);
        }
    }

    public void flashAFile(File selected) {
        if (selected != null) {
            String name = selected.getName();
            if (name.endsWith(".torch") || name.endsWith("torches")) {
                error0 = "Cannot decompress .torch or .torcher files!";
                error1 = "";
            } else if (name.endsWith(".hex") || name.endsWith(".lhf")) {
                String[] file = loadStrings(selected);
                if (file.length != 2) {
                    error0 = "Invalid logisim hex file format.";
                    error1 = "Expected 2 lines.";
                    return;
                } else if (!file[0].equals("v2.0 raw")) {
                    error0 = "Invalid logisim hex file format.";
                    error1 = "Header must be \"v2.0 raw\"";
                    return;
                } else {
                    try {
                        String[] split = file[1].split(" ");
                        byte[] data = new byte[split.length];
                        for (int i = 0; i < split.length; i++) {
                            data[i] = (byte) unhex(split[i]);
                        }
                        flasher.flash(data, "File flashed!");
                    } catch (Exception e) {
                        e.printStackTrace();
                        error0 = e.getClass().getName();
                        error1 = e.getMessage();
                    }
                }
            } else if (name.endsWith(".bin") || name.endsWith(".dat")) {
                flasher.flash(loadBytes(selected), "File flashed!");
            } else {
                error0 = "Filetype not recognised, assuming binary file.";
                error1 = "";
                flasher.flash(loadBytes(selected), "File flashed!");
            }
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

    public void compile() {
        new Thread(new Runnable() {
            public void run() {
                error0 = "";
                error1 = "";
                try {
                    compileProgram(loadStrings(srcPath), IS);
                } catch (CompilerError e) {
                    e.printStackTrace();
                    error0 = "Syntax error in line " + e.line;
                    error1 = e.message;
                }
            }
        }).start();
    }

    public void saveOut(byte[] data, String mOutPath) {
        println("Saving " + data.length + " bytes to " + mOutPath);
        String[] split = split(outPath, ".");
        String ext = split[split.length - 1].toLowerCase();
        if (ext.equals("hex") || ext.equals("lhf")) {
            String s = "";
            for (int i = 0; i < data.length; i++) {
                s += hex(data[i]);
                if (i < data.length - 1) {
                    s += " ";
                }
            }
            saveStrings(mOutPath, new String[]{"v2.0 raw", s});
        } else if (ext.equals("torch") || ext.equals("torcher")) {
            int[] torchData = new int[data.length];
            for (int i = 0; i < data.length; i++) {
                torchData[i] = Byte.toUnsignedInt(data[i]);
            }
            saveStrings(mOutPath, TorcherCompressor.compressBits(torchData, 8).toArray(new String[0]));
        } else {
            saveBytes(mOutPath, data);
        }
    }

    public static class CompilerError extends Throwable {
        private static final long serialVersionUID = -1285538276365577921L;
        int line;
        String message;

        public CompilerError(int Sln, String Smsg) {
            line = Sln + 1;
            message = Smsg;
        }
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
            } else {
                pIndent = false;
                s += c;
            }
        }
        return s;
    }

    public static String[] tokeniseGuide(String raw) {
        raw = raw.replaceAll(" ", "\t");
        String s = "";
        String[] splitTokens = "# @ + - * = / , ( )".split(" ");
        boolean inString = false;
        boolean isDoubleQuotes = false;
        boolean isEscaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
//			if (c == ',') {
//				s += " ";
//				s += c;
//			}
//			else if (c == '#') {
//				s += c;
//				s += " ";
//				if (i < raw.length() - 1) {
//					s += raw.charAt(i + 1);
//				}
//				i ++;
//			}
//			else if (c == '@') {
//				s += c;
//				s += " ";
//			}
//			else
//			{
//				s += c;
//			}
            boolean splitted = false;
            if (c == '\"') {
                if (inString && isDoubleQuotes && !isEscaped) {
                    inString = false;
                } else if (!inString) {
                    inString = true;
                    isEscaped = false;
                    isDoubleQuotes = true;
                }
            } else if (c == '\'') {
                if (inString && !isDoubleQuotes && !isEscaped) {
                    inString = false;
                } else if (!inString) {
                    inString = true;
                    isEscaped = false;
                    isDoubleQuotes = false;
                }
            }
            if (isEscaped) {
                isEscaped = false;
            } else if (c == '\\') {
                isEscaped = true;
            }
            if (inString) {
                if (c == '\t') {
                    c = ' ';
                }
            }
            else if (c == ';') {
                break; // Comment, ignore the rest.
            } else {
                for (String tkn : splitTokens) {
                    if (i < raw.length() - tkn.length() + 1) {
                        boolean splitValid = true;
                        for (int x = 0; x < tkn.length(); x++) {
                            if (tkn.charAt(x) != raw.charAt(i + x)) {
                                splitValid = false;
                                break;
                            }
                        }
                        if (splitValid) {
                            splitted = true;
                            if (i != 0) {
                                s += '\t';
                            }
                            s += c + "\t";
                            break;
                        }
                    }
                }
            }
            if (!splitted) {
                s += c;
            }
        }
        raw = s;
        s = "";
        boolean b = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (!b || c != '\t') {
                s += c;
            }
            b = c == '\t';
        }
        raw = s;
        s = "";
        b = false;
        for (int i = raw.length() - 1; i >= 0; i--) {
            char c = raw.charAt(i);
            if (b || c != '\t') {
                s = c + s;
                b = true;
            }
        }
        return split(s, "\t");
    }

    public static String[] tokenise(String raw) {
        return tokeniseGuide(raw);
    }

    @Deprecated
    public static String[] tokeniseOld(String raw) {
        raw = raw.replaceAll("\t", " ");
        raw = split(raw, ";")[0];
        String s = "";
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == ',') {
                s += " ";
                s += c;
            } else if (c == '#') {
                s += c;
                s += " ";
                if (i < raw.length() - 1) {
                    s += raw.charAt(i + 1);
                }
                i++;
            } else if (c == '@') {
                s += c;
                s += " ";
            } else {
                s += c;
            }
        }
        raw = s;
        s = "";
        boolean b = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (!b || c != ' ') {
                s += c;
            }
            b = c == ' ';
        }
        raw = s;
        s = "";
        b = false;
        for (int i = raw.length() - 1; i >= 0; i--) {
            char c = raw.charAt(i);
            if (b || c != ' ') {
                s = c + s;
                b = true;
            }
        }
        return split(s, " ");
    }

    public static Charset currentCharset = new Charset(8, false);

    public void compileProgram(String[] file, InstructionSet IS) throws CompilerError {
        currentCharset = new Charset(8, false);
        progEnabled = true;
        progText = "Assembling program: Tokenizing...";
        println("----TOKENISING----");
        progress = 0;
        int adrBits = IS.addressingBits;
        int maxWords = (int) Math.pow(2, IS.addressingBits);
        boolean padding = true;
        String[][] tokens = new String[file.length][];
        for (int i = 0; i < file.length; i++) {
            if (!isEmpty(file[i]) && !isComment(file[i])) {
                tokens[i] = tokenise(file[i]);
                printArray(tokens[i]);
            } else {
                tokens[i] = null;
            }
            progress = ((float) i) / file.length / 3f;
        }
//		for (int i = 0; i < tokens.length; i++) {
//			if (tokens[i].length > 1 && tokens[i][0].length() == 0 && IS.isValidLabel(tokens[i][1])) {
//				String[] newArr = new String[tokens[i].length - 1];
//				System.arraycopy(tokens[i], 1, newArr, 0, newArr.length);
//				tokens[i] = newArr;
//			}
//		}


        println("----FINDING IMPORTS----");
        progText = "Finding imports...";
        Charset charset = null;
        List<ImportedFile> imported = new ArrayList<>();
        for (int i = 0; i < tokens.length; i++) {
            progText = "Finding imports...";
            progress = ((float) i) / tokens.length;
            if (tokens[i] != null && tokens.length > 1 && tokens[i][0].equals("@")) {
                if (tokens[i][1].equals("include")) {
                    if (tokens.length < 3 || !tokens[i][2].startsWith("\"")) {
                        throw new CompilerError(i, "@include requires a String path!");
                    }
                    String path = new File(srcPath).getParent() + "/" + string(unescString(tokens[i][2].substring(1, tokens[i][2].length() - 1)));
                    println("Importing \"" + path + "\"...");
                    ImportedFile f = new ImportedFile(path);
                    f.line = i;
                    imported.add(f);
                    tokens[i] = null;
                } else if (tokens[i][1].equals("no_padding")) {
                    padding = false;
                    tokens[i] = null;
                } else if (tokens[i][1].equals("charset")) {
                    if (tokens.length < 3 || !tokens[i][2].startsWith("\"")) {
                        throw new CompilerError(i, "@charset requires a String path!");
                    }
                    String path = new File(srcPath).getParent() + "/" + string(unescString(tokens[i][2].substring(1, tokens[i][2].length() - 1)));
                    try {
                        charset = new Charset(new File(path));
                    } catch (Exception e) {
                        throw new CompilerError(i, "Character set is invalid!");
                    }
                } else if (tokens[i][1].equals("available_space")) {
                    if (tokens.length < 3) {
                        throw new CompilerError(i, "@available_space requires a value!");
                    }
                    maxWords = readVal(tokens[i][2], new HashMap<>(), i, adrBits, adrBits, new AsmNumberFormat[0]);
                }
            }
        }
        if (charset == null) {
            charset = new Charset(IS.wordBits, false);
        }
        currentCharset = charset;


        println("----IMPORTING----");
        progText = "importing...";
        int importedLineOffset = 0;
        for (int i = 0; i < imported.size(); i++) {
            tokens = imported.get(i).insertTokens(tokens, imported.get(i).line + importedLineOffset);
            importedLineOffset += imported.get(i).tokens.length;
            progress = ((float) i + 1) / (imported.size());
        }


        println("----PASS 1----");
        progText = "Assembling program: pass 1...";
        int address = 0;
        Map<String, Integer> labels = new HashMap<>();
        int[] compiled = new int[65536];
        int[] instructions = new int[tokens.length];
        boolean[] isInst = new boolean[tokens.length];
        int firstAddress = 0; //first used address
        for (int i = 0; i < tokens.length; i++) {
            //region labels
            if (tokens[i] != null && tokens[i].length > 0) {
                if (tokens[i][0].equals("*")) {
                    if (!tokens[i][1].equals("=")) {
                        throw new CompilerError(i, "Program start label must be a value!");
                    }
                    int mAddress = address;
                    address = resolveExpression(getSubArray(tokens[i], 2), labels, i, adrBits, adrBits, IS.numberFormats);
                    if (mAddress == 0 && firstAddress == 0) {
                        firstAddress = address;
                    }
                    if (address < mAddress) {
                        throw new CompilerError(i, "Start point goes backwards! (usually caused by overriding reserved space)");
                    }
                } else if (!tokens[i][0].equals("")) {
                    if (labels.containsKey(tokens[i][0])) {
                        println("Warning: label \"" + tokens[i][0] + "\" re-assigned!");
                    }
                    int labelAddress;
                    if (tokens[i].length > 1 && tokens[i][1].equals("=")) {
                        if (tokens[i].length < 2) {
                            throw new CompilerError(i, "Missing EXPRESSION!");
                        }
                        labelAddress = resolveExpression(getSubArray(tokens[i], 2), labels, i, adrBits, adrBits, IS.numberFormats);
                    } else if (!IS.isValidLabel(tokens[i][0])) {
                        throw new CompilerError(i, "Invalid label name! please pick another one.");
                    } else {
                        labelAddress = address;
                    }
                    labels.put(tokens[i][0], labelAddress);
                    println("label \"" + tokens[i][0] + "\": " + labelAddress);
                }
            }
            //endregion labels
            if (tokens[i] != null) {
                if (tokens[i].length != 1) {
                    if (tokens[i][1].equals("data") || tokens[i][1].equals("byte") || tokens[i][1].equals("bytes")) {
                        //read data length
                        boolean did = false;
                        for (int x = 2; x < tokens[i].length; x++) { //this needs to be improved later on
                            if (!tokens[i][x].equals(",")) {
                                if (!tokens[i][x].startsWith("\"") && !tokens[i][x].startsWith("\'") && !did) {
                                    address++;
                                    did = true;
                                }
                            } else {
                                did = false;
                            }
                            if (tokens[i][x].startsWith("\"") || tokens[i][x].startsWith("\'")) {
                                println("String length: " + unescString(tokens[i][x].substring(1, tokens[i][x].length() - 1)).length);
                                address += unescString(tokens[i][x].substring(1, tokens[i][x].length() - 1)).length;
                            }
                        }
                    } else if (tokens[i][1].equals("reserve")) {
                        //reserve a bunch of bytes
                        if (tokens[i].length != 3) {
                            throw new CompilerError(i, "Expected reserve (num. bytes), got \"reserve\"");
                        }
                        address += readVal(tokens[i][2], labels, i, adrBits, adrBits, IS.numberFormats);
                    } else if (!tokens[i][1].equals("=")) { //we must ignore label definitions
                        //find instruction
                        boolean matchFound = false;
                        for (int x = 0; x < IS.instructions.length; x++) {
                            if (IS.instructions[x].length == tokens[i].length - 1) {
                                boolean match = true;
                                int instruction = 0;
                                //check if tokens match
                                for (int y = 0; y < IS.instructions[x].length; y++) {
                                    instruction = x;
                                    if (!matchTokens(tokens[i][y + 1], IS.instructions[x][y], labels, i, IS.numberFormats)) {
                                        match = false;
                                        break;
                                    }
                                }
                                if (match) {
                                    instructions[i] = instruction;
                                    isInst[i] = true;
                                    //increment address for instruction word
                                    address += IS.calcLength(IS.instructions[x]);
//									address ++;
//									for (int y = 0; y < IS.instructions[x].length; y++) {
//										if (IS.instructions[x][y].indexOf(0) == '%') {
//											//increment address for data or address word
//											address += IS.calcLength(IS.instructions[x][y]);
//										}
//									}
                                    matchFound = true;
                                    break;
                                }
                            }
                        }
                        if (!matchFound) {
                            throw new CompilerError(i, "Instruction not found!");
                        }
                    }
                }
            }
            println(i + 1 + " / " + tokens.length);
            progress = ((float) i) / (tokens.length - 1f) / 3f + 1f / 3f;
            println("inst: " + hex(instructions[i], 2));
        }


        println("----PASS 2----");
        progText = "Assembling program: pass 2...";
        AssemblyDump[] dump = new AssemblyDump[tokens.length];
        address = 0;
        for (int i = 0; i < tokens.length; i++) {
            println(i + 1);
            if (tokens[i] == null) {
                println("Empty line at " + (i + 1));
            } else {
                dump[i] = new AssemblyDump();
                dump[i].startAddress = address;
                dump[i].tokens = tokens[i];
                if (!isInst[i]) {
                    //check data statements and garbages
                    if (tokens[i].length > 1 && (tokens[i][1].equals("data") || tokens[i][1].equals("byte") || tokens[i][1].equals("bytes"))) {
                        int tknStart = 2;
                        for (int x = 2; x < tokens[i].length; x++) { //this needs to be improved later on
                            String[] mTokens = null;
                            if (tokens[i][x].equals(",")) {
                                mTokens = getSubArray(tokens[i], tknStart, x);
                                tknStart = x + 1;
                            } else if (x == tokens[i].length - 1) {
                                mTokens = getSubArray(tokens[i], tknStart);
                                tknStart = x + 1;
                            }
                            if (mTokens != null) {
                                if (mTokens[0].startsWith("\"") || mTokens[0].startsWith("\'")) {
                                    Integer[] chars = unescString(mTokens[0].substring(1, mTokens[0].length() - 1));
                                    print("String: ");
                                    for (int y = 0; y < chars.length; y++) {
                                        print(((byte) (int) chars[y]) + " ");
                                        compiled[address] = (byte) (int) chars[y];
                                        address++;
                                    }
                                } else {
                                    compiled[address] = resolveExpression(mTokens, labels, i, IS.wordBits, adrBits, IS.numberFormats);
                                    address++;
                                }
                            }
                        }
                    } else if (tokens[i].length >= 2 && tokens[i][1].equalsIgnoreCase("reserve")) {
                        //reserve a bunch of bytes
                        if (tokens[i].length != 3) {
                            throw new CompilerError(i, "Expected reserve (num. bytes), got \"reserve\"");
                        }
                        address += readVal(tokens[i][2], labels, i, adrBits, adrBits, IS.numberFormats);
                    } else if (tokens[i][0].equals("*")) {
                        address = resolveExpression(getSubArray(tokens[i], 2), labels, i, adrBits, adrBits, IS.numberFormats);
                    }
                } else {
                    int inst = instructions[i];
                    compiled[address] = IS.addresses[inst];
                    address++;
                    for (int x = 0; x < IS.instructions[inst].length; x++) {
                        String s = IS.instructions[inst][x];
                        if (s.charAt(0) == '%') {
                            WordFormat spec = IS.special.get(s.substring(1, s.length() - 1));
                            int val = readVal(tokens[i][x + 1], labels, i, spec.numWords * IS.wordBits, adrBits, IS.numberFormats);
                            if (spec.numWordsOut > 1) {
                                int actual = (int) spec.getOut(val, address + spec.numWordsOut);
//								compiled[address] = val & 0xff;
//								compiled[address + 1] = (val >> 8) & 0xff;
                                for (int y = 0; y < spec.numWordsOut; y++) {
                                    compiled[address + y] = (actual >> (8 * y)) & 0xff;
                                }
                                address += spec.numWordsOut;
                            } else {
                                compiled[address] = val;
                                address++;
                            }
//							compiled[address] = readVal(tokens[i][x + 1], labels, i);
//							address ++;
                        }
                    }
                }
                dump[i].endAddress = address;
                dump[i].fetchCompiled(compiled);
            }
            progress = ((float) i) / ((float) tokens.length - 1) / 3f + 2f / 3f;
        }


        println("----CREATING DUMP----");
        progText = "Finishing: creating program dump...";
        int[] dumpIndent = null;
        List<String> assemblyDump = new ArrayList<>();
        assemblyDump.add("First line padded: " + (padding && firstAddress > 0));
        assemblyDump.add("");
        for (AssemblyDump d : dump) {
            if (d != null) {
                d.genStrings(adrBits, IS.wordBits);
                dumpIndent = d.checkIndentation(dumpIndent);
            }
        }
        for (AssemblyDump d : dump) {
            if (d != null) {
                assemblyDump.add(d.getString(dumpIndent));
            }
        }
        assemblyDump.add("");
        assemblyDump.add("Labels:");
        assemblyDump.add("");
        int labelIndent = 1;
        List<String> labelList = new ArrayList<>(labels.size());
        for (Map.Entry<String, Integer> e : labels.entrySet()) {
            labelList.add(e.getKey());
            if (e.getKey().length() >= labelIndent) {
                labelIndent = e.getKey().length() + 1;
            }
        }
        Collections.sort(labelList);
        for (String key : labelList) {
            String s = key;
            while (s.length() < labelIndent) {
                s += " ";
            }
            long label = labels.get(key);
            long a = (long) Math.ceil((float) IS.wordBits / 4f);
            s += "$" + String.format("%0" + a + "x", label);
            assemblyDump.add(s);
        }
        assemblyDump.add("");
        assemblyDump.add("Program length: " + address + " / " + maxWords + " bytes (" + (address / (float) maxWords * 100f) + "% of maximum size)");
        saveStrings(outPath + ".dump", assemblyDump.toArray(new String[0]));


        println("Padding: " + firstAddress);
        println("Final address: " + address);
        if (address > 65536/* maxProgramLength*/) {
            throw new CompilerError(0, "The program is too long! " + address + " words out of " + 256 + " supported words long");
        }
        byte[] tmp;
        int progsize;
        if (padding) {
            tmp = new byte[progsize = address];
            for (int i = 0; i < address; i++) {
                tmp[i] = (byte) compiled[i];
            }
        } else {
            tmp = new byte[progsize = address - firstAddress];
            for (int i = 0; i < progsize; i++) {
                tmp[i] = (byte) compiled[i + firstAddress];
            }
        }
        println("Done!");
        progText = "Saving...";
        saveOut(tmp, outPath);
        if (flashOnComplete) {
            flashOnComplete = false;
            progText = "Flashing...";
            flasher.flash(tmp, "Flashed! Program uses " + (Math.round(tmp.length / (float) maxWords * 1000f) / 10f) + "% of memory (" + progsize + " words).");
        } else {
            progText = "Done! Program uses " + (Math.round(tmp.length / (float) maxWords * 1000f) / 10f) + "% of memory (" + progsize + " words).";
        }
    }

    public String string(Integer[] array) {
        char[] chars = new char[array.length];
        for (int i = 0; i < array.length; i++) {
            chars[i] = (char) (int) array[i];
        }
        return new String(chars);
    }

    public Integer[] unescString(String raw) {
        List<Integer> chars = new ArrayList<>();
        for (int y = 0; y < raw.length(); y++) {
            char c = raw.charAt(y);
            if (c == '\\') {
                switch (raw.charAt(y + 1)) {
                    case ('b'):
                        c = '\b';
                        chars.add((int) c);
                        break;
                    case ('x'):
                        if (raw.length() < y + 4) {
                            throw new IllegalArgumentException("need exactly two hexadecimal characters for \\x characters!");
                        }
                        chars.add(unhex(raw.substring(y + 2, y + 4)));
                        break;
                    case ('t'):
                        c = '\t';
                        chars.add((int) c);
                        break;
                    case ('n'):
                        c = '\n';
                        chars.add((int) c);
                        break;
                    case ('f'):
                        c = '\f';
                        chars.add((int) c);
                        break;
                    case ('r'):
                        c = '\r';
                        chars.add((int) c);
                        break;
                    case ('0'):
                        chars.add(0);
                        break;
                    default:
                        chars.add((int) raw.charAt(y + 1));
                        break;
                }
                y++;
            } else {
                chars.add((int) c);
            }
        }
        List<Integer> out = new ArrayList<>();
        for (Integer leChar : chars) {
            out.add(currentCharset.translate((char) (int) leChar));
        }
        return out.toArray(new Integer[0]);
    }

    public boolean matchTokens(String src, String guide, Map<String, Integer> labels, int line, AsmNumberFormat[] numberFormats) throws CompilerError {
        println(guide, "->", src);
        if (IS.isSpecial(guide)) {
            for (AsmNumberFormat n : numberFormats) {
                if (src.startsWith(n.prefix)) {
                    return true;
                }
            }
            if (src.charAt(0) == '%' || src.charAt(0) == '$') {
                return true;
            } else if ("0123456789-".indexOf(src.charAt(0)) >= 0) {
                return true;
            } else if (IS.isValidLabel(src)) {
                return true;
            } else {
                if ((src.charAt(0) == '\"' && src.charAt(src.length() - 1) == '\"') || (src.charAt(0) == '\'' && src.charAt(src.length() - 1) == '\'')) {
                    String content = src.substring(1, src.length() - 1);
                    Integer[] chars = unescString(content);
                    if (chars.length != 1) {
                        throw new CompilerError(line, "Character constant must contain exactly one character!");
                    }
                    return true;
                }
                return false;
            }
        } else {
            return guide.equalsIgnoreCase(src);
        }
    }

    public boolean isComment(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t') {
                return c == ';';
            }
        }
        return false;
    }

    public boolean isEmpty(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t') {
                return false;
            }
        }
        return true;
    }

    public void printArray(String[] s) {
        for (int i = 0; i < s.length; i++) {
            if (s[i] == null) {
                print("null");
            } else {
                print("\"" + s[i] + "\"");
            }
            if (i < s.length - 1) {
                print(", ");
            }
        }
        println();
    }

    public Object[] append(Object[] array, Object obj) {
        array = (Object[]) expand(array, array.length + 1);
        array[array.length - 1] = obj;
        return array;
    }

    public String[] getSubArray(String[] raw, int start, int end) {
        String[] ar1 = new String[end - start];
        for (int i = 0; i < ar1.length; i++) {
            ar1[i] = raw[i + start];
        }
        return ar1;
    }

    public String[] getSubArray(String[] raw, int start) {
        return getSubArray(raw, start, raw.length);
    }

    public int resolveExpression(String[] tokens, Map<String, Integer> labels, int line, int format, int labelFormat, AsmNumberFormat[] numberFormats) throws CompilerError {
        print("Resolving expression \"");
        for (int i = 0; i < tokens.length; i++) {
            print(tokens[i]);
            if (i < tokens.length - 1) {
                print(" ");
            }
        }
        println("\"...");
        Expression e = new Expression(tokens);
        e.prep(labels, line);
        return e.resolve(labels, line, format, labelFormat, numberFormats);
    }


    public int readVal(String raw, Map<String, Integer> labels, int line, int format, int labelformat, AsmNumberFormat[] numberFormats) throws CompilerError {
        String decc = "-0123456789";
        String octc = "01234567";
        if (raw.charAt(0) == '\"' || raw.charAt(0) == '\'') {
            Integer[] arr = unescString(raw.substring(1, raw.length() - 1));
            if (arr.length != 1) {
                throw new CompilerError(line, "A character constant was expected!");
            }
            return arr[0];
        }
        for (AsmNumberFormat n : numberFormats) {
            if (raw.startsWith(n.prefix)) {
                return Integer.parseInt(raw, n.base);
            }
        }
        if (raw.charAt(0) == '$') {
            return readHex(raw.substring(1), line);
        } else if (raw.charAt(0) == '%') {
            return readBin(raw.substring(1), line);
        } else if ((raw.charAt(raw.length() - 1) == 'o' || raw.charAt(raw.length() - 1) == 'q') && octc.indexOf(raw.charAt(0)) != -1) {
            return readOct(raw.substring(0, raw.length() - 1), line);
        } else if (decc.indexOf(raw.charAt(0)) >= 0) {
            return readDec(raw, line);
        } else if (labels != null && (raw.charAt(0) == '<' || raw.charAt(0) == '>') && labels.containsKey(raw.substring(1)) && format * 2 == labelformat) {
            int labas = labels.get(raw.substring(1));
            switch (raw.charAt(0)) {
                case ('<'):
                    return labas & 0xff;
                case ('>'):
                    return (labas >> 8) & 0xff;
                default:
                    throw new CompilerError(line, "Expected \"<" + raw.substring(1) + "\" OR \">" + raw.substring(1) + "\", got \"" + raw + "\"");
            }
        } else if (labels != null && labels.containsKey(raw)) {
            int i = labels.get(raw);
            if (i > Math.pow(2, format)) {
                throw new CompilerError(line, "Label out of range: \"" + raw + "\" " + i + " > " + (int) Math.pow(2, format));
            }
            return i;
        } else {
            throw new CompilerError(line, "Invalid label or literal \"" + raw + "\"");
        }
    }


    public void verify(String lib, String raw, int line) throws CompilerError {
        lib = lib.toLowerCase();
        raw = raw.toLowerCase();
        for (int i = 0; i < raw.length(); i++) {
            int l = lib.indexOf(raw.charAt(i));
            if (l < 0) {
                throw new CompilerError(line, "Invalid literal " + raw + " here >" + raw.charAt(i) + "<");
            }
        }
    }

    public int readHex(String raw, int line) throws CompilerError {
        verify("0123456789abcdef", raw, line);
        return unhex(raw);
    }

    public int readBin(String raw, int line) throws CompilerError {
        verify("01", raw, line);
        return unbinary(raw);
    }

    public int readOct(String raw, int line) throws CompilerError {
        String oct = "01234567";
        verify(oct, raw, line);
        int ret = 0;
        for (int i = 0; i < raw.length(); i++) {
            int l = oct.indexOf(raw.charAt(i));
            ret = (ret << 3) | l;
        }
        return ret;
    }

    public int readDec(String raw, int line) throws CompilerError {
        if (raw.charAt(0) == '-') {
            if (raw.length() < 2) {
                throw new CompilerError(line, "Invalid literal " + raw + " here >" + raw.charAt(0) + "<");
            }
            verify("0123456789", raw.substring(1), line);
        } else {
            verify("0123456789", raw, line);
        }
        return Integer.parseInt(raw);
    }

    public byte[] dumpToSave;

    public void reportMem(byte[] buffer) {
        if (saveMemDump) {
            dumpToSave = buffer;
            selectOutput("Save memory dump...", "saveMemDump", null, asm);
            saveMemDump = false;
        }
    }

    public void saveMemDump(File file) {
        if (file != null) {
            saveOut(dumpToSave, file.getAbsolutePath());
        }
    }

}
