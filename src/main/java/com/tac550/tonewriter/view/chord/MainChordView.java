package com.tac550.tonewriter.view.chord;

import com.tac550.tonewriter.view.ChordViewController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainChordView extends ChordViewController {

	protected final List<PrepChordView> prepChords = new ArrayList<>(); // Stored left-to-right
	protected final List<PostChordView> postChords = new ArrayList<>(); // Stored right-to-left

	public PrepChordView addPrepChord() throws IOException {
		chantPhraseController.edited();

		PrepChordView controller = new PrepChordView();
		prepChords.add(controller);
		controller.setAssociatedChord(this);

		chantPhraseController.addPrepChord(controller, this, chordColor);

		return controller;
	}
	public PostChordView addPostChord() throws IOException {
		chantPhraseController.edited();

		PostChordView controller = new PostChordView();
		postChords.add(controller);
		controller.setAssociatedChord(this);

		chantPhraseController.addPostChord(controller, this, chordColor);

		return controller;
	}

	public void rotatePrepsOrPosts(SubChordView source, SubChordView target) {
		if (source.getClass() != target.getClass()) return;

		List<?> workingList = (source instanceof PrepChordView ? prepChords : postChords);

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
		for (PrepChordView chord : prepChords) {
			chantPhraseController.removeChord(chord);
		} for (PostChordView chord : postChords) {
			chantPhraseController.removeChord(chord);
		}
		prepChords.clear();
		postChords.clear();

		chantPhraseController.removeChord(this);
	}

	@Override
	public MainChordView getAssociatedMainChord() {
		return this;
	}

	public List<PrepChordView> getPreps() {
		return prepChords;
	}
	public List<PostChordView> getPosts() {
		return postChords;
	}

}
