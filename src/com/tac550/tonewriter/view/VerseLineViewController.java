package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.FXMLLoaderIO;
import com.tac550.tonewriter.io.LilyPondInterface;
import com.tac550.tonewriter.io.MidiInterface;
import com.tac550.tonewriter.model.AssignedChordData;
import com.tac550.tonewriter.model.AssignmentAction;
import com.tac550.tonewriter.model.RecitingChord;
import com.tac550.tonewriter.model.VerseLine;
import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
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

import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.function.Consumer;

public class VerseLineViewController {

	private MainSceneController mainController;
	private TopSceneController topController;

	private VerseLine verseLine;

	// How tall to make note buttons
	static final SimpleIntegerProperty NOTE_BUTTON_HEIGHT = new SimpleIntegerProperty(15);

	@FXML private StackPane rootPane;
	@FXML private GridPane mainContentPane;

	private boolean isSeparatorLine = false;
	@FXML private AnchorPane separatorPane;

	private final Stack<AssignmentAction> undoActions = new Stack<>();

	private int beforeBar = 0;
	private int afterBar = 1;
	@FXML private ImageView beforeBarView;
	@FXML private ImageView afterBarView;

	private ChantLineViewController[] associatedChantLines;
	private int selectedChantLine = 0;
	private String previousChantLine = "";

	@FXML private ChoiceBox<String> tonePhraseChoice;
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

	@FXML private Button playButton;

	private boolean changingAssignments = false;

	private int nextChordIndex = 0; // Index of the next chord to be assigned
	private int lastSyllableAssigned = -1; // Index of the last syllable to be assigned a chord
	private boolean doneAssigning = false;

	private int dragStartIndex = -1; // -1 means no drag has begun on this line

	private Consumer<VerseLineViewController> pendingActions;
	private boolean firstTab = false;

	@FXML private void initialize() {
		tonePhraseChoice.getSelectionModel().selectedIndexProperty().addListener((ov, old_val, new_val) -> {
			if (changingAssignments) return;

			selectedChantLine = new_val.intValue();

			if (!associatedChantLines[selectedChantLine].isSimilarTo(previousChantLine)) {
				resetChordAssignment();
			}

			previousChantLine = associatedChantLines[selectedChantLine].toString();
		});

		// Interface icons
		double iconSize = 22;
		plusIcon = new ImageView(getClass().getResource("/media/magnify.png").toExternalForm());
		minusIcon = new ImageView(getClass().getResource("/media/magnify-less.png").toExternalForm());
		plusIcon.setFitHeight(iconSize);
		plusIcon.setFitWidth(iconSize);
		minusIcon.setFitHeight(iconSize);
		minusIcon.setFitWidth(iconSize);

		// Buttons' initial states
		expandButton.setGraphic(plusIcon);
		playButton.setText("\u25B6");

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

		if (!line_text.isEmpty()) {
			isSeparatorLine = false;
			separatorPane.setVisible(false);
			mainContentPane.setVisible(true);

			// Create the verseLine object, replacing any excess spaces with a single space.
			verseLine = new VerseLine(line_text.strip().replaceAll(" +", " "));

			// If the number of syllables is the same as before, just change the content of the text elements.
			// This way assignments are not lost when making small changes.
			List<String> syllables = verseLine.getSyllables();
			if (lineTextFlow.getChildren().size() == syllables.size()) {
				int i = 0;
				for (String syllable : syllables) {
					SyllableText textElement = (SyllableText) lineTextFlow.getChildren().get(i);
					textElement.setText(syllable);

					i++;
				}
			} else {

				if (lastSyllableAssigned != -1)
					resetChordAssignment();

				lineTextFlow.getChildren().clear();

				for (String syllable : verseLine.getSyllables()) {
					SyllableText text = new SyllableText(syllable);
					text.setParent(this);
					text.setFont(Font.font("System", 28));
					text.setTextAlignment(TextAlignment.CENTER);

					lineTextFlow.getChildren().add(text);
				}

				// Fixes additional syllables not being visible because the text flow didn't resize.
				lineTextFlow.autosize();
			}

		} else {
			isSeparatorLine = true;
			separatorPane.setVisible(true);
			mainContentPane.setVisible(false);
		}
	}

