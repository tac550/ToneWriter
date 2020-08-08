package com.tac550.tonewriter.model;

import com.tac550.tonewriter.view.SyllableText;
import javafx.scene.control.Button;

import java.util.ArrayList;
import java.util.List;

public class AssignmentAction {

	public int previousChordIndex;
	public int previousLastSyllableAssigned;
	public final List<Button> buttons = new ArrayList<>();
	public final List<SyllableText> syllableTexts = new ArrayList<>();

}
