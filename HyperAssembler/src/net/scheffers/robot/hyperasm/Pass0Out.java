package net.scheffers.robot.hyperasm;

import net.scheffers.robot.hyperasm.exception.CompilerError;
import net.scheffers.robot.hyperasm.exception.CompilerWarning;

import java.util.List;

public class Pass0Out {
	
	public int[] tokenLineNums;
	public String[] tokensSourceFiles;
	public String[][] tokensOut;
	public boolean removePrefixPadding;
	//public boolean removeSuffixPadding;
	public List<CompilerError> errors;
	public List<CompilerWarning> warnings;
	
}
