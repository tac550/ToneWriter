package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.FXMLLoaderIO;
import com.tac550.tonewriter.io.LilyPondInterface;
import com.tac550.tonewriter.io.MidiInterface;
import com.tac550.tonewriter.model.*;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.chord.RecitingChordView;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Consumer;

public class VerseLineViewController {

	private MainSceneController mainController;
	private TopSceneController topController;

	private VerseLine verseLine;

	static final SimpleIntegerProperty NOTE_BUTTON_HEIGHT = new SimpleIntegerProperty(15);
	static final int NOTE_BUTTON_WIDTH = 39;
	private static final double EXPANDED_VIEW_PADDING = 5;

	private static final Image eighthNoteImage = new Image(Objects.requireNonNull(TopSceneController.class.getResource("/media/notes/eighth.png")).toExternalForm(), 8, -1, true, false);
	private static final Image quarterNoteImage = new Image(Objects.requireNonNull(TopSceneController.class.getResource("/media/notes/quarter.png")).toExternalForm(), 5, -1, true, false);
	private static final Image dottedQuarterNoteImage = new Image(Objects.requireNonNull(TopSceneController.class.getResource("/media/notes/dotted-quarter.png")).toExternalForm(), 8, -1, true, false);
	private static final Image halfNoteImage = new Image(Objects.requireNonNull(TopSceneController.class.getResource("/media/notes/half.png")).toExternalForm(), 5, -1, true, false);
	private static final Image dottedHalfNoteImage = new Image(Objects.requireNonNull(TopSceneController.class.getResource("/media/notes/dotted-half.png")).toExternalForm(), 8, -1, true, false);
	private static final Image wholeNoteImage = new Image(Objects.requireNonNull(TopSceneController.class.getResource("/media/notes/whole.png")).toExternalForm(), 8, -1, true, false);

	protected static final Image[] noteImages = new Image[] {eighthNoteImage, quarterNoteImage, dottedQuarterNoteImage,
			halfNoteImage, dottedHalfNoteImage, wholeNoteImage};

	@FXML private StackPane rootPane;
	@FXML private GridPane mainContentPane;

	private boolean isSeparatorLine = false;
	@FXML private AnchorPane separatorPane;

	private final Stack<AssignmentAction> undoActions = new Stack<>();

	private final IntegerProperty beforeBar = new SimpleIntegerProperty(0);
	private final IntegerProperty afterBar = new SimpleIntegerProperty(3);
	private IntegerProperty beforeBoundTo;
	@FXML private ImageView beforeBarView;
	@FXML private ImageView afterBarView;

	private boolean disableLineBreaks = false;

	private ChantPhraseViewController[] associatedChantPhrases;
	private int selectedChantPhrase = 0;
	private ChantPhrase previousChantPhrase = null;

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
		tonePhraseChoice.getSelectionModel().selectedIndexProperty().addListener((ov, oldVal, newVal) -> {
			if (changingAssignments) return;

			selectedChantPhrase = newVal.intValue();

			resetIfSelectedNotSimilarToPrevious();

			previousChantPhrase = associatedChantPhrases[selectedChantPhrase].generatePhraseModel();
		});

		// Interface icons
		double iconSize = 22;
		plusIcon = new ImageView(Objects.requireNonNull(getClass().getResource("/media/magnify.png")).toExternalForm());
		minusIcon = new ImageView(Objects.requireNonNull(getClass().getResource("/media/magnify-less.png")).toExternalForm());
		plusIcon.setFitHeight(iconSize);
		plusIcon.setFitWidth(iconSize);
		minusIcon.setFitHeight(iconSize);
		minusIcon.setFitWidth(iconSize);

		// Button appearance
		expandButton.setGraphic(plusIcon);
		expandButton.setVisible(false);
		playButton.setText("\u25B6");

		// Default height used when toggling Expand off.
		defaultHeight = mainContentPane.getPrefHeight();

		refreshTextStyle();
		refreshBarViews();

		// Barline views automatically refresh when barline selection changes.
		beforeBar.addListener((ov, oldVal, newVal) -> refreshBarViews());
		afterBar.addListener((ov, oldVal, newVal) -> refreshBarViews());

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
		if (scrollBar.isVisible())
			mainContentPane.setPrefHeight(mainContentPane.getPrefHeight() + scrollBarHeight);
		else
			mainContentPane.setPrefHeight(mainContentPane.getPrefHeight() - scrollBarHeight);

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

