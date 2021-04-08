package com.tac550.tonewriter.model;

import javafx.scene.paint.Color;

public class RecitingChord extends MainChord {

	public void setNumber(int number) {
		numText.setText(String.valueOf(number));
	}

	@Override
	public void setColor(Color color) {
		super.setColor(color);

		for (PrepChord chord : prepChords) {
			chord.setColor(color);
		} for (PostChord chord : postChords) {
			chord.setColor(color);
		}
	}

}
