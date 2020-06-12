package com.tac550.tonewriter.model;

import javafx.fxml.FXML;

public class PostChord extends SubChord {

	@FXML protected void initialize() {
		super.initialize();
		numText.setText("Post");
		disableButtons();
	}

	@Override
	public void delete() {
		((RecitingChord) getAssociatedMainChord()).getPosts().remove(this);

		chantLineController.removeChord(this);
	}

}
