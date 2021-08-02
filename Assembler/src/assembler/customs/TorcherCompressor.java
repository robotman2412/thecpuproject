package assembler.customs;

import java.util.ArrayList;


public class TorcherCompressor {

	/**
	 * @author RobotMan2412
	 */
	public static ArrayList<String> compressBits(int[] words, int bitsPerWord) {
		boolean[] bits = new boolean[words.length * bitsPerWord];
		for (int i = 0; i < words.length; i++) {
			for (int l = 0; l < bitsPerWord; l++) {
				bits[i * bitsPerWord + l] = (words[i] >> l & 1) > 0;
			}
		}
		return compressBits(bits);
	}

	/**
	 * @author Ecconia: ecconia.de
	 */
	public static ArrayList<String> compressBits(boolean[] bits) {
		final int maxBits = 15;

		ArrayList<Integer> chars = new ArrayList<Integer>();
		int tmpNumberBitPos = 0;
		int tmpNumber = 0;

		for (boolean bit : bits) {
			if (tmpNumberBitPos == maxBits) {
				chars.add(tmpNumber);
				tmpNumber = 0;
				tmpNumberBitPos = 0;
			}

			tmpNumber |= bit ? (1 << tmpNumberBitPos) : 0;
			tmpNumberBitPos++;
		}
		chars.add(tmpNumber);

		ArrayList<String> commands = new ArrayList<>();

		String commandBase = "/torcher b ";
		int character = commandBase.length();
		String output = commandBase;
		for (Integer integer : chars) {
			if (character == 100) {
				character = commandBase.length();
				commands.add(output);
				output = commandBase;
			}
			output += (char) ((int) integer + 256);
			character++;
		}
		commands.add(output);
		return commands;
	}

}
