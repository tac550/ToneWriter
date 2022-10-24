package com.tac550.tonewriter.io;

import com.tac550.tonewriter.util.TWUtils;
import javafx.concurrent.Task;

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import java.io.File;

public class MidiInterface {

	private static Sequencer sequencer = null;

	public static boolean sequencerActive() {
		return sequencer != null;
	}

	public static void playChord(File midiFile) {
		if (sequencer == null) return;

		// Playing the midi file
		Task<Void> midiTask = new Task<>() {
			@Override
			protected Void call() throws Exception {
				sequencer.stop();
				sequencer.setSequence(javax.sound.midi.MidiSystem.getSequence(midiFile));
				sequencer.start();

				return null;
			}
		};

		Thread midiThread = new Thread(midiTask);
		midiThread.start();
	}

	public static void setUpMidiSystem() { // TODO: Hangs app on Linux if run after resuming from suspend
		// Set up sequencer if not already done
		try {
			if (sequencer == null)
				sequencer = javax.sound.midi.MidiSystem.getSequencer();

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
