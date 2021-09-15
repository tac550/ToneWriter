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

	public static class AssignmentSyllableBuilder {

		private String _syllableText;
		private boolean _firstSyllableInWord;
		private boolean _bold;
		private boolean _italic;

		private List<AssignedChordData> _assignedChords;

		public AssignmentSyllableBuilder() { }

		public AssignmentSyllableBuilder syllableText(String _syllableText) {
			this._syllableText = _syllableText;
			return this;
		}
		public AssignmentSyllableBuilder firstSyllableInWord(boolean _firstSyllableInWord) {
			this._firstSyllableInWord = _firstSyllableInWord;
			return this;
		}
		public AssignmentSyllableBuilder bold(boolean _bold) {
			this._bold = _bold;
			return this;
		}
		public AssignmentSyllableBuilder italic(boolean _italic) {
			this._italic = _italic;
			return this;
		}
		public AssignmentSyllableBuilder assignedChords(List<AssignedChordData> _assignedChords) {
			this._assignedChords = _assignedChords;
			return this;
		}

	}
}
