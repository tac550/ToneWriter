package com.tac550.tonewriter.model;

import com.tac550.tonewriter.view.ChantChordController;
import javafx.fxml.FXML;

public class PostChord extends SubChord {

	@FXML protected void initialize() {
		super.initialize();
		numText.setText("Post");
		disableButtons();
	}

	@Override
	public void deleteAll() {
		((RecitingChord) getAssociatedMainChord()).getPosts().remove(this);

		chantLineController.removeChord(this);
	}

}
