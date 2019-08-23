package com.tac550.tonewriter.view;

import com.tac550.tonewriter.model.MappingAction;
import com.tac550.tonewriter.model.VerseLine;
import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Stack;

public class VerseLineViewController {

	private MainSceneController parentController;

	private VerseLine verseLine;

	@FXML private StackPane rootLayout;
	@FXML private GridPane mainContentPane;

	private boolean isSeparatorLine = false;
	@FXML private VBox separatorIndicatorBox;

	private Stack<MappingAction> undoActions = new Stack<>();

	private ChantLineViewController[] associatedChantLines;
	private int selectedChantLine = 0;

	@FXML private ChoiceBox<String> chantLineChoice;
	@FXML private TextFlow lineTextFlow;
	@FXML private RowConstraints textRow;

	@FXML private Pane chordButtonPane;

	@FXML private Text chordEntryText;
	@FXML private Button skipChordButton;

	@FXML private Button expandButton;
	private ImageView plusIcon;
	private ImageView minusIcon;
	private boolean view_expanded = false;
	private double defaultHeight;

	private boolean changingAssignments = false;

	private int currentChordIndex = 0; // Index of the chord currently being assigned
	private int lastSyllableAssigned = -1; // Index of the last syllable to be clicked
	private ChantChordController currentChord; // The chord currently being assigned

	@FXML private void initialize() {
		chantLineChoice.getSelectionModel().selectedIndexProperty().addListener((ov, old_val, new_val) -> {

			if (changingAssignments) return;

			selectedChantLine = new_val.intValue();
			resetChordAssignment();

		});

		// Interface icons
		double iconSize = 22;
		plusIcon = new ImageView(getClass().getResource("/media/magnify.png").toExternalForm());
		minusIcon = new ImageView(getClass().getResource("/media/magnify-less.png").toExternalForm());
		plusIcon.setFitHeight(iconSize);
		plusIcon.setFitWidth(iconSize);
		minusIcon.setFitHeight(iconSize);
		minusIcon.setFitWidth(iconSize);

		// Button's initial state
		expandButton.setGraphic(plusIcon);

		defaultHeight = mainContentPane.getPrefHeight();

		refreshTextStyle();

	}

	void setParentController(MainSceneController parent) {
		parentController = parent;
	}

	void setVerseLine(String line_text) {

		if (lastSyllableAssigned != -1) {
			resetChordAssignment();
		}

		if (!line_text.isEmpty()) {

			isSeparatorLine = false;
			separatorIndicatorBox.setVisible(false);

			lineTextFlow.getChildren().clear();

			// Create the verseLine object, replacing any excess spaces with a single space.
			verseLine = new VerseLine(line_text.trim().replaceAll(" +", " "));

			for (String syllable : verseLine.getSyllables()) {
				SyllableText text = new SyllableText(syllable);
				text.setParent(this);
				text.setFont(Font.font("System", 28));
				text.setTextAlignment(TextAlignment.CENTER);

				lineTextFlow.getChildren().add(text);
			}

			lineTextFlow.autosize(); // Fixes additional syllables not being visible because the text flow didn't resize.

		} else {

			isSeparatorLine = true;
			separatorIndicatorBox.setVisible(true);

		}

	}

	String getVerseLineText() {
		return verseLine.getLine();
	}

	void setChantLines(ChantLineViewController[] chant_lines) {
		changingAssignments = true;
		// Remember previous chant line selection, if any.
		ChantLineViewController previousChantLine = null;
		if (associatedChantLines != null) {
			previousChantLine = associatedChantLines[selectedChantLine];
		}

		// Show the proper chant line on the left
		associatedChantLines = chant_lines;
		selectedChantLine = 0;
		chantLineChoice.getItems().clear();

		for (ChantLineViewController chantLine : associatedChantLines) {
			chantLineChoice.getItems().add(chantLine.getName().replace("alternate", "alt"));
		}

		chantLineChoice.getSelectionModel().select(0);

		// ChoiceBox highlighting if choices are available
		if (chantLineChoice.getItems().size() > 1) {
			chantLineChoice.setStyle("-fx-base: #fcfc2f");
		} else {
			chantLineChoice.setStyle("");
		}

		// Only reset chord assignments if the new chant line selection is different from the previous one.
		if (!associatedChantLines[selectedChantLine].equals(previousChantLine)) {
			resetChordAssignment();
		}

		changingAssignments = false;

	}

	private void resetChordAssignment() {
		// Set up the first step of chord assignment
		for (Node syllable : lineTextFlow.getChildren()) {
			((SyllableText) syllable).clearSelection();
		}
		chordButtonPane.getChildren().clear();

		currentChordIndex = 0;
		lastSyllableAssigned = -1;
		nextChordAssignment();

	}

	@FXML private void undo() {
		if (undoActions.empty()) {
			return;
		}
		MappingAction action = undoActions.pop();

		for (Button button : action.buttons) {
			chordButtonPane.getChildren().remove(button);
		}

		for (SyllableText text : action.syllableTexts) {
			text.removeLastChord();
		}

		lastSyllableAssigned = action.previousLastSyllableAssigned;
		currentChordIndex = action.previousChordIndex;
		nextChordAssignment();

	}

