package com.tac550.tonewriter.model;

import com.tac550.tonewriter.io.LilyPondInterface;
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

	public String getPart(int part_index, VerseLineViewController vc) {
		return switch (part_index) {
			case 0 -> getSoprano(vc);
			case 1 -> getAlto(vc);
			case 2 -> getTenor(vc);
			case 3 -> getBass(vc);
			default -> "";
		};
	}

	private String getSoprano(VerseLineViewController vc) {
		return LilyPondInterface.adjustOctave(vc.getChordByIndex(chordIndex).getFields().split("-")[0],
				LilyPondInterface.ADJUSTMENT_SOPRANO) + duration;
	}
	private String getAlto(VerseLineViewController vc) {
		return LilyPondInterface.adjustOctave(vc.getChordByIndex(chordIndex).getFields().split("-")[1],
				LilyPondInterface.ADJUSTMENT_ALTO) + duration;
	}
	private String getTenor(VerseLineViewController vc) {
		return LilyPondInterface.adjustOctave(vc.getChordByIndex(chordIndex).getFields().split("-")[2],
				LilyPondInterface.ADJUSTMENT_TENOR) + duration;
	}
	private String getBass(VerseLineViewController vc) {
		return LilyPondInterface.adjustOctave(vc.getChordByIndex(chordIndex).getFields().split("-")[3],
				LilyPondInterface.ADJUSTMENT_BASS) + duration;
	}

}
