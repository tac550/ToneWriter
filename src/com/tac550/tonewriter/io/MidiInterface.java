package com.tac550.tonewriter.io;

import com.tac550.tonewriter.model.AssignedChordData;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.SyllableText;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import java.io.File;
import java.util.*;

public class MidiInterface {

	private static Sequencer sequencer = null;

	public static void playAssignedPhrase(SyllableText[] syllables) {
		if (sequencer == null) return;

		Task<Void> midiTask = new Task<>() {
			@Override
			protected Void call() throws Exception {
				// Setup before playing
				Map<Integer, List<AssignedChordData>> chordMap = new HashMap<>();
				int key = -1;

				String previousFields = null;
				for (SyllableText syllable : syllables) {
					for (AssignedChordData chord : syllable.getAssociatedChords()) {
						if (chord.getChordController().getFields().equals(previousFields)
								&& chord.getDuration().equals(LilyPondInterface.NOTE_QUARTER)) {
							chordMap.get(key).add(chord);
						} else {
							chordMap.put(++key, new ArrayList<>(Collections.singletonList(chord)));
						}

						previousFields = chord.getChordController().getFields();
					}
				}

				// Playing loop
				key = 0;
				while (chordMap.containsKey(key)) {
					for (AssignedChordData chord : chordMap.get(key)) {
						chord.getChordController().playMidi();
						Thread.sleep(1000 / Integer.parseInt(chord.getDuration().replace("4.", "3"))
								/ chordMap.get(key).size());
					}
					key++;
				}

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
				Thread stopThread = new Thread(() -> {
					Platform.runLater(() -> playButton.setStyle("-fx-base: #fffa61"));
					try {
						Thread.sleep(1000);
						Platform.runLater(() -> playButton.setStyle(""));
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				});
				stopThread.start();

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
