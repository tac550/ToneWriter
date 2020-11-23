package com.tac550.tonewriter.view;

import com.tac550.tonewriter.model.AssignedChordData;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

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
	
	Color defaultColor = MainApp.isDarkModeEnabled() ? Color.WHITE : Color.BLACK;
	private static final Color highlightColor = Color.DARKCYAN;
	
	void setParent(VerseLineViewController controller) {
		verseController = controller;
	}
	
	public SyllableText(String text) {
		super(text);

		setFill(defaultColor);

		hoverProperty().addListener((o, old_val, new_val) -> {
			if (active) {
				if (new_val) {
					setFill(highlightColor);
					verseController.syllableHovered();
				} else {
					setFill(defaultColor);
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
					verseController.playCurrectChord();
				}
			}

			if (event.getButton() == MouseButton.SECONDARY && !event.isControlDown()) {
				verseController.showSyllableMenu(this);
			}
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

	}
	
	void select(int chord_index, Color chord_color, Button note_button) {
		if (!clicked) active = false;

		nextNoteButtonYPos += VerseLineViewController.NOTE_BUTTON_HEIGHT.get();
		
		associatedChords.add(new AssignedChordData(chord_index, verseController));
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
		return (Color) getFill();
	}
	public void setColor(Color color) {
		defaultColor = color;
		setFill(color);
	}

	boolean getBold() {
		return bold;
	}
	boolean getItalic() {
		return italic;
	}
	void setBold(boolean a_bold) {
		bold = a_bold;
		refreshFont();
	}
	void setItalic(boolean a_italic) {
		italic = a_italic;
		refreshFont();
	}

	void deactivate() {
		setFill(defaultColor);
		active = false;
	}
	void reactivate() {
		setFill(defaultColor);
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
		setFont(Font.font(getFont().getName(), bold ? FontWeight.BOLD : FontWeight.NORMAL,
				italic ? FontPosture.ITALIC : FontPosture.REGULAR, getFont().getSize()));
	}

	void refreshStyle() {
		if (getFill().equals(Color.BLACK) || getFill().equals(Color.WHITE)) {
			defaultColor = MainApp.isDarkModeEnabled() ? Color.WHITE : Color.BLACK;
			setFill(defaultColor);
		}
	}

}
