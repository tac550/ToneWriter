package com.tac550.tonewriter.view.chord;

import javafx.scene.paint.Color;

public class RecitingChordView extends MainChordView {

	public void setNumber(int number) {
		numText.setText(String.valueOf(number));
	}

	@Override
	public void setColor(Color color) {
		super.setColor(color);

		for (PrepChordView chord : prepChords) {
			chord.setColor(color);
		} for (PostChordView chord : postChords) {
			chord.setColor(color);
		}
	}

}
