package net.scheffers.robot.emu.debugger;

import net.scheffers.robot.emu.Emulator;
import net.scheffers.robot.emu.GR8CPURev3_1;
import net.scheffers.robot.hyperasm.Label;
import net.scheffers.robot.hyperasm.Pass2Out;

import java.util.Arrays;

public class Variable {
	
	/** The name of the variable. */
	public String name;
	/** The label, if any, corresponding to this variable. */
	public String labelName;
	/** The address in memory at which the variable lies. */
	public short address;
	/** The number of bytes that the value takes up, after pointers. */
	public int numBytes;
	/** Whether or not the variable, if a number, is a signed number. */
	public boolean isSigned;
	/** Number of times the final type is a pointer. A pointer to a variable is always two bytes long. */
	public int pointerDepth;
	/**
	 * If true, the variable (after pointers) is treated like a null-terminated string and stored in <code>strValue</code>.
	 * If the numBytes field is not 1, then that is the assumed maximum length.
	 */
	public boolean interpretAsString;
	
	/** A string representation of the specified type. */
	public String typeName;
	
	/** The last read number value. */
	public long value;
	/** The string representation of the value. */
	public String strValue;
	
	public Variable() {
		numBytes = 1;
		isSigned = false;
		pointerDepth = 0;
		address = 0x1000;
		updateTypeName();
		updateValue();
	}
	
	public Variable(Pass2Out asm, String labelName) {
		Label label = asm.labels.get(labelName);
		if (label == null) {
			throw new IllegalArgumentException("No such label.");
		}
		numBytes = 1;
		address = (short) label.address;
		if (asm.labels.containsKey(labelName + "_hi")) {
			numBytes = 2;
		}
		else if (labelName.endsWith("_lo")) {
			String temp = labelName.substring(0, labelName.lastIndexOf('_'));
			String hi = temp + "_hi";
			if (asm.labels.containsKey(hi) && asm.labels.get(hi).address == label.address + 1) {
				numBytes = 2;
				name = temp;
			}
		}
		else if (labelName.matches("[a-zA-Z_][a-zA-Z0-9]*_0")) {
			String temp = labelName.substring(0, labelName.lastIndexOf('_'));
			String hi = temp + "_1";
			if (asm.labels.containsKey(hi) && asm.labels.get(hi).address == label.address + 1) {
				name = temp;
				numBytes = 2;
				for (int i = 2; asm.labels.containsKey(temp + "_" + i) && asm.labels.get(hi).address == label.address + i; i++) {
					numBytes = i + 1;
				}
			}
		}
	}
	
	public void updateTypeName() {
		char[] ptr = new char[pointerDepth];
		Arrays.fill(ptr, '*');
		String sPtr = new String(ptr);
		typeName = (isSigned ? "" : "u") + "int" + numBytes * 8 + "_t" + sPtr;
	}
	
	public void updateValue() {
		long actualAddress = address;
		for (int i = 0; i < pointerDepth; i++) {
			actualAddress = readInt(false, 2, actualAddress);
		}
		value = readInt(isSigned, numBytes, actualAddress);
		if (interpretAsString) {
			int maxLen = (numBytes != 1 ? numBytes : 65536);
			StringBuilder str = new StringBuilder();
			for (int i = 0; i < maxLen; i++) {
				char c = (char) readMem((short) (actualAddress + i));
				if (c == 0) break;
				str.append(c);
			}
			strValue = str.toString();
		}
		else
		{
			strValue = "" + value;
		}
	}
	
	protected long readInt(boolean isSigned, int numBytes, long address) {
		long value = 0;
		for (int i = 0; i < numBytes; i++) {
			value |= (long) readMem((short) ((address + i) & 0xffff)) << (i * 8);
		}
		if (isSigned && readMem((short) ((address + numBytes - 1) & 0xffff)) < 0) { // Sign bit check.
			for (int i = numBytes; i < Long.BYTES; i++) {
				value |= (long) 0xff << (i * 8); // Set bits so as to make the long representation negative too.
			}
		}
		return value;
	}
	
	protected byte readMem(short address) {
		GR8CPURev3_1 cpu = Emulator.inst.emuThread.cpu;
		return cpu.readMemory(address, true);
	}
	
}
