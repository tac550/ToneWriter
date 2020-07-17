package com.tac550.tonewriter.io;

import com.tac550.tonewriter.model.AssignedChordData;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.SyllableText;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Button;

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import java.io.File;
import java.util.*;

public class MidiInterface {

	private static Sequencer sequencer = null;

	public static void playAssignedPhrase(SyllableText[] syllables, Button playButton) {
		if (sequencer == null) return;

		playButton.setDisable(true);

		Task<Void> midiTask = new Task<>() {
			@Override
			protected Void call() throws Exception {
				// Setup before playing
				List<Button> buttons = new ArrayList<>();
				Map<Integer, List<AssignedChordData>> chordMap = new HashMap<>();
				int key = -1;

				String previousFieldsAndDur = null;
				for (SyllableText syllable : syllables) {
					// Place all the buttons into the buttons list in the order they occur
					buttons.addAll(syllable.getAssociatedButtons());

					for (AssignedChordData chord : syllable.getAssociatedChords()) {
						String fieldsAndDur = chord.getChordController().getFields() + chord.getDuration();
						// Group elements together in sequential lists in map if notes are same and duration is quarter.
						if (fieldsAndDur.equals(previousFieldsAndDur)
								&& chord.getDuration().equals(LilyPondInterface.NOTE_QUARTER)) {
							chordMap.get(key).add(chord);
						} else {
							chordMap.put(++key, new ArrayList<>(Collections.singletonList(chord)));
						}

						previousFieldsAndDur = fieldsAndDur;
					}
				}

				// Playing loop
				int buttonIndex = 0;
				key = 0;
				while (chordMap.containsKey(key)) {
					for (AssignedChordData chord : chordMap.get(key)) {
						Button currentButton = buttons.get(buttonIndex);
						String oldStyle = currentButton.getStyle();
						currentButton.setStyle("-fx-base: #fffa61");
						chord.getChordController().playMidi();
						// This sleep determines for how long the note plays.
						// Speeds recitative of more than 3 repeated notes up to a minimum value.
						// For non-recitative, bases speed on note value, adjusting some manually.
						// noinspection BusyWait
						Thread.sleep(1000
								/ (chordMap.get(key).size() > 3 ? Math.min(chordMap.get(key).size(), 8)
								: Integer.parseInt(chord.getDuration()
								.replace("4.", "3").replace("8", "6"))));

						currentButton.setStyle(oldStyle);
						buttonIndex++;
					}
					key++;
				}

				playButton.setDisable(false);
				return null;
			}
		};

		Thread midiThread = new Thread(midiTask);
		midiThread.start();
	}

	public static void playChord(File midiFile, Button playButton) {
		if (sequencer == null) return;

		// Playing the midi file
		Task<Void> midiTask = new Task<>() {
			@Override
			protected Void call() throws Exception {
				sequencer.stop();
				sequencer.setSequence(javax.sound.midi.MidiSystem.getSequence(midiFile));
				sequencer.start();

				// Thread which highlights and unhighlights the play button.
				new Thread(() -> {
					Platform.runLater(() -> playButton.setStyle("-fx-base: #fffa61"));
					try {
						Thread.sleep(1000);
						Platform.runLater(() -> playButton.setStyle(""));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}).start();

				return null;
			}
		};

		Thread midiThread = new Thread(midiTask);
		midiThread.start();
	}

	public static void setUpMidiSystem() { // TODO: Hangs app on Linux if run after resuming from suspend
		// Set up sequencer if not already done
		try {
			if (sequencer == null) {
				sequencer = javax.sound.midi.MidiSystem.getSequencer();
			}

			sequencer.open();
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
			TWUtils.showError("MIDI system unavailable!", true);
		}
	}

	public static void closeMidiSystem() {
		if (sequencer != null) sequencer.close();
	}

	public static void resetMidiSystem() {
		closeMidiSystem();
		setUpMidiSystem();
	}
}
