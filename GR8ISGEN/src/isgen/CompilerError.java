package isgen;

public class CompilerError extends Exception {
	private static final long serialVersionUID = -1285538276365577921L;
	public int instruction;
	public String message;
	public CompilerError(int instruction, String message) {
		this.instruction = instruction + 1;
		this.message = message;
	}
}
