package com.tac550.tonewriter.model;

import com.tac550.tonewriter.io.LilyPondWriter;
import com.tac550.tonewriter.view.ChantChordController;

public class ChordData {

	private String syllable;
	private ChantChordController chord;
	private String duration;
	
	public ChordData(String syllable_data, ChantChordController chord_data) {
		syllable = syllable_data;
		chord = chord_data;
		duration = "4";
	}
	
	public String getSyllable() {
		return syllable;
	}
	public ChantChordController getChord() {
		return chord;
	}
	public String getDuration() {
		return duration;
	}
	public void setDuration(String new_duration) {
		duration = new_duration;
	}
	
	public String getPart(int part_index) {
		switch (part_index) {
		
		case 0:
			return getSoprano();
			
		case 1:
			return getAlto();
			
		case 2:
			return getTenor();
			
		case 3:
			return getBass();
			
		default:
			return "";
		}
		
	}
	
	public String getSoprano() {
		return LilyPondWriter.parseNoteRelative(chord.getFields().split("-")[0], LilyPondWriter.ADJUSTMENT_SOPRANO) + duration;
	}
	public String getAlto() {
		return LilyPondWriter.parseNoteRelative(chord.getFields().split("-")[1], LilyPondWriter.ADJUSTMENT_ALTO) + duration;
	}
	public String getTenor() {
		return LilyPondWriter.parseNoteRelative(chord.getFields().split("-")[2], LilyPondWriter.ADJUSTMENT_TENOR) + duration;
	}
	public String getBass() {
		return LilyPondWriter.parseNoteRelative(chord.getFields().split("-")[3], LilyPondWriter.ADJUSTMENT_BASS) + duration;
	}
	
}
