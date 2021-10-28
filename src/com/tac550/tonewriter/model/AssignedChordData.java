package com.tac550.tonewriter.model;

import com.tac550.tonewriter.view.ChantChordController;
import com.tac550.tonewriter.view.VerseLineViewController;

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

	public ChantChordController getChordController(VerseLineViewController line_controller) {
		return line_controller.getChordByIndex(chordIndex);
	}
	public int getChordIndex() {
		return chordIndex;
	}

}
