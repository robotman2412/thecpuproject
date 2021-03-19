package net.scheffers.robot.soundtest;

import jutils.Table;

public class NoteTable {
	
	public static String[] noteNames = {
			"A", "A#", "B", "C", "C#", "D",
			"D#", "E", "F", "F#", "G", "G#"
	};
	
	public static double noteA0 = 27.5; // Hertz
	
	public static void main(String[] args) {
		Note[] notes = calcNotes(20000000 /* 20 MHz */, 256 /* 8-bit waveform */);
		
		Table table = new Table();
		table.add("note", "frequency", "divider", "error");
		for (Note note : notes) {
			table.add(
					String.format("%-2s%d", note.name, note.octave),
					Math.round(note.freq * 100.0) / 100.0,
					String.format("$%04x %5d", note.divider, note.divider),
					Math.round(note.error * 100.0) / 100.0
			);
		}
		System.out.println("Note information:");
		table.print(3);
		
		table = new Table();
		table.add("; name", "divider", "     true freq", "    error");
		table.add(String.format("FREQ_LENGTH = %d", notes.length));
		for (Note note : notes) {
			table.add(
					String.format("FREQ_%s%d", note.name.replace('#', '$'), note.octave),
					String.format("DATA %d", note.divider),
					String.format("; %8.2f Hz", note.freq),
					String.format("%8.2f Hz", note.error)
			);
		}
		System.out.println("Note table:");
		table.print(3);
		
		table = new Table();
		table.add("; name", "number");
		for (int i = 0; i < notes.length; i++) {
			Note note = notes[i];
			table.add(
					String.format("NOTE_%s%d", note.name.replace('#', '$'), note.octave),
					String.format("= %d", i)
			);
		}
		table.print(3);
		
		table = new Table();
		table.add("; name", "string");
		table.add(String.format("NOTENAME_LENGTH = %d", notes.length));
		for (Note note : notes) {
			table.add(
					String.format("NOTENAME_%s%d", note.name.replace('#', '$'), note.octave),
					String.format("DATA \"%-2s%d\"", note.name, note.octave)
			);
		}
		System.out.println("Name table:");
		table.print(3);
		
		table = new Table();
		table.add(";  ", "entry");
		for (Note note : notes) {
			String noteid = String.format("NOTENAME_%s%d", note.name.replace('#', '$'), note.octave);
			table.add(
					"",
					String.format("DATA <%s,", noteid),
					String.format(">%s", noteid)
			);
		}
		table.print(3);
	}
	
	/**
	 * Calculates all the information for notes A1 through G#8.
	 * @param baseFreq the base clock frequency for the sound generation circuitry
	 * @param divider the fixed clock divider applied after the variable divider
	 * @return an array of all the notes' information
	 */
	public static Note[] calcNotes(double baseFreq, double divider) {
		baseFreq /= divider;
		int numOctaves = 8;
		int startingOctave = 1;
		Note[] notes = new Note[12 * numOctaves];
		
		for (int i = 0; i < 12 * numOctaves; i++) {
			int noteNum = 12 * startingOctave + i;
			int octave = (i + 9) / 12 + startingOctave;
			// Find the frequency and divider.
			double trueFreq = noteA0 * Math.pow(2, noteNum / 12.0);
			double trueDivider = baseFreq / trueFreq;
			// Round them.
			int lossyDivider = (int) Math.round(trueDivider);
			// Find the error.
			double lossyFreq = baseFreq / lossyDivider;
			double error = lossyFreq - trueFreq;
			// Enter it.
			Note note = new Note();
			note.freq = trueFreq;
			note.divider = lossyDivider;
			note.error = error;
			note.name = noteNames[noteNum % 12];
			note.octave = octave;
			notes[i] = note;
		}
		
		return notes;
	}
	
}
