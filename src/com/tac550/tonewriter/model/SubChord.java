package com.tac550.tonewriter.model;

import com.tac550.tonewriter.view.ChantChordController;

public abstract class SubChord extends ChantChordController {
	private ChantChordController associatedRecitingChord;

	public void setAssociatedChord(ChantChordController chord) {
		associatedRecitingChord = chord;
	}
	public ChantChordController getAssociatedRecitingChord() {
		return associatedRecitingChord;
	}

}
