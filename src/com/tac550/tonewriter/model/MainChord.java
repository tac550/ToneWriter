package com.tac550.tonewriter.model;

import com.tac550.tonewriter.view.ChantChordController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class MainChord extends ChantChordController {

	protected final ArrayList<PrepChord> prepChords = new ArrayList<>(); // Stored left-to-right
	protected final ArrayList<PostChord> postChords = new ArrayList<>(); // Stored right-to-left

	public PrepChord addPrepChord(String values) throws IOException {
		chantLineController.edited();

		PrepChord prepChord = chantLineController.addPrepChord(this, chordColor);
		prepChords.add(prepChord);
		prepChord.setAssociatedChord(this);
		prepChord.setFields(values);

		return prepChord;
	}
	public PostChord addPostChord(String values) throws IOException {
		chantLineController.edited();

		PostChord postChord = chantLineController.addPostChord(this, chordColor);
		postChords.add(postChord);
		postChord.setAssociatedChord(this);
		postChord.setFields(values);

		return postChord;
	}

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

	@Override
	public void deleteAll() { // TODO: ConcurrentModificationException
		for (PrepChord chord : prepChords) {
			chord.deleteAll();
		} for (PostChord chord : postChords) {
			chord.deleteAll();
		}
		chantLineController.removeChord(this);
	}

	@Override
	public MainChord getAssociatedMainChord() {
		return this;
	}

	public ArrayList<PrepChord> getPreps() {
		return prepChords;
	}
	public ArrayList<PostChord> getPosts() {
		return postChords;
	}

}
