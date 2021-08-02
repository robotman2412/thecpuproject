package assembler;

import jdk.nashorn.internal.runtime.regexp.joni.exception.SyntaxException;

public class WordFormat {
	
	public boolean isInOpcode;
	/** In opcode only! */
	public int selfStart;
	/** In opcode only! */
	public int selfEnd;
	/** In opcode only! */
	public int opcodeStart;
	/** In opcode only! */
	public int opcodeEnd;
	/** In opcode only! */
	public int numBits;
	
	public int numWords;
	public boolean isBigEndian;
	public int maxVal;
	public int numWordsOut;
	public boolean isRelative;
	public boolean isSigned;
	public WordFormat(int wordBits, int mNumWords, boolean mIsBigEndian) {
		numWordsOut = numWords = mNumWords;
		isBigEndian = mIsBigEndian;
		maxVal = (int)Math.pow(2, wordBits * mNumWords);
	}
	public WordFormat(int wordBits, String format) {
		assert(format.matches("([0-9]+)(,(LittleEndian|BigEndian)((\\+([0-9]+))?)?(,(Signed|Unsigned))?)"));
		int i = format.indexOf(',');
		int max = format.lastIndexOf(',');
		if (max == -1 || max == i) {
			max = format.length();
		}
		numWordsOut = numWords = Integer.parseUnsignedInt((i >= 0) ? format.substring(0, i) : format);
		if (numWords > 1) {
			if (i == -1) {
				throw new SyntaxException("Expected \"LittleEndian\" or \"BigEndian\"");
			}
			isBigEndian = format.substring(i + 1, max).matches("(BigEndian)(\\+[0-9]+)?");
		}
		else if (numWords < 1) {
			throw new SyntaxException("Number of words must be at least 1");
		}
		if (format.matches("([0-9]+)(,(LittleEndian|BigEndian)(\\+([0-9]+)).*)")) {
			int m = format.substring(i + 1).indexOf('+');
			numWordsOut = Integer.parseInt(format.substring(i + m + 2, max));
			if (numWordsOut < 0) {
				throw new SyntaxException("Number of words out must be at least 1");
			}
		}
		isSigned = format.matches("([0-9]+)(,(LittleEndian|BigEndian)((\\+([0-9]+))?)?,Signed)");
		maxVal = (int)Math.pow(2, wordBits * numWords);
	}
	
	public WordFormat (String format) { //in opcode only
		assert(format.matches("[0-9]+,[0-9]+to[0-9]+->[0-9]+to[0-9]+"));
		isInOpcode = true;
		int i0 = format.indexOf(',');
		int i1 = format.indexOf("to");
		int i2 = format.indexOf("->");
		int i3 = format.indexOf("to", i2 + 1);
		//numBits =
	}

	/**
	 * @param in the address denoted in the assembly source
	 * @param addressFrom the address directly after the relative addressing words
	 * @return the address to put in the program
	 */
	public long getOut(long in, long addressFrom) {
		if (isRelative) {
			return in - addressFrom;
		}
		else
		{
			return in;
		}
	}
}
