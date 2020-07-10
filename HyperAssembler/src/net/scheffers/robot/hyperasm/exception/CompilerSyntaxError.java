package net.scheffers.robot.hyperasm.exception;

public class CompilerSyntaxError extends CompilerError {
	
	public CompilerSyntaxError(String source, int line, String message) {
		super(source, "Syntax error in line " + line + ": " + message, line);
	}
	
	public CompilerSyntaxError(String source, int line, Throwable cause) {
		super(source, "Syntax error in line " + line + ": uncaught error", line);
	}
	
	public CompilerSyntaxError(String source, int line, String message, Throwable cause) {
		super(source, "Syntax error in line " + line + ": " + message, line, cause);
	}
	
	public CompilerSyntaxError(String source, int line) {
		super(source, "Syntax error in line " + line + ": unknown error", line);
	}
	
}
