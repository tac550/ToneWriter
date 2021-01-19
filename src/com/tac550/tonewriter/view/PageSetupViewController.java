package com.tac550.tonewriter.view;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class PageSetupViewController {

	private TopSceneController parentController;
	
	@FXML private ChoiceBox<String> paperSizeChoice;
	@FXML private CheckBox noHeaderCheckBox;

	@FXML private ToggleGroup pageNumberPosGroup;
	@FXML private RadioButton evenSpreadRadioButton;
	@FXML private RadioButton oddSpreadRadioButton;

	void setParentController(TopSceneController controller) {
		parentController = controller;
	}

	void setPaperSize(String size) {
		paperSizeChoice.setValue(size);
	}

	void setNoHeader(boolean no_header) {
		noHeaderCheckBox.setSelected(no_header);
	}
	void setSpreadSetting(boolean even_spread) {
		if (even_spread) evenSpreadRadioButton.setSelected(true);
		else oddSpreadRadioButton.setSelected(true);
	}

	@FXML private void initialize() {
		paperSizeChoice.getItems().addAll(TopSceneController.PAPER_SIZES);

		noHeaderCheckBox.selectedProperty().addListener((ov, oldVal, newVal) -> {
			for (Toggle toggle : pageNumberPosGroup.getToggles())
				((Node) toggle).setDisable(newVal);
		});
	}

	@FXML private void handleOK() {
		parentController.setPaperSize(paperSizeChoice.getValue());
		parentController.setNoHeader(noHeaderCheckBox.isSelected());
		parentController.setEvenSpread(evenSpreadRadioButton.isSelected());

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
