package com.tac550.tonewriter.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects when a process is finished and invokes the associated listeners.
 */
public class ProcessExitDetector extends Thread {

	/** The process for which we have to detect the end. */
	private final Process process;
	/** The associated listeners to be invoked at the end of the process. */
	private final List<ProcessListener> listeners = new ArrayList<>();

	/**
	 * Starts the detection for the given process
	 * @param process the process for which we have to detect when it is finished
	 */
	public ProcessExitDetector(Process process) {
		try {
			// test if the process is finished
			process.exitValue();
			throw new IllegalArgumentException("The process is already ended");
		} catch (IllegalThreadStateException exc) {
			this.process = process;
		}
	}

	public void run() {
		try {
			// wait for the process to finish
			process.waitFor();
			// invokes the listeners
			for (ProcessListener listener : listeners) {
				listener.processFinished(process);
			}
		} catch (InterruptedException ignored) {
		}
	}

	/** Adds a process listener.
	 * @param listener the listener to be added
	 */
	public void addProcessListener(ProcessListener listener) {
		listeners.add(listener);
	}

}