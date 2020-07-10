package net.scheffers.robot.hyperasm.exception;

public class CompilerError extends RuntimeException {
	
	public final int line;
	public final String source;
	
	protected CompilerError(String source, String rawMsg, int line) {
		super(rawMsg);
		this.line = line;
		this.source = source;
	}
	
	protected CompilerError(String source, String rawMsg, int line, Throwable cause) {
		super(rawMsg, cause);
		this.line = line;
		this.source = source;
	}
	
	public CompilerError(String source, int line, String message) {
		super("Error in line " + line + ": " + message);
		this.line = line;
		this.source = source;
	}
	
	public CompilerError(String source, int line, Throwable cause) {
		super("Error in line " + line + ": uncaught error: " + cause.getMessage(), cause);
		this.line = line;
		this.source = source;
	}
	
	public CompilerError(String source, int line, String message, Throwable cause) {
		super("Error in line " + line + ": " + message, cause);
		this.line = line;
		this.source = source;
	}
	
	public CompilerError(String source, int line) {
		super("Error in line " + line + ": unknown error");
		this.line = line;
		this.source = source;
	}
	
}
