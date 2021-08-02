package assembler;

import java.util.HashMap;
import java.util.Map;

public class InstructionSet {
	
	public String[][] instructions;
	public int[] lengths;
	public int[] addresses;
	public char[] noLabelChars;
	public String[] noLabelStrings;
	public int wordBits;
	public int addressingBits;
	public int instWords;
	public int maxVal;
	public Map<String, WordFormat> special;
	public AsmNumberFormat[] numberFormats;
	
	public InstructionSet() {
		instructions = new String[0][0];
		addresses = new int[0];
		lengths = new int[0];
		special = new HashMap<>();
		noLabelChars = new char[0];
		noLabelStrings = new String[0];
	}

	public boolean isValidLabel(String label) {
		if ("1234567890-".indexOf(label.charAt(0)) != -1) {
			return false;
		}
		for (char c : label.toCharArray()) {
			for (char mC : noLabelChars) {
				if (c == mC) {
					return false;
				}
			}
		}
		for (String s : noLabelStrings) {
			if (s.equalsIgnoreCase(label)) {
				return false;
			}
		}
		return true;
	}
	
	public void addInstruction(String line) {
		String[] split = split(Assembler.collapse(line), " ");
		instructions = (String[][]) Assembler.expand(instructions, instructions.length + 1);
		instructions[instructions.length - 1] = Assembler.tokeniseGuide(split(line, "\"")[1]);
		addresses = Assembler.expand(addresses, addresses.length + 1);
		addresses[addresses.length - 1] = Assembler.unhex(split[0]);
		lengths = Assembler.expand(lengths, lengths.length + 1);
		lengths[lengths.length - 1] = calcLength(instructions[instructions.length - 1]);
		Assembler.print("$" + String.format("%02x", addresses[addresses.length - 1], 2) + ": ");
		System.out.println(Assembler.join(Assembler.tokeniseGuide(split(line, "\"")[1]), " "));
	}
	
	public int calcLength(String[] tokens) {
		int len = 1;
		for (String s : tokens) {
			if (s.matches("%([a-zA-Z0-9]++)%") && special.containsKey(s.substring(1, s.length() - 1))) {
				len += special.get(s.substring(1, s.length() - 1)).numWordsOut;
			}
		}
		return len;
	}
	
	public static String[] split(String raw, String regex) {
		return raw.split(regex);
	}

	public boolean isSpecial(String token) {
		System.out.println(token);
		if (token.length() < 3 || token.indexOf("%") != 0 || token.lastIndexOf("%") != token.length() - 1) {
			return false;
		}
		else
		{
			return special.containsKey(token.substring(1, token.length() - 1));
		}
	}
}

