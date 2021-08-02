package assembler.customs;

import assembler.Assembler;
import assembler.InstructionSet;

public class AminoIS extends InstructionSet {

	public AminoIS() {
		instructions = new String[][] {
				Assembler.tokeniseGuide("mov %src%, to %dest%"), //64
				Assembler.tokeniseGuide("add to %dest%"), //56
				Assembler.tokeniseGuide("sub to %dest%"), //48
				Assembler.tokeniseGuide("nand to %dest%"), //40
				Assembler.tokeniseGuide("nor to %dest%"), //32
				Assembler.tokeniseGuide("xor to %dest%"), //24
				Assembler.tokeniseGuide("shift R, %dest%"), //16
				//13, 14, 15 are invalid
				Assembler.tokeniseGuide("if flag"), //11
				Assembler.tokeniseGuide("comp"), //10
				Assembler.tokeniseGuide("in"), //9
				Assembler.tokeniseGuide("out"), //8
				Assembler.tokeniseGuide("imm %data%") //0
		};
		addresses = new int[] {64, 56, 48, 40, 32, 24, 16, 11, 10, 9, 8, 0};
		lengths = new int[]   {1,  1,  1,  1,  1,  1,  1,  1,  1,  1, 1, 2};
		maxVal = 255;
		noLabelChars = "!@#$%^&*-=+[]{}(),./<>?'\"\\;:".toCharArray();
		noLabelStrings = new String[] {
				"mov",
				"add",
				"sub",
				"nand",
				"nor",
				"xor",
				"shift",
				"if",
				"comp",
				"in",
				"out",
				"imm",
				"R",
				"to",
				"flag"
		};
		wordBits = 8;
		instWords = 1;
		addressingBits = 8;
	}

	@Override
	public boolean isSpecial(String token) {
		return token.equals("%src%") || token.equals("%dest%") || token.equals("%data%");
	}

	@Override
	public int calcLength(String[] tokens) {
		return tokens[0].equalsIgnoreCase("imm") ? 2 : 1;
	}

}
