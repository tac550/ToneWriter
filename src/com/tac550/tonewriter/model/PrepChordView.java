package com.tac550.tonewriter.model;

import javafx.fxml.FXML;

public class PrepChordView extends SubChordView {

	@FXML protected void initialize() {
		super.initialize();
		numText.setText("Prep");
		disableButtons();
	}

	@Override
	public void delete() {
		getAssociatedMainChord().getPreps().remove(this);
		chantPhraseController.removeChord(this);
	}

}
