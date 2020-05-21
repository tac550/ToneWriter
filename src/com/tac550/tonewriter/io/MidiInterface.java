package com.tac550.tonewriter.io;

import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import java.io.File;

public class MidiInterface {

	private static Sequencer sequencer = null;

	public static void playMidi(File midiFile, Button playButton) {

		if (sequencer == null) return;

		// Playing the midi file
		Task<Void> midiTask = new Task<>() {
			@Override
			protected Void call() throws Exception {
				sequencer.stop();
				sequencer.setSequence(javax.sound.midi.MidiSystem.getSequence(midiFile));
				sequencer.start();

				// Thread to stop midi playback after it's had long enough to finish. Also highlights the play button.
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

	public static void setUpMidiSystem() {
		// Set up sequencer if not already done
		try {
			if (sequencer == null) {
				sequencer = javax.sound.midi.MidiSystem.getSequencer();
			}

			sequencer.open();
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
			TWUtils.showAlert(AlertType.ERROR, "Error", "MIDI system unavailable!", true);
		}
	}

	/*
	 * This fixes the application not closing correctly if the user played midi.
	 */
	public static void closeMidiSystem() {
		if (sequencer != null) sequencer.close();
	}

	public static void resetMidiSystem() {
		closeMidiSystem();
		setUpMidiSystem();
	}
}
