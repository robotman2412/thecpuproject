package net.scheffers.robot.hyperasm;

import jutils.Table;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Pass2Out extends Pass1Out {
	
	public long[] wordsOut;
	
	public Pass2Out() {
		
	}
	
	public Pass2Out(Pass1Out in) {
		labels = new HashMap<>();
		tokensOut = in.tokensOut;
		errors = in.errors;
		warnings = in.warnings;
		removePrefixPadding = in.removePrefixPadding;
		tokensSourceFiles = in.tokensSourceFiles;
		tokenLineNums = in.tokenLineNums;
		totalLength = in.totalLength;
		lineStartAddresses = in.lineStartAddresses;
		lineLengths = in.lineLengths;
		lineInsnArgs = in.lineInsnArgs;
		lineInsns = in.lineInsns;
		labels = in.labels;
		isa = in.isa;
	}
	
	public void saveLHF(File file) throws IOException {
		FileOutputStream out = new FileOutputStream(file);
		out.write("v1.0 raw\n".getBytes());
		out.write(String.format("%02X", wordsOut[0]).getBytes());
		for (int i = 1; i < wordsOut.length; i++) {
			out.write(String.format(" %02X", wordsOut[i]).getBytes());
		}
		out.close();
	}
	
	public void saveOldDump(File file) throws IOException {
		Table table = new Table();
		FileOutputStream out = new FileOutputStream(file);
		for (int i = 0; i < tokensOut.length; i++) {
			if (tokensOut[i] == null || tokensOut[i].length == 0 || (tokensOut[i].length == 1 && tokensOut[i][0].length() == 0)) {
				continue;
			}
			long start = lineStartAddresses[i];
			long len = lineLengths[i];
			if (len > 0) {
				StringBuilder dataCat = new StringBuilder();
				String[] tknShite;
				if (tokensOut[i].length < 2) {
					tknShite = new String[]{""};
				} else {
					tknShite = new String[tokensOut[i].length - 2];
					System.arraycopy(tokensOut[i], 2, tknShite, 0, tknShite.length);
				}
				for (int x = 0; x < len; x++) {
					dataCat.append(String.format("%02x ", wordsOut[(int) start + x]));
				}
				String HOW = tokensOut[i].length > 1 ? tokensOut[i][1] : "";
				table.add(String.format("$%04x - $%04x", start, start + len - 1), tokensOut[i][0], HOW, AssemblerCore.stitchTokens(tknShite), dataCat);
			} else {
				String[] tknShite;
				if (tokensOut[i].length < 2) {
					tknShite = new String[]{""};
				} else {
					tknShite = new String[tokensOut[i].length - 2];
					System.arraycopy(tokensOut[i], 2, tknShite, 0, tknShite.length);
				}
				String HOW = tokensOut[i].length > 1 ? tokensOut[i][1] : "";
				table.add(String.format("$%04x", start), tokensOut[i][0], HOW, AssemblerCore.stitchTokens(tknShite), "");
			}
		}
		String[] lines = table.format(3);
		byte[] newline = {'\r', '\n'};
		for (String s : lines) {
			out.write(s.getBytes());
			out.write(newline);
		}
		out.write(newline);
		out.write("Labels:\r\n".getBytes());
		out.write(newline);
		List<String> labelNames = new ArrayList<>(labels.keySet());
		Collections.sort(labelNames);
		table = new Table();
		for (String label : labelNames) {
			long address = labels.get(label).address;
			if (address > 256) {
				table.add(label, String.format("%04x", address));
			} else {
				table.add(label, String.format("%02x", address));
			}
		}
		lines = table.format(3);
		for (String s : lines) {
			out.write(s.getBytes());
			out.write(newline);
		}
		out.close();
	}
	
}














