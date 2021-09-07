package com.tac550.tonewriter.model;

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
}