			// Create the verseLine with the given text, removing any leading hyphens and replacing any excess spaces.
			verseLine = new VerseLine(line_text.strip().replaceAll("^-+", "").replace(" -", "-")
					.replaceAll(" +", " "));

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
				if (hasAssignments())
					resetChordAssignment();

				lineTextFlow.getChildren().clear();

				for (String syllable : syllables) {
					SyllableText text = new SyllableText(syllable);
					text.setParent(this);
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
		boolean updateBefore = !before.equals(LilyPondInterface.BAR_UNCHANGED);
		boolean updateAfter = !after.equals(LilyPondInterface.BAR_UNCHANGED);

		if (updateBefore) beforeBar.set(List.of(LilyPondInterface.barStrings).indexOf(before));
		if (updateAfter) afterBar.set(List.of(LilyPondInterface.barStrings).indexOf(after));

		if (updateBefore || updateAfter)
			topController.projectEdited();
	}

	void linkBeforeBarLine(IntegerProperty otherBar) {
		if (beforeBar.isBound())
			beforeBar.unbindBidirectional(beforeBoundTo);

		// Take the value of the other bar before binding.
		beforeBar.set(otherBar.get());

		beforeBar.bindBidirectional(otherBar);
		beforeBoundTo = otherBar;
	}

	String getVerseLineText() {
		return verseLine.getLine();
	}

	void setPhraseChoices(ChantPhraseViewController[] chant_lines) {
		setPhraseChoices(chant_lines, -1);
	}

