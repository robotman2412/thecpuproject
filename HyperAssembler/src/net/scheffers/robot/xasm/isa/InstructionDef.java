package net.scheffers.robot.xasm.isa;

import net.scheffers.robot.hyperasm.AssemblerCore;
import net.scheffers.robot.xasm.expression.Expression;
import org.json.JSONArray;
import org.json.JSONObject;

public class InstructionDef {

    public String[] tokenPattern;
    public int numWords;
    public Type type;
    public ComplexInsnPart[] complex;
    public int[] complexArgLens;
    public long singleWord;
    public ArgumentInsnPart[] arguments;
	public int numArgs;
	
	protected InstructionDef() {
        
    }
    
    public InstructionDef(JSONObject raw, int isaWordLen) {
	    tokenPattern = AssemblerCore.tokeniseLine(raw.getString("name"));
	    singleWord = Expression.unhex(raw.getString("hex"));
	    if (raw.has("args") && raw.getJSONArray("args").length() > 0) {
            JSONArray rawArgs = raw.getJSONArray("args");
            type = Type.ONE_WORD_WITH_ARG;
            numArgs = rawArgs.length();
            arguments = new ArgumentInsnPart[numArgs];
            numWords = 1;
            for (int i = 0; i < rawArgs.length(); i++) {
                arguments[i] = new ArgumentInsnPart(rawArgs.getJSONObject(i).getJSONObject("type"), isaWordLen);
                numWords += arguments[i].numWords;
            }
        }
	    else
        {
            type = Type.ONE_WORD;
            numArgs = 0;
            numWords = 1;
        }
    }
    
    public static InstructionDef singleWord(long insnCode, String[] tokenPattern) {
        InstructionDef def = new InstructionDef();
        def.singleWord = insnCode;
        def.tokenPattern = tokenPattern.clone();
        def.type = Type.ONE_WORD;
        def.numArgs = 0;
        def.numWords = 1;
        return def;
    }

    public static InstructionDef singleWordSingleArg(long insnCode, String[] tokenPattern) {
        return singleWordComplexArg(insnCode, new ArgumentInsnPart[]{new ArgumentInsnPart()}, tokenPattern);
    }

    public static InstructionDef singleWordDoubleArg(long insnCode, boolean isLittleEndian, String[] tokenPattern) {
        return singleWordComplexArg(insnCode, new ArgumentInsnPart[]{new ArgumentInsnPart(2, isLittleEndian)}, tokenPattern);
    }
    
    public static InstructionDef singleWordComplexArg(long insnCode, ArgumentInsnPart[] arguments, String[] tokenPattern) {
        InstructionDef def = new InstructionDef();
        int words = 1;
        for (ArgumentInsnPart part : arguments) {
            words += part.numWords;
        }
        def.numWords = words;
        def.type = Type.ONE_WORD_WITH_ARG;
        def.arguments = arguments;
        def.numArgs = arguments.length;
        def.tokenPattern = tokenPattern.clone();
        def.singleWord = insnCode;
        return def;
    }
    
    public static InstructionDef complex(int numWords, ComplexInsnPart[] parts, int[] argBitLens, String[] tokenPattern) {
        InstructionDef def = new InstructionDef();
        def.complex = parts;
        def.numWords = numWords;
        def.complexArgLens = argBitLens;
        def.type = Type.COMPLEX;
        def.numArgs = argBitLens.length;
        def.tokenPattern = tokenPattern;
        return def;
    }
    
    public long[] getBytes(long[] resolvedArgs, int wordBits) {
        switch (type) {
            case ONE_WORD:
                if (resolvedArgs != null && resolvedArgs.length > 0) {
                    throw new RuntimeException("Got arguments for instruction without arguments!");
                }
                return new long[] {singleWord};
            case ONE_WORD_WITH_ARG:
                if (resolvedArgs.length > arguments.length) {
                    throw new RuntimeException("Got too many arguments for instruction!");
                }
                return getBytesArgumented(resolvedArgs, wordBits);
            case COMPLEX:
                if (resolvedArgs.length > complexArgLens.length) {
                    throw new RuntimeException("Got too many arguments for complex instruction!");
                }
                return getBytesComplex(resolvedArgs, wordBits);
            default:
                throw new RuntimeException("Instruction definition does not have a type!");
        }
    }

    protected long[] getBytesArgumented(long[] resolvedArgs, int wordBits) {
        long[] out = new long[numWords];
        out[0] = singleWord;
        int indexial = 1;
        for (int x = 0; x < resolvedArgs.length; x++) {
            ArgumentInsnPart arg = arguments[x];
            long res = resolvedArgs[x];
            long resMask = (1 << wordBits) - 1;
            if (arg.numWords == 1) {
                if ((res & ~resMask) != 0) {
                    new IllegalArgumentException("Double word given where single word was required!").printStackTrace();
                }
                out[indexial] = res & resMask;
            }
            else if (arg.isLittleEndian) {
                for (int y = 0; y < arg.numWords; y++) {
                    out[indexial + y] = res & resMask;
                    res >>= wordBits;
                }
                indexial += arg.numWords;
            }
            else
            {
                for (int y = arg.numWords - 1; y >= 0; y--) {
                    out[indexial + y] = res & resMask;
                    res >>= wordBits;
                }
                indexial += arg.numWords;
            }
        }
        return out;
    }

    protected long[] getBytesComplex(long[] resolvedArgs, int wordBits) {
        throw new UnsupportedOperationException("unimplemented");
    }

    public enum Type {
        ONE_WORD,
        ONE_WORD_WITH_ARG,
        COMPLEX
    }
    
}
