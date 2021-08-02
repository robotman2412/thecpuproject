package assembler;

import processing.core.PApplet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImportedFile {
	
	public static Assembler asm;

	public String[] raw;
	public String[][] tokens;
	public String type;
	public String path;
	public int line;
	
	public ImportedFile(String mPath) {
		path = mPath;
		raw = PApplet.loadStrings(new File(path));
		tokens = new String[raw.length][];
		tokens = new String[raw.length][];
		asm.progText = "Finding imports: Tokenising \"" + path + "\"...";
		for (int i = 0; i < raw.length; i++) {
			if (!asm.isEmpty(raw[i]) && !asm.isComment(raw[i])) {
				tokens[i] = Assembler.tokenise(raw[i]);
				asm.printArray(tokens[i]);
			}
			else
			{
				tokens[i] = null;
			}
			asm.progress = ((float)i) / raw.length;
		}
	}
	
	/**
	 * Inserts the tokens at the specified line.
	 * Does not remove any existing tokens.
	 * @return
	 */
	public String[][] insertTokens(String[][] mTokens, int line) {
		List<String[]> total = new ArrayList<String[]>();
		for (String[] s : mTokens) {
			total.add(s);
		}
		List<String[]> add = new ArrayList<String[]>();
		for (String[] s : tokens) {
			add.add(s);
		}
		total.addAll(line, add);
		return total.toArray(new String[0][]);
	}
	
}
