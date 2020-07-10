package net.scheffers.robot.hyperasm.isa;

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
    
}
