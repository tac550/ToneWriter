package com.tac550.tonewriter.model;

import java.util.*;

public class ChantPhrase {

	private final String name;
	private final String comment;

	private final List<Chord> chords; // In the order they appear in .tone files.

	public ChantPhrase(String name, String comment, List<Chord> chords) {
		this.name = name; this.comment = comment; this.chords = chords;
	}

	public String getName() {
		return name;
	}
	public String getComment() {
		return comment;
	}
	public boolean hasComment() {
		return !comment.isEmpty();
	}
	public List<Chord> getChords() {
		return chords;
	}

	public List<Chord> getChordsMelodyOrder() {
		List<Chord> inOrder = new ArrayList<>();

		Queue<Chord> preps = new ArrayDeque<>();
		Stack<Chord> posts = new Stack<>();
		Chord mainChord = chords.getFirst(); // First chord in save order will always be a main chord.
		assert mainChord.getName().matches("\\d") || mainChord.getName().equalsIgnoreCase("End");

		for (int i = 1; i < chords.size(); i++) {
			Chord current = chords.get(i);
			if (current.getName().matches("\\d") || current.getName().equalsIgnoreCase("End")) {
				while (!preps.isEmpty())
					inOrder.add(preps.remove());
				inOrder.add(mainChord);
				while (!posts.isEmpty())
					inOrder.add(posts.pop());

				mainChord = current;
			} else if (current.getName().equalsIgnoreCase("Prep")) {
				preps.add(current);
			} else {
				posts.add(current);
			}
		}

		while (!preps.isEmpty())
			inOrder.add(preps.remove());
		inOrder.add(mainChord);
		while (!posts.isEmpty())
			inOrder.add(posts.pop());

		return inOrder;
	}

	@Override
	public String toString() {
		StringBuilder finalString = new StringBuilder();

		finalString.append(getName()).append(String.format("%n"));

		// Place chant line comment on the next line, if any.
		if (hasComment())
			finalString.append(String.format("Comment: %s%n", getComment()));

		// For each chord in the chant line...
		for (Chord chord : getChords()) {
			if (chord.getName().matches("\\d") || chord.getName().equalsIgnoreCase("End")) {
				finalString.append(String.format("%s: %s%s%n", chord.getName().equalsIgnoreCase("End") ? "END" : chord.getName(), chord.getFields(),
						chord.hasComment() ? ": " + chord.getComment() : ""));
				for (Chord prep : chord.getPreps()) { // Preps save out first
					finalString.append(String.format("\tPrep: %s%s%n", prep.getFields(),
							prep.hasComment() ? ": " + prep.getComment() : ""));
				}
				for (Chord post : chord.getPosts()) { // Posts second
					finalString.append(String.format("\tPost: %s%s%n", post.getFields(),
							post.hasComment() ? ": " + post.getComment() : ""));
				}
			}
		}

		return finalString.toString();
	}

	public boolean isSimilarTo(ChantPhrase other) {
		if (other == null) return false;

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

		private List<Chord> _chords = new ArrayList<>();

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
		public ChantPhraseBuilder chords(List<Chord> _chords) {
			this._chords = _chords;
			return this;
		}
	}
}
