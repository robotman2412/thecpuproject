package net.scheffers.robot.hyperasm.isa;

public class ComplexInsnPart {
    
    public int startBit;
    public int bitLen;
    public int argSrc;
    
    public ComplexInsnPart(int startBit, int bitLen, int argSrc) {
        this.startBit = startBit;
        this.bitLen = bitLen;
        this.argSrc = argSrc;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new ComplexInsnPart(startBit, bitLen, argSrc);
    }
    
}
