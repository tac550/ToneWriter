package com.tac550.tonewriter.model;

import com.tac550.tonewriter.view.ChordViewController;

public abstract class SubChordView extends ChordViewController {
	private MainChordView associatedMainChord;

	public void setAssociatedChord(MainChordView chord) {
		associatedMainChord = chord;
	}
	public MainChordView getAssociatedMainChord() {
		return associatedMainChord;
	}

}
