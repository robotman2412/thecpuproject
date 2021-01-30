package net.scheffers.robot.ishelper;

import jutils.JUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class ISHelper {
	
	public static void main(String[] args) {
		JUtils.getArgs(args);
		List<String> in = new LinkedList<>();
		int[] colLengths = new int[32];
		boolean doNumber = false;
		
		Scanner scanner = new Scanner(System.in);
		lol: while (true) {
			String x = scanner.nextLine();
			String[] _x = x.split("(\r\n|\r|\n)");
			for (String s : _x) {
				if (s != null) {
					if (s.equals("go")) {
						break lol;
					} else if (s.equals("go number")) {
						doNumber = true;
						break lol;
					} else {
						in.add(s);
						int i = 0;
						for (String m : s.split("\t+")) {
							colLengths[i] = Math.max((m.length() + 7) / 4, colLengths[i]);
							i++;
						}
					}
				}
			}
		}
		
		for (int i = 0; i < in.size(); i++) {
			String[] split = in.get(i).split("\t+");
			if (doNumber) {
				System.out.printf("%02X\t", i);
			}
			for (int x = doNumber ? 1 : 0; x < split.length; x++) {
				System.out.print(split[x]);
				if (x < split.length - 1) {
					for (int y = 0; y < colLengths[x] - split[x].length() / 4; y++) {
						System.out.print('\t');
					}
				}
			}
			System.out.println();
		}
		System.out.flush();
	}
	
}



