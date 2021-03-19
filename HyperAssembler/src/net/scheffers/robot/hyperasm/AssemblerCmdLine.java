package net.scheffers.robot.hyperasm;

import jutils.JUtils;
import net.scheffers.robot.hyperasm.exception.CompilerError;
import net.scheffers.robot.hyperasm.exception.CompilerWarning;
import net.scheffers.robot.hyperasm.isa.InstructionSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class AssemblerCmdLine {
	
	public static void main(String[] args) {
		JUtils.getArgs(args);
		
		String inPath = getArg("in", "-in", "i");
		String isaPath = getArg("isa", "-isa", "s");
		String outPath = getArg("out", "-out", "o");
		String charsetName = getArg("charset", "-charset");
		if (charsetName == null) charsetName = "IBM437";
		boolean noDump = getArg("nodump", "-nodump", "D") != null;
		boolean verbose = getArg("verbose", "-verbose", "V") != null;
		boolean mkExe = getArg("gexe", "-gexe", "G") != null;
		
		Charset charset;
		try {
			charset = Charset.forName(charsetName);
		} catch (Exception e) {
			charset = Charset.defaultCharset();
			System.err.println("No such charset \"" + charsetName + "\" found, defaulting to \"" + charset.name() + "\".");
		}
		
		if (inPath == null || isaPath == null || outPath == null) {
			getHelpYouFuckwit();
			return;
		}
		
		InstructionSet isa;
		try {
			isa = new InstructionSet(new File(isaPath), verbose);
		} catch (Exception e) {
			System.err.println("Error reading instruction set!");
			e.printStackTrace();
			return;
		}
		
		Pass2Out out;
		try {
			System.out.println("Assembling \"" + inPath + "\"...");
			System.out.println();
			out = AssemblerCore.simpleFullAssemble(inPath, isa, charset);
			if (!out.warnings.isEmpty()) {
				System.out.println("Warnings:");
				for (CompilerWarning warning : out.warnings) {
					System.out.println(warning.source);
					System.out.println(warning.getMessage());
					System.out.println();
				}
			}
			if (!out.errors.isEmpty()) {
				System.err.println("Errors:");
				for (CompilerError error : out.errors) {
					System.err.println(error.source);
					System.err.println(error.getMessage());
					System.err.println();
				}
			}
		} catch (IOException e) {
			System.err.println("Could not access input file(s)!");
			e.printStackTrace();
			return;
		}
		
		try {
			if (mkExe) {
				System.out.println("Saving gr8nix object file to \"" + outPath + "\"...");
				byte[] prg = new byte[out.wordsOut.length - (int)out.paddingLength];
				for (int i = 0; i < prg.length; i++) {
					prg[i] = (byte) out.wordsOut[i + (int) out.paddingLength];
				}
				mkGR8NIXBinary(new File(outPath), out, prg);
			}
			else if (outPath.matches("^.*?\\.(hex|lhf)$")) {
				System.out.println("Saving HEX file to \"" + outPath + "\"...");
				out.saveLHF(new File(outPath));
			}
			else
			{
				System.out.println("Saving binary file to \"" + outPath + "\"...");
				out.saveRaw(new File(outPath));
			}
			if (!noDump) {
				System.out.println("Saving dump file to \"" + outPath + ".dump\"...");
				out.saveOldDump(new File(outPath + ".dump"));
			}
		} catch (IOException e) {
			System.err.println("Could not write output!");
			e.printStackTrace();
		}
	}
	
	public static void mkGR8NIXBinary(File file, Pass2Out asm, byte[] prg) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		long startAddress = asm.paddingLength;
		if (prg.length > 65535) {
			System.err.printf("Program is too long!\n%s bytes / 65536 bytes.\n", prg.length);
			return;
		}
		else
		{
			System.out.printf("Program length: %s bytes / 65536 bytes.\n", prg.length);
		}
		
		byte[] bin = {
				'g', 'r', '8', 'o', // Magic.
				(byte) 0x01, // Type: Statically linked executable.
				(byte) 0x01, // Num sections: 1.
				// Section:
					(byte) 0x07, // Type: RWX.
					(byte) 0x00, // Offset.
					(byte) 0x00,
					0, 0, // File offset.
					(byte) (prg.length & 0xff), // Length.
					(byte) (prg.length >>> 8),
				(byte) 0x00, // Num symbols: 0.
				(byte) 0x00,
				// No symbols.
				(byte) 0x00, // Num external symbols: 0.
				(byte) 0x00,
				// No external symbols.
		};
		int fileOffset = bin.length;
		bin[9] = (byte) (fileOffset & 0xff);
		bin[10] = (byte) (fileOffset >>> 8);
		// Headers.
		out.write(bin);
		// Main section data.
		out.write(prg);
		
		// Finish up.
		out.flush();
		out.close();
	}
	
	public static void getHelpYouFuckwit() {
		System.out.println("Options:");
		System.out.println("    --in       -i   Path to the input file.");
		System.out.println("    --isa      -s   Path to the instruction set definition.");
		System.out.println("    --in       -o   Path to the output file.");
		System.out.println("    --charset       Name of the character set to use, default \"IBM437\".");
		System.out.println("    --nodump   -D   Do not save an assembly dump along with the output file.");
		System.out.println("    --verbose  -V   Assemble the program with the verbose instruction set.");
		System.out.println("    --gexe     -G   Format the output as a GR8NIX executable object file.");
	}
	
	public static String getArg(String... options) {
		for (String s : options) {
			String fuck = JUtils.getArg(s);
			if (!fuck.equals("null")) {
				return fuck;
			}
		}
		return null;
	}
	
}
