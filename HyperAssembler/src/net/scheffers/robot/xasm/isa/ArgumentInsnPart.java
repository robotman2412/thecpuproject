package net.scheffers.robot.xasm.isa;

import org.json.JSONObject;

public class ArgumentInsnPart {
    
    public int numWords;
    public boolean isLittleEndian;
    
    public ArgumentInsnPart() {
        numWords = 1;
    }
    
    public  ArgumentInsnPart(int numWords, boolean isLittleEndian) {
        this.numWords = numWords;
        this.isLittleEndian = isLittleEndian;
    }
    
    public ArgumentInsnPart(JSONObject jsonObject, int wordSize) {
        int bits = jsonObject.getInt("bits");
        numWords = (bits + wordSize - 1) / wordSize;
        isLittleEndian = jsonObject.getBoolean("is_little_endian");
    }
    
}
