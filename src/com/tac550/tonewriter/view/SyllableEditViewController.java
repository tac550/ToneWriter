package com.tac550.tonewriter.view;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class SyllableEditViewController {

	VerseLineViewController parentController;
	
	@FXML TextField syllableTextField;
	
	public void setParentController(VerseLineViewController controller) {
		parentController = controller;
	}
	
	@FXML private void initialize() {
		
	}
	
	public void setSyllableText(String text) {
		syllableTextField.setText(text);
	}
	
	@FXML private void handleOK() {
		if (!syllableTextField.getText().isEmpty()) { // Sending an empty line would cause the verse line controller to think it's a separator. 
			parentController.setVerseLine(syllableTextField.getText());	
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
