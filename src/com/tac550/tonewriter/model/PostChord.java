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
	public void addPrepChord() {}
	@Override
	public void addPostChord() {}

	@Override
	public void deleteAll() {
		((RecitingChord) getAssociatedRecitingChord()).getPosts().remove(this);

		chantLineController.removeChord(this);
	}

	@Override
	public void rotatePrepsOrPosts(ChantChordController source, ChantChordController target) {

	}

}