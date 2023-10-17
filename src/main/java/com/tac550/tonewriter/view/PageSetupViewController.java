package com.tac550.tonewriter.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class PageSetupViewController {

	private TopSceneController parentController;
	
	@FXML private ChoiceBox<String> paperSizeChoice;
	@FXML private CheckBox noHeaderCheckBox;

	@FXML private ToggleGroup pageNumberPosGroup;
	@FXML private RadioButton evenSpreadRadioButton;
	@FXML private RadioButton oddSpreadRadioButton;

	@FXML private CheckBox equalMarginsCheckBox;
	@FXML private TextField topMargin, bottomMargin, leftMargin, rightMargin;
	private List<TextField> marginFields;
	@FXML private ChoiceBox<String> topUnits, bottomUnits, leftUnits, rightUnits;
	private List<ChoiceBox<String>> unitChoices;
	private static final String[] marginUnitsOptions = {"mm", "cm", "in", "pt"};

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

	void setMargins(float top, String top_units, float bottom, String bottom_units,
					float left, String left_units, float right, String right_units) {
		Platform.runLater(() -> {
			equalMarginsCheckBox.setSelected(top == bottom && top == left && top == right && top_units.equals(bottom_units)
					&& top_units.equals(left_units) && top_units.equals(right_units));

			topMargin.setText(String.valueOf(top));
			if (!equalMarginsCheckBox.isSelected()) {
				bottomMargin.setText(String.valueOf(bottom));
				leftMargin.setText(String.valueOf(left));
				rightMargin.setText(String.valueOf(right));
			}

			topUnits.setValue(top_units);
			if (!equalMarginsCheckBox.isSelected()) {
				bottomUnits.setValue(bottom_units);
				leftUnits.setValue(left_units);
				rightUnits.setValue(right_units);
			}
		});
	}

	@FXML private void initialize() {
		paperSizeChoice.getItems().addAll(TopSceneController.PAPER_SIZES);

		noHeaderCheckBox.selectedProperty().addListener((ov, oldVal, newVal) -> {
			for (Toggle toggle : pageNumberPosGroup.getToggles())
				((Node) toggle).setDisable(newVal);
		});

		unitChoices = List.of(topUnits, bottomUnits, leftUnits, rightUnits);
		for (ChoiceBox<String> choiceBox : unitChoices)
			choiceBox.getItems().addAll(marginUnitsOptions);

		marginFields = List.of(topMargin, bottomMargin, leftMargin, rightMargin);
		for (TextField textField : marginFields)
			textField.setTextFormatter(new TextFormatter<>(c -> {
				if (c.getControlNewText().isEmpty()) return c;
				try {
					float result = Float.parseFloat(c.getControlNewText());

					if (result < 0) return null;
					else return c;
				} catch (NumberFormatException e) {
					return null;
				}
			}));

		equalMarginsCheckBox.selectedProperty().addListener((ov, oldVal, newVal) -> {
			for (int i = 1; i < unitChoices.size(); i++) {
				unitChoices.get(i).setDisable(newVal);
				if (newVal)
					unitChoices.get(i).valueProperty().bind(topUnits.valueProperty());
				else
					unitChoices.get(i).valueProperty().unbind();
			}
			for (int i = 1; i < marginFields.size(); i++) {
				marginFields.get(i).setDisable(newVal);
				if (newVal)
					marginFields.get(i).textProperty().bind(topMargin.textProperty());
				else
					marginFields.get(i).textProperty().unbind();
			}
		});
		// Select checkbox, triggering above listener for initial state
		equalMarginsCheckBox.setSelected(true);
	}

	@FXML private void handleOK() {
		parentController.setPaperSize(paperSizeChoice.getValue());
		parentController.setNoHeader(noHeaderCheckBox.isSelected());
		parentController.setEvenSpread(evenSpreadRadioButton.isSelected());
		parentController.setMargins(Float.parseFloat(topMargin.getText()), topUnits.getValue(),
				Float.parseFloat(bottomMargin.getText()), bottomUnits.getValue(),
				Float.parseFloat(leftMargin.getText()), leftUnits.getValue(),
				Float.parseFloat(rightMargin.getText()), rightUnits.getValue());

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
