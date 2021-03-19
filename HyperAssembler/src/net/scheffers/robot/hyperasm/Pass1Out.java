package net.scheffers.robot.hyperasm;

import javafx.util.Pair;
import net.scheffers.robot.hyperasm.isa.InstructionDef;
import net.scheffers.robot.hyperasm.isa.InstructionSet;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Pass1Out extends Pass0Out {
	
	public InstructionSet isa;
	public long totalLength;
	public long paddingLength;
	public long[] lineStartAddresses;
	public long[] lineLengths;
	public String[][] lineInsnArgs;
	public InstructionDef[] lineInsns;
	public Map<String, Label> labels;
	
	public List<Pair<Integer, Integer>> pieAddresses;
	
	public Pass1Out() {
		
	}
	
	public Pass1Out(Pass0Out in, int nLines) {
		lineInsnArgs = new String[nLines][];
		lineInsns = new InstructionDef[nLines];
		lineLengths = new long[nLines];
		lineStartAddresses = new long[nLines];
		pieAddresses = new LinkedList<>();
		labels = new HashMap<>();
		tokensOut = in.tokensOut;
		errors = in.errors;
		warnings = in.warnings;
		removePrefixPadding = in.removePrefixPadding;
		tokensSourceFiles = in.tokensSourceFiles;
		tokenLineNums = in.tokenLineNums;
	}
	
}
