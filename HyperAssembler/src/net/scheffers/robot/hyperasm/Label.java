package net.scheffers.robot.hyperasm;

public class Label {
	
	public long address;
	public String fileName;
	public int lineNum;
	public boolean wasExported;
	
	public Label(String fileName, int lineNum, long address, boolean wasExported) {
		this.fileName = fileName;
		this.lineNum = lineNum;
		this.address = address;
		this.wasExported = wasExported;
	}
	
}
