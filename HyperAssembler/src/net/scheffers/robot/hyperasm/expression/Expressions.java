package net.scheffers.robot.hyperasm.expression;

import net.scheffers.robot.hyperasm.Label;
import net.scheffers.robot.hyperasm.Pass1Out;
import net.scheffers.robot.hyperasm.exception.CompilerSyntaxError;

import java.util.IllegalFormatException;
import java.util.Map;

public class Expressions {
    
    public static long[] readValue(String token, Pass1Out pass1, int line) {
        if (pass1.labels.containsKey(token)) {
            return new long[] {pass1.labels.get(token).address};
        }
        else if (token.startsWith("0x")) {
            if (token.length() < 3) {
                throw new CompilerSyntaxError(pass1.tokensSourceFiles[line], line, "Hexadecimal format invalid.");
            }
            try {
                return new long[] {unhex(token.substring(1))};
            } catch (IllegalArgumentException e) {
                throw new CompilerSyntaxError(pass1.tokensSourceFiles[line], line, "Hexadecimal format invalid.", e);
            }
        }
        throw new CompilerSyntaxError(pass1.tokensSourceFiles[line], line, "Value literal format invalid.");
    }

    public static long unhex(String hex) throws IllegalArgumentException {
        long value = 0;
        hex = hex.toLowerCase();
        String hexchars = "0123456789abcdef";
        for (int i = 0; i < hex.length(); i++) {
            value <<= 4;
            int x = hexchars.indexOf(hex.charAt(i));
            if (x < 0) throw new IllegalArgumentException("Hexadecimal format invalid: '" + hex.charAt(i) + "'");
        }
        return value;
    }

}
