package assembler.customs;

import assembler.*;
import jutils.gui.Button;
import processing.core.PApplet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssemblerForAmino extends Assembler {

	public static void main(String[] args) {
		PApplet.main(AssemblerForAmino.class.getName());
	}

	public AssemblerForAmino() {
		showAsmg = false;
		doFlasher = false;
		asmgPath = "Built-in for Aminotreal and DemRedstoneOp's Serial CPU.";
		IS = new AminoIS();
	}

	@Override
	public void setup() {
		super.setup();
		compile = new Button(this, 5, 95, 60, 20, "compile", false, () -> {
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
		});
		surface.setTitle("Assembler for Aminotreal and DemRedstoneOp's serial CPU by RobotMan2412");
		surface.setResizable(true);
	}

	@Override
	public void compileProgram(String[] file, InstructionSet IS) throws CompilerError {
		int maxWords = (int)Math.pow(2, IS.addressingBits);
		progEnabled = true;
		progText = "Assembling program: Tokenizing...";
		println("----TOKENISING----");
		progress = 0;
		int adrBits = IS.addressingBits;
		boolean padding = true;
		String[][] tokens = new String[file.length][];
		for (int i = 0; i < file.length; i++) {
			if (!isEmpty(file[i]) && !isComment(file[i])) {
				tokens[i] = tokenise(file[i]);
				printArray(tokens[i]);
			}
			else
			{
				tokens[i] = null;
			}
			progress = ((float)i) / file.length / 3f;
		}



		println("----INTERPRETING ASSEMBLER INSTRUCTIONS----");
		progText = "Interpreting assembler instructions...";
		List<ImportedFile> imported = new ArrayList<ImportedFile>();
		for (int i = 0; i < tokens.length; i++) {
			progText = "Interpreting assembler instructions...";
			progress = ((float)i) / tokens.length;
			if (tokens[i] != null && tokens[i][0].equals("@")) {
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
				}
				else if (tokens[i][1].equals("no_padding")) {
					padding = false;
					tokens[i] = null;
				}
			}
		}



		println("----IMPORTING----");
		progText = "importing...";
		int importedLineOffset = 0;
		for (int i = 0; i < imported.size(); i++) {
			tokens = imported.get(i).insertTokens(tokens, imported.get(i).line + importedLineOffset);
			importedLineOffset += imported.get(i).tokens.length;
			progress = ((float)i + 1) / (imported.size());
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
				}
				else if (!tokens[i][0].equals("")) {
					if (labels.containsKey(tokens[i][0])) {
						println("Warning: label \"" + tokens[i][0] + "\" re-assigned!");
					}
					int labelAddress;
					if (tokens[i].length > 1 && tokens[i][1].equals("=")) {
						if (tokens[i].length < 2) {
							throw new CompilerError(i, "Missing EXPRESSION!");
						}
						labelAddress = resolveExpression(getSubArray(tokens[i], 2), labels, i, adrBits, adrBits, IS.numberFormats);
					}
					else if (!IS.isValidLabel(tokens[i][0])){
						throw new CompilerError(i, "Invalid label name! please pick another one.");
					}
					else
					{
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
						for (int x = 2; x < tokens[i].length; x ++) { //this needs to be improved later on
							if (!tokens[i][x].equals(",")) {
								if (!tokens[i][x].startsWith("\"") && !tokens[i][x].startsWith("\'") && !did) {
									address ++;
									did = true;
								}
							}
							else
							{
								did = false;
							}
							if (tokens[i][x].startsWith("\"") || tokens[i][x].startsWith("\'")) {
								println("String length: " + unescString(tokens[i][x].substring(1, tokens[i][x].length() - 1)).length);
								address += unescString(tokens[i][x].substring(1, tokens[i][x].length() - 1)).length;
							}
						}
					}
					else if (tokens[i][1].equals("reserve")) {
						//reserve a bunch of bytes
						if (tokens[i].length != 3) {
							throw new CompilerError(i, "Expected reserve (num. bytes), got \"reserve\"");
						}
						address += readVal(tokens[i][2], labels, i, adrBits, adrBits, IS.numberFormats);
					}
					else if (!tokens[i][1].equals("=")) { //we must ignore label definitions
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
			progress = ((float)i) / (tokens.length - 1f) / 3f + 1f / 3f;
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
			}
			else
			{
				dump[i] = new AssemblyDump();
				dump[i].startAddress = address;
				dump[i].tokens = tokens[i];
				if (!isInst[i]) {
					//check data statements and garbages
					if (tokens[i].length > 1 && (tokens[i][1].equals("data") || tokens[i][1].equals("byte") || tokens[i][1].equals("bytes"))) {
						int tknStart = 2;
						for (int x = 2; x < tokens[i].length; x ++) { //this needs to be improved later on
							String[] mTokens = null;
							if (tokens[i][x].equals(",")) {
								mTokens = getSubArray(tokens[i], tknStart, x);
								tknStart = x + 1;
							}
							else if (x == tokens[i].length - 1) {
								mTokens = getSubArray(tokens[i], tknStart);
								tknStart = x + 1;
							}
							if (mTokens != null) {
								if (mTokens[0].startsWith("\"") || mTokens[0].startsWith("\'")) {
									Integer[] chars = unescString(mTokens[0].substring(1, mTokens[0].length() - 1));
									print("String: ");
									for (int y = 0; y < chars.length; y++) {
										print(((byte)(int)chars[y]) + " ");
										compiled[address] = (byte)(int)chars[y];
										address ++;
									}
								}
								else
								{
									compiled[address] = resolveExpression(mTokens, labels, i, IS.wordBits, adrBits, IS.numberFormats);
									address ++;
								}
							}
						}
					}
					else if (tokens[i].length >= 2 && tokens[i][1].equalsIgnoreCase("reserve")) {
						//reserve a bunch of bytes
						if (tokens[i].length != 3) {
							throw new CompilerError(i, "Expected reserve (num. bytes), got \"reserve\"");
						}
						address += readVal(tokens[i][2], labels, i, adrBits, adrBits, IS.numberFormats);
					}
					else if (tokens[i][0].equals("*")) {
						address = resolveExpression(getSubArray(tokens[i], 2), labels, i, adrBits, adrBits, IS.numberFormats);
					}
				}
				else
				{
					int inst = instructions[i];
					compiled[address] = IS.addresses[inst];
					address ++;
					for (int x = 0; x < IS.instructions[inst].length; x++) {
						String s = IS.instructions[inst][x];
						switch (s) {
							case "%src%":
								compiled[address - 1] |= readVal(tokens[i][x + 1], labels, i, IS.addressingBits, IS.addressingBits, IS.numberFormats) << 3;
								break;
							case "%dest%":
								compiled[address - 1] |= readVal(tokens[i][x + 1], labels, i, IS.addressingBits, IS.addressingBits, IS.numberFormats);
								break;
							case "%data%":
								compiled[address] = readVal(tokens[i][x + 1], labels, i, IS.wordBits, IS.addressingBits, IS.numberFormats);
								address++;
								break;
						}
//						if (s.charAt(0) == '%') {
//							WordFormat spec = IS.special.get(s.substring(1, s.length() - 1));
//							int val = readVal(tokens[i][x + 1], labels, i, spec.numWords * IS.wordBits, adrBits);
//							if (spec.numWordsOut > 1) {
//								int actual = (int)spec.getOut(val, address + spec.numWordsOut);
////								compiled[address] = val & 0xff;
////								compiled[address + 1] = (val >> 8) & 0xff;
//								for (int y = 0; y < spec.numWordsOut; y++) {
//									compiled[address + y] = (actual >> (8 * y)) & 0xff;
//								}
//								address += spec.numWordsOut;
//							}
//							else
//							{
//								compiled[address] = val;
//								address ++;
//							}
////							compiled[address] = readVal(tokens[i][x + 1], labels, i);
////							address ++;
//						}
					}
				}
				dump[i].endAddress = address;
				dump[i].fetchCompiled(compiled);
			}
			progress = ((float)i) / ((float)tokens.length - 1) / 3f + 2f / 3f;
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
		for (Map.Entry<String, Integer> e : labels.entrySet()) {
			if (e.getKey().length() >= labelIndent) {
				labelIndent = e.getKey().length() + 1;
			}
		}
		for (Map.Entry<String, Integer> e : labels.entrySet()) {
			String s = e.getKey();
			while (s.length() < labelIndent) {
				s += " ";
			}
			long a = (long) Math.ceil((float)IS.wordBits / 4f);
			s += "$" + String.format("%0" + a + "x", e.getValue());
			assemblyDump.add(s);
		}
		assemblyDump.add("");
		assemblyDump.add("Program length: " + address + " / " + maxWords + " bytes (" + (address / (float)maxWords * 100f) + "% of maximum size)");
		saveStrings(outPath + ".dump", assemblyDump.toArray(new String[0]));



		println("Padding: " + firstAddress);
		println("Final address: " + address);
		if (address > maxWords) {
			throw new CompilerError(0, "The program is too long! " + address + " words out of " + 256 + " supported words long");
		}
		byte[] tmp;
		int progsize;
		if (padding) {
			tmp = new byte[progsize = address];
			for (int i = 0; i < address; i++) {
				tmp[i] = (byte) compiled[i];
			}
		}
		else
		{
			tmp = new byte[progsize = address - firstAddress];
			for (int i = 0; i < progsize; i++) {
				tmp[i] = (byte) compiled[i + firstAddress];
			}
		}
		println("Done!");
		progText = "Saving...";
		saveOut(tmp, outPath);
		progText = "Done! Program uses " + (Math.round(tmp.length / (float)maxWords * 1000f) / 10f) + "% of memory (" + progsize + " words).";
	}

}
