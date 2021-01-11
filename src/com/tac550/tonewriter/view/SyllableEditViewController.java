package com.tac550.tonewriter.view;

import com.tac550.tonewriter.util.TWUtils;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class SyllableEditViewController {

	private VerseLineViewController parentController;
	
	@FXML private TextField syllableTextField;

	@FXML private GridPane barGrid;
	private final ToggleGroup beforeToggles = new ToggleGroup();
	@FXML private FlowPane beforePane;
	private final ToggleGroup afterToggles = new ToggleGroup();
	@FXML private FlowPane afterPane;

	private int initialBeforeBar;
	private int initialAfterBar;

	static final String[] beforeBarStrs = new String[] {"", "|", "||", ".|:", "[|:"};
	static final String[] afterBarStrs = new String[] {"", "|", "||", ".|:", "[|:", ":|.|:", ":|][|:", ":|]", ":|.", "|."};
	static final Image[] barImages = new Image[] {
			new Image(SyllableEditViewController.class.getResource("/media/bars/noBar.png").toExternalForm()),
			new Image(SyllableEditViewController.class.getResource("/media/bars/singleBar.png").toExternalForm()),
			new Image(SyllableEditViewController.class.getResource("/media/bars/doubleBar.png").toExternalForm()),
			new Image(SyllableEditViewController.class.getResource("/media/bars/beginRepeat.png").toExternalForm()),
			new Image(SyllableEditViewController.class.getResource("/media/bars/beginClosedRepeat.png").toExternalForm()),
			new Image(SyllableEditViewController.class.getResource("/media/bars/beginEndRepeat.png").toExternalForm()),
			new Image(SyllableEditViewController.class.getResource("/media/bars/beginEndClosedRepeat.png").toExternalForm()),
			new Image(SyllableEditViewController.class.getResource("/media/bars/endClosedRepeat.png").toExternalForm()),
			new Image(SyllableEditViewController.class.getResource("/media/bars/endRepeat.png").toExternalForm()),
			new Image(SyllableEditViewController.class.getResource("/media/bars/endBar.png").toExternalForm())};

	void setParentController(VerseLineViewController controller) {
		parentController = controller;

		if (parentController.notFirstInItem())
			barGrid.getChildren().removeIf(node -> GridPane.getColumnIndex(node) == null
					|| GridPane.getColumnIndex(node) == 0);
		else
			setUpBarOptions(beforeBarStrs, beforePane, beforeToggles);

		setUpBarOptions(afterBarStrs, afterPane, afterToggles);
	}

	private void setUpBarOptions(String[] barStrs, FlowPane flowPane, ToggleGroup toggleGroup) {
		for (int i = 0; i < barStrs.length; i++) {
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

		if (beforeToggles.getToggles().size() > 0)
			beforeToggles.selectToggle(beforeToggles.getToggles().get(before));
		afterToggles.selectToggle(afterToggles.getToggles().get(after));
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

		int selectedBefore = beforeToggles.getToggles().size() > 0 ?
				beforeToggles.getToggles().indexOf(beforeToggles.getSelectedToggle()) : initialBeforeBar;
		int selectedAfter = afterToggles.getToggles().indexOf(afterToggles.getSelectedToggle());
		if (selectedBefore != initialBeforeBar || selectedAfter != initialAfterBar)
			parentController.selectBarlines(beforeBarStrs[selectedBefore], afterBarStrs[selectedAfter]);

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
