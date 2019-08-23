package com.tac550.tonewriter.io;

import com.tac550.tonewriter.view.ChantChordController;
import com.tac550.tonewriter.view.ChantLineViewController;
import com.tac550.tonewriter.view.MainApp;
import com.tac550.tonewriter.view.MainSceneController;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.apache.commons.text.TextStringBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

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

	public boolean saveTone(File toneFile) {
		try {
			// Clear out old save data.
			// noinspection ResultOfMethodCallIgnored
			toneFile.delete();
			if (!toneFile.createNewFile()) {
				return false;
			}

			// Set up PrintWriter
			FileWriter fileWriter = new FileWriter(toneFile);
			PrintWriter printWriter = new PrintWriter(fileWriter);

			// Header info
			printWriter.println("VERSION: " + MainApp.APP_VERSION);
			printWriter.println("Key Signature: " +
					keySig.replace("♯", "s").replace("♭", "f"));
			printWriter.println("Composer Text: " + composerText);
			printWriter.println();
			printWriter.println();

			// Line name which is marked first repeated. Filled when found.
			String firstRepeated = "";

			// For each chant line...
			for (ChantLineViewController chantLine : chantLines) {

				if (chantLine.getFirstRepeated()) {
					firstRepeated = chantLine.getName();
				}

				printWriter.println(chantLine.getName());

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

				printWriter.println();

			}

			// Footer info
			printWriter.println();
		    printWriter.println("First Repeated: " + firstRepeated);

			printWriter.close();
			fileWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	public boolean loadTone(MainSceneController main_scene, File toneFile) {
		this.mainScene = main_scene;

		String firstRepeated;
		float versionSaved;

		try {

			// Load entire tone file and split it as necessary
			TextStringBuilder fileStringBuilder = new TextStringBuilder();
			Files.lines(toneFile.toPath(), StandardCharsets.UTF_8).forEach(fileStringBuilder::appendln);
			String[] sections = fileStringBuilder.toString().split("\\r?\\n\\r?\\n\\r?\\n");
			String[] headerAndFooter = String.join("\n", sections[0], sections[2]).split("\\r?\\n");
			String[] chantLines = sections[1].split("\\r?\\n\\r?\\n");

			versionSaved = Float.parseFloat(headerAndFooter[0].split(":")[1].trim());
			keySig = headerAndFooter[1].split(":")[1].trim().replace("s", "♯").replace("f", "♭");
			composerText = headerAndFooter[2].split(":")[1].trim();
			firstRepeated = headerAndFooter[3].split(":")[1].trim();

			// Version checking
			if (versionSaved > Float.parseFloat(MainApp.APP_VERSION)) {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Warning");
				alert.setHeaderText(String.format(Locale.US,
						"This tone was created with a newer version of %s. Be advised there may be issues.",
						MainApp.APP_NAME));
				
				alert.showAndWait();
			}
			
			// Loading chant lines
			main_scene.clearChantLines();

			for (String chantLine : chantLines) {
				readChantLine(chantLine);
			}

		} catch (IOException e) {
			e.printStackTrace();
			main_scene.clearChantLines();
			return false;
		}

		main_scene.setCurrentKey(keySig);
		main_scene.setComposerText(composerText);
		main_scene.recalcCLNames();
		main_scene.setFirstRepeated(firstRepeated);
		
		return true;
	}

	private void readChantLine(String chantLine) throws IOException {

		ChantLineViewController currentChantLine = mainScene.createChantLine(false);
		Scanner chantLineScanner = new Scanner(chantLine);
		String chantLineLine;

		ChantChordController currentMainChord = null;

		String chantLineName = chantLineScanner.nextLine();

		while (chantLineScanner.hasNextLine() && (chantLineLine = chantLineScanner.nextLine()) != null) {
			if (chantLineLine.startsWith("Comment: ")) {
				String[] commentData = chantLineLine.split(": ");
				String comment = String.join(": ", Arrays.copyOfRange(commentData, 1, commentData.length));
				currentChantLine.setComment(comment);

				continue;
			}

			String[] chordData = chantLineLine.split(": ");
			String fields = chordData[1];
			// Since the comment may include ": "s, we need to account for the possibility that it's been split and re-combine it here.
			String comment = String.join(": ", Arrays.copyOfRange(chordData, 2, chordData.length));
			
			// Add the appropriate chord type.
			if (!chantLineLine.startsWith("\t") && !chantLineLine.contains("END")) {
				currentMainChord = currentChantLine.addRecitingChord();
				currentMainChord.setFields(fields);
				currentMainChord.setComment(comment);
			} else if (chantLineLine.contains("Post")) {
				assert currentMainChord != null;
				ChantChordController postChord = currentMainChord.addPostChord(fields);
				postChord.setComment(comment);
			} else if (chantLineLine.contains("Prep")) {
				assert currentMainChord != null;
				ChantChordController prepChord = currentMainChord.addPrepChord(fields);
				prepChord.setComment(comment);
			} else if (chantLineLine.contains("END")) {
				currentMainChord = currentChantLine.addEndChord();
				currentMainChord.setFields(fields);
				currentMainChord.setComment(comment);
			}
		}
		
		if (chantLineName.contains("'")) {
			currentChantLine.makePrime();
		} else if (chantLineName.contains("alt")) {
			currentChantLine.makeAlternate();
		}

		chantLineScanner.close();
	}

	public static boolean createToneFile(File file_to_create) {
		if (file_to_create.getParentFile().mkdirs() || file_to_create.getParentFile().exists()) {
			try {
				return file_to_create.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		} else return false;
	}
}
