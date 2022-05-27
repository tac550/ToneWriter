package com.tac550.tonewriter.view;

import com.tac550.tonewriter.model.AssignedChordData;
import com.tac550.tonewriter.model.AssignmentSyllable;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SyllableText extends Text {

	private VerseLineViewController verseController;
	
	private final List<AssignedChordData> associatedChords = new ArrayList<>();
	private final List<Button> associatedButtons = new ArrayList<>();
	
	private boolean active = true;
	// Was this the one that was clicked?
	private boolean clicked = false;
	private int nextNoteButtonYPos = 0;

	// Formatting
	private boolean bold = false;
	private boolean italic = false;
	private boolean forceHyphen = false;

	private static final int fontSize = 28;
	private static final Font regularFont = Font.loadFont(Objects.requireNonNull(SyllableText.class.getResource("/styles/fonts/OpenSans-Regular.ttf")).toExternalForm(), fontSize);
	private static final Font boldFont = Font.loadFont(Objects.requireNonNull(SyllableText.class.getResource("/styles/fonts/OpenSans-Bold.ttf")).toExternalForm(), fontSize);
	private static final Font italicFont = Font.loadFont(Objects.requireNonNull(SyllableText.class.getResource("/styles/fonts/OpenSans-Italic.ttf")).toExternalForm(), fontSize);
	private static final Font boldItalicFont = Font.loadFont(Objects.requireNonNull(SyllableText.class.getResource("/styles/fonts/OpenSans-BoldItalic.ttf")).toExternalForm(), fontSize);
	
	Color defaultColor = MainApp.isDarkModeEnabled() ? Color.WHITE : Color.BLACK;
	private static final Color highlightColor = Color.DARKCYAN;
	
	void setParent(VerseLineViewController controller) {
		verseController = controller;
	}
	
	public SyllableText(String text) {
		super(text);

		setFont(regularFont);
		applyDefaultFill();

		hoverProperty().addListener((ov, oldVal, newVal) -> {
			if (active) {
				if (newVal) {
					setFill(highlightColor);
					verseController.syllableHovered();
				} else {
					applyDefaultFill();
					verseController.syllableUnHovered();
				}
			}
		});
		
		setOnMouseClicked(event -> {
			if (active) {
				if (event.getButton() == MouseButton.PRIMARY) {
					verseController.syllableUnHovered();

					clicked = true;
					verseController.syllableClicked(this);

					verseController.syllableHovered();
				} else if (event.isControlDown()) {
					verseController.playCurrentChord();
				}
			}

			if (event.getButton() == MouseButton.SECONDARY && !event.isControlDown())
				verseController.showSyllableMenu(this);
		});

		// Drag assignment events
		setOnMouseDragReleased(event -> {
			if (active) verseController.syllableDragCompleted(this);
		});
		setOnDragDetected(event -> {
			if (active) verseController.syllableDragStarted(this);
		});
		setOnMouseDragEntered(event -> {
			if (active) verseController.syllableDragEntered(this);
		});
		setOnMouseDragExited(event -> verseController.syllableDragExited());
		setOnMouseReleased(event -> verseController.syllableDragReleased());

		// Associated buttons stay vertically aligned with their syllable text.
		layoutXProperty().addListener((ov, oldVal, newVal) -> {
			for (Button button : associatedButtons)
				button.setLayoutX(newVal.doubleValue());
		});

		// Listen for changes to this syllable's text and disable the option to force hyphen visibility if there is none
		textProperty().addListener((ov, oldVal, newVal) -> {
			if (!newVal.startsWith("-") && forceHyphen)
				forceHyphen = false;
		});
	}

	public void applyDefaultFill() {
		setFill(defaultColor);
	}

	void select(int chord_index, Color chord_color, Button note_button) {
		if (!clicked) active = false;

		nextNoteButtonYPos += VerseLineViewController.NOTE_BUTTON_HEIGHT.get();
		
		associatedChords.add(new AssignedChordData(chord_index));
		associatedButtons.add(note_button);
		
		setColor(chord_color);
	}
	void clearSelection() {
		active = true;
		nextNoteButtonYPos = 0;
		setColor(MainApp.isDarkModeEnabled() ? Color.WHITE : Color.BLACK);
		
		associatedChords.clear();
		associatedButtons.clear();
	}

	public Color getColor() {
		return defaultColor;
	}
	public void setColor(Color color) {
		defaultColor = color;
		setFill(color);
	}

	public boolean getBold() {
		return bold;
	}
	public boolean getItalic() {
		return italic;
	}
	public boolean getForceHyphen() {
		return forceHyphen;
	}
	public void setBold(boolean a_bold) {
		bold = a_bold;
		refreshFont();
	}
	public void setItalic(boolean a_italic) {
		italic = a_italic;
		refreshFont();
	}
	public void setForceHyphen(boolean force_hyphen) {
		forceHyphen = force_hyphen;
	}

	void deactivate() {
		applyDefaultFill();
		active = false;
	}
	void reactivate() {
		applyDefaultFill();
		active = true;
	}
	
	void setNoteDuration(String duration, int chord_index) {
		associatedChords.get(chord_index).setDuration(duration);
	}
	String getNoteDuration(int chord_index) {
		return associatedChords.get(chord_index).getDuration();
	}
	
	public AssignedChordData[] getAssociatedChords() {
		return associatedChords.toArray(new AssignedChordData[] {});
	}
	public List<Button> getAssociatedButtons() {
		return associatedButtons;
	}
	
	int getNextNoteButtonPosY() {
		return nextNoteButtonYPos;
	}
	
	void removeLastChord() {
		associatedChords.remove(associatedChords.size() - 1);
		associatedButtons.remove(associatedButtons.size() - 1);
		
		nextNoteButtonYPos -= VerseLineViewController.NOTE_BUTTON_HEIGHT.get();
		
		if (!associatedButtons.isEmpty()) {
			String colorString = "#" + associatedButtons.get(associatedButtons.size() - 1).getStyle().split("#")[1];
			
			setColor(Color.valueOf(colorString));
		} else {
			clearSelection();
		}
	}

	private void refreshFont() {
		if (bold && italic)
			setFont(boldItalicFont);
		else if (bold)
			setFont(boldFont);
		else if (italic)
			setFont(italicFont);
		else
			setFont(regularFont);
	}

	void refreshStyle() {
		if (getFill().equals(Color.BLACK) || getFill().equals(Color.WHITE)) {
			defaultColor = MainApp.isDarkModeEnabled() ? Color.WHITE : Color.BLACK;
			applyDefaultFill();
		}
	}

	AssignmentSyllable generateSyllableModel() {
		return new AssignmentSyllable.AssignmentSyllableBuilder().syllableText(getText())
				.bold(bold).italic(italic).forceHyphen(forceHyphen).assignedChords(associatedChords).buildAssignmentSyllable();
	}

}
