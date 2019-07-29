package com.tac550.tonewriter.view;

import java.util.ArrayList;

import com.tac550.tonewriter.model.ChordData;

import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class SyllableText extends Text {

	static final String NOTE_QUARTER = "4";
	static final String NOTE_DOTTED_QUARTER = "4.";
	static final String NOTE_HALF = "2";
	static final String NOTE_EIGHTH = "8";
	
	private VerseLineViewController parentController;
	
	private ArrayList<ChordData> associatedChords = new ArrayList<>();
	private ArrayList<Button> associatedButtons = new ArrayList<>();
	
	private boolean active = true;
	// Was this the one that was clicked?
	private boolean clicked = false;
	private int nextNoteButtonYPos = 0;
	
	private Color defaultColor = MainApp.darkModeEnabled() ? Color.WHITE : Color.BLACK;
	private Color highlightColor = Color.DARKCYAN;
	
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
				} else {
					setFill(defaultColor);
				}	
			}
		});
		
		setOnMouseClicked((event) -> {
			if (active) {
				clicked = true;
				parentController.syllableClicked(this);	
			}
		});
	}
	
	void select(ChantChordController chord, Button note_button) {
		if (clicked) {
			nextNoteButtonYPos += MainApp.NOTE_BUTTON_HEIGHT;
			clicked = false;
		} else {
			active = false;
		}
		
		associatedChords.add(new ChordData(getText(), chord));
		associatedButtons.add(note_button);
		
		setColor(chord.getColor());
	}
	void clearSelection() {
		active = true;
		nextNoteButtonYPos = 0;
		setColor(MainApp.darkModeEnabled() ? Color.WHITE : Color.BLACK);
		
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
	
	void setNoteDuration(String duration, Button note_button) {
		associatedChords.get(associatedButtons.indexOf(note_button)).setDuration(duration);
	}
	
	public ChordData[] getAssociatedChords() {
		return associatedChords.toArray(new ChordData[] {});
	}
	
	int getNextNoteButtonPosY() {
		return nextNoteButtonYPos;
	}
	
	void removeLastChord() {
		associatedChords.remove(associatedChords.size()-1);
		associatedButtons.remove(associatedButtons.size()-1);
		
		nextNoteButtonYPos -= MainApp.NOTE_BUTTON_HEIGHT;
		
		if (!associatedButtons.isEmpty()) {
			String colorString = "#" + associatedButtons.get(associatedButtons.size()-1).getStyle().split("#")[1];
			
			setColor(Color.valueOf(colorString));
		} else {
			clearSelection();
		}
	}

	void refreshStyle() {
		if (getFill().equals(Color.BLACK) || getFill().equals(Color.WHITE)) {
			defaultColor = MainApp.darkModeEnabled() ? Color.WHITE : Color.BLACK;
			setFill(defaultColor);
		}
	}

}
