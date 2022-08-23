package com.tac550.tonewriter.model;

import javafx.fxml.FXML;

public class PostChordView extends SubChordView {

	@FXML protected void initialize() {
		super.initialize();
		numText.setText("Post");
		disableButtons();
	}

	@Override
	public void delete() {
		getAssociatedMainChord().getPosts().remove(this);
		chantPhraseController.removeChord(this);
	}

}
