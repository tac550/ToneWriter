package com.tac550.tonewriter.model;

import com.tac550.tonewriter.io.LilyPondInterface;
import com.tac550.tonewriter.view.ChantChordController;
import com.tac550.tonewriter.view.VerseLineViewController;

public class AssignedChordData {

	private final VerseLineViewController lineController;

	private final String syllable;
	private final int chordIndex;
	private String duration;

	public AssignedChordData(String syllable_data, int chord_index, VerseLineViewController line_controller) {
		lineController = line_controller;

		syllable = syllable_data;
		chordIndex = chord_index;
		duration = "4";
	}

	public String getSyllable() {
		return syllable;
	}
	public void setDuration(String new_duration) {
		duration = new_duration;
	}
	public String getDuration() {
		return duration;
	}

	public ChantChordController getChordController() {
		return lineController.getChordByIndex(chordIndex);
	}

	public String getPart(int part_index) {
		return switch (part_index) {
			case 0 -> getSoprano();
			case 1 -> getAlto();
			case 2 -> getTenor();
			case 3 -> getBass();
			default -> "";
		};

	}

	private String getSoprano() {
		return LilyPondInterface.parseNoteRelative(lineController.getChordByIndex(chordIndex).getFields().split("-")[0],
				LilyPondInterface.ADJUSTMENT_SOPRANO) + duration;
	}
	private String getAlto() {
		return LilyPondInterface.parseNoteRelative(lineController.getChordByIndex(chordIndex).getFields().split("-")[1],
				LilyPondInterface.ADJUSTMENT_ALTO) + duration;
	}
	private String getTenor() {
		return LilyPondInterface.parseNoteRelative(lineController.getChordByIndex(chordIndex).getFields().split("-")[2],
				LilyPondInterface.ADJUSTMENT_TENOR) + duration;
	}
	private String getBass() {
		return LilyPondInterface.parseNoteRelative(lineController.getChordByIndex(chordIndex).getFields().split("-")[3],
				LilyPondInterface.ADJUSTMENT_BASS) + duration;
	}

}
