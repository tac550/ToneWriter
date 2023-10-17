package com.tac550.tonewriter.io;

import com.tac550.tonewriter.util.TWUtils;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import java.io.File;
import java.io.IOException;

public class MidiInterface {

	private static Sequencer sequencer = null;

	public static boolean sequencerActive() {
		return sequencer != null;
	}

	public static void playMidiFile(File midiFile) {
		if (sequencer == null) return;

		Thread midiThread = new Thread(() -> {
			sequencer.stop();
			try {
				sequencer.setSequence(javax.sound.midi.MidiSystem.getSequence(midiFile));
			} catch (InvalidMidiDataException | IOException e) {
				throw new RuntimeException(e);
			}
			sequencer.start();
		});
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
