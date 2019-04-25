package com.tac550.tonewriter.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.io.FileUtils;

import com.tac550.tonewriter.view.ChantChordController;
import com.tac550.tonewriter.view.ChantLineViewController;
import com.tac550.tonewriter.view.MainApp;
import com.tac550.tonewriter.view.MainSceneController;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class ToneReaderWriter {

	private ArrayList<ChantLineViewController> chantLines;
	
	private String composerText;
	
	private String keySig;

	private MainSceneController mainScene;
	
	public ToneReaderWriter(ArrayList<ChantLineViewController> lines, String key, String composer) {
		chantLines = lines;
		keySig = key;
		composerText = composer;
	}
	public ToneReaderWriter(ArrayList<ChantLineViewController> lines) {
		chantLines = lines;
	}
	
	public boolean saveTone(File directory) {
		try {
			// Clear out old save data.
			FileUtils.cleanDirectory(directory);
		    
			// Line name which is marked first repeated. Filled when found.
			String firstRepeated = "";

			// For each chant line...
			for (ChantLineViewController chantLine : chantLines) {

				if (chantLine.getFirstRepeated()) {
					firstRepeated = chantLine.getName();
				}

				// Create the output .dat file with the name of the chant line.
				FileWriter fileWriter = new FileWriter(directory.getAbsolutePath() + File.separator + chantLine.getName() + ".dat");
				PrintWriter printWriter = new PrintWriter(fileWriter);

				// Place chant line comment on the first line, if any.
			    if (chantLine.hasComment()) {
			    	printWriter.println("Comment: " + chantLine.getComment());
			    }
			    
			    // For each chord in the chant line...
			    for (ChantChordController chord : chantLine.getChords()) {
			    	if (chord.getType() > 0) { // Main chords with preps and posts
			    		printWriter.println(chord.getName() + ": " + chord.getFields() + 
				    			(chord.hasComment() ? ": " + chord.getComment() : ""));
			    		for (ChantChordController child : chord.getPrepsAndPosts()) {
			    			if (child.getType() == -1) { // Preps save out first
			    				printWriter.println("\tPrep: " + child.getFields() + 
						    			(child.hasComment() ? ": " + child.getComment() : ""));
			    			}
			    		}
			    		for (ChantChordController child : chord.getPrepsAndPosts()) {
			    			if (child.getType() == -2) { // Posts second
			    				printWriter.println("\tPost: " + child.getFields() + 
						    			(child.hasComment() ? ": " + child.getComment() : ""));
			    			}
			    		}
			    	} else if (chord.getType() == -3) { // Ending chords
			    		printWriter.println("END: " + chord.getFields() + 
			    				(chord.hasComment() ? ": " + chord.getComment() : ""));
			    		for (ChantChordController child : chord.getPrepsAndPosts()) {
			    			printWriter.println("\tPrep: " + child.getFields() + 
			    			(child.hasComment() ? ": " + child.getComment() : ""));
			    		}
			    	}
			    }

			    printWriter.close();
			    fileWriter.close();

			}

			// Write out the Info file.
			FileWriter infoWriter = new FileWriter(directory.getAbsolutePath() + File.separator + "_Info.dat");
		    PrintWriter infoPrintWriter = new PrintWriter(infoWriter);
		    infoPrintWriter.println("VERSION: " + MainApp.APP_VERSION);
		    infoPrintWriter.println("First Repeated: " + firstRepeated);
		    infoPrintWriter.println("Key Signature: " + keySig.replace("♯", "s").replace("♭", "f"));
		    infoPrintWriter.println("Composer Text: " + composerText);

		    infoPrintWriter.close();
		    infoWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	public boolean loadTone(MainSceneController main_scene, File directory) {
		this.mainScene = main_scene;

		String firstRepeated;
		float versionSaved;

		try {
			// Loading from info file
			File infoFile = new File(directory.getAbsolutePath() + File.separator + "_Info.dat");
			FileReader fileReader = new FileReader(infoFile);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			versionSaved = Float.parseFloat(bufferedReader.readLine().split(":")[1].trim());
			firstRepeated = bufferedReader.readLine().split(":")[1].trim();
			keySig = bufferedReader.readLine().split(":")[1].trim().replace("s", "♯").replace("f", "♭");
			composerText = bufferedReader.readLine().split(":")[1].trim();
			fileReader.close();
			
			// Version checking
			if (versionSaved > Float.parseFloat(MainApp.APP_VERSION)) {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Warning");
				alert.setHeaderText(String.format(Locale.US, "This tone was created with a newer version of %s. Be advised there may be issues.", MainApp.APP_NAME));
				
				alert.showAndWait();
			}
			
			// Loading chant lines
			main_scene.clearChantLines();
			List<File> toneFiles = Objects.requireNonNull(Arrays.asList(Objects.requireNonNull(directory.listFiles())));

			// Keep track of the letter of chant line we're loading files for.
			char readingFilesFor = 'A';
			
			while (containsFilesFor(toneFiles, readingFilesFor)) {
				List<File> AltsForLine = new ArrayList<>();
				File primeFile = null;
				File mainFile = null;
				
				// For each file we found that's associated with the current line letter...
				for (File file : getLineFiles(toneFiles, readingFilesFor)) {
					String fileName = file.getName().replace(".dat", "");
					if (!fileName.contains("Info") && !fileName.contains("Cadence")) {
						if (fileName.startsWith(String.valueOf(readingFilesFor))) {
							if (fileName.endsWith(String.valueOf(readingFilesFor))) {
								// File both starts and ends with the CL letter, so it's the normal chant line file.
								mainFile = file;
							} else if (fileName.endsWith("'")) {
								// It's the prime for the letter.
								primeFile = file;
							} else {
								// It's an alternate.
								AltsForLine.add(file);
							}
						}
					}
				}
				
				// Load in the main file first.
				readFile(mainFile);
				
				// Then sort and load any alternates.
				Collections.sort(AltsForLine);
				for (File file : AltsForLine) {
					readFile(file);
				}
				
				// Then load the prime, if any.
				if (primeFile != null) {
					readFile(primeFile);
				}
				
				readingFilesFor++;
			}
			
			// Load the cadence last.
			readFile(new File(directory.getAbsolutePath() + File.separator + "Cadence.dat"));

			bufferedReader.close();
			fileReader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			main_scene.clearChantLines();
			return false;
		}

		main_scene.setCurrentKey(keySig);
		main_scene.setComposerText(composerText);
		main_scene.recalcCLNames();
		main_scene.setFirstRepeated(firstRepeated);
		
		// This was already done by recalcCLNames() above, but this updates the listing with the firstRepeated info.
		main_scene.syncCVLMapping();
		
		return true;
	}

	private void readFile(File file) throws IOException {

		ChantLineViewController currentChantLine = mainScene.createChantLine(false);
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String chantFileLine;
		ChantChordController currentMainChord = null;

		boolean firstLine = true;
		
		while ((chantFileLine = bufferedReader.readLine()) != null) {
			if (firstLine) { // First line used for chant line comment, if any
				if (chantFileLine.startsWith("Comment: ")) {
					String[] commentData = chantFileLine.split(": ");
					String comment = String.join(": ", Arrays.copyOfRange(commentData, 1, commentData.length));
					currentChantLine.setComment(comment);
					
					firstLine = false;
					continue;
				}
				firstLine = false;
			}
			
			String[] chordData = chantFileLine.split(": ");
			String fields = chordData[1];
			// Since the comment may include ": "s, we need to account for the possibility that it's been split and re-combine it here.
			String comment = String.join(": ", Arrays.copyOfRange(chordData, 2, chordData.length));
			
			// Add the appropriate chord type.
			if (!chantFileLine.startsWith("\t") && !chantFileLine.contains("END")) {
				currentMainChord = currentChantLine.addRecitingChord();
				currentMainChord.setFields(fields);
				currentMainChord.setComment(comment);
			} else if (chantFileLine.contains("Post")) {
				assert currentMainChord != null;
				ChantChordController postChord = currentMainChord.addPostChord(fields);
				postChord.setComment(comment);
			} else if (chantFileLine.contains("Prep")) {
				assert currentMainChord != null;
				ChantChordController prepChord = currentMainChord.addPrepChord(fields);
				prepChord.setComment(comment);
			} else if (chantFileLine.contains("END")) {
				currentMainChord = currentChantLine.addEndChord();
				currentMainChord.setFields(fields);
				currentMainChord.setComment(comment);
			}
		}
		
		if (file.getName().contains("'")) {
			currentChantLine.makePrime();
		} else if (file.getName().contains("alt")) {
			currentChantLine.makeAlternate();
		}
		
		bufferedReader.close();
		fileReader.close();
	}

	private static boolean containsFilesFor(List<File> file_list, char character) {
		boolean result = false;
		
		for (File file : file_list) {
			if (file.getName().startsWith(String.valueOf(character))
					&& !file.getName().contains("Cadence")) {
				result = true;
				break;
			}
		}
		
		return result;
		
	}
	
	private static List<File> getLineFiles(List<File> file_list, char character) {
		List<File> lineFiles = new ArrayList<>();
		
		for (File file : file_list) {
			if (file.getName().startsWith(String.valueOf(character))
					&& !file.getName().contains("Cadence")) {
				
				lineFiles.add(file);
			}
		}
		
		return lineFiles;
		
	}

}
