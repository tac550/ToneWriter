package com.tac550.tonewriter.model;

import com.tac550.tonewriter.view.ChantChordController;

public abstract class SubChord extends ChantChordController {
	private MainChord associatedMainChord;

	public void setAssociatedChord(MainChord chord) {
		associatedMainChord = chord;
	}
	public MainChord getAssociatedMainChord() {
		return associatedMainChord;
	}

}
