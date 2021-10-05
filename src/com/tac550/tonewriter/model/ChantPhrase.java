package com.tac550.tonewriter.model;

import java.util.ArrayList;
import java.util.List;

public class ChantPhrase {

	private final String name;
	private final String comment;

	private final List<ChantChord> chords; // In the order they appear in .tone files.

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

	@Override
	public String toString() {
		StringBuilder finalString = new StringBuilder();

		finalString.append(getName()).append(String.format("%n"));

		// Place chant line comment on the first line, if any.
		if (!getComment().isEmpty())
			finalString.append(String.format("Comment: %s%n", getComment()));

		// For each chord in the chant line...
		for (ChantChord chord : getChords()) {
			if (chord.getName().matches("[0-9]") || chord.getName().equalsIgnoreCase("End")) {
				finalString.append(String.format("%s: %s%s%n", chord.getName().equalsIgnoreCase("End") ? "END" : chord.getName(), chord.getFields(),
						!chord.getComment().isEmpty() ? ": " + chord.getComment() : ""));
				for (ChantChord prep : chord.getPreps()) { // Preps save out first
					finalString.append(String.format("\tPrep: %s%s%n", prep.getFields(),
							!prep.getComment().isEmpty() ? ": " + prep.getComment() : ""));
				}
				for (ChantChord post : chord.getPosts()) { // Posts second
					finalString.append(String.format("\tPost: %s%s%n", post.getFields(),
							!post.getComment().isEmpty() ? ": " + post.getComment() : ""));
				}
			}
		}

		return finalString.toString();
	}

	public boolean isSimilarTo(ChantPhrase other) {
		if (!other.getName().equals(this.getName()) || other.getChords().size() != this.getChords().size())
			return false;
		for (int i = 0; i < this.getChords().size(); i++) {
			if (!other.getChords().get(i).getName().equals(this.getChords().get(i).getName()))
				return false;
		}

		return true;
	}

	public static class ChantPhraseBuilder {
		private String _name = "";
		private String _comment = "";

		private List<ChantChord> _chords = new ArrayList<>();

		public ChantPhraseBuilder() { }

		public ChantPhrase buildChantPhrase() {
			return new ChantPhrase(_name, _comment, _chords);
		}

		public ChantPhraseBuilder name(String _name) {
			this._name = _name;
			return this;
		}
		public ChantPhraseBuilder comment(String _comment) {
			this._comment = _comment;
			return this;
		}
		public ChantPhraseBuilder chords(List<ChantChord> _chords) {
			this._chords = _chords;
			return this;
		}
	}
}