	public void setBarlines(String before, String after) {
		if (!before.equals("unchanged")) beforeBar = List.of(SyllableEditViewController.beforeBarStrs).indexOf(before);
		if (!after.equals("unchanged")) afterBar = List.of(SyllableEditViewController.afterBarStrs).indexOf(after);

		if (!before.equals("unchanged") || !after.equals("unchanged")) {
			refreshBarViews();
			topController.projectEdited();
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

		int previousSelection = tonePhraseChoice.getSelectionModel().getSelectedIndex() == -1 ? 0 :
				tonePhraseChoice.getSelectionModel().getSelectedIndex();

		// Load in new chant line choices
		associatedChantLines = chant_lines;
		tonePhraseChoice.getItems().clear();
		for (ChantLineViewController chantLine : associatedChantLines) {
			tonePhraseChoice.getItems().add(chantLine.getName().replace("alternate", "alt"));
		}

		// Determine initial chant line selection.
		if (mainController.manualCLAssignmentEnabled()) {
			// If we're in manual assignment mode, try to auto-select a chant line similar to the previous one.
			if (!previousChantLine.isEmpty()) {
				// If we don't find a similar chant line below, default to the previous selection.
				selectedChantLine = previousSelection;

				int i = 0;
				for (ChantLineViewController chantLine : associatedChantLines) {
					if (chantLine.isSimilarTo(previousChantLine)) {
						// The first time we find a similar chant line, select it and stop searching.
						selectedChantLine = i;
						break;
					}

					i++;
				}

			} else {
				// If there is no previous chant line, or it was empty, select the previous index (usually has same letter)
				selectedChantLine = previousSelection;
			}
		} else if (initial_choice != -1) {
			selectedChantLine = initial_choice;
		} else {
			// If we're not in manual assignment mode, just select the first by default.
			selectedChantLine = 0;
		}
		tonePhraseChoice.getSelectionModel().select(selectedChantLine);

		// ChoiceBox highlighting if choices are available
		if (tonePhraseChoice.getItems().size() > 1) {
			tonePhraseChoice.setStyle("-fx-base: #fcfc2f");
		} else {
			tonePhraseChoice.setStyle("");
		}

		// Only reset chord assignments if the new chant line selection is structurally different from the previous one
		// or has a different name.
		if (!associatedChantLines[selectedChantLine].isSimilarTo(previousChantLine)) {
			resetChordAssignment();
		}

		changingAssignments = false;

		// Save chant line information for later.
		previousChantLine = associatedChantLines[selectedChantLine].toString();

		// Run pending actions, if any, now that a tone has been loaded and the phrase choices assigned.
		// Preserve existing project edited state if not first tab, otherwise reset it.
		if (pendingActions != null) {
			Platform.runLater(() -> {
				boolean wasEdited = topController.getProjectEdited();

				lineTextFlow.layout();
				(firstTab ? pendingActions.andThen(ctr -> topController.resetProjectEditedStatus())
						: pendingActions).accept(this);
				pendingActions = null;

				if (!firstTab && !wasEdited)
					topController.resetProjectEditedStatus();
			});
		}
	}

	public void setPendingActions(boolean first_tab, Consumer<VerseLineViewController> actions) {
		pendingActions = actions;
		firstTab = first_tab;
	}

	private void resetChordAssignment() {
		// Set up the first step of chord assignment
		for (SyllableText syllable : getSyllables())
			syllable.clearSelection();

		chordButtonPane.getChildren().clear();
		undoActions.clear();

		nextChordIndex = 0;
		lastSyllableAssigned = -1;
		nextChordAssignment();
	}

	@FXML private void handleUndo() {
		if (undoActions.empty()) return;

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

	private ChantChordController getCurrentChord() {
		return associatedChantLines[selectedChantLine].getChords().get(nextChordIndex == 0 ? 0 : nextChordIndex - 1);
	}
	public ChantChordController getChordByIndex(int index) {
		return associatedChantLines[selectedChantLine].getChords().get(index);
	}

	private boolean notAssigning() {
		return associatedChantLines == null || doneAssigning;
	}

	private void nextChordAssignment() {
		if (associatedChantLines == null) return;
		// If we have not placed or skipped the last chord...
		if (nextChordIndex < associatedChantLines[selectedChantLine].getChords().size()) {
			doneAssigning = false;

			chordEntryText.setText(getChordByIndex(nextChordIndex).getName());
			chordEntryText.setFill(getChordByIndex(nextChordIndex).getColor());
			skipChordButton.setDisable(false);
			nextChordIndex++;

			// Special disabling based on prep/post/normal
			deactivateAll();

			if (!(getCurrentChord() instanceof RecitingChord)) {
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

		} else { // If we have just placed or skipped the last chord for the line
			doneAssigning = true;

			chordEntryText.setText("Done!");
			chordEntryText.setFill(Color.BLACK);
			skipChordButton.setDisable(true);

			// Disable all syllables
			for (SyllableText syllable : getSyllables())
				syllable.deactivate();
		}

		topController.projectEdited();
	}

	void deactivateAll() {
		for (SyllableText syllNode : getSyllables())
			syllNode.deactivate();
	}

	void syllableClicked(SyllableText clicked_text) {
		if (notAssigning()) return;

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
	void playCurrectChord() {
		if (notAssigning()) return;
		getCurrentChord().playMidi();
	}

	void syllableHovered() {
		if (notAssigning()) return;
		if (topController.hoverHighlightEnabled()) {
			getCurrentChord().setHighlighted(true);
		}
	}
	void syllableUnHovered() {
		if (notAssigning()) return;
		getCurrentChord().setHighlighted(false);
	}

	void syllableDragStarted(SyllableText dragged_text) {
		if (notAssigning()) return;

		// Only allow drag operation to continue if assigning a reciting chord.
		if (getCurrentChord() instanceof RecitingChord) {
			dragStartIndex = lineTextFlow.getChildren().indexOf(dragged_text);
			dragged_text.startFullDrag();

		}
	}
	void syllableDragEntered(SyllableText entered_text) {
		if (notAssigning() || dragStartIndex == -1) return;

		int dragEnteredIndex = lineTextFlow.getChildren().indexOf(entered_text);
		int smaller = Math.min(dragStartIndex, dragEnteredIndex);
		int larger = Math.max(dragStartIndex, dragEnteredIndex);
		int i = 0;
		for (SyllableText syllable : getSyllables()) {
			if (i >= smaller && i <= larger)
				syllable.setFill(getCurrentChord().getColor());
			else
				syllable.setFill(syllable.defaultColor);

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

		if (notAssigning() || dragStartIndex == dragEndIndex) return;

		int smaller = Math.min(dragStartIndex, dragEndIndex);
		int larger = Math.max(dragStartIndex, dragEndIndex);

		assignChord(smaller, larger);

		dragStartIndex = -1;
	}
	private void defaultSyllableColors() {
		for (SyllableText syllable : getSyllables())
			syllable.setFill(syllable.defaultColor);
	}

	@FXML private void skipChordAction() {
		skipChord();

		if (doneAssigning)
			topController.autoSaveProjectIfUnsaved();
	}
	public void skipChord() {
		// Set up an undo action frame
		AssignmentAction undoFrame = new AssignmentAction();
		undoFrame.previousLastSyllableAssigned = lastSyllableAssigned;
		undoFrame.previousChordIndex = nextChordIndex - 1;
		undoActions.push(undoFrame);

		nextChordAssignment();
	}

	public void assignChordSilently(int first_syll, int last_syll) {
		assignChord(first_syll, last_syll, true);
	}
	private void assignChord(int first_syll, int last_syll) {
		assignChord(first_syll, last_syll, false);
	}
	private void assignChord(int first_syll, int last_syll, boolean silent) {
		// First, play the chord if chord playing is on.
		if (!silent && topController.playMidiAsAssigned()) {
			getCurrentChord().playMidi();
		}

		// Set up an undo action frame
		AssignmentAction undoFrame = new AssignmentAction();
		undoFrame.previousLastSyllableAssigned = lastSyllableAssigned;
		undoFrame.previousChordIndex = nextChordIndex - 1;

		for (int i = first_syll; i <= last_syll; i++) {
			SyllableText currentText = (SyllableText) lineTextFlow.getChildren().get(i);
			undoFrame.syllableTexts.add(currentText);

			Button noteButton;

			if (nextChordIndex == associatedChantLines[selectedChantLine].getChords().size()
					&& i == last_syll) { // Final instance of the last chord in the chant line gets special duration.
				if (mainController.isLastVerseLineOfSection(this)) {
					noteButton = createChordButton(currentText, getCurrentChord());

					undoFrame.buttons.add(noteButton);
					currentText.select(nextChordIndex == 0 ? 0 : nextChordIndex - 1, getCurrentChord().getColor(), noteButton);
					currentText.setNoteDuration(LilyPondInterface.NOTE_WHOLE,
							currentText.getAssociatedButtons().size() - 1);
				} else {
					noteButton = createChordButton(currentText, getCurrentChord());

					undoFrame.buttons.add(noteButton);
					currentText.select(nextChordIndex == 0 ? 0 : nextChordIndex - 1, getCurrentChord().getColor(), noteButton);
					currentText.setNoteDuration(LilyPondInterface.NOTE_HALF,
							currentText.getAssociatedButtons().size() - 1);
				}
			} else {
				noteButton = createChordButton(currentText, getCurrentChord());

				undoFrame.buttons.add(noteButton);
				currentText.select(nextChordIndex == 0 ? 0 : nextChordIndex - 1, getCurrentChord().getColor(), noteButton);
			}

		}

		lastSyllableAssigned = last_syll;
		nextChordAssignment();

		if (!silent && doneAssigning)
			topController.autoSaveProjectIfUnsaved();

		undoActions.push(undoFrame);
	}

	private Button createChordButton(SyllableText syllable, ChantChordController chord) {
		int buttonIndex = syllable.getAssociatedButtons().size();
		int chordIndex = associatedChantLines[selectedChantLine].getChords().indexOf(chord);

		Button noteButton = new Button(getCurrentChord().getName());
		noteButton.setStyle(String.format(Locale.US, "-fx-base: %s", TWUtils.toRGBCode(getCurrentChord().getColor())));
		chordButtonPane.getChildren().add(noteButton);
		noteButton.setLayoutX(syllable.getLayoutX());
		noteButton.setLayoutY(syllable.getNextNoteButtonPosY());
		noteButton.maxHeightProperty().bind(NOTE_BUTTON_HEIGHT);
		noteButton.prefHeightProperty().bind(NOTE_BUTTON_HEIGHT);
		noteButton.minHeightProperty().bind(NOTE_BUTTON_HEIGHT);

		noteButton.setPrefWidth(30);
		noteButton.setPadding(Insets.EMPTY);

		noteButton.setOnTouchPressed(te ->
				topController.showTouchNoteMenu(syllable, noteButton, te));
		noteButton.setOnMouseClicked(me -> {
			if (me.getButton() == MouseButton.PRIMARY && !me.isSynthesized()) {
				if (me.getClickCount() == 2) { // Double click assigns half note
					topController.projectEdited();
					syllable.setNoteDuration(LilyPondInterface.NOTE_HALF, buttonIndex);
				}
				topController.showNoteMenu(syllable, noteButton);
			} else if (me.getButton() == MouseButton.SECONDARY) { // Right click plays chord associated with button
				getChordByIndex(chordIndex).playMidi();
			}
		});
		noteButton.setOnMouseEntered((me) -> {
			if (topController.hoverHighlightEnabled()) {
				getChordByIndex(chordIndex).setHighlighted(true);
			}
		});
		noteButton.setOnMouseExited((me) -> getChordByIndex(chordIndex).setHighlighted(false));

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
			mainContentPane.setPrefHeight(textRow.getPrefHeight() + 5 + maxLayoutY + NOTE_BUTTON_HEIGHT.get()
					+ scrollBarPadding);
			expandButton.setGraphic(minusIcon);

			view_expanded = true;
		}
	}

	@FXML private void handlePlay() {
		MidiInterface.playAssignedPhrase(getSyllables(), playButton);
	}

	@FXML private void handleEditSyllables() {

		FXMLLoaderIO.loadFXMLLayoutAsync("syllableEditView.fxml", loader -> {
			VBox rootLayout = loader.getRoot();
			SyllableEditViewController controller = loader.getController();

			controller.setParentController(this);
			controller.setSyllableText(verseLine.getLine());
			controller.setBarSelections(beforeBar, afterBar);

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
		return lineTextFlow.getChildren().stream().map(node -> (SyllableText) node).toArray(SyllableText[]::new);
	}

	void showSyllableMenu(SyllableText syllable) {
		topController.showSyllableMenu(syllable);
	}

	public String getTonePhraseChoice() {
		return tonePhraseChoice.getValue();
	}
	public void setTonePhraseChoice(String choice) {
		if (tonePhraseChoice.getItems().contains(choice)) {
			tonePhraseChoice.getSelectionModel().select(choice);
		}
	}

	public String getBeforeBar() {
		return SyllableEditViewController.beforeBarStrs[beforeBar];
	}
	public String getAfterBar() {
		return  SyllableEditViewController.afterBarStrs[afterBar];
	}

	public void setAssignmentDurations(List<String> durations) {
		int i = 0;
		for (SyllableText syllable : getSyllables()) {
			for (AssignedChordData chordData : syllable.getAssociatedChords()) {
				chordData.setDuration(durations.get(i));
				i++;
			}
		}
	}

	public boolean isSeparator() {
		return isSeparatorLine;
	}

	void refreshTextStyle() {
		for (SyllableText syllable : getSyllables())
			syllable.refreshStyle();

		separatorPane.setStyle("-fx-background-color: " + (MainApp.isDarkModeEnabled() ? "#585c5f;" : "#f4f4f4;"));
	}

	void refreshBarViews() {
		beforeBarView.setImage(SyllableEditViewController.barImages[beforeBar]);
		afterBarView.setImage(SyllableEditViewController.barImages[afterBar]);
	}

	public void verseEdited() {
		mainController.verseEdited();
	}

	boolean notFirstInItem() {
		return mainController.getVerseLineControllers().indexOf(this) != 0;
	}

}
