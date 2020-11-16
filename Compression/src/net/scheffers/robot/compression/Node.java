package net.scheffers.robot.compression;

import jutils.Table;

import java.util.List;

/**
 * A node used to create and represent the compression.
 */
public class Node implements Comparable<Node> {
	
	public int frequency;
	public byte value;
	public Node left;
	public Node right;
	public int id;
	
	@Override
	public int compareTo(Node other) {
		return frequency - other.frequency;
	}
	
	@Override
	public String toString() {
		if (left != null && right != null) {
			String[] left = this.left.toString().split("\n");
			String[] right = this.right.toString().split("\n");
			String[] merged = new String[Math.max(left.length, right.length)];
			int wL = left[0].length();
			int wR = right[0].length();
			String tempL = temp(wL);
			String tempR = temp(wR);
			for (int i = 0; i < merged.length; i++) {
				String l = i < left.length ? left[i] : tempL;
				String r = i < right.length ? right[i] : tempR;
				merged[i] = l + ' ' + r;
			}
			return null;
		}
		else if (left == null && right == null) {
			if (value < ' ') {
				return " . ";
			}
			else if (value > 126) {
				return " . ";
			}
			else
			{
				return "'" + (char) value + "'";
			}
		}
		else
		{
			return " ? ";
		}
	}
	
	private String temp(int num) {
		char[] arr = new char[num];
		for (int i = 0; i < num; i++) {
			arr[i] = ' ';
		}
		return new String(arr);
	}
	
	public void what(Table table) {
		table.add(frequency, value());
		if (left != null) {
			left.what(table);
		}
		if (right != null) {
			right.what(table);
		}
	}
	
	public String value() {
		if (left != null && right != null) {
			String left = this.left.value();
			String right = this.right.value();
			return left + right;
		}
		else if (left == null && right == null) {
			if (value < ' ') {
				return ".";
			}
			else if (value > 126) {
				return ".";
			}
			else
			{
				return "" + (char) value;
			}
		}
		else
		{
			return "?";
		}
	}
	
	public void addAllTo(List<Node> all) {
		all.add(this);
		if (left != null) {
			left.addAllTo(all);
		}
		if (right != null) {
			right.addAllTo(all);
		}
	}
}
