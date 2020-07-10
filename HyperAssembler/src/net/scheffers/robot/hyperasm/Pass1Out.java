package net.scheffers.robot.hyperasm;

import net.scheffers.robot.hyperasm.isa.InstructionDef;

import java.util.Map;

public class Pass1Out extends Pass0Out {
	
	public long totalLength;
	long[] lineStartAddresses;
	long[] lineLengths;
	String[][] lineInsnArgs;
	InstructionDef[] lineInsns;
	Map<String, Label> labels;
	
}
