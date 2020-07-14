package com.tac550.tonewriter.model;

import com.tac550.tonewriter.view.ChantChordController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainChord extends ChantChordController {

	protected final List<PrepChord> prepChords = new ArrayList<>(); // Stored left-to-right
	protected final List<PostChord> postChords = new ArrayList<>(); // Stored right-to-left

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

	public void rotatePrepsOrPosts(SubChord source, SubChord target) {
		if (source.getClass() != target.getClass()) return;

		List<?> workingList = (source instanceof PrepChord ? prepChords : postChords);

		int sourceIndex = workingList.indexOf(source);
		int targetIndex = workingList.indexOf(target);

		if (sourceIndex < targetIndex) {
			Collections.rotate(workingList.subList(sourceIndex, targetIndex + 1), -1);
		} else {
			Collections.rotate(workingList.subList(targetIndex, sourceIndex + 1), 1);
		}
	}

	@Override
	public void delete() {
		for (PrepChord chord : prepChords) {
			chantLineController.removeChord(chord);
		} for (PostChord chord : postChords) {
			chantLineController.removeChord(chord);
		}
		prepChords.clear();
		postChords.clear();

		chantLineController.removeChord(this);
	}

	@Override
	public MainChord getAssociatedMainChord() {
		return this;
	}

	public List<PrepChord> getPreps() {
		return prepChords;
	}
	public List<PostChord> getPosts() {
		return postChords;
	}

}
