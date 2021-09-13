package com.tac550.tonewriter.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChantChord {

	private final String name;
	private final String comment;

	private final List<ChantChord> preps;
	private final List<ChantChord> posts;

	private final String soprano;
	private final String alto;
	private final String tenor;
	private final String bass;

	public ChantChord(String name, String comment, List<ChantChord> preps, List<ChantChord> posts, String soprano, String alto, String tenor, String bass) {
		this.name = name; this.comment = comment; this.preps = preps; this.posts = posts;
		this.soprano = soprano; this.alto = alto; this.tenor = tenor; this.bass = bass;
	}

	public String getName() {
		return name;
	}
	public String getComment() {
		return comment;
	}
	public List<ChantChord> getPreps() {
		return preps;
	}
	public List<ChantChord> getPosts() {
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

	public void addPrep(ChantChord prep) {
		preps.add(prep);
	}
	public void addPost(ChantChord post) {
		posts.add(post);
	}

	public static class ChantChordBuilder {
		private String _name = "";
		private String _comment = "";

		private List<ChantChord> _preps = new ArrayList<>();
		private List<ChantChord> _posts = new ArrayList<>();

		private String _soprano = "r";
		private String _alto = "r";
		private String _tenor = "r";
		private String _bass = "r";

		public ChantChordBuilder() { }

		public ChantChord buildChord() {
			return new ChantChord(_name, _comment, _preps, _posts, _soprano, _alto, _tenor, _bass);
		}

		public ChantChordBuilder name(String _name) {
			this._name = _name;
			return this;
		}
		public ChantChordBuilder comment(String _comment) {
			this._comment = _comment;
			return this;
		}
		public ChantChordBuilder soprano(String _soprano) {
			this._soprano = _soprano;
			return this;
		}
		public ChantChordBuilder alto(String _alto) {
			this._alto = _alto;
			return this;
		}
		public ChantChordBuilder tenor(String _tenor) {
			this._tenor = _tenor;
			return this;
		}
		public ChantChordBuilder bass(String _bass) {
			this._bass = _bass;
			return this;
		}
	}
}
