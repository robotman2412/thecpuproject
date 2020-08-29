package net.scheffers.robot.hyperasm;

import net.scheffers.robot.hyperasm.isa.InstructionDef;

import java.util.HashMap;
import java.util.Map;

public class Pass2Out extends Pass1Out {
	
	public long[] wordsOut;
	
	public Pass2Out() {
		
	}
	
	public Pass2Out(Pass1Out in, int nLines) {
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
		totalLength = in.totalLength;
		lineStartAddresses = in.lineStartAddresses;
		lineLengths = in.lineLengths;
		lineInsnArgs = in.lineInsnArgs;
		lineInsns = in.lineInsns;
		labels = in.labels;
	}
	
}
