package com.tac550.tonewriter.io;

import com.tac550.tonewriter.model.EndChord;
import com.tac550.tonewriter.model.PostChord;
import com.tac550.tonewriter.model.PrepChord;
import com.tac550.tonewriter.model.RecitingChord;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.ChantChordController;
import com.tac550.tonewriter.view.ChantLineViewController;
import com.tac550.tonewriter.view.MainApp;
import com.tac550.tonewriter.view.MainSceneController;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert.AlertType;
import org.apache.commons.text.TextStringBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

public class ToneReaderWriter {

	private final List<ChantLineViewController> chantLines;

	private String poetText;
	private String composerText;
	private String keySig;
	private final MainSceneController associatedMainScene;

	private MainSceneController mainScene;

	public ToneReaderWriter(List<ChantLineViewController> lines, MainSceneController main_scene,
	                        String key, String poet, String composer) {
		chantLines = lines;
		associatedMainScene = main_scene;
		keySig = key;
		poetText = poet;
		composerText = composer;
	}
	public ToneReaderWriter(List<ChantLineViewController> lines, MainSceneController main_scene) {
		chantLines = lines;
		associatedMainScene = main_scene;
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
					keySig.replace("\u266F", "s").replace("\u266D", "f"));
			printWriter.println("Tone: " + poetText);
			printWriter.println("Composer: " + composerText);
			printWriter.println("Manually Assign Phrases: " + associatedMainScene.manualCLAssignmentEnabled());
			printWriter.println();
			printWriter.println();

			// Line name which is marked first repeated. Filled when found.
			String firstRepeated = "";

			// For each chant line...
			for (ChantLineViewController chantLine : chantLines) {

				if (chantLine.getFirstRepeated()) {
					firstRepeated = chantLine.getName();
				}

				printWriter.println(chantLine.toString());

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

		String firstRepeated = "";
		float versionSaved;

		try {

			// Load entire tone file and split it as necessary
			TextStringBuilder fileStringBuilder = new TextStringBuilder();
			Files.lines(toneFile.toPath(), StandardCharsets.UTF_8).forEach(fileStringBuilder::appendln);
			// Triple newlines delimit sections
			String[] sections = fileStringBuilder.toString().split("\\r?\\n\\r?\\n\\r?\\n");
			String[] header;
			String[] chantLines;
			String[] footer;
			if (sections.length == 3) {
				header = sections[0].split("\\r?\\n");
				// Double newlines delimit chant lines
				chantLines = sections[1].split("\\r?\\n\\r?\\n");
				footer = sections[2].split("\\r?\\n");
			} else {
				header = sections[0].split("\\r?\\n");
				chantLines = null;
				footer = null;
			}

			versionSaved = Float.parseFloat(tryReadingLine(header, 0, "0"));
			keySig = tryReadingLine(header, 1, "C major")
					.replace("s", "\u266F").replace("f", "\u266D");
			if (versionSaved < 0.6) {
				composerText = tryReadingLine(header, 2, "");
			} else {
				poetText = tryReadingLine(header, 2, "");
				composerText = tryReadingLine(header, 3, "");
			}
			associatedMainScene.setManualCLAssignmentSilently(
					Boolean.parseBoolean(tryReadingLine(header, versionSaved < 0.6 ? 3 : 4, "false")));

			if (footer != null) {
				firstRepeated = tryReadingLine(footer, 0, "");
			}

			// Version warning
			if (versionSaved > Float.parseFloat(MainApp.APP_VERSION)) {

				TWUtils.showAlert(AlertType.INFORMATION, "Warning", String.format(Locale.US,
						"This tone was created with a newer version of %s. Be advised there may be issues.",
						MainApp.APP_NAME), true);

			} else if (versionSaved == 0) {

				TWUtils.showAlert(AlertType.ERROR, "Error", "Error loading tone file; it appears to be corrupted", true);

				return false;
			}
			
			// Loading chant lines
			main_scene.clearChantLines();

			if (chantLines != null) {
				for (String chantLine : chantLines) {
					readChantLine(chantLine);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
			main_scene.clearChantLines();
			return false;
		}

		main_scene.setKeySignature(keySig);
		if (versionSaved < 0.6) {
			String[] headerParts = composerText.split("-", 2);
			main_scene.setHeaderText(headerParts[0].trim(), headerParts.length > 1 ? headerParts[1].trim() : "");
		} else {
			main_scene.setHeaderText(poetText, composerText);
		}

		main_scene.recalcCLNames();
		main_scene.setFirstRepeated(firstRepeated);
		
		return true;
	}

	private String tryReadingLine(String[] section, int line_index, String default_value) {

		if (line_index < section.length && section[line_index].contains(":")) {
			String[] elements = section[line_index].split(":");

			// Always return the last element or empty string if there is nothing after the only ":" in the line.
			return elements[elements.length - 1].trim();
		}

		return default_value;
	}

	private void readChantLine(String chantLine) throws IOException {

		Task<FXMLLoader> currentChantLineLoader = mainScene.createChantLine(false);
		ChantLineViewController currentChantLine = null;
		try {
			currentChantLine = currentChantLineLoader.get().getController();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		assert currentChantLine != null;

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
				PostChord postChord = null;
				if (currentMainChord instanceof RecitingChord rMainChord)
					postChord = rMainChord.addPostChord(fields);
				assert postChord != null;
				postChord.setComment(comment);
			} else if (chantLineLine.contains("Prep")) {
				assert currentMainChord != null;
				PrepChord prepChord;
				if (currentMainChord instanceof EndChord eMainChord)
					prepChord = eMainChord.addPrepChord(fields);
				else
					prepChord = ((RecitingChord) currentMainChord).addPrepChord(fields);
				assert prepChord != null;
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
				// If the file already exists, delete it first (User already selected to overwrite)
				if (file_to_create.exists()) {
					if (!file_to_create.delete()) return false;
				}
				return file_to_create.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		} else return false;
	}
}
