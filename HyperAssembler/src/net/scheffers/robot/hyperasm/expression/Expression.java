package net.scheffers.robot.hyperasm.expression;

import net.scheffers.robot.hyperasm.Pass1Out;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Expression {
	
	public Expression left;
	public Expression right;
	public String raw;
	public Operator operator;
	
	public Expression() {
		raw = "";
	}
	
	public long[] resolveValue(String labelPrefix, Pass1Out pass1, int index) throws Exception {
		if (operator == Operator.LITERAL || operator == null) {
			if (raw.matches("[+\\-*/]")) {
				throw new Exception("Invalid literal.");
			}
			return readValue(labelPrefix, raw, pass1, index);
		}
		long[] a = left.resolveValue(labelPrefix, pass1, index);
		long[] b = right.resolveValue(labelPrefix, pass1, index);
		if (a.length != 1 || b.length != 1) {
			throw new Exception("Expected two numbers or characters.");
		}
		long res = 0;
		switch (operator) {
			case ADD:
				res = a[0] + b[0];
				break;
			case SUBTRACT:
				res = a[0] - b[0];
				break;
			case MULTIPLY:
				res = a[0] * b[0];
				break;
			case DIVIDE:
				res = a[0] / b[0];
				break;
		}
		return new long[] {res};
	}
	
	public static Expression parse(String[] tokens, Pass1Out pass1) throws Exception {
		List<Expression> expr = new LinkedList<>();
		for (String token : tokens) {
			Expression e = new Expression();
			e.raw = token;
			expr.add(e);
		}
		return parseInternal(expr, pass1);
	}
	
	protected static Expression parseInternal(List<Expression> expr, Pass1Out pass1) throws Exception {
		
		if (expr.size() == 1) {
			Expression e = expr.get(0);
			e.operator = Operator.LITERAL;
			return e;
		}
		
		// Step two: parentheses.
		List<Expression> apparently = new LinkedList<>();
		for (int i = 0; i < expr.size(); i++) {
			// Check for parentheses.
			if (expr.get(i).raw.equals("(")) {
				int depth = 1;
				apparently.clear();
				// Then slurp it up...
				while (depth > 0) {
					expr.remove(i);
					if (i >= expr.size()) {
						throw new Exception("Missing closing parenthesis!");
					}
					Expression e = expr.get(i);
					String s = e.raw;
					if (s.equals(")")) {
						depth --;
						if (depth == 0) {
							break;
						}
					}
					else if (s.equals("(")) {
						depth ++;
					}
					apparently.add(e);
				}
				expr.set(i, parseInternal(apparently, pass1));
			}
		}
		
		// Step three: squash unary minus.
		
		// Step four: multiply and divide.
		for (int i = 1; i < expr.size(); i += 2) {
			Expression e = expr.get(i);
			String s = e.raw;
			if (s.equals("*")) {
				if (i >= expr.size() - 1) {
					throw new Exception("Dangling operator.");
				}
				Expression left = expr.get(i - 1);
				Expression right = expr.get(i + 1);
				Expression another = new Expression();
				another.left = left;
				another.right = right;
				another.operator = Operator.MULTIPLY;
				expr.remove(i);
				expr.remove(i);
				expr.set(i - 1, another);
				i -= 2; // to negate increment
			}
			else if (s.equals("/")) {
				if (i >= expr.size() - 1) {
					throw new Exception("Dangling operator.");
				}
				Expression left = expr.get(i - 1);
				Expression right = expr.get(i + 1);
				Expression another = new Expression();
				another.left = left;
				another.right = right;
				another.operator = Operator.DIVIDE;
				expr.remove(i);
				expr.remove(i);
				expr.set(i - 1, another);
				i -= 2; // to negate increment
			}
		}
		
		// Step five: add and subtract.
		for (int i = 1; i < expr.size(); i += 2) {
			Expression e = expr.get(i);
			String s = e.raw;
			if (s.equals("+")) {
				if (i >= expr.size() - 1) {
					throw new Exception("Dangling operator.");
				}
				Expression left = expr.get(i - 1);
				Expression right = expr.get(i + 1);
				Expression another = new Expression();
				another.left = left;
				another.right = right;
				another.operator = Operator.ADD;
				expr.remove(i);
				expr.remove(i);
				expr.set(i - 1, another);
				i -= 2; // to negate increment
			}
			else if (s.equals("-")) {
				if (i >= expr.size() - 1) {
					throw new Exception("Dangling operator.");
				}
				Expression left = expr.get(i - 1);
				Expression right = expr.get(i + 1);
				Expression another = new Expression();
				left.operator = Operator.LITERAL;
				right.operator = Operator.LITERAL;
				another.left = left;
				another.right = right;
				another.operator = Operator.SUBTRACT;
				expr.remove(i);
				expr.remove(i);
				expr.set(i - 1, another);
				i -= 2; // to negate increment
			}
		}
		
		// Sanity check.
		if (expr.size() > 1) {
			throw new Exception("Invalid expression.");
		}
		
		return expr.get(0);
	}
	
	public static long[] resolve(String labelPrefix, String[] tokens, Pass1Out pass1, int index) throws Exception {
		return parse(tokens, pass1).resolveValue(labelPrefix, pass1, index);
	}
	
	public static long[] readValue(String labelPrefix, String token, Pass1Out pass1, int index) {
		return readValue(labelPrefix, token, pass1, index, 0xff);
	}
	
	public static boolean isValidToken(String s, Pass1Out pass1, int index) {
		if (s.matches("^'.*'$|^\".*\"$")) {
			try {
				unescapeAnother(s.substring(1, s.length() - 1), IBM437);
				return true;
			} catch (Exception e) {
				return false;
			}
		}
		else if (s.matches("[a-zA-Z_][a-zA-Z0-9_]*(?:.[a-zA-Z0-9_]+)?")) {
			return pass1.isa.isValidLabelName(s);
		}
		else if (s.matches("[<>][a-zA-Z_][a-zA-Z0-9_]*(?:.[a-zA-Z0-9_]+)?")) {
			return pass1.isa.isValidLabelName(s.substring(1));
		}
		else if (s.matches(".[a-zA-Z_][a-zA-Z0-9_]*")) {
			return pass1.isa.isValidLabelName(s.substring(1));
		}
		else if (s.matches("[<>].[a-zA-Z_][a-zA-Z0-9_]*")) {
			return pass1.isa.isValidLabelName(s.substring(2));
		}
		else
		{
			return s.matches(
					"^((\\$[0-9a-zA-Z]+)|([0-9a-zA-Z][hH])|(0x[0-9a-zA-Z])|" + // Hexadecimal.
							"(0[qo][0-7]+)|([0-7][oqOQ])|" + // Octal.
							"(%[01]+)|" + // Binary.
							"(-?[0-9]+))$" // Decimal.
			);
		}
	}
	
	//region reading
	public static Charset IBM437 = Charset.forName("IBM437");
	
	public static String unescapeString(String escaped, Charset charset) {
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
					case('x'):
						if (i >= chars.length - 4) {
							throw new IllegalArgumentException("Not enough characters left for hex escape sequence!");
						}
						char hex = (char) unhex(escaped.substring(i + 2, i + 4));
						int amount = 1;
						int tmp = hex >>> 8;
						while (tmp != 0) {
							amount ++;
							tmp >>>= 8;
						}
						ByteBuffer buf = ByteBuffer.allocate(amount);
						produced.append(charset.decode(buf));
						break;
					case('u'):
						if (i >= chars.length - 6) {
							throw new IllegalArgumentException("Not enough characters left for unicode escape sequence!");
						}
						char unicode = (char) unhex(escaped.substring(i + 2, i + 6));
						produced.append(unicode);
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
	
	/**
	 * Why the fuck does Charset not just have a method for this shit?
	 * (disregarding the long[] output part).
	 */
	public static List<Long> encodeAChar(char c, Charset charset) {
		byte[] whatTheFuck = charset.encode(new String(new char[]{c})).array();
		List<Long> whyIsThisShitRequired = new ArrayList<>(whatTheFuck.length);
		for (byte b : whatTheFuck) {
			whyIsThisShitRequired.add((long) (b & 0xff));
		}
		return whyIsThisShitRequired;
	}
	
	public static long[] unescapeAnother(String escaped, Charset charset) {
		char[] chars = escaped.toCharArray();
		int i = 0;
		List<Long> fuckingBullshiteArray = new ArrayList<>(escaped.length() * 2);
		while (i < chars.length) {
			char c = chars[i];
			if (c == '\\') {
				if (i >= chars.length - 1) {
					fuckingBullshiteArray.addAll(encodeAChar(c, charset));
					System.err.println("String escape sequence at end of string!");
					break;
				}
				char esx = chars[i + 1];
				switch (esx) {
					//region simple_esx
					case('0'):
						fuckingBullshiteArray.addAll(encodeAChar('\0', charset));
						break;
					case('n'):
						fuckingBullshiteArray.addAll(encodeAChar('\n', charset));
						break;
					case('r'):
						fuckingBullshiteArray.addAll(encodeAChar('\r', charset));
						break;
					case('f'):
						fuckingBullshiteArray.addAll(encodeAChar('\f', charset));
						break;
					case('\\'):
						fuckingBullshiteArray.addAll(encodeAChar('\\', charset));
						break;
					case('\''):
						fuckingBullshiteArray.addAll(encodeAChar('\'', charset));
						break;
					case('\"'):
						fuckingBullshiteArray.addAll(encodeAChar('\"', charset));
						break;
					case('t'):
						fuckingBullshiteArray.addAll(encodeAChar('\t', charset));
						break;
					case('b'):
						fuckingBullshiteArray.addAll(encodeAChar('\b', charset));
						break;
					//endregion simple_esx
					case('x'):
						if (i >= chars.length - 4) {
							throw new IllegalArgumentException("Not enough characters left for hex escape sequence!");
						}
						long hex = unhex(escaped.substring(i + 2, i + 4));
						fuckingBullshiteArray.add(hex);
						break;
					case('u'):
						if (i >= chars.length - 6) {
							throw new IllegalArgumentException("Not enough characters left for unicode escape sequence!");
						}
						char unicode = (char) unhex(escaped.substring(i + 2, i + 6));
						fuckingBullshiteArray.addAll(encodeAChar(unicode, charset));
						break;
				}
				i ++;
			}
			else
			{
				fuckingBullshiteArray.addAll(encodeAChar(c, charset));
			}
			i ++;
		}
		long[] ohMyGodWhatTheFuckWhyAreYouLikeThisJava = new long[fuckingBullshiteArray.size()];
		thereIsLiterallyNoReasonIAddedThisStupidFuckingLabel: i = 0;
		for (Long fuckingBullshit : fuckingBullshiteArray) {
			ohMyGodWhatTheFuckWhyAreYouLikeThisJava[i] = fuckingBullshit;
			i ++;
		}
		return ohMyGodWhatTheFuckWhyAreYouLikeThisJava;
	}
	
	public static long[] readString(String string, Charset charset) {
		byte[] boites = string.getBytes(charset == null ? StandardCharsets.US_ASCII : charset);
		long[] sentence = new long[boites.length];
		for (int i = 0; i < boites.length; i++) {
			sentence[i] = boites[i] & 0xff;
		}
		return sentence;
	}
	
	public static long[] readValue(String labelPrefix, String token, Pass1Out pass1, int index, long bitmask) {
		if (token.matches("^'.*'$|^\".*\"$")) {
			return unescapeAnother(token.substring(1, token.length() - 1), IBM437);
		}
		else if (token.charAt(0) == '.' && pass1.labels.containsKey(labelPrefix + token)) {
			return new long[] {pass1.labels.get(labelPrefix + token).address};
		}
		else if (token.startsWith("<.") && pass1.labels.containsKey(labelPrefix + token.substring(2))) {
			return new long[] {pass1.labels.get(labelPrefix + token.substring(2)).address & bitmask};
		}
		else if (token.startsWith(">.") && pass1.labels.containsKey(labelPrefix + token.substring(2))) {
			return new long[] {pass1.labels.get(labelPrefix + token.substring(2)).address / (bitmask + 1)};
		}
		else if (token.startsWith("<") && pass1.labels.containsKey(token.substring(1))) {
			return new long[] {pass1.labels.get(token.substring(1)).address & bitmask};
		}
		else if (token.startsWith(">") && pass1.labels.containsKey(token.substring(1))) {
			return new long[] {pass1.labels.get(token.substring(1)).address / (bitmask + 1)};
		}
		else if (pass1.labels.containsKey(token)) {
			return new long[] {pass1.labels.get(token).address};
		}
		else if (token.startsWith("0x")) {
			return new long[] {unhex(token.substring(2))};
		}
		else if (token.startsWith("$")) {
			return new long[] {unhex(token.substring(1))};
		}
		else if (token.matches("[0-9a-fA-F]h")) {
			return new long[] {unhex(token.substring(0, token.length() - 1))};
		}
		else if (token.matches("0[qo][0-7]+")) {
			return new long[] {unoctal(token.substring(2))};
		}
		else if (token.matches("[0-7]+[qo]")) {
			return new long[] {unoctal(token.substring(0, token.length() - 1))};
		}
		else if (token.startsWith("%")) {
			return new long[] {unbin(token.substring(1))};
		}
		else if (token.matches("-?[0-9]+")) {
			return new long[] {Long.parseLong(token) & bitmask};
		}
		else if ((token.charAt(0) == '<' || token.charAt(0) == '>') && pass1.isa.isValidLabelName(token.substring(1))) {
			throw new IllegalArgumentException("No such label '" + token.substring(1) + "'.");
		}
		else if (pass1.isa.isValidLabelName(token)) {
			throw new IllegalArgumentException("No such label '" + token + "'.");
		}
		throw new IllegalArgumentException("Invalid label or literal '" + token + "'.");
	}
	
	public static long unbin(String binary) throws IllegalArgumentException {
		if (!binary.matches("[01]+")) {
			throw new IllegalArgumentException("Binary literal invalid.");
		}
		long value = 0;
		for (int i = 0; i < binary.length(); i++) {
			value <<= 1;
			value |= binary.charAt(i) == '1' ? 1 : 0;
		}
		return value;
	}
	
	public static long unoctal(String octal) throws IllegalArgumentException {
		if (octal.length() < 1) {
			throw new IllegalArgumentException("Expected octal literal.");
		}
		long value = 0;
		String octalChars = "01234567";
		for (int i = 0; i < octal.length(); i++) {
			value <<= 3;
			int x = octalChars.indexOf(octal.charAt(i));
			if (x < 0) throw new IllegalArgumentException("Octal literal invalid: '" + octal.charAt(i) + "'");
			value |= x;
		}
		return value;
	}
	
	public static long unhex(String hex) throws IllegalArgumentException {
		if (hex.length() < 1) {
			throw new IllegalArgumentException("Expected hexadecimal literal.");
		}
		long value = 0;
		hex = hex.toLowerCase();
		String hexchars = "0123456789abcdef";
		for (int i = 0; i < hex.length(); i++) {
			value <<= 4;
			int x = hexchars.indexOf(hex.charAt(i));
			if (x < 0) throw new IllegalArgumentException("Hexadecimal format invalid: '" + hex.charAt(i) + "'");
			value |= x;
		}
		return value;
	}
	//endregion
	
}
