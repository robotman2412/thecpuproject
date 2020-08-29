package net.scheffers.robot.hyperasm;

import net.scheffers.robot.hyperasm.isa.InstructionDef;

import java.util.HashMap;
import java.util.Map;

public class Pass1Out extends Pass0Out {
	
	public long totalLength;
	public long[] lineStartAddresses;
	public long[] lineLengths;
	public String[][] lineInsnArgs;
	public InstructionDef[] lineInsns;
	public Map<String, Label> labels;
	
	public Pass1Out() {
		
	}
	
	public Pass1Out(Pass0Out in, int nLines) {
		lineInsnArgs = new String[nLines][];
		lineInsns = new InstructionDef[nLines];
		lineLengths = new long[nLines];
		lineStartAddresses = new long[nLines];
		labels = new HashMap<>();
		tokensOut = in.tokensOut;
		errors = in.errors;
		warnings = in.warnings;
		removePrefixPadding = in.removePrefixPadding;
		tokensSourceFiles = in.tokensSourceFiles;
		tokenLineNums = in.tokenLineNums;
	}
	
}
