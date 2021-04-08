package com.tac550.tonewriter.view;

import com.tac550.tonewriter.util.TWUtils;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

import java.util.Objects;

public class VLineEditViewController {

	private VerseLineViewController parentController;
	
	@FXML private TextField syllableTextField;

	private final ToggleGroup beforeToggles = new ToggleGroup();
	@FXML private FlowPane beforePane;
	private final ToggleGroup afterToggles = new ToggleGroup();
	@FXML private FlowPane afterPane;

	@FXML private CheckBox disableBreaksCheckBox;

	private int initialBeforeBar;
	private int initialAfterBar;

	// Limit the number of selectable before barline options to the first beforeOptionsLimit items in afterBarStrs.
	static final int firstBarOptionsLimit = 3;
	static final String[] barStrings = new String[] {" ", ".|:", "[|:", "|", "||", ":|.|:", ":|][|:", "|.", "'", ":|.", ":|]", "!"};
	static final Image[] barImages = new Image[] {
			new Image(Objects.requireNonNull(VLineEditViewController.class.getResource("/media/bars/noBar.png")).toExternalForm()),
			new Image(Objects.requireNonNull(VLineEditViewController.class.getResource("/media/bars/beginRepeat.png")).toExternalForm()),
			new Image(Objects.requireNonNull(VLineEditViewController.class.getResource("/media/bars/beginClosedRepeat.png")).toExternalForm()),
			new Image(Objects.requireNonNull(VLineEditViewController.class.getResource("/media/bars/singleBar.png")).toExternalForm()),
			new Image(Objects.requireNonNull(VLineEditViewController.class.getResource("/media/bars/doubleBar.png")).toExternalForm()),
			new Image(Objects.requireNonNull(VLineEditViewController.class.getResource("/media/bars/beginEndRepeat.png")).toExternalForm()),
			new Image(Objects.requireNonNull(VLineEditViewController.class.getResource("/media/bars/beginEndClosedRepeat.png")).toExternalForm()),
			new Image(Objects.requireNonNull(VLineEditViewController.class.getResource("/media/bars/endBar.png")).toExternalForm()),
			new Image(Objects.requireNonNull(VLineEditViewController.class.getResource("/media/bars/tickBar.png")).toExternalForm()),
			new Image(Objects.requireNonNull(VLineEditViewController.class.getResource("/media/bars/endRepeat.png")).toExternalForm()),
			new Image(Objects.requireNonNull(VLineEditViewController.class.getResource("/media/bars/endClosedRepeat.png")).toExternalForm()),
			new Image(Objects.requireNonNull(VLineEditViewController.class.getResource("/media/bars/dashedBar.png")).toExternalForm())};

	void setParentController(VerseLineViewController controller) {
		parentController = controller;

		setUpBarOptions(parentController.notFirstInItem() ? barStrings.length : firstBarOptionsLimit,
				beforePane, beforeToggles);
		setUpBarOptions(barStrings.length, afterPane, afterToggles);
	}

	private void setUpBarOptions(int barStrs, FlowPane flowPane, ToggleGroup toggleGroup) {
		for (int i = 0; i < barStrs; i++) {
			RadioButton optionButton = new RadioButton();
			ImageView optionImage = new ImageView(barImages[i]);
			optionImage.setPreserveRatio(true);
			optionImage.setFitHeight(50);
			optionButton.setGraphic(optionImage);

			optionButton.setToggleGroup(toggleGroup);
			if (i == 0) optionButton.setSelected(true);

			flowPane.getChildren().add(optionButton);
		}
	}
	
	void setSyllableText(String text) {
		syllableTextField.setText(text);
	}

	void setBarSelections(int before, int after) {
		initialBeforeBar = before;
		initialAfterBar = after;

		if (beforeToggles.getToggles().size() > before)
			beforeToggles.selectToggle(beforeToggles.getToggles().get(before));
		if (afterToggles.getToggles().size() > after)
			afterToggles.selectToggle(afterToggles.getToggles().get(after));
	}

	void setDisableLineBreaks(boolean disable) {
		disableBreaksCheckBox.setSelected(disable);
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

		// Set bar line info if it differs from initial values
		int selectedBefore = beforeToggles.getToggles().indexOf(beforeToggles.getSelectedToggle());
		int selectedAfter = afterToggles.getToggles().indexOf(afterToggles.getSelectedToggle());
		if (selectedBefore != initialBeforeBar || selectedAfter != initialAfterBar)
			parentController.setBarlines(barStrings[selectedBefore], barStrings[selectedAfter]);

		// Same for whether to disable line breaks
		if (disableBreaksCheckBox.isSelected() != parentController.getDisableLineBreaks())
			parentController.setDisableLineBreaks(disableBreaksCheckBox.isSelected());

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
