package com.tac550.tonewriter.model;

import java.util.ArrayList;

import com.tac550.tonewriter.view.SyllableText;

import javafx.scene.control.Button;

public class MappingAction {

	public int previousChordIndex;
	public int previousLastSyllableAssigned;
	public ArrayList<Button> buttons = new ArrayList<>();
	public ArrayList<SyllableText> syllableTexts = new ArrayList<>();
	
}
