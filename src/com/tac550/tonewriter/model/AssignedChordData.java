package com.tac550.tonewriter.model;

import com.tac550.tonewriter.io.LilyPondInterface;
import com.tac550.tonewriter.view.ChantChordController;

public class AssignedChordData {

	private final String syllable;
	private final ChantChordController chord;
	private String duration;

	public AssignedChordData(String syllable_data, ChantChordController chord_data) {
		syllable = syllable_data;
		chord = chord_data;
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
		return LilyPondInterface.parseNoteRelative(chord.getFields().split("-")[0],
				LilyPondInterface.ADJUSTMENT_SOPRANO) + duration;
	}
	private String getAlto() {
		return LilyPondInterface.parseNoteRelative(chord.getFields().split("-")[1],
				LilyPondInterface.ADJUSTMENT_ALTO) + duration;
	}
	private String getTenor() {
		return LilyPondInterface.parseNoteRelative(chord.getFields().split("-")[2],
				LilyPondInterface.ADJUSTMENT_TENOR) + duration;
	}
	private String getBass() {
		return LilyPondInterface.parseNoteRelative(chord.getFields().split("-")[3],
				LilyPondInterface.ADJUSTMENT_BASS) + duration;
	}

}
