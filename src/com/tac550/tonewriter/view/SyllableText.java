package com.tac550.tonewriter.view;

import com.tac550.tonewriter.model.AssignedChordData;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class SyllableText extends Text {

	private VerseLineViewController parentController;
	
	private final List<AssignedChordData> associatedChords = new ArrayList<>();
	private final List<Button> associatedButtons = new ArrayList<>();
	
	private boolean active = true;
	// Was this the one that was clicked?
	private boolean clicked = false;
	private int nextNoteButtonYPos = 0;
	
	Color defaultColor = MainApp.isDarkModeEnabled() ? Color.WHITE : Color.BLACK;
	private static final Color highlightColor = Color.DARKCYAN;
	
	void setParent(VerseLineViewController controller) {
		parentController = controller;
	}
	
	SyllableText(String text) {
		super(text);

		setFill(defaultColor);

		hoverProperty().addListener((o, old_val, new_val) -> {
			if (active) {
				if (new_val) {
					setFill(highlightColor);
					parentController.syllableHovered();
				} else {
					setFill(defaultColor);
					parentController.syllableUnHovered();
				}
			}
		});
		
		setOnMouseClicked(event -> {
			if (active) {
				if (event.getButton() == MouseButton.PRIMARY) {
					parentController.syllableUnHovered();

					clicked = true;
					parentController.syllableClicked(this);

					parentController.syllableHovered();
				} else {
					parentController.syllableAltClicked();
				}
			}
		});

		// Drag assignment events
		setOnMouseDragReleased(event -> {
			if (active) parentController.syllableDragCompleted(this);
		});
		setOnDragDetected(event -> {
			if (active) parentController.syllableDragStarted(this);
		});
		setOnMouseDragEntered(event -> {
			if (active) parentController.syllableDragEntered(this);
		});
		setOnMouseDragExited(event -> parentController.syllableDragExited());
		setOnMouseReleased(event -> parentController.syllableDragReleased());

	}
	
	void select(int chord_index, Color chord_color, Button note_button) {
		if (!clicked) active = false;

		nextNoteButtonYPos += MainApp.NOTE_BUTTON_HEIGHT.get();
		
		associatedChords.add(new AssignedChordData(getText(), chord_index, parentController));
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
	
	private void setColor(Color color) {
		defaultColor = color;
		setFill(color);
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
	List<Button> getAssociatedButtons() {
		return associatedButtons;
	}
	
	int getNextNoteButtonPosY() {
		return nextNoteButtonYPos;
	}
	
	void removeLastChord() {
		associatedChords.remove(associatedChords.size() - 1);
		associatedButtons.remove(associatedButtons.size() - 1);
		
		nextNoteButtonYPos -= MainApp.NOTE_BUTTON_HEIGHT.get();
		
		if (!associatedButtons.isEmpty()) {
			String colorString = "#" + associatedButtons.get(associatedButtons.size() - 1).getStyle().split("#")[1];
			
			setColor(Color.valueOf(colorString));
		} else {
			clearSelection();
		}
	}

	void refreshStyle() {
		if (getFill().equals(Color.BLACK) || getFill().equals(Color.WHITE)) {
			defaultColor = MainApp.isDarkModeEnabled() ? Color.WHITE : Color.BLACK;
			setFill(defaultColor);
		}
	}

}
