package assembler;

import assembler.Assembler.CompilerError;

import java.util.Map;

public class ExpressionPart {
	
	static Assembler asm;
	String type;
	int value;
	String operator;
	
	public ExpressionPart() {
		
	}
	
	public ExpressionPart(String token) {
		operator = token;
		switch(token) {
			default:
				type = "LITERAL";
				break;
			case("+"):
			case("-"):
			case("/"):
			case("*"):
				type = "OPERATOR";
				break;
		}
	}
	
	public ExpressionPart(int mValue) {
		type = "LITERAL";
		value = mValue;
		operator = "" + mValue;
	}
	
	public int resolve(Map<String, Integer> labels, int line, int format, int adrFormat, AsmNumberFormat[] numberFormats) throws CompilerError {
		return asm.readVal(operator, labels, line, format, adrFormat, numberFormats);
	}
	
}
