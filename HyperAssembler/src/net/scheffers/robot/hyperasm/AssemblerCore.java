package net.scheffers.robot.hyperasm;

import com.sun.istack.internal.Nullable;
import net.scheffers.robot.hyperasm.exception.CompilerError;
import net.scheffers.robot.hyperasm.exception.CompilerSyntaxError;
import net.scheffers.robot.hyperasm.exception.CompilerWarning;
import net.scheffers.robot.hyperasm.importing.DirectoryImportSupplier;
import net.scheffers.robot.hyperasm.importing.ImportSupplier;
import net.scheffers.robot.hyperasm.importing.NopImportSupplier;
import net.scheffers.robot.hyperasm.isa.InstructionDef;
import net.scheffers.robot.hyperasm.isa.InstructionSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AssemblerCore {
	
	public static void main(String[] args) {
		InstructionSet isa = new InstructionSet();
		isa.wordBits = 8;
		isa.instructions = new InstructionDef[] {
				InstructionDef.singleWordSingleArg(0x01, new String[] {"load", "%"}),
				InstructionDef.singleWordDoubleArg(0x02, true, new String[] {"store", "%"}),
				InstructionDef.singleWord(0xff, new String[] {"halt"}),
				InstructionDef.singleWord(0x03, new String[] {"copy", "your", "to", "ass"})
		};
		isa.recalcMap();
		try {
			simpleFullAssemble("D:\\!intellij\\gr8cpu\\HyperAssembler\\testingofassembly.asm", isa);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String[] tokeniseLine(String raw) {
		raw = raw.replaceAll(" ", "\t");
		StringBuilder s = new StringBuilder();
		String[] splitTokens = "# @ + - * = / , ( )".split(" ");
		boolean inString = false;
		boolean isDoubleQuotes = false;
		boolean isEscaped = false;
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
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
			} else if (c == ';') {
				break; //comment, ignore the rest
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
								s.append('\t');
							}
							s.append(c).append("\t");
							break;
						}
					}
				}
			}
			if (!splitted) {
				s.append(c);
			}
		}
		raw = s.toString();
		//remove duplicate whitespace
		s = new StringBuilder();
		boolean b = false;
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (!b || c != '\t') {
				s.append(c);
			}
			b = c == '\t';
		}
		raw = s.toString();
		//remove trailing whitespace
		if (raw.endsWith("\t")) {
			raw = raw.substring(0, raw.length() - 1);
		}
		return raw.split("\t");
	}
	
	public static Pass2Out simpleFullAssemble(String fileName, InstructionSet instructionSet) throws IOException {
		DirectoryImportSupplier supplier = new DirectoryImportSupplier(System.getProperty("user.dir"));
		File res = supplier.resolveFile(fileName);
		if (res == null) {
			throw new FileNotFoundException(fileName);
		}
		supplier.setDir(res.getParent());
		String[] rawLines = supplier.getStringFile(fileName).split("\\r\\n|\\r|\\n");
		String[][] tokensedLines = new String[rawLines.length][];
		for (int i = 0; i < rawLines.length; i++) {
			tokensedLines[i] = tokeniseLine(rawLines[i]);
		}
		Pass0Out pass0 = pass0(tokensedLines, supplier, fileName);
		Pass1Out pass1 = pass1(pass0, instructionSet);
		return pass2(pass1, instructionSet);
	}
	
	/**
	 * Prepares the assembly file for assembling.
	 * Does imports and metadata.
	 * @param tokensIn the tokenised assembly file
	 * @param importSupplier the import supplier in case the file needs to import something, or null if importing is disabled
	 * @return metadata and tokens needed to start pass 1 of assembling
	 */
	@SuppressWarnings("SuspiciousListRemoveInLoop")
	public static Pass0Out pass0(String[][] tokensIn, @Nullable ImportSupplier importSupplier, String sourceNameIn) {
		if (importSupplier == null) {
			importSupplier = new NopImportSupplier();
		}
		List<String[]> tokensOut = new ArrayList<>(Arrays.asList(tokensIn));
		List<String> sourceNamesOut = new ArrayList<>(tokensOut.size());
		List<Integer> lineNumbersOut = new ArrayList<>(tokensOut.size());
		List<CompilerError> errors = new ArrayList<>();
		List<CompilerWarning> warnings = new ArrayList<>();
		boolean removePrefixPadding = false;
		boolean importsLeft = false;
		//boolean removeSuffixPadding = false;
		
		//"import" the mail assembly file
		for (int i = 0; i < tokensOut.size(); i++) {
			sourceNamesOut.add(sourceNameIn);
			lineNumbersOut.add(i + 1);
		}
		
		//import assembly files
		do {
			for (int i = 0; i < tokensOut.size(); i++) {
				String[] tokens = tokensOut.get(i);
				if (tokens.length == 0) {
					continue;
				}
				if (!tokens[0].equals("@")) {
					continue;
				}
				if (tokens.length < 2) {
					errors.add(new CompilerSyntaxError(sourceNamesOut.get(i), lineNumbersOut.get(i), "Expected metadata, got end of line, for line: @"));
					continue;
				}
				//we only care about importing assembly files and as such, stuff happens
				if (tokens[1].equalsIgnoreCase("include")) {
					if (tokens.length != 3 || !tokens[2].matches("^\".+\"$")) {
						errors.add(new CompilerSyntaxError(sourceNamesOut.get(i), lineNumbersOut.get(i), "@include requires one string path!"));
						continue;
					}
					String name = unescapeString(tokens[2].substring(1, tokens[2].length() - 1));
					if (!importSupplier.isFileAvailable(name)) {
						errors.add(new CompilerError(sourceNamesOut.get(i), lineNumbersOut.get(i), "Assembly file to import does not exist, for line: " + stitchTokens(tokens)));
						continue;
					}
					String[] split;
					try {
						split = importSupplier.getStringFile(name).split("\\r\\n|\\r|\\n");
					} catch (IOException e) {
						errors.add(new CompilerError(sourceNamesOut.get(i), lineNumbersOut.get(i), "could not import file", e));
						continue;
					}
					//It warns suspicious, but this is correct as more elements will be added.
					tokensOut.remove(i);
					lineNumbersOut.remove(i);
					sourceNamesOut.remove(i);
					for (int x = 0; x < split.length; x++) {
						tokensOut.add(i + x, tokeniseLine(split[x]));
						lineNumbersOut.add(i + x, x + 1);
						sourceNamesOut.add(i + x, name);
						if (split[x].toLowerCase().matches("^@[ \\t]*include")) {
							importsLeft = true;
						}
					}
				}
			}
		} while(importsLeft);
		
		//TODO: import binary files and parse other metadata
		
		//put into output
		Pass0Out out = new Pass0Out();
		out.errors = errors;
		out.warnings = warnings;
		out.removePrefixPadding = removePrefixPadding;
		int[] ints = new int[lineNumbersOut.size()];
		for (int i = 0; i < lineNumbersOut.size(); i++) {
			ints[i] = lineNumbersOut.get(i);
		}
		out.tokenLineNums = ints;
		out.tokensOut = tokensOut.toArray(new String[0][]);
		out.tokensSourceFiles = sourceNamesOut.toArray(new String[0]);
		
		return out;
	}
	
	/**
	 * First pass of assembling.
	 * Finds the instructions and labels, total length, prepares instruction arguments for pass 2.
	 * @param in the data prepared for assembling
	 * @param isa the instructionset for assembling
	 * @return data needed to finish assembling in pass 2
	 */
	public static Pass1Out pass1(Pass0Out in, InstructionSet isa) {
		long currentAddress = 0;
		int nLines = in.tokensOut.length;
		Pass1Out out = new Pass1Out(in, nLines);
		for_pass1: for (int i = 0; i < nLines; i++) {
			out.lineStartAddresses[i] = currentAddress;
			String[] line = out.tokensOut[i];
			if (line.length == 0) {
				continue;
			}
			if (line[0].length() > 0) {
				if (line[0].equals("*")) {
					//TODO: set start address
				} else if (!isa.isValidLabelName(line[0])) {
					out.errors.add(new CompilerSyntaxError(out.tokensSourceFiles[i], out.tokenLineNums[i],
							"Invalid label name, please pick another."
					));
					continue;
				} else if (line.length == 1 || !line[1].equals("=")) {
					if (out.labels.containsKey(line[0])) {
						Label label = out.labels.get(line[0]);
						label.address = currentAddress;
						if (label.lineNum != 0) {
							out.warnings.add(new CompilerWarning(out.tokensSourceFiles[i], out.tokenLineNums[i],
									"Label \"" + line[0] + "\" from file " + label.fileName + " line " + label.lineNum +
											" redefined in file " + out.tokensSourceFiles[i] + " line " + out.tokenLineNums[i] + "."
							));
						} else {
							//the label was marked exported, but not yet defined, so a warning is not in order
							label.lineNum = out.tokenLineNums[i];
						}
					} else {
						out.labels.put(line[0], new Label(out.tokensSourceFiles[i], out.tokenLineNums[i], currentAddress, false));
					}
				} else if (line.length < 3) {
					out.errors.add(new CompilerSyntaxError(out.tokensSourceFiles[i], out.tokenLineNums[i],
							"Please insert an expression."
					));
					continue;
				} else {
					//TODO: set by expr
				}
			}
			//TODO: check for data / reserve statements
			if (line.length > 1 && !line[1].equals("=")) { //check for insn
				String first = line[1];
				InstructionDef[] toCheck = isa.firstTokenMap.get(first.toLowerCase());
				if (toCheck == null) {
					out.errors.add(new CompilerSyntaxError(out.tokensSourceFiles[i], out.tokenLineNums[i],
							"Instruction not found!"
					));
					continue;
				}
				for_insnsearch: for (InstructionDef def : toCheck) {
					int indexial = 1;
					int argIndexial = 0;
					for (int x = 0; x < def.tokenPattern.length; x++) {
						if (indexial >= line.length) {
							continue for_insnsearch; //this can't be a match
						}
						out.lineInsnArgs[i] = new String[def.numArgs];
						//TODO: allow expressions in instruction
						if (def.tokenPattern[x].equals("%")) {
							//any token will pass for now, this will change with expressions in instructions
							out.lineInsnArgs[i][argIndexial] = line[indexial];
							argIndexial ++;
						}
						else if (!line[indexial].equalsIgnoreCase(def.tokenPattern[x])) {
							continue for_insnsearch; //this can't be a match
						}
						indexial ++;
					}
					//we have found a match
					out.lineInsns[i] = def;
					out.lineLengths[i] = def.numWords;
					currentAddress += def.numWords;
					continue for_pass1;
				}
				//we have found no match
				StringBuilder builder = new StringBuilder();
				for (InstructionDef def : toCheck) {
					builder.append('\n');
					builder.append(stitchTokens(def.tokenPattern, true));
				}
				out.errors.add(new CompilerSyntaxError(out.tokensSourceFiles[i], out.tokenLineNums[i],
						"Instruction not found!\nDid you mean:" + builder
				));
				continue;
			}
		}
		out.totalLength = currentAddress;
		return out;
	}
	
	/**
	 * Seconds pass of assembling.
	 * Finds the values of arguments for the instructions and constructs the final program.
	 * @param in the data from pass 1
	 * @param isa the instructionset for assembling
	 * @return the final program and data needed to build an assembly dump
	 */
	public static Pass2Out pass2(Pass1Out in, InstructionSet isa) {
		int nLines = in.tokensOut.length;
		Pass2Out out = new Pass2Out(in, nLines);
		out.wordsOut = new long[(int) out.totalLength];
		for (int i = 0; i < nLines; i++) {
			if (out.lineInsns[i] == null) {
				continue;
			}
			InstructionDef insn = out.lineInsns[i];
			String[] args = out.lineInsnArgs[i];
			if ((args == null && insn.numArgs != 0) || insn.numArgs != args.length) {
				out.errors.add(new CompilerError(out.tokensSourceFiles[i], i, "Number of arguments found do not match instruction, this is a bug."));
				continue;
			}
			long address = out.lineStartAddresses[i];
			//TODO: resolve arguments properly
			long[] insnOut = insn.getBytes(new long[insn.numArgs], isa.wordBits);
			if (insnOut == null || insn.numWords != insnOut.length || insn.numWords != out.lineLengths[i]) {
				out.errors.add(new CompilerError(out.tokensSourceFiles[i], i, "Number of words do not match instruction, this is a bug."));
				continue;
			}
			System.arraycopy(insnOut, 0, out.wordsOut, (int) address, insn.numWords);
		}
		return out;
	}
	
	/**
	 * Stitches back together an already tokenised string, for user readability in assembly dumps and warnings.
	 * @param tokens the tokens to stitch
	 * @return the stitched tokens
	 */
	public static String stitchTokens(String[] tokens) {
		return stitchTokens(tokens, false);
	}
	
	/**
	 * Stitches back together an already tokenised string, for user readability in assembly dumps and warnings.
	 * @param tokens the tokens to stitch
	 * @param isInsnPattern whether or not to inperpret this as an instruction token format pattern   
	 * @return the stitched tokens
	 */
	public static String stitchTokens(String[] tokens, boolean isInsnPattern) {
		StringBuilder out = new StringBuilder();
		boolean skip = true;
		for (String s : tokens) {
			if (!skip && !s.equals(",")) {
				out.append(' ');
			}
			skip = s.equals("#") || s.length() == 0;
			if (isInsnPattern && s.equals("%")) {
				out.append("...");
			}
			else
			{
				out.append(s);
			}
		}
		return out.toString();
	}
	
	public static String unescapeString(String escaped) {
		char[] chars = escaped.toCharArray();
		int i = 0;
		StringBuilder produced = new StringBuilder();
		while (i < chars.length) {
			char c = chars[i];
			if (c == '\\') {
				if (i >= chars.length - 1) {
					produced.append(c);
					System.err.println("String escape sequence at end of string!");
					return produced.toString();
				}
				char esx = chars[i + 1];
				switch (esx) {
					//region simple_esx
					case('0'):
						produced.append('\0');
						break;
					case('n'):
						produced.append('\n');
						break;
					case('r'):
						produced.append('\r');
						break;
					case('f'):
						produced.append('\f');
						break;
					case('\\'):
						produced.append('\\');
						break;
					case('\''):
						produced.append('\'');
						break;
					case('\"'):
						produced.append('\"');
						break;
					case('t'):
						produced.append('\t');
						break;
					case('b'):
						produced.append('\b');
						break;
					//endregion simple_esx
					case('u'):
						if (i >= chars.length - 5) {
							System.err.println("Not enough characters left for unicode escape sequence!");
							produced.append("\\u");
							break;
						}
						break;
				}
				i ++;
			}
			else
			{
				produced.append(c);
			}
			i ++;
		}
		return produced.toString();
	}
	
}
