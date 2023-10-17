package com.tac550.tonewriter.view.chord;

import javafx.fxml.FXML;

public class EndChordView extends MainChordView {

	@FXML protected void initialize() {
		super.initialize();
		numText.setText("End");
		preButton.setDisable(false);
		posButton.setDisable(true);
	}

}
