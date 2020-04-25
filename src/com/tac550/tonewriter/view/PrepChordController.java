package com.tac550.tonewriter.view;

import javafx.fxml.FXML;

public class PrepChordController extends ChantChordController {

	@FXML protected void initialize() {
		super.initialize();
		numText.setText("Prep");
		disableButtons();
	}

}
