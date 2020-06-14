package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.FXMLLoaderIO;
import com.tac550.tonewriter.model.AssignmentAction;
import com.tac550.tonewriter.model.RecitingChord;
import com.tac550.tonewriter.model.VerseLine;
import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

import java.util.*;

public class VerseLineViewController {

	private MainSceneController mainController;
	private TopSceneController topController;

	private VerseLine verseLine;

	@FXML private StackPane rootPane;
	@FXML private GridPane mainContentPane;

	private boolean isSeparatorLine = false;
	@FXML private VBox separatorIndicatorBox;

	private final Stack<AssignmentAction> undoActions = new Stack<>();

	private ChantLineViewController[] associatedChantLines;
	private int selectedChantLine = 0;

	@FXML private ChoiceBox<String> chantLineChoice;
	@FXML private TextFlow lineTextFlow;
	@FXML private RowConstraints textRow;

	@FXML private ScrollPane syllableScrollPane;
	private ScrollBar scrollBar;
	private double scrollBarHeight = 0;

	@FXML private Pane chordButtonPane;

	@FXML private Text chordEntryText;
	@FXML private Button skipChordButton;

	@FXML private Button expandButton;
	private ImageView plusIcon;
	private ImageView minusIcon;
	private boolean view_expanded = false;
	private double defaultHeight;

	private boolean changingAssignments = false;

	private int nextChordIndex = 0; // Index of the next chord to be assigned
	private int lastSyllableAssigned = -1; // Index of the last syllable to be assigned a chord
	private ChantChordController currentChord; // The chord currently being assigned

	private int dragStartIndex = -1; // -1 means no drag has begun on this line

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

		// Default height used when toggling Expand off.
		defaultHeight = mainContentPane.getPrefHeight();

		refreshTextStyle();

