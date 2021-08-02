package assembler;

import assembler.Assembler.CompilerError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Expression extends ExpressionPart {
	
	String[] tokens;
	List<ExpressionPart> expression;
	
	public Expression() {
		expression = new ArrayList<ExpressionPart>();
		type = "EXPRESSION";
	}
	
	public Expression(String[] mTokens) {
		tokens = mTokens;
		expression = new ArrayList<>();
		type = "EXPRESSION";
	}
	
	@Override
	public int resolve(Map<String, Integer> labels, int line, int format, int adrFormat, AsmNumberFormat[] numberFormats) throws CompilerError {
		for (int i = 1; i < expression.size();) {
			ExpressionPart e = expression.get(i);
			if (e.type.equals("OPERATOR")) {
				boolean isDiv = false;
				switch(e.operator) {
					default:
						i += 2;
						break;
					case("/"):
						isDiv = true;
					case("*"):
						ExpressionPart p;
						if (i == 0 || i == expression.size() - 1) {
							throw new CompilerError(line, "Missing LITERAL or EXPRESSION at " + e.operator);
						}
						if (expression.get(i - 1).type.equals("OPERATOR") || expression.get(i + 1).type.equals("OPERATOR")) {
							throw new CompilerError(line, "Expected LITERAL or EXPRESSION, but found OPERATOR at" + e.operator);
						}
						if (isDiv) {
							p = new ExpressionPart(expression.get(i - 1).resolve(labels, line, format, adrFormat, numberFormats) / expression.get(i + 1).resolve(labels, line, format, adrFormat, numberFormats));
						}
						else
						{
							p = new ExpressionPart(expression.get(i - 1).resolve(labels, line, format, adrFormat, numberFormats) * expression.get(i + 1).resolve(labels, line, format, adrFormat, numberFormats));
						}
						expression.set(i - 1, p);
						expression.remove(i + 1);
						expression.remove(i);
						break;
				}
			}
			else
			{
				throw new CompilerError(line, "Expected OPERATOR, but got " + e.type);
			}
		}
		int total;
		int i = 1;
		if (expression.get(0).operator.equals("-")) {
			total = 0;
			i = 0;
		}
		else
		{
			total = expression.get(0).resolve(labels, line, format, adrFormat, numberFormats);
		}
		for (; i < expression.size(); i += 2) {
			ExpressionPart e = expression.get(i);
			if (e.type.equals("OPERATOR")) {
				switch(e.operator) {
					default:
						break;
					case("+"):
						total += expression.get(i + 1).resolve(labels, line, format, adrFormat, numberFormats);
						break;
					case("-"):
						total -= expression.get(i + 1).resolve(labels, line, format, adrFormat, numberFormats);
						break;
				}
			}
			else
			{
				throw new CompilerError(line, "Expected OPERATOR, but got " + e.type);
			}
		}
		return total;
	}
	
	public void prep(Map<String, Integer> labels, int line) throws CompilerError {
		int bracDepth = 0;
		String[] expTkn = new String[0];
		for (String token : tokens) {
			switch(token) {
				default:
					if (bracDepth > 0) {
						expTkn = (String[]) Assembler.append(expTkn, token);
					}
					else
					{
						expression.add(new ExpressionPart(token));
					}
					break;
				case("("):
					bracDepth ++;
					break;
				case(")"):
					bracDepth --;
					if (bracDepth == 0) {
						Expression mExp = new Expression(expTkn);
						mExp.prep(labels, line);
						expression.add(mExp);
						expTkn = new String[0];
					}
					else if (bracDepth < 0) {
						throw new CompilerError(line, "too many closing parentheses!");
					}
					break;
			}
		}
		if (bracDepth > 0) {
			throw new CompilerError(line, "too many opening parentheses!");
		}
	}
	
	public void printInfo(int indent) {
		for (ExpressionPart p : expression) {
			if (p instanceof Expression) {
				Expression e = (Expression)p;
				e.printInfo(indent + 1);
			}
			else
			{
				for (int i = 0; i < indent; i++) {
					Assembler.print(" ");
				}
				Assembler.println(p.operator);
			}
		}
	}
	
}
