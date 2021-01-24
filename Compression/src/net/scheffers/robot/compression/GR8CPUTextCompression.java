package net.scheffers.robot.compression;

import jutils.JUtils;
import jutils.Table;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GR8CPUTextCompression {
	
	public static Map<String, String> args;
	
	public static void main(String[] rawArgs) {
		args = JUtils.getArgs(rawArgs);
		
		//region shitty arguments
		String message;
		if (args.containsKey("m") && args.get("m") != null) {
			message = args.get("m");
		}
		else if (args.containsKey("message") && args.get("message") != null) {
			message = args.get("message");
		}
		else
		{
			try {
				message = JUtils.awaitLine("Enter message: ");
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		
		boolean isRev2 = false;
		if (args.containsKey("r2")) {
			isRev2 = true;
		}
		//endregion shitty arguments
		
		byte[] raw = message.getBytes(StandardCharsets.US_ASCII);
		
		// Ok, time to do some actually interesting fucking bullshite.
		int[] occuranceMap = new int[256];
		// Map occurances onto bytes.
		for (byte b : raw) {
			occuranceMap[b & 0xff]++;
		}
		// Map occurances onto nodes.
		int nextID = 0;
		List<Node> occurances = new LinkedList<>();
		for (int i = 0; i < 256; i++) {
			if (occuranceMap[i] > 0) {
				Node node = new Node();
				node.frequency = occuranceMap[i];
				node.value = (byte) i;
				node.id = nextID;
				nextID ++;
				occurances.add(node);
			}
		}
		// Merge two least occurring nodes sequentially.
		while (occurances.size() > 1) {
			// Note: The least valued are last in the list after sorting.
			occurances.sort(null);
			// Merge the two nodes.
			Node merged = new Node();
			Node left = occurances.get(1);
			Node right = occurances.get(0);
			merged.left = left;
			merged.right = right;
			// That includes adding their frequencies.
			merged.frequency = left.frequency + right.frequency;
			merged.id = nextID;
			nextID ++;
			// Replace them in the node list.
			occurances.remove(0);
			occurances.set(0, merged);
		}
		
		// This produces our tree.
		Node tree = occurances.get(0);
		
		// Fuck off with your stupid tree lol.
		exportRev2(tree);
	}
	
	public static void exportRev2(Node tree) {
		List<Node> all = new LinkedList<>();
		tree.addAllTo(all);
		Table table = new Table();
		for (Node node : all) {
			if (node.left != null) {
				String lDat = dat(node.left);
				String rDat = dat(node.right);
				table.add("tree_" + node.id, "data " + lDat + ", " + rDat);
			}
		}
		table.print(4);
		System.out.println("tree_head = tree_" + tree.id);
	}
	
	private static String dat(Node node) {
		if (node.left != null) {
			return "tree_" + node.id;
		}
		else
		{
			return "$" + String.format("%02x", node.value | 0x80);
		}
	}
	
}