	@FXML private void remove() {
		parentController.removeVerseLine(this);
	}

	StackPane getRootLayout() {
		return rootLayout;
	}

	private void nextChordAssignment() {
		// If we have not placed or skipped the last chord...
		if (currentChordIndex < associatedChantLines[selectedChantLine].getChords().size()) {
			currentChord = associatedChantLines[selectedChantLine].getChords().get(currentChordIndex);
			chordEntryText.setText(String.valueOf(currentChord.getName()));
			chordEntryText.setFill(currentChord.getColor());
			skipChordButton.setDisable(false);
			currentChordIndex++;

			// Special disabling based on prep/post/normal
			if (currentChord.getType() < 0) {
				if (lastSyllableAssigned == -1) {
					((SyllableText) lineTextFlow.getChildren().get(0)).reactivate(); // Activate the first one.
					for (int i = 1; i < lineTextFlow.getChildren().size(); i++) { // If the first chord is a prep/post, do the special case.
						((SyllableText) lineTextFlow.getChildren().get(i)).deactivate();
					}
				} else {
					if (lastSyllableAssigned < lineTextFlow.getChildren().size() - 1) { // Avoid error if there is no next SyllableText
						((SyllableText) lineTextFlow.getChildren().get(lastSyllableAssigned)).reactivate(); // Reactivate current one, in case of undoing P/P from next syllable.
						((SyllableText) lineTextFlow.getChildren().get(lastSyllableAssigned+1)).reactivate(); // Reactivate next one, in case first chord P/P case was activated before.
						for (int i = lastSyllableAssigned+2; i < lineTextFlow.getChildren().size(); i++) {
							((SyllableText) lineTextFlow.getChildren().get(i)).deactivate();
						}
					}
				}
			} else {
				for (int i = lastSyllableAssigned; i < lineTextFlow.getChildren().size(); i++) {
					if (i < 0) continue; // In case we haven't assigned anything yet (lastSyllableAssigned is -1)
					((SyllableText) lineTextFlow.getChildren().get(i)).reactivate();
				}
			}

		} else { // If we have just placed or skipped the last chord
			currentChord = null;
			chordEntryText.setText("Done!");
			chordEntryText.setFill(Color.BLACK);
			skipChordButton.setDisable(true);

			for (Node syllable : lineTextFlow.getChildren()) { // Disable all syllables
				((SyllableText) syllable).deactivate();
			}
		}
	}

	void syllableClicked(SyllableText clicked_text) {
		// First, play the chord if chord playing is on.
		if (parentController.playMidiAsAssigned()) {
			currentChord.playMidi();
		}

		// Set up an undo action frame to store what happens.
		MappingAction undoFrame = new MappingAction();
		undoFrame.previousLastSyllableAssigned = lastSyllableAssigned;
		undoFrame.previousChordIndex = currentChordIndex - 1;

		final int indexClicked = lineTextFlow.getChildren().indexOf(clicked_text);

		if (lastSyllableAssigned == indexClicked) {
			lastSyllableAssigned--;
		} else if (lastSyllableAssigned >= 0) {
			// Deactivate the previous syllable which may still have been active from last time
			((SyllableText) lineTextFlow.getChildren().get(lastSyllableAssigned)).deactivate();
		}


		for (int i = lastSyllableAssigned+1; i <= indexClicked; i++) {
			SyllableText currentText = (SyllableText) lineTextFlow.getChildren().get(i);
			undoFrame.syllableTexts.add(currentText);

			Button noteButton;

			if (currentChordIndex == associatedChantLines[selectedChantLine].getChords().size()
					&& i == indexClicked) { // If placing the final instance of the last chord in the chant line, make it a half note.
				noteButton = createNoteButton(currentText, true, currentChord);

				undoFrame.buttons.add(noteButton);
				currentText.select(currentChord, noteButton);
				currentText.setNoteDuration(SyllableText.NOTE_HALF, noteButton);
			} else {
				noteButton = createNoteButton(currentText, false, currentChord);

				undoFrame.buttons.add(noteButton);
				currentText.select(currentChord, noteButton);
			}

		}

		lastSyllableAssigned = indexClicked;
		nextChordAssignment();

		undoActions.push(undoFrame);
	}

	@FXML private void skipChord() {
		// Set up an undo action frame to store what happens.
		MappingAction undoFrame = new MappingAction();
		undoFrame.previousLastSyllableAssigned = lastSyllableAssigned;
		undoFrame.previousChordIndex = currentChordIndex - 1;
		undoActions.push(undoFrame);

		nextChordAssignment();
	}

