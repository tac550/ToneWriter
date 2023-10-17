package com.tac550.tonewriter.model;

import java.util.ArrayList;
import java.util.List;

public class AssignmentSyllable {

	private final String syllableText;
	private final boolean firstSyllableInWord;
	private final boolean bold;
	private final boolean italic;
	private final boolean forceHyphen;

	private final List<AssignedChordData> assignedChords;

	public AssignmentSyllable(String syllable_text, boolean bold, boolean italic, boolean force_hyphen, List<AssignedChordData> assigned_chords) {
		this.syllableText = syllable_text; this.bold = bold; this.italic = italic; this.forceHyphen = force_hyphen;
		this.assignedChords = assigned_chords; this.firstSyllableInWord = !syllable_text.startsWith("-");
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
	public boolean isForcingHyphen() {
		return forceHyphen;
	}
	public String getFormatData() {
		return (isBold() ? "b" : "") + (isItalic() ? "i" : "") + (isForcingHyphen() ? "h" : "");
	}
	public List<AssignedChordData> getAssignedChords() {
		return assignedChords;
	}

	public static class AssignmentSyllableBuilder {

		private String _syllableText = "Text";
		private boolean _bold = false;
		private boolean _italic = false;
		private boolean _forceHyphen = false;

		private List<AssignedChordData> _assignedChords = new ArrayList<>();

		public AssignmentSyllableBuilder() { }

		public AssignmentSyllable buildAssignmentSyllable() {
			return new AssignmentSyllable(_syllableText, _bold, _italic, _forceHyphen, _assignedChords);
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
		public AssignmentSyllableBuilder forceHyphen(boolean _forceHyphen) {
			this._forceHyphen = _forceHyphen;
			return this;
		}
		public AssignmentSyllableBuilder assignedChords(List<AssignedChordData> _assignedChords) {
			this._assignedChords = _assignedChords;
			return this;
		}

	}
}
