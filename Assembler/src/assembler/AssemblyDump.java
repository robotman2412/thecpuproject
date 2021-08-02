package assembler;

public class AssemblyDump {
	
	public int startAddress;
	public int endAddress;
	public String[] tokens;
	public int[] compiled;
	
	String addressStr;
	String compiledStr;
	String tokenStr;
	String labelStr;
	String instStr;
	
	public AssemblyDump() {
		
	}
	
	public void fetchCompiled(int[] array) {
		compiled = new int[endAddress - startAddress];
		for (int i = 0; i < compiled.length; i++) {
			compiled[i] = array[startAddress + i];
		}
	}
	
	public void genStrings(int adrBits, int wordBits) {
		int adrNibbles = (int)Math.ceil(adrBits / 4f);
		int wordNibbles = (int)Math.ceil(wordBits / 4f);
		addressStr = "$" + String.format("%0" + adrNibbles + "x", startAddress);
		if (endAddress > startAddress) {
			addressStr += " - $" + String.format("%0" + adrNibbles + "x", endAddress - 1);
		}
		addressStr += "  ";
		tokenStr = "";
		labelStr = tokens[0] + "   ";
		instStr = "";
		if (tokens.length > 1) {
			instStr = tokens[1] + "   ";
		}
		for (int i = 2; i < tokens.length; i++) {
			tokenStr += tokens[i] + " ";
		}
		compiledStr = "";
		for (int i : compiled) {
			compiledStr += String.format("%0" + wordNibbles + "x", i) + " ";
		}
	}
	
	public String getString(int[] indentation) {
		indentation = checkIndentation(indentation);
		return assureIndentation(addressStr, indentation[0]) + assureIndentation(labelStr, indentation[2]) + assureIndentation(instStr, indentation[3]) + assureIndentation(tokenStr, indentation[1]) + compiledStr;
	}
	
	private String rep(String rep, int max) {
		String s = "";
		for (int i = 0; i < max; i++) {
			s += rep;
		}
		return s;
	}
	
	private String assureIndentation(String s, int indent) {
		return s.length() <= indent ? s + rep(" ", indent - s.length()) : s;
	}
	
	public int[] checkIndentation(int[] indentation) {
		if (indentation == null) {
			indentation = new int[4];
		}
		return new int[] {
			checkIndent(addressStr, indentation[0]),
			checkIndent(tokenStr, indentation[1]),
			checkIndent(labelStr, indentation[2]),
			checkIndent(instStr, indentation[3])
		};
	}
	
	private int checkIndent(String s, int i) {
		return i < s.length() ? s.length() + 1 : i;
	}
	
}
