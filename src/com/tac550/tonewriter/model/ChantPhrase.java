package com.tac550.tonewriter.model;

import java.util.List;

public class ChantPhrase {

	private String name;
	private String comment;

	private List<ChantChord> chords;

	public ChantPhrase(String name, String comment, List<ChantChord> chords) {
		this.name = name; this.comment = comment; this.chords = chords;
	}

	public String getName() {
		return name;
	}
	public String getComment() {
		return comment;
	}
	public List<ChantChord> getChords() {
		return chords;
	}

}
