package com.tac550.tonewriter.model;

public class AssignedChordData {

	private final int chordIndex;
	private String duration;

	public AssignedChordData(int chord_index) {
		chordIndex = chord_index;
		duration = "4";
	}
	public AssignedChordData(int chord_index, String duration) {
		this.chordIndex = chord_index;
		this.duration = duration;
	}

	public void setDuration(String new_duration) {
		duration = new_duration;
	}
	public String getDuration() {
		return duration;
	}

	public int getChordIndex() {
		return chordIndex;
	}

}
