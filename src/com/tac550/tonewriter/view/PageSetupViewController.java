package com.tac550.tonewriter.view;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.stage.Stage;

public class PageSetupViewController {

	private TopSceneController parentController;
	
	@FXML private ChoiceBox<String> paperSizeChoice;
	@FXML private CheckBox noHeaderCheckBox;

	void setParentController(TopSceneController controller) {
		parentController = controller;
	}

	void setPaperSize(String size) {
		paperSizeChoice.setValue(size);
	}

	void setNoHeader(boolean no_header) {
		noHeaderCheckBox.setSelected(no_header);
	}

	@FXML private void initialize() {
		paperSizeChoice.getItems().addAll(TopSceneController.PAPER_SIZES);
	}

	@FXML private void handleOK() {
		parentController.setPaperSize(paperSizeChoice.getValue());
		parentController.setNoHeader(noHeaderCheckBox.isSelected());

		closeStage();
	}
	
	@FXML private void handleCancel() {
		closeStage();
	}
	
	private void closeStage() {
		Stage stage = (Stage) paperSizeChoice.getScene().getWindow();
		stage.close();
	}
	
}