		// Prepare scroll pane to be set up at the appropriate time.
		if (syllableScrollPane.getSkin() == null) {
			// Skin is not yet attached, wait until skin is attached to access the scroll bars
			syllableScrollPane.skinProperty().addListener(new ChangeListener<>() {
				@Override
				public void changed(ObservableValue<? extends Skin<?>> obs, Skin<?> oldValue, Skin<?> newValue) {
					syllableScrollPane.skinProperty().removeListener(this);
					setUpScrollPane();
				}
			});
		} else {
			// Skin is already attached, just access the scroll bars
			setUpScrollPane();
		}

	}

	// Sets up scroll bar behavior (makes sure scroll bar does not block view)
	private void setUpScrollPane() {

		for (Node node : syllableScrollPane.lookupAll(".scroll-bar")) {
			if (node instanceof ScrollBar scrollBarNode) {
				scrollBar = scrollBarNode;
				if (scrollBar.getOrientation() == Orientation.HORIZONTAL && scrollBar.getParent() == syllableScrollPane) {
					scrollBar.heightProperty().addListener((obs, oldVal, newVal) -> {
						scrollBarHeight = newVal.doubleValue();
						Platform.runLater(this::adjustViewportAreaForScrollbar);
					});

					// Example 2: Listen to visibility changes
					scrollBar.visibleProperty().addListener((obs, oldValue, newValue) ->
							adjustViewportAreaForScrollbar());
				}
			}
		}
	}

	private void adjustViewportAreaForScrollbar() {
		if (scrollBar.isVisible()) {
			mainContentPane.setPrefHeight(mainContentPane.getPrefHeight() + scrollBarHeight);
		} else {
			mainContentPane.setPrefHeight(mainContentPane.getPrefHeight() - scrollBarHeight);
		}

		rootPane.requestLayout();
	}

	void setParentControllers(MainSceneController parent, TopSceneController top_controller) {
		mainController = parent;
		topController = top_controller;
	}

	void setVerseLine(String line_text) {

		if (lastSyllableAssigned != -1) {
			resetChordAssignment();
		}

		if (!line_text.isEmpty()) {

			isSeparatorLine = false;
			separatorIndicatorBox.setVisible(false);
			mainContentPane.setVisible(true);

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

			// Fixes additional syllables not being visible because the text flow didn't resize.
			lineTextFlow.autosize();

		} else {

			isSeparatorLine = true;
			separatorIndicatorBox.setVisible(true);
			mainContentPane.setVisible(false);

		}

	}

	String getVerseLineText() {
		return verseLine.getLine();
	}

	void setChantLines(ChantLineViewController[] chant_lines) {
		setChantLines(chant_lines, -1);
	}

	void setChantLines(ChantLineViewController[] chant_lines, int initial_choice) {
		changingAssignments = true;
		// Remember previous chant line selection, if any.
		ChantLineViewController previousChantLine = null;
		if (associatedChantLines != null) {
			previousChantLine = associatedChantLines[selectedChantLine];
		}

		// Load in new chant line choices
		associatedChantLines = chant_lines;
		chantLineChoice.getItems().clear();
		for (ChantLineViewController chantLine : associatedChantLines) {
			chantLineChoice.getItems().add(chantLine.getName().replace("alternate", "alt"));
		}

		// Keep chant line selection and chord assignments (if any) if previous selection is still available.
		int index;
		if (initial_choice != -1) {
			selectedChantLine = initial_choice;
			chantLineChoice.getSelectionModel().select(initial_choice);
		} else if (nextChordIndex > 1 && (index = Arrays.asList(chant_lines).indexOf(previousChantLine)) != -1) {
			selectedChantLine = index;
			chantLineChoice.getSelectionModel().select(index);
		} else {
			selectedChantLine = 0;
			chantLineChoice.getSelectionModel().select(0);
		}

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

		nextChordIndex = 0;
		lastSyllableAssigned = -1;
		nextChordAssignment();

	}

	@FXML private void undo() {
		if (undoActions.empty()) {
			return;
		}
		AssignmentAction action = undoActions.pop();

		for (Button button : action.buttons) {
			chordButtonPane.getChildren().remove(button);
		}

		for (SyllableText text : action.syllableTexts) {
			text.removeLastChord();
			text.reactivate();
		}

		lastSyllableAssigned = action.previousLastSyllableAssigned;
		nextChordIndex = action.previousChordIndex;
		nextChordAssignment();

	}

	@FXML private void remove() {
		mainController.removeVerseLine(this);
	}

	StackPane getRootPane() {
		return rootPane;
	}

	private void nextChordAssignment() {
		if (associatedChantLines == null) return;
		// If we have not placed or skipped the last chord...
		if (nextChordIndex < associatedChantLines[selectedChantLine].getChords().size()) {
			currentChord = associatedChantLines[selectedChantLine].getChords().get(nextChordIndex);
			chordEntryText.setText(String.valueOf(currentChord.getName()));
			chordEntryText.setFill(currentChord.getColor());
			skipChordButton.setDisable(false);
			nextChordIndex++;

			// Special disabling based on prep/post/normal
			deactivateAll();

			if (!(currentChord instanceof RecitingChord)) {
				if (nextChordIndex == 1) { // If no chords have been assigned yet...
					((SyllableText) lineTextFlow.getChildren().get(0)).reactivate(); // Activate only the first syllable.
				} else {
					// Activate current syllable.
					((SyllableText) lineTextFlow.getChildren().get(Math.max(lastSyllableAssigned, 0))).reactivate();
					if (lastSyllableAssigned < lineTextFlow.getChildren().size() - 1) { // Avoid error if there is no next SyllableText
						// Activate next syllable. (repeats above operation if lastSyllableAssigned is -1)
						((SyllableText) lineTextFlow.getChildren().get(lastSyllableAssigned+1)).reactivate();
					}
				}
			} else { // If current chord is a reciting (numbered) chord
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

	void deactivateAll() {
		for (Node syllNode : lineTextFlow.getChildren()) {
			((SyllableText) syllNode).deactivate();
		}
	}

	void syllableClicked(SyllableText clicked_text) {
		if (currentChord == null) return;

		final int indexClicked = lineTextFlow.getChildren().indexOf(clicked_text);

		if (lastSyllableAssigned == -1) {
			assignChord(0, indexClicked);
		} else if (lastSyllableAssigned == indexClicked) {
			// If the clicked syllable already has a chord, this keeps it activated for further chord assignments.
			assignChord(lastSyllableAssigned, indexClicked);
		} else {
			assignChord(lastSyllableAssigned + 1, indexClicked);
		}

	}
	void syllableAltClicked() {
		if (currentChord == null) return;
		currentChord.playMidi();
	}

	void syllableHovered() {
		if (currentChord == null) return;
		if (topController.hoverHighlightEnabled()) {
			currentChord.setHighlighted(true);
		}
	}
	void syllableUnHovered() {
		if (currentChord == null) return;
		currentChord.setHighlighted(false);
	}

	void syllableDragStarted(SyllableText dragged_text) {
		if (currentChord == null) return;

		// Only allow drag operation to continue if assigning a reciting chord.
		if (currentChord instanceof RecitingChord) {
			dragStartIndex = lineTextFlow.getChildren().indexOf(dragged_text);
			dragged_text.startFullDrag();

		}
	}
	void syllableDragEntered(SyllableText entered_text) {
		if (currentChord == null || dragStartIndex == -1) return;

		int dragEnteredIndex = lineTextFlow.getChildren().indexOf(entered_text);
		int smaller = Math.min(dragStartIndex, dragEnteredIndex);
		int larger = Math.max(dragStartIndex, dragEnteredIndex);
		int i = 0;
		for (Node syllNode : lineTextFlow.getChildren()) {
			if (i >= smaller && i <= larger) {
				((SyllableText) syllNode).setFill(currentChord.getColor());
			} else {
				((SyllableText) syllNode).setFill(((SyllableText) syllNode).defaultColor);
			}

			i++;
		}
	}
	void syllableDragExited() {
		defaultSyllableColors();
	}
	void syllableDragReleased() {
		defaultSyllableColors();

		dragStartIndex = -1;
	}
	void syllableDragCompleted(SyllableText released_text) {
		if (dragStartIndex == -1) return; // Drag did not start on this line - don't proceed.

		int dragEndIndex = lineTextFlow.getChildren().indexOf(released_text);

		if (currentChord == null || dragStartIndex == dragEndIndex) return;

		int smaller = Math.min(dragStartIndex, dragEndIndex);
		int larger = Math.max(dragStartIndex, dragEndIndex);

		assignChord(smaller, larger);

		dragStartIndex = -1;
	}
	private void defaultSyllableColors() {
		for (Node syllNode : lineTextFlow.getChildren()) {
			((SyllableText) syllNode).setFill(((SyllableText) syllNode).defaultColor);
		}

	}

	@FXML private void skipChord() {
		// Set up an undo action frame to store what happens.
		AssignmentAction undoFrame = new AssignmentAction();
		undoFrame.previousLastSyllableAssigned = lastSyllableAssigned;
		undoFrame.previousChordIndex = nextChordIndex - 1;
		undoActions.push(undoFrame);

		nextChordAssignment();
	}

	private void assignChord(int firstSyllable, int lastSyllable) {
		// First, play the chord if chord playing is on.
		if (topController.playMidiAsAssigned()) {
			currentChord.playMidi();
		}

		// Set up an undo action frame to store what happens.
		AssignmentAction undoFrame = new AssignmentAction();
		undoFrame.previousLastSyllableAssigned = lastSyllableAssigned;
		undoFrame.previousChordIndex = nextChordIndex - 1;

		for (int i = firstSyllable; i <= lastSyllable; i++) {
			SyllableText currentText = (SyllableText) lineTextFlow.getChildren().get(i);
			undoFrame.syllableTexts.add(currentText);

			Button noteButton;

			if (nextChordIndex == associatedChantLines[selectedChantLine].getChords().size()
					&& i == lastSyllable) { // Final instance of the last chord in the chant line gets special duration.
				if (mainController.isLastVerseLineOfSection(this)) {
					noteButton = createNoteButton(currentText, currentChord);

					undoFrame.buttons.add(noteButton);
					currentText.select(currentChord, noteButton);
					currentText.setNoteDuration(SyllableText.NOTE_WHOLE, currentText.getAssociatedButtons().size() - 1);
				} else {
					noteButton = createNoteButton(currentText, currentChord);

					undoFrame.buttons.add(noteButton);
					currentText.select(currentChord, noteButton);
					currentText.setNoteDuration(SyllableText.NOTE_HALF, currentText.getAssociatedButtons().size() - 1);
				}
			} else {
				noteButton = createNoteButton(currentText, currentChord);

				undoFrame.buttons.add(noteButton);
				currentText.select(currentChord, noteButton);
			}

		}

		lastSyllableAssigned = lastSyllable;
		nextChordAssignment();

		undoActions.push(undoFrame);
	}

	private Button createNoteButton(SyllableText syllable, ChantChordController chord) {
		int futureButtonIndex = syllable.getAssociatedButtons().size();

		Button noteButton = new Button(currentChord.getName());
		noteButton.setStyle(String.format(Locale.US, "-fx-base: %s", TWUtils.toRGBCode(currentChord.getColor())));
		chordButtonPane.getChildren().add(noteButton);
		noteButton.setLayoutX(syllable.getLayoutX());
		noteButton.setLayoutY(syllable.getNextNoteButtonPosY());
		noteButton.maxHeightProperty().bind(MainApp.NOTE_BUTTON_HEIGHT);
		noteButton.prefHeightProperty().bind(MainApp.NOTE_BUTTON_HEIGHT);
		noteButton.minHeightProperty().bind(MainApp.NOTE_BUTTON_HEIGHT);

		noteButton.setPrefWidth(30);
		noteButton.setPadding(Insets.EMPTY);

		noteButton.setOnTouchPressed(te ->
				TopSceneController.showTouchNoteMenu(syllable, noteButton, te));
		noteButton.setOnMouseClicked(me -> {
			if (me.getButton() == MouseButton.PRIMARY && !me.isSynthesized()) {
				if (me.getClickCount() == 2) { // Double click assigns half note
					syllable.setNoteDuration(SyllableText.NOTE_HALF, futureButtonIndex);
				}
				TopSceneController.showNoteMenu(syllable, noteButton);
			} else if (me.getButton() == MouseButton.SECONDARY) { // Right click plays chord associated with button
				chord.playMidi();
			}
		});
		noteButton.setOnMouseEntered((me) -> {
			if (topController.hoverHighlightEnabled()) {
				chord.setHighlighted(true);
			}
		});
		noteButton.setOnMouseExited((me) -> chord.setHighlighted(false));

		return noteButton;
	}

	@FXML private void toggleExpand() {

		double scrollBarPadding = scrollBar.isVisible() ? scrollBarHeight : 0;

		if (view_expanded) {
			mainContentPane.setPrefHeight(defaultHeight + scrollBarPadding);
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
			mainContentPane.setPrefHeight(textRow.getPrefHeight() + 5 + maxLayoutY + MainApp.NOTE_BUTTON_HEIGHT.get()
					+ scrollBarPadding);
			expandButton.setGraphic(minusIcon);

			view_expanded = true;
		}
	}

	@FXML private void editSyllables() {

		FXMLLoaderIO.loadFXMLLayoutAsync("syllableEditView.fxml", loader -> {
			BorderPane rootLayout = loader.getRoot();
			SyllableEditViewController controller = loader.getController();

			controller.setParentController(this);
			controller.setSyllableText(verseLine.getLine());

			Platform.runLater(() -> {
				Stage syllableStage = new Stage();
				syllableStage.setTitle("Edit Line");
				syllableStage.getIcons().add(MainApp.APP_ICON);
				syllableStage.setScene(new Scene(rootLayout));
				syllableStage.initModality(Modality.APPLICATION_MODAL);
				syllableStage.setResizable(false);
				syllableStage.initOwner(rootPane.getScene().getWindow());
				syllableStage.show();
			});
		});

	}

	public SyllableText[] getSyllables() {
		List<SyllableText> infoList = new ArrayList<>();

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

		separatorIndicatorBox.setStyle("-fx-background-color: " + (MainApp.isDarkModeEnabled() ? "#585c5f;" : "#f4f4f4;"));
	}

	public void edited() {
		mainController.verseEdited();
	}

}
