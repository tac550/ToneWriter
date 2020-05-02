package com.tac550.tonewriter.model;

import com.tac550.tonewriter.view.ChantChordController;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class RecitingChord extends ChantChordController {

	private final ArrayList<PrepChord> prepChords = new ArrayList<>(); // Stored left-to-right
	private final ArrayList<PostChord> postChords = new ArrayList<>(); // Stored right-to-left

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

	@Override
	public void addPrepChord() throws IOException {
		chantLineController.edited();
		addPrepChord("");
	}
	@Override
	public void addPostChord() throws IOException {
		chantLineController.edited();
		addPostChord("");
	}

	public PrepChord addPrepChord(String values) throws IOException {
		PrepChord prepChord = chantLineController.addPrepChord(this, chordColor);
		prepChords.add(prepChord);
		prepChord.setAssociatedChord(this);
		prepChord.setFields(values);

		return prepChord;
	}
	public PostChord addPostChord(String values) throws IOException {
		PostChord postChord = chantLineController.addPostChord(this, chordColor);
		postChords.add(postChord);
		postChord.setAssociatedChord(this);
		postChord.setFields(values);

		return postChord;
	}

	@Override
	public void deleteAll() {
		for (PrepChord chord : prepChords) {
			chord.deleteAll();
		} for (PostChord chord : postChords) {
			chord.deleteAll();
		}
		chantLineController.removeChord(this);
	}

	@Override
	public RecitingChord getAssociatedRecitingChord() {
		return this;
	}

	@Override
	public void rotatePrepsOrPosts(ChantChordController source, ChantChordController target) {
		ArrayList<ChantChordController> resultList = new ArrayList<>(source instanceof PrepChord ? prepChords : postChords);

		int sourceIndex = resultList.indexOf(source);
		int targetIndex = resultList.indexOf(target);

		if (sourceIndex < targetIndex) {
			Collections.rotate(resultList.subList(sourceIndex, targetIndex + 1), -1);
		} else {
			Collections.rotate(resultList.subList(targetIndex, sourceIndex + 1), 1);
		}

		if (source instanceof PrepChord) {
			prepChords.clear();
			resultList.forEach(item -> prepChords.add((PrepChord) item));
		} else {
			postChords.clear();
			resultList.forEach(item -> postChords.add((PostChord) item));
		}

	}

	public ArrayList<PrepChord> getPreps() {
		return prepChords;
	}
	public ArrayList<PostChord> getPosts() {
		return postChords;
	}

}
