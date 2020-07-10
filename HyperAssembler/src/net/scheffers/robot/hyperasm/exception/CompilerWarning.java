package net.scheffers.robot.hyperasm.exception;

public class CompilerWarning extends CompilerError {
	
	boolean isLightWarning;
	
	public CompilerWarning(String source, int line, String message) {
		super(source, "Warning in line " + line + ": " + message, line);
	}
	
	public CompilerWarning(String source, int line, Throwable cause) {
		super(source, "Warning in line " + line + ": uncaught error", line);
	}
	
	public CompilerWarning(String source, int line, String message, Throwable cause) {
		super(source, "Warning in line " + line + ": " + message, line, cause);
	}
	
	public CompilerWarning(String source, int line) {
		super(source, "Warning in line " + line + ": unknown error", line);
	}
	
	public CompilerWarning(String source, int line, String message, boolean isLightWarning) {
		this(source, line, message);
		this.isLightWarning = isLightWarning;
	}
	
	public CompilerWarning(String source, int line, Throwable cause, boolean isLightWarning) {
		this(source, line, cause);
		this.isLightWarning = isLightWarning;
	}
	
	public CompilerWarning(String source, int line, String message, Throwable cause, boolean isLightWarning) {
		this(source, line, message, cause);
		this.isLightWarning = isLightWarning;
	}
	
	public CompilerWarning(String source, int line, boolean isLightWarning) {
		this(source, line);
		this.isLightWarning = isLightWarning;
	}
	
}
