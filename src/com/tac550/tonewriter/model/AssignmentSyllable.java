package com.tac550.tonewriter.model;

import java.util.List;

public class AssignmentSyllable {

	private final String syllableText;
	private final boolean firstSyllableInWord;
	private final boolean bold;
	private final boolean italic;

	private final List<AssignedChordData> assignedChords;

	public AssignmentSyllable(String syllable_text, boolean bold, boolean italic, List<AssignedChordData> assigned_chords) {
		this.syllableText = syllable_text; this.firstSyllableInWord = !syllable_text.startsWith("-");
		this.bold = bold; this.italic = italic; this.assignedChords = assigned_chords;
	}

	public String getSyllableText() {
		return syllableText;
	}
	public boolean isFirstSyllableInWord() {
		return firstSyllableInWord;
	}
	public boolean isBold() {
		return bold;
	}
	public boolean isItalic() {
		return italic;
	}
	public List<AssignedChordData> getAssignedChords() {
		return assignedChords;
	}

}
