package com.tac550.tonewriter.model;

import com.tac550.tonewriter.view.ChantChordController;
import javafx.fxml.FXML;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class EndChord extends ChantChordController {

	private final ArrayList<PrepChord> prepChords = new ArrayList<>(); // Stored left-to-right

	@FXML protected void initialize() {
		super.initialize();
		numText.setText("End");
		preButton.setDisable(false);
		posButton.setDisable(true);
	}

	@Override
	public void addPrepChord() throws IOException {
		chantLineController.edited();
		addPrepChord("");
	}
	@Override
	public void addPostChord() {}

	public PrepChord addPrepChord(String values) throws IOException {
		PrepChord prepChord = chantLineController.addPrepChord(this, chordColor);
		prepChords.add(prepChord);
		prepChord.setAssociatedChord(this);
		prepChord.setFields(values);

		return prepChord;
	}

	@Override
	public void deleteAll() {
		for (PrepChord chord : prepChords) {
			chord.deleteAll();
		}

		chantLineController.removeChord(this);
	}

	@Override
	public RecitingChord getAssociatedRecitingChord() {
		return null;
	}

	@Override
	public void rotatePrepsOrPosts(ChantChordController source, ChantChordController target) {
		ArrayList<ChantChordController> resultList = new ArrayList<>(prepChords);

		int sourceIndex = resultList.indexOf(source);
		int targetIndex = resultList.indexOf(target);

		if (sourceIndex < targetIndex) {
			Collections.rotate(resultList.subList(sourceIndex, targetIndex + 1), -1);
		} else {
			Collections.rotate(resultList.subList(targetIndex, sourceIndex + 1), 1);
		}

		prepChords.clear();
		resultList.forEach(item -> prepChords.add((PrepChord) item));
	}

	public ArrayList<PrepChord> getPreps() {
		return prepChords;
	}

}
