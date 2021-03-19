package net.scheffers.robot.xasm;

@FunctionalInterface
public interface AssemblyDirective {
	
	void accept(AssemblyContext context, String variant, String[] tokens);
	
}
