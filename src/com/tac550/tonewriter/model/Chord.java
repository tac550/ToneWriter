package com.tac550.tonewriter.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Chord {

	private final String name;
	private final String comment;

	private final List<Chord> preps;
	private final List<Chord> posts;

	private final String soprano;
	private final String alto;
	private final String tenor;
	private final String bass;

	public Chord(String name, String comment, List<Chord> preps, List<Chord> posts, String soprano, String alto, String tenor, String bass) {
		this.name = name; this.comment = comment; this.preps = preps; this.posts = posts;
		this.soprano = soprano; this.alto = alto; this.tenor = tenor; this.bass = bass;
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
	public List<Chord> getPreps() {
		return preps;
	}
	public List<Chord> getPosts() {
		return posts;
	}
	public String getSoprano() {
		return soprano;
	}
	public String getAlto() {
		return alto;
	}
	public String getTenor() {
		return tenor;
	}
	public String getBass() {
		return bass;
	}
	public String getFields() {
		return String.format(Locale.US,
				"%s-%s-%s-%s",
				getSoprano().isEmpty() ? " " : getSoprano(),
				getAlto().isEmpty() ? " " : getAlto(),
				getTenor().isEmpty() ? " " : getTenor(),
				getBass().isEmpty() ? " " : getBass());
	}
	public String getPart(int index) {
		return switch (index) {
			case 0 -> getSoprano();
			case 1 -> getAlto();
			case 2 -> getTenor();
			case 3 -> getBass();
			default -> "";
		};
	}

	public void addPrep(Chord prep) {
		preps.add(prep);
	}
	public void addPost(Chord post) {
		posts.add(post);
	}

	public static class ChordBuilder {
		private String _name = "";
		private String _comment = "";

		private final List<Chord> _preps = new ArrayList<>();
		private final List<Chord> _posts = new ArrayList<>();

		private String _soprano = "r";
		private String _alto = "r";
		private String _tenor = "r";
		private String _bass = "r";

		public ChordBuilder() { }

		public Chord buildChord() {
			return new Chord(_name, _comment, _preps, _posts, _soprano, _alto, _tenor, _bass);
		}

		public ChordBuilder name(String _name) {
			this._name = _name;
			return this;
		}
		public ChordBuilder comment(String _comment) {
			this._comment = _comment;
			return this;
		}
		public ChordBuilder soprano(String _soprano) {
			this._soprano = _soprano;
			return this;
		}
		public ChordBuilder alto(String _alto) {
			this._alto = _alto;
			return this;
		}
		public ChordBuilder tenor(String _tenor) {
			this._tenor = _tenor;
			return this;
		}
		public ChordBuilder bass(String _bass) {
			this._bass = _bass;
			return this;
		}
	}
}
