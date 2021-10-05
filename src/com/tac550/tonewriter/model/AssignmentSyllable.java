package com.tac550.tonewriter.model;

import java.util.ArrayList;
import java.util.List;

public class AssignmentSyllable {

	private final String syllableText;
	private final boolean firstSyllableInWord;
	private final boolean bold;
	private final boolean italic;

	private final List<AssignedChordData> assignedChords;

	public AssignmentSyllable(String syllable_text, boolean bold, boolean italic, List<AssignedChordData> assigned_chords) {
		this.syllableText = syllable_text; this.bold = bold; this.italic = italic; this.assignedChords = assigned_chords;
		this.firstSyllableInWord = !syllable_text.startsWith("-");
	}

	public String getSyllableText() {
		return syllableText;
	}
	public boolean isBold() {
		return bold;
	}
	public boolean isItalic() {
		return italic;
	}
	public String getFormatData() {
		return (isBold() ? "b" : "") + (isItalic() ? "i" : "");
	}
	public List<AssignedChordData> getAssignedChords() {
		return assignedChords;
	}

	public static class AssignmentSyllableBuilder {

		private String _syllableText = "Text";
		private boolean _bold = false;
		private boolean _italic = false;

		private List<AssignedChordData> _assignedChords = new ArrayList<>();

		public AssignmentSyllableBuilder() { }

		public AssignmentSyllable buildAssignmentSyllable() {
			return new AssignmentSyllable(_syllableText, _bold, _italic, _assignedChords);
		}

		public AssignmentSyllableBuilder syllableText(String _syllableText) {
			this._syllableText = _syllableText;
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