	void setPhraseChoices(ChantPhraseViewController[] chant_lines, int initial_choice) {
		changingAssignments = true;

		int previousSelection = tonePhraseChoice.getSelectionModel().getSelectedIndex() == -1 ? 0 :
				tonePhraseChoice.getSelectionModel().getSelectedIndex();

		boolean identicalChoices = Arrays.equals(associatedChantPhrases, chant_lines);
		// Load in new chant line choices
		associatedChantPhrases = chant_lines;
		tonePhraseChoice.getItems().clear();
		for (ChantPhraseViewController chantPhrase : associatedChantPhrases)
			tonePhraseChoice.getItems().add(TWUtils.shortenPhraseName(chantPhrase.getName()));

		// Determine initial chant line selection.
		if (mainController.manualCLAssignmentEnabled()) {
			// If we're in manual assignment mode, try to auto-select a chant line similar to the previous one.
			if (previousChantPhrase != null) {
				// If we don't find a similar chant line below, default to the previous selection if it's within the new
				// range of available chant phrases; otherwise select the first.
				selectedChantPhrase = associatedChantPhrases.length > previousSelection ? previousSelection : 0;

				// If the chant line choices are identical (nothing is changing), just take the previous selection.
				if (!identicalChoices) {
					int i = 0;
					for (ChantPhraseViewController chantPhrase : associatedChantPhrases) {
						if (chantPhrase.generatePhraseModel().isSimilarTo(previousChantPhrase)) {
							// The first time we find a similar chant line, select it and stop searching.
							selectedChantPhrase = i;
							break;
						}

						i++;
					}
				}

			} else {
				// If there is no previous chant line, or it was empty, select the previous index (usually has same letter)
				selectedChantPhrase = initial_choice > -1 ? initial_choice : 0;
			}
		} else {
			// If we're not in manual assignment mode, just select the first by default.
			selectedChantPhrase = 0;
		}
		tonePhraseChoice.getSelectionModel().select(selectedChantPhrase);

		// ChoiceBox highlighting if choices are available
		if (tonePhraseChoice.getItems().size() > 1)
			tonePhraseChoice.setStyle("-fx-base: #fcfc2f");
		else
			tonePhraseChoice.setStyle("");

		resetIfSelectedNotSimilarToPrevious();

		changingAssignments = false;

		// Save chant line information for later.
		previousChantPhrase = associatedChantPhrases[selectedChantPhrase].generatePhraseModel();

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

	private void resetIfSelectedNotSimilarToPrevious() {
		// Only reset chord assignments if the selected chant line is structurally different from the previous one.
		if (!associatedChantPhrases[selectedChantPhrase].generatePhraseModel().isSimilarTo(previousChantPhrase))
			resetChordAssignment();
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

		for (Button button : action.buttons)
			chordButtonPane.getChildren().remove(button);

		for (SyllableText text : action.syllableTexts) {
			text.removeLastChord();
			text.activate();
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

	private ChordViewController getCurrentChord() {
		return associatedChantPhrases[selectedChantPhrase].getChords().get(nextChordIndex == 0 ? 0 : nextChordIndex - 1);
	}
	public ChordViewController getChordByIndex(int index) {
		return associatedChantPhrases[selectedChantPhrase].getChords().get(index);
	}

	private boolean notAssigning() {
		return associatedChantPhrases == null || doneAssigning;
	}

	private void nextChordAssignment() {
		if (associatedChantPhrases == null) return;
		// If we have not placed or skipped the last chord...
		if (nextChordIndex < associatedChantPhrases[selectedChantPhrase].getChords().size()) {
			doneAssigning = false;

			chordEntryText.setText(getChordByIndex(nextChordIndex).getName());
			chordEntryText.setFill(getChordByIndex(nextChordIndex).getColor());
			skipChordButton.setDisable(false);
			nextChordIndex++;

			refreshSyllableActivation();
		} else { // If we have just placed or skipped the last chord for the line
			doneAssigning = true;

			chordEntryText.setText("Done!");
			chordEntryText.setFill(Color.BLACK);
			skipChordButton.setDisable(true);

			// Disable all syllables
			for (SyllableText syllable : getSyllables())
				syllable.deactivate();
		}

		// Update view expansion UI
		expandButton.setVisible(view_expanded || expansionNecessary());

		topController.projectEdited();
	}

	protected void refreshSyllableActivation() {
		// Don't change syllable activation state during a drag operation
		if (dragStartIndex != -1) return;

		deactivateAll();

		if (!doneAssigning) {
			if (!(getCurrentChord() instanceof RecitingChordView) && !MainApp.isChordPlacementUnrestricted()) {
				if (nextChordIndex == 1) { // If no chords have been assigned yet...
					((SyllableText) lineTextFlow.getChildren().getFirst()).activate(); // Activate only the first syllable.
				} else {
					// Activate current syllable.
					((SyllableText) lineTextFlow.getChildren().get(Math.max(lastSyllableAssigned, 0))).activate();
					if (lastSyllableAssigned < lineTextFlow.getChildren().size() - 1) { // Avoid error if there is no next SyllableText
						// Activate next syllable. (repeats above operation if lastSyllableAssigned is -1)
						((SyllableText) lineTextFlow.getChildren().get(lastSyllableAssigned+1)).activate();
					}
				}
			} else { // If current chord is a reciting (numbered) chord
				for (int i = lastSyllableAssigned; i < lineTextFlow.getChildren().size(); i++) {
					if (i < 0) continue; // In case we haven't assigned anything yet (lastSyllableAssigned is -1)
					((SyllableText) lineTextFlow.getChildren().get(i)).activate();
				}
			}
		}
	}

	void deactivateAll() {
		for (SyllableText syllNode : getSyllables())
			syllNode.deactivate();
	}

	void syllableClicked(SyllableText clicked_text) {
		if (notAssigning()) return;

		final int indexClicked = lineTextFlow.getChildren().indexOf(clicked_text);

		if (!hasAssignments())
			assignChord(0, indexClicked);
		else if (lastSyllableAssigned == indexClicked)
			// If the clicked syllable already has a chord, this keeps it activated for further chord assignments.
			assignChord(lastSyllableAssigned, indexClicked);
		else
			assignChord(lastSyllableAssigned + 1, indexClicked);
	}
	void playCurrentChord() {
		if (notAssigning()) return;
		getCurrentChord().playMidi();
	}

	void syllableHovered() {
		if (notAssigning()) return;
		if (topController.hoverHighlightEnabled())
			getCurrentChord().setHighlighted(true);
	}
	void syllableUnHovered() {
		if (notAssigning()) return;
		getCurrentChord().setHighlighted(false);
	}

	void syllableDragStarted(SyllableText dragged_text) {
		if (notAssigning()) return;

		// Only allow drag operation to continue if assigning a reciting chord or overriding restrictions.
		if (getCurrentChord() instanceof RecitingChordView || MainApp.isChordPlacementUnrestricted()) {
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
				syllable.applyDefaultFill();

			i++;
		}
	}
	void syllableDragExited() {
		defaultSyllableColors();
	}
	void syllableDragReleased() {
		defaultSyllableColors();

		cleanUpAfterDragOperation();
	}
	void syllableDragCompleted(SyllableText released_text) {
		if (dragStartIndex == -1) return; // Drag did not start on this line - don't proceed.

		int dragEndIndex = lineTextFlow.getChildren().indexOf(released_text);

		if (notAssigning() || dragStartIndex == dragEndIndex) return;

		int smaller = Math.min(dragStartIndex, dragEndIndex);
		int larger = Math.max(dragStartIndex, dragEndIndex);

		assignChord(smaller, larger);

		cleanUpAfterDragOperation();
	}
	private void defaultSyllableColors() {
		for (SyllableText syllable : getSyllables())
			syllable.applyDefaultFill();
	}
	private void cleanUpAfterDragOperation() {
		dragStartIndex = -1;
		refreshSyllableActivation();
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
		if (!silent && topController.playMidiAsAssigned())
			getCurrentChord().playMidi();

		// Set up an undo action frame
		AssignmentAction undoFrame = new AssignmentAction();
		undoFrame.previousLastSyllableAssigned = lastSyllableAssigned;
		undoFrame.previousChordIndex = nextChordIndex - 1;

		for (int i = first_syll; i <= last_syll; i++) {
			SyllableText currentText = (SyllableText) lineTextFlow.getChildren().get(i);
			undoFrame.syllableTexts.add(currentText);

			Button noteButton = createChordButton(currentText, getCurrentChord());

			undoFrame.buttons.add(noteButton);
			currentText.select(nextChordIndex == 0 ? 0 : nextChordIndex - 1, getCurrentChord().getColor(), noteButton);

			if (nextChordIndex == associatedChantPhrases[selectedChantPhrase].getChords().size() && i == last_syll)
				currentText.setNoteDuration(mainController.isLastVerseLineOfSection(this) ? LilyPondInterface.NOTE_WHOLE : LilyPondInterface.NOTE_HALF,
						currentText.getAssociatedButtons().size() - 1);
		}

		lastSyllableAssigned = last_syll;
		nextChordAssignment();

		if (!silent && doneAssigning)
			topController.autoSaveProjectIfUnsaved();

		undoActions.push(undoFrame);
	}

	private Button createChordButton(SyllableText syllable, ChordViewController chord) {
		int buttonIndex = syllable.getAssociatedButtons().size();
		int chordIndex = associatedChantPhrases[selectedChantPhrase].getChords().indexOf(chord);

		Button noteButton = new Button(getCurrentChord().getName(), new ImageView(quarterNoteImage));
		noteButton.setAlignment(Pos.CENTER_LEFT);
		noteButton.setStyle(String.format(Locale.US, "-fx-base: %s", TWUtils.toRGBCode(getCurrentChord().getColor())));
		chordButtonPane.getChildren().add(noteButton);
		noteButton.setLayoutX(syllable.getLayoutX());
		noteButton.setLayoutY(syllable.getNextNoteButtonPosY());
		noteButton.maxHeightProperty().bind(NOTE_BUTTON_HEIGHT);
		noteButton.prefHeightProperty().bind(NOTE_BUTTON_HEIGHT);
		noteButton.minHeightProperty().bind(NOTE_BUTTON_HEIGHT);

		noteButton.setPrefWidth(NOTE_BUTTON_WIDTH);
		noteButton.setPadding(Insets.EMPTY);

		noteButton.setOnMouseClicked(me -> {
			if (me.getButton() == MouseButton.PRIMARY && !me.isSynthesized()) {
				if (me.getClickCount() == 2) { // Double click doubles note duration
					String selectedDur = syllable.getNoteDuration(buttonIndex);
					String doubled = TWUtils.addDurations(selectedDur, selectedDur);
					if (doubled != null && TopSceneController.durationMapping.contains(doubled)) {
						topController.projectEdited();
						syllable.setNoteDuration(doubled, buttonIndex);
					}
				}
				topController.showNoteMenu(syllable, noteButton);
			} else if (me.getButton() == MouseButton.SECONDARY) { // Right click plays chord associated with button
				getChordByIndex(chordIndex).playMidi();
			}
		});
		noteButton.setOnMouseEntered((me) -> {
			if (topController.hoverHighlightEnabled())
				getChordByIndex(chordIndex).setHighlighted(true);
		});
		noteButton.setOnMouseExited((me) -> getChordByIndex(chordIndex).setHighlighted(false));

		return noteButton;
	}

	private double scrollBarCurrentPixels() {
		return (scrollBar != null && scrollBar.isVisible()) ? scrollBarHeight : 0;
	}

	// Gets the LayoutY value of the lowest NoteButton in this line
	private double greatestNoteButtonLayoutY() {
		double maxLayoutY = 0;
		for (Node node : chordButtonPane.getChildren())
			if (node.getLayoutY() > maxLayoutY)
				maxLayoutY = node.getLayoutY();

		return maxLayoutY;
	}

	@FXML private void toggleExpand() {
		if (!expansionNecessary())
			expandButton.setVisible(false);

		if (view_expanded) {
			mainContentPane.setPrefHeight(getDefaultPaneHeight());
			expandButton.setGraphic(plusIcon);
		} else {
			mainContentPane.setPrefHeight(getExpandedPaneHeight());
			expandButton.setGraphic(minusIcon);
		}

		view_expanded = !view_expanded;
	}

	private boolean expansionNecessary() {
		return getExpandedPaneHeight() > getDefaultPaneHeight();
	}
	private double getDefaultPaneHeight() {
		return defaultHeight + scrollBarCurrentPixels();
	}
	private double getExpandedPaneHeight() {
		return textRow.getPrefHeight() + EXPANDED_VIEW_PADDING
				+ greatestNoteButtonLayoutY() + NOTE_BUTTON_HEIGHT.get() + scrollBarCurrentPixels();
	}

	@FXML private void handlePlay() {
		if (!MidiInterface.sequencerActive()) return;

		SyllableText[] syllables = getSyllables();

		playButton.setDisable(true);

		Task<Void> midiTask = new Task<>() {
			@Override
			protected Void call() throws Exception {
				// Setup before playing
				List<Button> buttons = new ArrayList<>();
				Map<Integer, List<AssignedChordData>> chordMap = new HashMap<>();
				int key = -1;

				String previousFieldsAndDur = null;
				for (SyllableText syllable : syllables) {
					// Place all the buttons into the buttons list in the order they occur
					buttons.addAll(syllable.getAssociatedButtons());

					for (AssignedChordData chord : syllable.getAssociatedChords()) {
						String fieldsAndDur = getChordByIndex(chord.getChordIndex()).getFields() + chord.getDuration();
						// Group elements together in sequential lists in map if notes are same and duration is quarter.
						if (fieldsAndDur.equals(previousFieldsAndDur)
								&& chord.getDuration().equals(LilyPondInterface.NOTE_QUARTER)) {
							chordMap.get(key).add(chord);
						} else {
							chordMap.put(++key, new ArrayList<>(Collections.singletonList(chord)));
						}

						previousFieldsAndDur = fieldsAndDur;
					}
				}

				// Playing loop
				int buttonIndex = 0;
				key = 0;
				SyllableText lastSyllable = null;
				while (chordMap.containsKey(key)) {
					for (AssignedChordData chord : chordMap.get(key)) {
						Button currentButton = buttons.get(buttonIndex);
						String oldButtonStyle = currentButton.getStyle();
						Platform.runLater(() -> currentButton.setStyle("-fx-base: #fffa61"));

						for (SyllableText syllable : syllables) {
							if (syllable.getAssociatedButtons().contains(currentButton) && syllable != lastSyllable) {
								if (lastSyllable != null)
									Platform.runLater(lastSyllable::applyDefaultFill);

								Platform.runLater(() -> syllable.setFill(Color.web("#edbd11")));
								lastSyllable = syllable;
							}
						}

						getChordByIndex(chord.getChordIndex()).playMidi();
						// This sleep determines for how long the note plays.
						// Speeds recitative of more than 3 repeated notes up to a maximum value.
						// For non-recitative, bases speed on note value, adjusting some manually.
						// noinspection BusyWait
						Thread.sleep((1000
								/ (chordMap.get(key).size() > 3 ? Math.min(chordMap.get(key).size(), 5)
								: Integer.parseInt(chord.getDuration().replace("8", "6")
								.replace("4.", "3").replace("2.", "2"))))
								+ (chord.getDuration().contains("2.") ? 200 : 0));

						Platform.runLater(() -> currentButton.setStyle(oldButtonStyle));
						buttonIndex++;
					}
					key++;
				}
				if (lastSyllable != null)
					Platform.runLater(lastSyllable::applyDefaultFill);

				playButton.setDisable(false);
				return null;
			}
		};

		Thread midiThread = new Thread(midiTask);
		midiThread.start();
	}

	@FXML private void handleEdit() {
		FXMLLoaderIO.loadFXMLLayoutAsync("/fxml/VLineEditView.fxml", loader -> {
			VBox rootLayout = loader.getRoot();
			VLineEditViewController controller = loader.getController();

			controller.setParentController(this);
			controller.setSyllableText(verseLine.getLine());
			controller.setBarSelections(beforeBar.get(), afterBar.get());
			controller.setDisableLineBreaks(disableLineBreaks);

			Platform.runLater(() -> {
				Stage lineEditStage = new Stage();
				lineEditStage.setTitle("Edit Line");
				lineEditStage.getIcons().add(MainApp.APP_ICON);
				lineEditStage.setScene(new Scene(rootLayout));
				lineEditStage.initModality(Modality.APPLICATION_MODAL);
				lineEditStage.setResizable(false);
				lineEditStage.initOwner(rootPane.getScene().getWindow());
				lineEditStage.show();
			});
		});
	}

	public SyllableText[] getSyllables() {
		return lineTextFlow.getChildren().stream().map(node -> (SyllableText) node).toArray(SyllableText[]::new);
	}

	void showSyllableMenu(SyllableText syllable) {
		topController.showSyllableMenu(syllable);
	}
	void hideSyllableMenu() {
		topController.hideSyllableMenu();
	}

	public void setTonePhraseChoice(String choice) {
		if (tonePhraseChoice.getItems().contains(choice))
			tonePhraseChoice.getSelectionModel().select(choice);
	}

	public String getBeforeBar() {
		return LilyPondInterface.barStrings[beforeBar.get()];
	}
	public String getAfterBar() {
		return LilyPondInterface.barStrings[afterBar.get()];
	}

	public boolean getDisableLineBreaks() {
		return disableLineBreaks;
	}
	public void setDisableLineBreaks(boolean disable) {
		disableLineBreaks = disable;

		topController.projectEdited();
	}

	public IntegerProperty afterBarProperty() {
		return afterBar;
	}

	public void setAssignmentDurations(List<String> durations) {
		int i = 0;
		for (SyllableText syllable : getSyllables()) {
			for (int j = 0; j < syllable.getAssociatedChords().length; j++) {
				syllable.setNoteDuration(durations.get(i), j);
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

	private void refreshBarViews() {
		beforeBarView.setImage(VLineEditViewController.barImages[beforeBar.get()]);
		afterBarView.setImage(VLineEditViewController.barImages[afterBar.get()]);
	}

	public void verseEdited() {
		mainController.verseEdited();
	}

	boolean notFirstInItem() {
		return mainController.getVerseLineControllers().indexOf(this) != 0;
	}

	boolean hasAssignments() {
		return lastSyllableAssigned != -1;
	}

	AssignmentLine generateLineModel() {
		List<AssignmentSyllable> syllables = lineTextFlow.getChildren().stream().map(s -> ((SyllableText) s).generateSyllableModel()).toList();
		ChantPhrase selectedPhrase = associatedChantPhrases != null ? associatedChantPhrases[selectedChantPhrase].generatePhraseModel() : null;

		return new AssignmentLine.AssignmentLineBuilder().selectedChantPhrase(selectedPhrase)
				.syllables(syllables).beforeBar(LilyPondInterface.barStrings[beforeBar.get()])
				.afterBar(LilyPondInterface.barStrings[afterBar.get()]).separator(isSeparatorLine)
				.systemBreakDisabled(disableLineBreaks).buildAssignmentLine();
	}

}
