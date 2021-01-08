package com.tac550.tonewriter.view;

import com.tac550.tonewriter.util.TWUtils;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SyllableEditViewController {

	private VerseLineViewController parentController;
	
	@FXML TextField syllableTextField;
	
	void setParentController(VerseLineViewController controller) {
		parentController = controller;
	}
	
	void setSyllableText(String text) {
		syllableTextField.setText(text);
	}

	@FXML private void initialize() {
		syllableTextField.setTextFormatter(new TWUtils.inputFormatter());
	}

	@FXML private void handleOK() {
		// Sending an empty line would cause the verse line controller to think it's a separator.
		// Also don't send if it's the same as the existing verse line.
		if (!syllableTextField.getText().isEmpty() && !syllableTextField.getText().equals(parentController.getVerseLineText())) {
			parentController.setVerseLine(syllableTextField.getText());

			parentController.verseEdited();
		}
		
		closeStage();
	}
	
	@FXML private void handleCancel() {
		closeStage();
	}
	
	private void closeStage() {
		Stage stage = (Stage) syllableTextField.getScene().getWindow();
		stage.close();
	}
	
}
