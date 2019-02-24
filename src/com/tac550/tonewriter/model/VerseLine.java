package com.tac550.tonewriter.model;

import java.util.ArrayList;
import java.util.Arrays;

public class VerseLine {
	
	private String line;
	
	private String[] syllables;
	
	public VerseLine(String line_text) {
		refreshLine(line_text);
	}
	
	private void refreshLine(String line) {
		this.line = line;
		syllables = splitSyllables(line);
	}
	
	private String[] splitSyllables(String line) {
		ArrayList<String> new_syllables = new ArrayList<String>();
        new_syllables.addAll(Arrays.asList(line.replace("-", "_-").replace(" ", "_ ").split("_")));
		
		return new_syllables.toArray(new String[0]);
	}

	public String getLine() {
		return line;
	}
	public void setLine(String line) {
		refreshLine(line);
	}
	public String[] getSyllables() {
		return syllables;
	}
	public String getAllSyllables() {
		return String.join("", syllables);
	}
	
}
