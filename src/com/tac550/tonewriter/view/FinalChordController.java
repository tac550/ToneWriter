package com.tac550.tonewriter.view;

import javafx.fxml.FXML;

public class FinalChordController extends ChantChordController {

	@FXML protected void initialize() {
		super.initialize();
		numText.setText("End");
		preButton.setDisable(false);
		posButton.setDisable(true);
	}

}