	private Button createNoteButton(SyllableText syllable, boolean finalNote, ChantChordController chord) {
		Button noteButton = new Button(currentChord.getName());
		noteButton.setStyle(String.format(Locale.US, "-fx-base: %s", TWUtils.toRGBCode(currentChord.getColor())));
		chordButtonPane.getChildren().add(noteButton);
		noteButton.setLayoutX(syllable.getLayoutX());
		noteButton.setLayoutY(syllable.getNextNoteButtonPosY());
		noteButton.setMaxHeight(MainApp.NOTE_BUTTON_HEIGHT);
		noteButton.setPrefHeight(MainApp.NOTE_BUTTON_HEIGHT);
		noteButton.setMinHeight(MainApp.NOTE_BUTTON_HEIGHT);
		noteButton.setPrefWidth(30);
		noteButton.setPadding(Insets.EMPTY);

		ContextMenu noteMenu = new ContextMenu();
		CheckMenuItem quarterNote = new CheckMenuItem("quarter note");
		CheckMenuItem dottedQuarterNote = new CheckMenuItem("dotted quarter note");
		CheckMenuItem halfNote = new CheckMenuItem("half note");
		CheckMenuItem eighthNote = new CheckMenuItem("eighth note");
		if (finalNote) {
			halfNote.setSelected(true);
		}
		else quarterNote.setSelected(true);

		// Right click functionality plays chord associated with button
		noteButton.setOnMouseClicked((e) -> {
			if (e.getButton() == MouseButton.SECONDARY) {
				chord.playMidi();
			}
		}); // TODO: Highlight chord in above pane on hover

		// context menu for changing chord duration
		noteMenu.getItems().addAll(quarterNote, dottedQuarterNote, halfNote, eighthNote);
		noteMenu.setOnAction(event -> {
			for (MenuItem item : noteMenu.getItems()) {
				// Deselect previous item when another is checked
				if (!item.getText().equals(((CheckMenuItem) event.getTarget()).getText())) {
					((CheckMenuItem) item).setSelected(false);
				}
			}
			// Something must always be selected. Clicking the currently selected one should have no effect (re-select)
			if (!((CheckMenuItem) event.getTarget()).isSelected()) {
				((CheckMenuItem) event.getTarget()).setSelected(true);
			}

			// Individual actions
			if (event.getTarget().equals(quarterNote)) {
				syllable.setNoteDuration(SyllableText.NOTE_QUARTER, noteButton);
			} else if (event.getTarget().equals(dottedQuarterNote)) {
				syllable.setNoteDuration(SyllableText.NOTE_DOTTED_QUARTER, noteButton);
			} else if (event.getTarget().equals(halfNote)) {
				syllable.setNoteDuration(SyllableText.NOTE_HALF, noteButton);
			} else if (event.getTarget().equals(eighthNote)) {
				syllable.setNoteDuration(SyllableText.NOTE_EIGHTH, noteButton);
			}
		});

		noteButton.setOnAction(event ->
				noteMenu.show(noteButton, Side.BOTTOM, 0, 0));

		return noteButton;
	}

	@FXML private void toggleExpand() {
		if (view_expanded) {
			mainContentPane.setPrefHeight(defaultHeight);
			expandButton.setGraphic(plusIcon);

			view_expanded = false;
		} else {
			// Get note button with greatest LayoutY value
			double maxLayoutY = 0;
			for (Node node : chordButtonPane.getChildren()) {
				if (node.getLayoutY() > maxLayoutY) {
					maxLayoutY = node.getLayoutY();
				}
			}
			// The following line might do nothing if less than minimum height.
			mainContentPane.setPrefHeight(textRow.getPrefHeight() + 5 + maxLayoutY + MainApp.NOTE_BUTTON_HEIGHT);
			expandButton.setGraphic(minusIcon);

			view_expanded = true;
		}
	}

	@FXML private void editSyllables() {
		Platform.runLater(() -> {
			try {
				// Load layout from fxml file
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(MainApp.class.getResource("syllableEditView.fxml"));
				BorderPane rootLayout = loader.load();
				SyllableEditViewController controller = loader.getController();
				controller.setParentController(this);
				controller.setSyllableText(verseLine.getLine());

				Stage syllableStage = new Stage();
				syllableStage.setTitle("Edit Line");
				syllableStage.getIcons().add(new Image(getClass().getResourceAsStream("/media/AppIcon.png")));
				syllableStage.setScene(new Scene(rootLayout));
				syllableStage.initModality(Modality.APPLICATION_MODAL);
				syllableStage.setResizable(false);
				syllableStage.show();

			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public SyllableText[] getSyllables() {
		ArrayList<SyllableText> infoList = new ArrayList<>();

		for (Node node : lineTextFlow.getChildren()) {
			infoList.add((SyllableText) node);
		}

		return infoList.toArray(new SyllableText[] {});
	}

	boolean isSeparator() {
		return isSeparatorLine;
	}

	void refreshTextStyle() {
		for (Object item : lineTextFlow.getChildren()) {
			SyllableText text = (SyllableText) item;
			text.refreshStyle();
		}

		separatorIndicatorBox.setStyle("-fx-background-color: " + (MainApp.darkModeEnabled() ? "#585c5f;" : "#f4f4f4;"));
	}

}
