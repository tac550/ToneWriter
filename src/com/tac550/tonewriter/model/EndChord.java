package com.tac550.tonewriter.model;

import javafx.fxml.FXML;

public class EndChord extends MainChord {

	@FXML protected void initialize() {
		super.initialize();
		numText.setText("End");
		preButton.setDisable(false);
		posButton.setDisable(true);
	}

}
