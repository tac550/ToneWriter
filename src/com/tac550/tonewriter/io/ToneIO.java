package com.tac550.tonewriter.io;

import com.tac550.tonewriter.model.ChantChord;
import com.tac550.tonewriter.model.ChantPhrase;
import com.tac550.tonewriter.model.Tone;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.MainApp;
import javafx.scene.control.Alert.AlertType;
import org.apache.commons.text.TextStringBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ToneIO {

	private static final int MAX_RECENT_TONES = 20;

	public static boolean saveToneToFile(Tone tone, File toneFile) {
		// Clear out old save data.
		// noinspection ResultOfMethodCallIgnored
		toneFile.delete();

		try {
			// Create new file
			if (!toneFile.createNewFile())
				return false;

			// Save to the file
			FileWriter fileWriter = new FileWriter(toneFile, StandardCharsets.UTF_8);
			saveToneTo(tone, fileWriter);

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private static void saveToneTo(Tone tone, Writer destination) {
		try (PrintWriter printWriter = new PrintWriter(destination)) {

			// Header info
			writePairTo(printWriter, "VERSION", MainApp.APP_VERSION);
			writePairTo(printWriter, "Key Signature", tone.getKeySignature().replace("\u266F", "s").replace("\u266D", "f"));
			writePairTo(printWriter, "Tone", tone.getToneText());
			writePairTo(printWriter, "Composer", tone.getComposerText());
			writePairTo(printWriter, "Manually Assign Phrases", tone.isManuallyAssignPhrases());
			writeBlankLine(printWriter);
			writeBlankLine(printWriter);

			for (ChantPhrase chantPhrase : tone.getChantPhrases())
				printWriter.println(chantPhrase);

			// Footer info
			writeBlankLine(printWriter);
			writePairTo(printWriter, "First Repeated", tone.getFirstRepeated());

		}
	}

	private static String getToneString(Tone tone) {
		try (StringWriter sw = new StringWriter()) {
			saveToneTo(tone, sw);
			return sw.toString();
		} catch (IOException e) {
			return null;
		}
	}
	public static String getToneHash(Tone tone) {

		StringBuilder hashBuilder = new StringBuilder();
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");

			// Record MD5 hash of the current tone data (what its file would contain if saved)
			byte[] hashBytes = md.digest(Objects.requireNonNull(getToneString(tone)).getBytes());
			for (byte b : hashBytes)
				hashBuilder.append(String.format("%02x", b));

		} catch (NoSuchAlgorithmException e) {
			TWUtils.showError("Platform does not support MD5 algorithm!", true);
			return null;
		}

		return hashBuilder.toString();
	}

	public static boolean tonesSimilar(Tone tone1, Tone tone2) { // TODO: Remove in favor of model handling this.
		if (tone1.getChantPhrases().size() != tone2.getChantPhrases().size())
			return false;

		int i = 0;
		for (ChantPhrase phrase : tone1.getChantPhrases()) {
			if (!tone2.getChantPhrases().get(i).isSimilarTo(phrase))
				return false;

			i++;
		}

		return true;
	}

	private static void writePairTo(PrintWriter writer, String label, Object value) {
		writer.println(String.format("%s: %s", label, String.valueOf(value).replace(":", "\\:")));
	}
	private static void writeBlankLine(PrintWriter writer) {
		writer.println();
	}

	private static String readFromSection(String[] section, int line_index, String default_value) {

		if (line_index < section.length && section[line_index].matches(".*[^\\\\]:.*")) { // Matches colon WITHOUT escape
			String[] elements = section[line_index].split("[^\\\\]:");

			// Always return the last element or empty string if there is nothing after the only ":" in the line.
			return elements[elements.length - 1].trim().replace("\\:", ":");
		}

		return default_value;
	}

	public static Tone loadTone(File tone_file) {
		Tone.ToneBuilder toneBuilder = new Tone.ToneBuilder();

		try {
			// Load entire tone file and split it as necessary
			TextStringBuilder fileStringBuilder = new TextStringBuilder();
			Files.lines(tone_file.toPath(), StandardCharsets.UTF_8).forEach(fileStringBuilder::appendln);
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

			// Determine tone version information
			String versionSaved = readFromSection(header, 0, "0");
			boolean pre0_6 = TWUtils.versionCompare(versionSaved, "0.6") == 2;
			boolean futureVersion = TWUtils.versionCompare(versionSaved, MainApp.APP_VERSION, 2) == 1;

			toneBuilder.keySignature(readFromSection(header, 1, "C major")
					.replace("s", "\u266F").replace("f", "\u266D"));
			if (pre0_6) {
				String headerText = readFromSection(header, 2, "");
				String[] headerParts = headerText.split("-", 2);
				toneBuilder.toneText(headerParts[0].trim());
				toneBuilder.composerText(headerParts.length > 1 ? headerParts[1].trim() : "");
			} else {
				toneBuilder.toneText(readFromSection(header, 2, ""));
				toneBuilder.composerText(readFromSection(header, 3, ""));
			}
			toneBuilder.manualAssignment(Boolean.parseBoolean(readFromSection(header, pre0_6 ? 3 : 4, "false")));

			if (footer != null)
				toneBuilder.firstRepeated(readFromSection(footer, 0, ""));

			// Version warning
			if (futureVersion) {
				TWUtils.showAlert(AlertType.INFORMATION, "Warning", String.format(Locale.US,
						"This tone was created with a newer version of %s (%s). Be advised you may encounter problems.",
						MainApp.APP_NAME, versionSaved), true);
			} else if (versionSaved.equals("0")) {
				TWUtils.showAlert(AlertType.ERROR, "Error", "Error loading tone file; it appears to be corrupted", true);
				return null;
			}

			List<ChantPhrase> phrases = new ArrayList<>();
			if (chantLineStrings != null) {
				for (String line : chantLineStrings)
					phrases.add(loadChantLine(line));
			}
			toneBuilder.chantPhrases(phrases);

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (NumberFormatException e) {
			TWUtils.showError(String.format("This tone file was created in a newer version of %s and is not compatible.",
					MainApp.APP_NAME), true);
			return null;
		}

		return toneBuilder.buildTone();
	}
	private static ChantPhrase loadChantLine(String chant_line) throws IOException {
		ChantPhrase.ChantPhraseBuilder phraseBuilder = new ChantPhrase.ChantPhraseBuilder();
		List<ChantChord> chords = new ArrayList<>();
		try (Scanner chantLineScanner = new Scanner(chant_line)) {
			String chantLineLine;

			phraseBuilder.name(chantLineScanner.nextLine());

			ChantChord currentMainChord = null;

			while (chantLineScanner.hasNextLine() && (chantLineLine = chantLineScanner.nextLine()) != null) {
				// Apply chant line comment
				if (chantLineLine.startsWith("Comment: ")) {
					String[] commentData = chantLineLine.split(": ");
					phraseBuilder.comment(extractComment(commentData, 1));

					continue;
				}

				String[] chordData = chantLineLine.split(": ");
				String fields = chordData[1];
				String[] parts = fields.split("-");
				String comment = extractComment(chordData, 2);

				ChantChord.ChantChordBuilder chordBuilder = new ChantChord.ChantChordBuilder();
				chordBuilder.name(chordData[0].trim().replace("END", "End"));
				chordBuilder.soprano(parts[0]);
				chordBuilder.alto(parts[1]);
				chordBuilder.tenor(parts[2]);
				chordBuilder.bass(parts[3]);
				chordBuilder.comment(comment);

				ChantChord currentChord = chordBuilder.buildChord();

				// Add the appropriate chord type.
				if (chordData[0].contains("Post")) {
					assert currentMainChord != null;
					currentMainChord.addPost(currentChord);
				} else if (chordData[0].contains("Prep")) {
					assert currentMainChord != null;
					currentMainChord.addPrep(currentChord);
				} else {
					currentMainChord = currentChord;
				}
				chords.add(currentChord);
			}
			phraseBuilder.chords(chords);
		}
		return phraseBuilder.buildChantPhrase();
	}

	private static String extractComment(String[] split_input, int comment_start_offset) {
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

	public static LinkedList<File> getRecentTones() {
		try {
			String recentsFilePath = getRecentsFilePath();
			try (Stream<String> fileStream = Files.lines(Paths.get(recentsFilePath))) {
				return fileStream.limit(MAX_RECENT_TONES).map(File::new).collect(Collectors.toCollection(LinkedList::new));
			}
		} catch (IOException e) {
			return new LinkedList<>();
		}
	}

	public static void bumpRecentTone(File tone_file) {
		LinkedList<File> recents = getRecentTones();
		recents.remove(tone_file);
		recents.addFirst(tone_file);
		writeRecentTones(recents);
	}
	public static void writeRecentTones(List<File> tone_files) {
		try {
			Path recentsPath = Paths.get(getRecentsFilePath());
			if (!Files.exists(recentsPath.getParent()))
				Files.createDirectories(recentsPath.getParent());
			Files.write(recentsPath,
					(Iterable<String>)tone_files.stream().limit(MAX_RECENT_TONES).map(File::getAbsolutePath)::iterator);
		} catch (IOException e) {
			System.out.println("Failed to write to recent tones list!");
			e.printStackTrace();
		}
	}

	private static String getRecentsFilePath() throws IOException {
		String appDataDir = MainApp.getPlatformSpecificAppDataDir();
		if (appDataDir == null) throw new IOException("Unknown platform.");
		return appDataDir + File.separator + "RecentTones.txt";
	}
}
