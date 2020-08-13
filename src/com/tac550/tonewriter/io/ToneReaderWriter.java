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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
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
		chantLines = new ArrayList<>(lines);
		associatedMainScene = main_scene;
		keySig = key;
		poetText = poet;
		composerText = composer;
	}
	public ToneReaderWriter(List<ChantLineViewController> lines, MainSceneController main_scene) {
		chantLines = new ArrayList<>(lines);
		associatedMainScene = main_scene;
	}

	public boolean saveToneToFile(File toneFile) {
		// Clear out old save data.
		// noinspection ResultOfMethodCallIgnored
		toneFile.delete();

		try {
			// Create new file
			if (!toneFile.createNewFile())
				return false;

			// Set up PrintWriter
			try (FileWriter fileWriter = new FileWriter(toneFile);
			     PrintWriter printWriter = new PrintWriter(fileWriter)) {

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
			}

			// Save to the file
			FileWriter fileWriter = new FileWriter(toneFile);
			saveToneTo(fileWriter);

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private void saveToneTo(Writer destination) {
		try (PrintWriter printWriter = new PrintWriter(destination)) {

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

		}
	}

	public String getToneString() {
		try (StringWriter sw = new StringWriter()) {
			saveToneTo(sw);
			return sw.toString();
		} catch (IOException e) {
			return null;
		}
	}
	public String getCurrentToneHash() {

		StringBuilder hashBuilder = new StringBuilder();
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");

			// Record MD5 hash of the current tone data (what its file would contain if saved)
			byte[] hashBytes = md.digest(getToneString().getBytes());
			for (byte b : hashBytes)
				hashBuilder.append(String.format("%02x", b));

		} catch (NoSuchAlgorithmException e) {
			TWUtils.showError("Platform does not support MD5 algorithm!", true);
			return null;
		}

		return hashBuilder.toString();
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
			String[] chantLineStrings;
			String[] footer;
			if (sections.length == 3) {
				header = sections[0].split("\\r?\\n");
				// Double newlines delimit chant lines
				chantLineStrings = sections[1].split("\\r?\\n\\r?\\n");
				footer = sections[2].split("\\r?\\n");
			} else {
				header = sections[0].split("\\r?\\n");
				chantLineStrings = null;
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
						"This tone was created with a newer version of %s (%s). Be advised you may encounter problems.",
						MainApp.APP_NAME, versionSaved), true);

			} else if (versionSaved == 0) {

				TWUtils.showAlert(AlertType.ERROR, "Error", "Error loading tone file; it appears to be corrupted", true);

				return false;
			}

			// Loading chant lines
			if (chantLineStrings != null) {

				if (tonesSimilar(chantLineStrings)) { // Tones have same structure; don't reload any UI
					for (int i = 0; i < chantLineStrings.length; i++) {
						if (!chantLines.get(i).toString().replaceAll("\\s+", "")
								.equals(chantLineStrings[i].replaceAll("\\s+", ""))) {
							// If the lines are not identical, modify to match new values.
							modifyChantLine(chantLineStrings[i], chantLines.get(i));
						}
					}

				} else { // Tones don't have same structure; do a full reload.
					main_scene.clearChantLines();

					for (int i = 0; i < chantLineStrings.length; i++) {
						loadChantLine(i, chantLineStrings[i]);
					}
				}

			} else {
				// No chant lines found in file
				mainScene.clearChantLines();
			}

		} catch (IOException e) {
			e.printStackTrace();
			// Clear chant lines if there's an error
			mainScene.clearChantLines();
			return false;
		} catch (NumberFormatException e) {
			TWUtils.showError(String.format("This tone file was created in a newer version of %s and is not compatible.",
					MainApp.APP_NAME), true);
			return false;
		}

		mainScene.setKeySignature(keySig);
		if (versionSaved < 0.6) {
			String[] headerParts = composerText.split("-", 2);
			mainScene.setHeaderText(headerParts[0].trim(), headerParts.length > 1 ? headerParts[1].trim() : "");
		} else {
			mainScene.setHeaderText(poetText, composerText);
		}

		mainScene.recalcCLNames();
		mainScene.setFirstRepeated(firstRepeated);
		
		return true;
	}

	private boolean tonesSimilar(String[] chant_lines) {
		if (chant_lines.length != chantLines.size())
			return false;

		int i = 0;
		for (String line : chant_lines) {
			if (!chantLines.get(i).isSimilarTo(line))
				return false;

			i++;
		}

		return true;
	}
	public boolean loadedToneSimilarTo(File tone_file) throws IOException {
		String toneString = Files.readString(tone_file.toPath());

		String[] parts = toneString.split("\\r?\\n\\r?\\n\\r?\\n");

		return tonesSimilar(parts[1].split("\\r?\\n\\r?\\n"));
	}

	private String tryReadingLine(String[] section, int line_index, String default_value) {

		if (line_index < section.length && section[line_index].contains(":")) {
			String[] elements = section[line_index].split(":");

			// Always return the last element or empty string if there is nothing after the only ":" in the line.
			return elements[elements.length - 1].trim();
		}

		return default_value;
	}

	// TODO: Clean this and following method to reduce repetition
	private void loadChantLine(int index, String chant_line) throws IOException {

		try (Scanner chantLineScanner = new Scanner(chant_line)) {
			ChantLineViewController currentChantLine = null;
			String chantLineLine;

			Task<FXMLLoader> currentChantLineLoader = mainScene.createChantLine(index, false);
			try {
				currentChantLine = currentChantLineLoader.get().getController();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			assert currentChantLine != null;

			// Name / CL type parsing
			String chantLineName = chantLineScanner.nextLine();
			if (chantLineName.contains("'")) {
				currentChantLine.makePrime();
			} else if (chantLineName.contains("alt")) {
				currentChantLine.makeAlternate();
			}

			ChantChordController currentMainChord = null;

			while (chantLineScanner.hasNextLine() && (chantLineLine = chantLineScanner.nextLine()) != null) {
				// Apply chant line comment
				if (chantLineLine.startsWith("Comment: ")) {
					String[] commentData = chantLineLine.split(": ");
					currentChantLine.setComment(extractComment(commentData, 1));

					continue;
				}

				String[] chordData = chantLineLine.split(": ");
				String fields = chordData[1];
				String comment = extractComment(chordData, 2);

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
		}

	}

	private void modifyChantLine(String chant_line, ChantLineViewController existing_line) {
		Scanner chantLineScanner = new Scanner(chant_line);
		String chantLineLine;

		int assigning = 0;
		String[] mainChord = null;
		Stack<String[]> postChords = new Stack<>();

		chantLineScanner.nextLine(); // Skip over name line

		while (chantLineScanner.hasNextLine() && (chantLineLine = chantLineScanner.nextLine()) != null) {
			// Apply chant line comment
			if (chantLineLine.startsWith("Comment: ")) {
				String[] commentData = chantLineLine.split(": ");
				existing_line.setComment(extractComment(commentData, 1));

				continue;
			}

			String[] chordData = chantLineLine.split(": ");
			String fields = chordData[1];
			String comment = extractComment(chordData, 2);

			if (!chantLineLine.startsWith("\t")) {
				if (mainChord != null) {
					existing_line.getChords().get(assigning).setFields(mainChord[0]);
					existing_line.getChords().get(assigning).setComment(mainChord[1]);
					assigning++;
				}
				while (!postChords.isEmpty()) {
					String[] postChord = postChords.pop();
					existing_line.getChords().get(assigning).setFields(postChord[0]);
					existing_line.getChords().get(assigning).setComment(postChord[1]);
					assigning++;
				}
				mainChord = new String[]{fields, comment};
			} else if (chantLineLine.contains("Prep")) {
				existing_line.getChords().get(assigning).setFields(fields);
				existing_line.getChords().get(assigning).setComment(comment);
				assigning++;
			} else if (chant_line.contains("Post")) {
				if (mainChord != null) {
					existing_line.getChords().get(assigning).setFields(mainChord[0]);
					existing_line.getChords().get(assigning).setComment(mainChord[1]);
					assigning++;

					mainChord = null;
				}

				postChords.push(new String[]{fields, comment});
			}
		}
	}

	private String extractComment(String[] split_input, int comment_start_offset) {
		// Since the comment may include ": "s, we need to account for the possibility that it's been split and re-combine it.
		return String.join(": ", Arrays.copyOfRange(split_input, comment_start_offset, split_input.length));
	}

	public static boolean createToneFile(File file_to_create) {
		if (file_to_create.getParentFile().mkdirs() || file_to_create.getParentFile().exists()) {
			try {
				// If the file already exists, delete it first (overwrite)
				if (file_to_create.exists()) {
					if (!file_to_create.delete()) return false;
				}
				return file_to_create.createNewFile();
			} catch (IOException e) {
				TWUtils.showError("Failed to create .tone file!", false);
				return false;
			}
		} else {
			TWUtils.showError("Failed to create directory for .tone file!", false);
			return false;
		}
	}
}
