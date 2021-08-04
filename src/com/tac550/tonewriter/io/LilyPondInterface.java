package com.tac550.tonewriter.io;

import com.tac550.tonewriter.model.AssignedChordData;
import com.tac550.tonewriter.util.DesktopInterface;
import com.tac550.tonewriter.util.ProcessExitDetector;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.*;
import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LilyPondInterface {

	// Constants for how much to adjust notes for each part when applying relative octaves.
	// Each ' shifts the user's input up one octave and each , shifts the same down one octave.
	// Changing these will cause previously-saved tone data to be read with incorrect octaves.
	public static final String ADJUSTMENT_SOPRANO = "'";
	public static final String ADJUSTMENT_ALTO = "'";
	public static final String ADJUSTMENT_TENOR = "'";
	public static final String ADJUSTMENT_BASS = "";

	// LilyPond duration codes
	public static final String NOTE_EIGHTH = "8";
	public static final String NOTE_QUARTER = "4";
	public static final String NOTE_DOTTED_QUARTER = "4.";
	public static final String NOTE_HALF = "2";
	public static final String NOTE_DOTTED_HALF = "2.";
	public static final String NOTE_WHOLE = "1";

	// Constants to make some array indices easier to read.
	private static final int PART_SOPRANO = 0;
	private static final int PART_ALTO = 1;
	private static final int PART_TENOR = 2;
	private static final int PART_BASS = 3;

	private static final int measureBreakBeatThreshold = 8;

	// Fields for chord preview rendering system
	private static final Map<String, File[]> uniqueChordRenders = new HashMap<>();
	private static final Map<String, List<ChantChordController>> pendingChordControllers = new HashMap<>();

	private static File lastLilypondFile;
	private static Process lastExportProcess;
	private static boolean exportCancelled = false;

	// Renders chord previews for the tone UI
	public static void renderChord(ChantChordController chordView, String keySignature) throws IOException {
		final String chordID = chordView.getFields().replace("<", "(").replace(">", ")") + "-"
				+ keySignature.replace("\u266F", "s").replace("\u266D", "f ");
		if (!uniqueChordRenders.containsKey(chordID)) {
			// First time we're seeing this chord
			File lilypondFile = LilyPondInterface.createTempLYChordFile(chordID);

			uniqueChordRenders.put(chordID, null);

			String[] parts = chordView.getFields().split("-");

			List<String> lines = Files.readAllLines(lilypondFile.toPath(), StandardCharsets.UTF_8);

			lines.set(14, "  \\key " + keySignatureToLilyPond(keySignature));
			lines.set(20, parseNoteRelative(parts[PART_SOPRANO], ADJUSTMENT_SOPRANO));
			lines.set(25, parseNoteRelative(parts[PART_ALTO], ADJUSTMENT_ALTO));
			lines.set(30, parseNoteRelative(parts[PART_TENOR], ADJUSTMENT_TENOR));
			lines.set(35, parseNoteRelative(parts[PART_BASS], ADJUSTMENT_BASS));
			Files.write(lilypondFile.toPath(), lines, StandardCharsets.UTF_8);

			File outputFile = new File(lilypondFile.getAbsolutePath().replace(".ly", ".png"));
			File midiFile = new File(lilypondFile.getAbsolutePath().replace(".ly",
					MainApp.getPlatformSpecificMidiExtension()));

			File[] results = new File[] {outputFile, midiFile};
			pendingChordControllers.put(chordID, new ArrayList<>(Collections.singletonList(chordView)));
			executeLilyPondRender(lilypondFile, true, () -> {
				uniqueChordRenders.put(chordID, results);
				for (ChantChordController controller : pendingChordControllers.getOrDefault(chordID, new ArrayList<>()))
					controller.setMediaFilesDirectly(uniqueChordRenders.get(chordID));

				pendingChordControllers.remove(chordID);
			});
		} else if (uniqueChordRenders.get(chordID) == null) {
			// Render already started; automatically acquire the render when it's finished.
			pendingChordControllers.get(chordID).add(chordView);
		} else {
			// Chord already rendered; use existing files.
			chordView.setMediaFilesDirectly(uniqueChordRenders.get(chordID));
		}

	}

	// The function that handles final output.
	public static boolean exportItems(File saving_dir, String file_name, String project_title, MainSceneController[] items,
	                                  String paperSize, boolean no_header, boolean even_spread, String[] margin_info, TopSceneController top_scene) throws IOException {
		exportCancelled = false;
		lastLilypondFile = new File(saving_dir.getAbsolutePath() + File.separator + file_name + ".ly");

		if (!saveToLilyPondFile(lastLilypondFile, project_title, items, paperSize, no_header, even_spread, margin_info)) {
			top_scene.exportMenuFailure();
			return false;
		}

		if (MainApp.lilyPondAvailable()) {
			top_scene.exportMenuWorking();

			executeLilyPondRender(lastLilypondFile, false, () -> {
				if (exportCancelled) return; // If user cancelled before we got here, stop. // TODO: Potential bug where a LP temp file is left behind?
				if (lastExportProcess.exitValue() != 0) {
					Platform.runLater(top_scene::exportMenuFailure);
					return;
				}

				try {
					Platform.runLater(top_scene::exportMenuSuccess);
					// After the render is complete, ask the OS to open the resulting PDF file.
					if (top_scene.autoOpenCompletedExports())
						openLastExportPDF();

					// Delete the lilypond file if the option to save it isn't set
					if (!MainApp.prefs.getBoolean(MainApp.PREFS_SAVE_LILYPOND_FILE, false)) {
						if (!lastLilypondFile.delete()) {
							TWUtils.showError("Failed to delete LilyPond file, continuing...", false);
						}
					} // TODO: Consider making sure this has a chance to happen in event of cancellation or failure.

				} catch (Exception e) {
					Platform.runLater(top_scene::exportMenuFailure);
					// If the final rendered PDF can't be opened, open the folder instead (.ly file should be there even
					// if it's not set to be saved).
					if (top_scene.autoOpenCompletedExports())
						openLastExportFolder();
				}
			});
		} else {
			top_scene.exportMenuSuccess();
			if (top_scene.autoOpenCompletedExports())
				openLastExportFolder();
		}

		return true;
	}

	public static boolean saveToLilyPondFile(File lilypond_file, String project_title, MainSceneController[] items,
	                                         String paperSize, boolean no_header, boolean even_spread, String[] margin_info) throws IOException {

		// Create the LilyPond output file, and if it already exists, delete the old one.
		if (lilypond_file.exists()) {
			// Have to do this because MacOS doesn't like overwriting existing files
			if (!lilypond_file.delete()) {
				TWUtils.showError("Error deleting existing LilyPond file. Continuing anyway...", false);
			}
		}

		if (!lilypond_file.createNewFile()) {
			TWUtils.showError("Failed to create new LilyPond file", true);
			return false;
		}

		// Copy the render template file to the output path.
		try {
			TWUtils.exportFSResource(no_header && items.length > 1 ? "exportTemplateNoHeader.ly"
					: "exportTemplate.ly", lilypond_file);
		} catch (Exception e2) {
			e2.printStackTrace();
			return false;
		}

		// The buffer in which we'll store the output file as we build it.
		List<String> lines = new ArrayList<>(Files.readAllLines(lilypond_file.toPath(), StandardCharsets.UTF_8));

		// Replacing paper size, title, and tagline info.
		lines.set(2, "#(set-default-paper-size \"" + paperSize.split(" \\(")[0] + "\")");
		if (!no_header || items.length == 1) {
			lines.set(7, lines.get(7).replace("$PROJECT_TITLE",
					items.length == 1 ? (items[0].getLargeTitle() ? "\\fontsize #3 \"" : "\"")
							+ reformatTextForHeaders(items[0].getTitle()) + "\"" : "\"" + project_title + "\""));
			lines.set(9, lines.get(9).replace("$VERSION", MainApp.APP_VERSION)
					.replace("$APPNAME", MainApp.APP_NAME));
			if (items.length == 1 && items[0].getLargeTitle()) {
				lines.set(14, lines.get(14).replace("\\fromproperty #'header:instrument", "\\fontsize #-3 \\fromproperty #'header:instrument"));
				lines.set(15, lines.get(15).replace("\\fromproperty #'header:instrument", "\\fontsize #-3 \\fromproperty #'header:instrument"));
			}

			if (!even_spread) {
				lines.set(14, lines.get(14).replace("  evenHeaderMarkup =", "  oddHeaderMarkup ="));
				lines.set(15, lines.get(15).replace("  oddHeaderMarkup =", "  evenHeaderMarkup ="));
			}

			lines.set(17, "  top-margin = %s\\%s".formatted(margin_info[0], margin_info[1]));
			lines.set(18, "  bottom-margin = %s\\%s".formatted(margin_info[2], margin_info[3]));
			lines.set(19, "  left-margin = %s\\%s".formatted(margin_info[4], margin_info[5]));
			lines.set(20, "  right-margin = %s\\%s".formatted(margin_info[6], margin_info[7]));
		} else {
			lines.set(12, "  top-margin = %s\\%s".formatted(margin_info[0], margin_info[1]));
			lines.set(13, "  bottom-margin = %s\\%s".formatted(margin_info[2], margin_info[3]));
			lines.set(14, "  left-margin = %s\\%s".formatted(margin_info[4], margin_info[5]));
			lines.set(15, "  right-margin = %s\\%s".formatted(margin_info[6], margin_info[7]));
		}

		// Add a blank line before scores begin
		lines.add("");

		int index = 0;
		for (MainSceneController item : items) {

			// Bypass caching item source if single-item export (may differ from multi-item export)
			if (items.length == 1)
				lines.add(generateItemSource(item, MainApp.prefs.getBoolean(MainApp.PREFS_SAVE_MIDI_FILE, false)));
			else
				lines.add(item.getLilyPondSource());

			// Remove page break at beginning of item listing, if present.
			if (index == 0)
				lines.set(lines.size() - 1, lines.get(lines.size() - 1).replaceFirst("\n\\\\pageBreak\n", ""));

			index++;
		}

		// Remove extra newline at end of file (result is one blank line)
		lines.set(lines.size() - 1, lines.get(lines.size() - 1).replaceAll("\n$", ""));

		// Write the file back out.
		Files.write(lilypond_file.toPath(), lines, StandardCharsets.UTF_8);

		return true;
	}

	public static String generateItemSource(MainSceneController item, boolean generate_midi) {
		int midiTempo = 150;
		if (generate_midi && item.hasAssignments()) { // Ask user to indicate preferred MIDI tempo
			boolean done = false;
			while (!done) {
				TextInputDialog dialog = new TextInputDialog(String.valueOf(midiTempo));
				dialog.setTitle("MIDI Tempo");
				dialog.setHeaderText("Enter MIDI tempo (quarter-note beats per minute)");
				dialog.initOwner(item.getParentStage());
				Optional<String> result = dialog.showAndWait();
				if (result.isPresent()) {
					try {
						midiTempo = Integer.parseInt(result.get());
						done = true;
					} catch (NumberFormatException e) {
						TWUtils.showError("Invalid input.", true);
					}
				} else done = true;
			}
		}

		List<String> lines = new ArrayList<>();

		// Comment for purposes of future parsing of output (for internal project file renders)
		lines.add("% " + item.getTitle() + "\n");

		// Page break if requested, but only if this item is not the first.
		if (item.getPageBreak())
			lines.add("\\pageBreak\n");

		// Perform layout procedure
		String[] results = generateNotationAndLyrics(item.getVerseLineControllers());

		// Pattern which matches valid LilyPond notes
		Pattern noteDataPattern = Pattern.compile("[a-g][\\S]*[0-9]");

		// Check all four parts for any note data - if any exists, we need to include a staff or staves.
		boolean createStaff = Stream.of(results).limit(4).anyMatch(part -> noteDataPattern.matcher(part).find());

		// Manual title markup goes here, if any.
		// This allows displaying title and subtitle before top text.
		if (!item.getFinalTitleContent().isEmpty() || !item.getSubtitle().isEmpty()) {
			Collections.addAll(lines, "\\markup \\column {");

			// Title, if not hidden...
			if (!item.getFinalTitleContent().isEmpty())
				Collections.addAll(lines, "  \\fill-line \\bold %s{\\justify { %s } }".formatted(item.getLargeTitle() ?
						"\\fontsize #3 " : "\\fontsize #1 ", reformatTextForNotation(item.getFinalTitleContent())));
			// ...and subtitle, if present
			if (!item.getSubtitle().isEmpty())
				Collections.addAll(lines, "  \\fill-line %s{\\justify { %s } } \\vspace #0.5".formatted(
						"\\fontsize #0.5 ", reformatTextForNotation(item.getSubtitle())));

			Collections.addAll(lines, "  \\vspace #0.25", "}\n", "\\noPageBreak\n");
		}

		// Top verse, if any
		if (item.getExtendTextSelection() == 1) {
			lines.add(reformatTextForNotation(generateExtendedText(item.getTopVerseChoice(),
					item.getVerseAreaText(), item.getBreakOnlyOnBlank())) + (createStaff ? "\\noPageBreak\n" : ""));
		} else if (!item.getTopVerse().isEmpty()) {
			Collections.addAll(lines, "\\markup \\column {",
					String.format("  \\vspace #0.5 \\justify { \\halign #-1 \\bold {%s} %s \\vspace #0.5",
							item.getTopVerseChoice(), reformatTextForNotation(item.getTopVerse()) + " } "),
					"}\n",
					createStaff ? "\\noPageBreak\n" : "");
		}

		if (createStaff) {
			// Score header
			Collections.addAll(lines, "\\score {\n", "  \\header {",
					String.format("    piece = \"%s\"", reformatTextForHeaders(item.getLeftHeaderText())),
					String.format("    opus = \"%s\"", reformatTextForHeaders(item.getRightHeaderText())),
					"    instrument = \"\"",
					"  }\n");

			Collections.addAll(lines, "  \\new ChoirStaff <<", "    \\new Staff \\with {",
					"      \\once \\override Staff.TimeSignature #'stencil = ##f % Hides the time signatures in the upper staves");
			if (generate_midi) lines.add("      midiInstrument = #\"choir aahs\"");
			Collections.addAll(lines, "    } <<", "      \\key " + keySignatureToLilyPond(item.getKeySignature()),
					"      \\new Voice = \"soprano\" { \\voiceOne {" + results[PART_SOPRANO] + " } }",
					"      \\new Voice = \"alto\" { \\voiceTwo {" + results[PART_ALTO] + " } }",
					"    >>", "    \\new Lyrics \\with {", "      \\override VerticalAxisGroup #'staff-affinity = #CENTER",
					"    } \\lyricsto \"soprano\" { \\lyricmode {" + results[4] + " } }\n");

			// If the tenor and bass parts are not empty, include a lower staff.
			if (noteDataPattern.matcher(results[PART_TENOR]).find() || noteDataPattern.matcher(results[PART_BASS]).find())
				Collections.addAll(lines, "    \\new Staff \\with {",
					"      \\once \\override Staff.TimeSignature #'stencil = ##f % Hides the time signatures in the lower staves");
			if (generate_midi) lines.add("      midiInstrument = #\"choir aahs\"");
			Collections.addAll(lines, "    } <<", "      \\clef bass",
					"      \\key " + keySignatureToLilyPond(item.getKeySignature()),
					"      \\new Voice = \"tenor\" { \\voiceOne {" + results[PART_TENOR] + " } }",
					"      \\new Voice = \"bass\" { \\voiceTwo {" + results[PART_BASS] + " } }",
					"    >>");

			Collections.addAll(lines, "  >>\n", "  \\layout {", "    \\context {", "      \\Score",
					"      defaultBarType = \"|\" % Split barlines delimit phrases",
					"      \\remove \"Bar_number_engraver\" % removes the bar numbers at the start of each system",
					"      \\accidentalStyle neo-modern-voice-cautionary",
					"    }", "  }");
			if (generate_midi) lines.add("  \\midi {\n    \\tempo 4 = %d\n  }".formatted(midiTempo));
			lines.add("}\n");

		} else {
			lines.add("\\markup \\column { \\vspace #0.5 }\n");
		}

		// Bottom verse, if any
		if (item.getExtendTextSelection() == 2) {
			lines.add((createStaff ? "\\noPageBreak\n" : "") + reformatTextForNotation(generateExtendedText(item.getBottomVerseChoice(),
					item.getVerseAreaText(), item.getBreakOnlyOnBlank())));
		} else if (!item.getBottomVerse().isEmpty()) {
			Collections.addAll(lines, createStaff ? "\\noPageBreak\n" : "",
					"\\markup \\column {",
					String.format("  \\justify { \\halign #-1 \\bold {%s} %s \\vspace #1",
							item.getBottomVerseChoice(), reformatTextForNotation(item.getBottomVerse()) + " } "),
					"}\n");
		}

		return String.join("\n", lines);
	}

	private static String generateExtendedText(String verse_choice, String extended_text, boolean break_only_on_blank) {
		StringBuilder resultText = new StringBuilder();

		String[] lines = extended_text.split("\n");

		resultText.append(String.format("\\markup \\column { \\vspace #1 \\justify { \\halign #-1 \\bold {%s} %s } \\vspace #0.125 }%s\n",
				verse_choice, lines[0], break_only_on_blank && lines.length > 1 && !lines[1].isBlank() ? " \\noPageBreak" : ""));

		int blankLineCounter = 0;
		for (int i = 1; i < lines.length; i++) {
			if (lines[i].isBlank()) {
				blankLineCounter++;
			} else {
				resultText.append(String.format("\\markup \\column {%s \\justify { %s } \\vspace #0.125 }%s\n",
						blankLineCounter > 0 ? " \\vspace #" + blankLineCounter : "", lines[i],
						break_only_on_blank && lines.length > i+1 && !lines[i+1].isBlank() ? " \\noPageBreak" : ""));
				blankLineCounter = 0;
			}
		}

		return resultText.toString();
	}

	private static String[] generateNotationAndLyrics(List<VerseLineViewController> verse_lines) {
		// Part buffers for the item  { S  A    T  B }
		String[] parts = new String[] {"", "", "", ""};
		// Buffer for the piece's text.
		StringBuilder verseText = new StringBuilder();
		// Add initial bar line, if there are any vlines.
		if (verse_lines.size() > 0)
			verseText.append(String.format("\\bar \"%s\"", verse_lines.get(0).getBeforeBar()));

		for (VerseLineViewController verseLineController : verse_lines)
			generateLine(parts, verseText, verseLineController);

		// Insert invisible barlines where indicated, after reversing possible incorrect orderings of bars/slur starts
		parts[PART_SOPRANO] = parts[PART_SOPRANO].replace("$bar  \\(", "\\( $bar")
				.replace("$bar", "\\bar \"\"");

		return new String[] {parts[0], parts[1], parts[2], parts[3], verseText.toString()};
	}

	private static void generateLine(String[] parts, StringBuilder verseText, VerseLineViewController verseLineController) {
		// Do nothing if this is a verse separator line.
		if (!verseLineController.isSeparator()) {
			StringBuilder verseLine = new StringBuilder();
			// Number of beats in the line. This determines where the visible barline goes.
			float measureBeats = generateNotatedLine(parts, List.of(verseLineController.getSyllables()), verseLine,
					verseLineController.getDisableLineBreaks());
			// Add barline style indicator to the soprano part.
			parts[PART_SOPRANO] += String.format(" \\bar \"%s\"", verseLineController.getAfterBar());

			// Insert time signature and the line's text into the lyrics buffer.
			verseText.append(String.format(" %s %s", generateTimeSignature(measureBeats), verseLine));
		}
	}

	private static float generateNotatedLine(String[] parts, List<SyllableText> syllableList, StringBuilder verseLine,
	                                         boolean disableLineBreaks) {
		float measureBeats = 0;
		float breakCount = 0;

		// For each syllable in the line...
		for (SyllableText syllable : syllableList) {
			// Note buffers for this syllable.          { S  A    T  B }
			String[] syllableNoteBuffers = new String[] {"", "", "", ""};
			// Buffer for the syllable's text.
			StringBuilder syllableTextBuffer = new StringBuilder();

			// Whether or not notes were combined for each part in the previous chord.
			// Note that notes being combined means either being made into one note with a
			// duration which is the sum of the two, or that the notes were tied (because there can be no single note with such a duration)
			boolean[] noteCombined = new boolean[] {false, false, false, false};
			// True if the corresponding part needed a combination 2 chords ago.
			boolean[] previousNoteCombined = new boolean[] {false, false, false, false};
			// Notes for each part to consider "current" when testing whether to combine.
			// Gets set if a note was combined on the last chord.
			String[] tempCurrentNotes = new String[] {"", "", "", ""};

			List<AssignedChordData> chordList = Arrays.asList(syllable.getAssociatedChords());
			// For each chord assigned to the syllable...

			if (!chordList.isEmpty())
				addSyllaleToLyrics(syllableList, syllable, syllableTextBuffer);

			for (AssignedChordData chordData : chordList) {

				// Add an additional chord indicator for the syllable, unless the soprano part was combined two chords ago,
				// or it contains a rest. We only check the soprano part because the text is mapped to it.
				if (chordList.indexOf(chordData) != 0 && !previousNoteCombined[PART_SOPRANO]
						&& !chordData.getPart(PART_SOPRANO).contains("r")) {
					// For chords subsequent to the first for each syllable, we add this to the lyric line
					// to tell Lilypond this syllable has an additional chord attached to it.
					syllableTextBuffer.append(" _ ");
				}

				// CHORD DATA PROCESSING

				// Flag to keep track of whether this chord will be hidden. Chord hiding cleans up repeated quarter
				// notes for extended periods of recitative. We assume the chord will be hidden by default, but there
				// will be many opportunities for the chord to remain visible based upon what's going on around it.
				// We must check the notes in each part to see if the chord shouldn't be hidden.
				boolean hideThisChord = true;

				boolean lastChordInLine = false;
				boolean currentNoteIsEighth = false;

				// For each part...
				for (int i = 0; i < 4; i++) {
					// We need to get a previous note, the current note, and the next note in order to do this processing.
					String previousNote = "";
					String currentNote;
					String nextNote = "";

					// PREVIOUS NOTE

					// If this is the first chord associated with this syllable...
					if (chordList.indexOf(chordData) == 0) {
						// And if this is the first syllable in the whole text...
						if (syllableList.indexOf(syllable) == 0) {
							// Then we know we need to show the chord. It'll be the first chord on the page!
							hideThisChord = false;
						} else {
							AssignedChordData[] previousSyllableChords = syllableList.get(syllableList.indexOf(syllable) - 1).getAssociatedChords();
							if (previousSyllableChords.length == 0)
								// If the previous syllable has no chords assigned, then we know we shouldn't be hiding this one.
								hideThisChord = false;
							else
								// Otherwise, the previous note is the note from the last chord from the previous syllable.
								previousNote = previousSyllableChords[previousSyllableChords.length - 1].getPart(i);
						}
					} else {
						// The previous note is the note just before this one on this same syllable.
						previousNote = chordList.get(chordList.indexOf(chordData) - 1).getPart(i);
					}

					// CURRENT NOTE

					// If there is an alternate current note... (the previous one was combined)
					if (!tempCurrentNotes[i].isEmpty())
						// Then the current note will be the temporarily designated one.
						currentNote = tempCurrentNotes[i];
					else
						currentNote = chordData.getPart(i);

					currentNoteIsEighth = currentNote.contains("8");

					// NEXT NOTE

					// If this is the last chord associated with this syllable...
					if (chordList.indexOf(chordData) == chordList.size() - 1) {
						// And if this isn't the last syllable with chords on it...
						if (syllableList.indexOf(syllable) < syllableList.size() - 1
								&& syllableList.get(syllableList.indexOf(syllable) + 1).getAssociatedChords().length > 0) {
							// Then the next note is the note from the first chord of the next syllable.
							nextNote = syllableList.get(syllableList.indexOf(syllable) + 1).getAssociatedChords()[0].getPart(i);
						} else {
							// This is the last chord associated with the last syllable with chords on it,
							// so we definitely do not want to hide it (it's the last chord on the page!)
							hideThisChord = false;
							lastChordInLine = true;
						}
						// If there's another chord associated with this syllable...
					} else {
						// Then the next note is the note from the next chord on this same syllable.
						nextNote = chordList.get(chordList.indexOf(chordData) + 1).getPart(i);

						// Since we have determined that there is at least one more chord on this syllable,
						// we have to decide whether the current note should be combined with the next note.

						// If the current and next notes both have the same pitch
						// AND, if we're on a subdivision, the previous note was also combined...
						if (currentNote.replaceAll("[^A-Za-z',]+", "").equals(nextNote.replaceAll("[^A-Za-z',]+", ""))
								&& (((int) measureBeats == measureBeats) || previousNoteCombined[i])) {

							// Try to do the combination
							String addedNotes = combineNotes(currentNote, nextNote);
							if (!addedNotes.isEmpty()) { // Complete note combining only if combining notes didn't fail
								// If the previous note was also combined, remove it (and any tokens after it).
								if (!tempCurrentNotes[i].isEmpty())
									syllableNoteBuffers[i] = removeLastNote(syllableNoteBuffers[i]);

								// Add the combined note(s) to the buffer.
								syllableNoteBuffers[i] += " " + addedNotes;
								// Add duration of this/these note(s) to the beat total but only if we're on the soprano part (we only need to count beats for one part).
								// Subtract duration of previous note if it was also combined, so it isn't counted twice.
								if (i == 0) measureBeats += getBeatDuration(addedNotes)
										- (previousNoteCombined[i] ? getBeatDuration(tempCurrentNotes[i]) : 0);

								// If the notes were combined into one... (not tied)
								if (!addedNotes.contains("~"))
									// The new note becomes the temporary current note for the current part.
									tempCurrentNotes[i] = addedNotes;
								else
									// If the combination resulted in a tie, the temporary current note is the second of the two tied notes.
									tempCurrentNotes[i] = addedNotes.split(" ")[1];

								// Remember that we just did a note combination for the current part.
								noteCombined[i] = true;
							}
						}
					}

					// This is just protection against some kind of error resulting in empty notes. Just don't hide the chord in this case.
					if (previousNote.equals("") || currentNote.equals("") || nextNote.equals(""))
						hideThisChord = false;

					// If the previous, current, and next notes are not all quarters and/or not all the same pitch...
					if (!previousNote.equals(currentNote) || !currentNote.equals(nextNote) || !currentNote.contains("4"))
						// Don't hide the current chord because neighboring chords are different.
						hideThisChord = false;

				}

				// If hideThisChord remained true after all the checks for all the parts in the chord...
				if (hideThisChord)
					for (int i = 0; i < 4; i++)
						// Add the flag that hides the note for each part of the chord before adding the notes themselves.
						syllableNoteBuffers[i] += " \\noteHide";

				// Add note data for each part to the buffers...
				for (int i = 0; i < 4; i++) {
					// ...but only if the note was not combined... (in which case it will already have been added)
					if (!noteCombined[i]) {
						// ...and the one before that was not combined.
						if (!previousNoteCombined[i]) {
							syllableNoteBuffers[i] += " " + chordData.getPart(i);
							// Add duration of this note to the beat total only if we're on the soprano part (we only need to count beats for one part).
							if (i == 0) measureBeats += getBeatDuration(chordData.getPart(i));
						} else {
							// If the previous note was combined, we clear the temp field for the current part and reset the flag.
							previousNoteCombined[i] = false;
							tempCurrentNotes[i] = "";
						}
					} else {
						// If the note was combined, set up flags for the next chord.
						noteCombined[i] = false;
						previousNoteCombined[i] = true;
					}

				}

				// Decide whether to place an invisible barline after this chord (allows for line breaking here).
				// Individual lines can disable this. Also, don't  try subdividing if this is the last chord in the
				// phrase, we haven't reached the beat threshold for adding an optional break, the next syllable is th
				// last and has only one chord, or the note which would precede the possible break point is an eighth note.
				if (!disableLineBreaks && !lastChordInLine && measureBeats > measureBreakBeatThreshold
						* breakCount + measureBreakBeatThreshold && (syllableList.indexOf(syllable) != syllableList.size() - 2
						|| syllableList.get(syllableList.size() - 1).getAssociatedChords().length != 1) && !currentNoteIsEighth)
					breakCount += trySubdividing(syllableNoteBuffers, syllableTextBuffer);

			}
			// That's it for each chord in the syllable.

			// SLUR AND BEAM PROCESSING

			// For each part...
			for (int i = 0; i < 4; i++)
				syllableNoteBuffers[i] = applySlursAndBeams(syllableNoteBuffers[i]);

			// SYLLABLE BUFFER SAVE-OUTS

			// Add the text buffer for the syllable to that of the line.
			verseLine.append(syllableTextBuffer);

			// Add the note data for the syllable from the buffers to the final part strings.
			for (int i = 0; i < 4; i++)
				parts[i] += syllableNoteBuffers[i];

		}
		return measureBeats;
	}

	private static String removeLastNote(String syllableNoteBuffer) {
		String[] tokens = syllableNoteBuffer.split(" ");
		// This flag gets set if the previous token removed was in a note group.
		boolean noteGroup = false;
		// Work backward through the tokens.
		for (int i1 = tokens.length - 1; i1 >= 0; i1--) {
			if (tokens[i1].contains("a") || tokens[i1].contains("b") || tokens[i1].contains("c") ||
					tokens[i1].contains("d") || tokens[i1].contains("e") || tokens[i1].contains("f") ||
					tokens[i1].contains("g") || tokens[i1].contains("r")) { // TODO: Might want to rethink this
				if (noteGroup) {
					// If we hit the beginning of the note group (last token to remove)...
					if (tokens[i1].contains("<")) {
						// remove it and we're done.
						tokens[i1] = "";
						break;
					}
				} else if (tokens[i1].contains(">")) { // If the note we're trying to remove is part of a note group...
					// Set the flag.
					noteGroup = true;
				}

				tokens[i1] = "";

				if (!noteGroup)
					// Stop here because we just removed the previous note.
					break;

			} else {
				// Remove tokens that aren't notes from the end.
				tokens[i1] = "";
			}
		}

		// Remove any excess spaces as we reconstruct the note buffer.
		StringBuilder editedBuffer = new StringBuilder();
		for (String token : tokens) {
			if (token.isEmpty()) continue;
			editedBuffer.append(" ").append(token);
		}
		// Save back to the buffer.
		return editedBuffer.toString();
	}

	private static String applySlursAndBeams(String syllableNoteBuffer) {
		boolean addedSlur = false;

		String[] tokens = syllableNoteBuffer.trim().split(" ");

		// If this part has any hidden notes, or all notes are tied, or the part contains any rests, skip adding a slur.
		if (syllableNoteBuffer.contains("noteHide")
				|| Arrays.stream(tokens).limit(tokens.length - 1).allMatch(val -> val.contains("~"))
				|| Arrays.stream(tokens).anyMatch(val -> Pattern.matches("r\\S", val)))
			return syllableNoteBuffer;

		String startSymbol = "\\(";
		String endSymbol = "\\)";

		// If the syllable contains nothing but two eighth chords, apply beam instead of slur
		// (prevents auto-connecting beam to notes from previous syllable).
		int eighthNotes = (int) Arrays.stream(tokens).filter(t ->
				t.contains("8") || !t.matches(".*\\d.*")).count();
		if (eighthNotes == tokens.length) {
			startSymbol = "[";
			endSymbol = "]";
		}

		// Reconstruct the syllable note buffer, adding the beginning slur parenthesis after the first note (as LilyPond syntax dictates).
		StringBuilder finalString = new StringBuilder();
		// For each token in the buffer...
		for (int i1 = 0; i1 < tokens.length; i1++) {
			// If the token is a barline indicator, append it and the following as if one token
			if (tokens[i1].equals("$bar")) {
				finalString.append(" ").append(tokens[i1])
						.append(" ").append(i1 + 1 < tokens.length ? tokens[i1 + 1] : "");
				i1++; // Don't try to append i1 + 1 again
				continue;
			}
			// If it's not the first token, we haven't added the slur yet, and the previous token was not the beginning of a
			// note group... (two notes occurring in one part, which will be split across two tokens)
			if (i1 > 0 && !addedSlur && !tokens[i1 - 1].contains("<")) {
				// Add the beginning slur parenthesis and the current token.
				finalString.append(" %s ".formatted(startSymbol)).append(tokens[i1]);
				addedSlur = true;
			} else {
				// Otherwise skip empty tokens or add the current one.
				if (tokens[i1].isEmpty()) continue;
				finalString.append(" ").append(tokens[i1]);
			}
		}

		// Save the new string to the note buffer.
		String result = finalString.toString();

		// Close parentheses to complete the slur for the syllable, if there is one.
		if (addedSlur) {
			if (result.endsWith("$bar "))
				result = TWUtils.replaceLast(result,
						Matcher.quoteReplacement("$bar "), Matcher.quoteReplacement("%s $bar".formatted(endSymbol)));
			else
				result += ("%s".formatted(endSymbol));
		}

		return result;
	}

	private static void addSyllaleToLyrics(List<SyllableText> syllableList, SyllableText syllable, StringBuilder syllableTextBuffer) {
		// Add any formatting flags for the syllable first.
		syllableTextBuffer.append(syllable.getBold() ? " \\lyricBold " : "")
				.append(syllable.getItalic() ? " \\lyricItalic " : "");

		// Add syllable to the text buffer, throwing away any (presumably leading) hyphens beforehand.
		syllableTextBuffer.append(reformatTextForNotation(syllable.getFormattedText().replace("-", "")));

		// If this is not the last syllable in the text,
		if (syllableList.indexOf(syllable) < syllableList.size() - 1)
			// If the next syllable starts with a hyphen, it is part of the same word, so we need to add these dashes immediately after the current syllable.
			// This ensures that syllables belonging to one word split across a distance are engraved correctly by LilyPond.
			if (syllableList.get(syllableList.indexOf(syllable) + 1).getFormattedText().startsWith("-"))
				syllableTextBuffer.append(" -- ");

		// Add closing formatting flag if necessary.
		if (syllable.getBold() || syllable.getItalic())
			syllableTextBuffer.append(" \\lyricRevert ");
	}

	private static float trySubdividing(String[] partBuffers, StringBuilder syllableTextBuffer) {
		if (!syllableTextBuffer.toString().endsWith(" -- ")) {
			partBuffers[PART_SOPRANO] += " $bar ";

			return 1;
		} else return 0;

	}

	// Generates LilyPond time signature notation for a x/4 time signature with given total duration.
	private static String generateTimeSignature(float measure_duration) {
		// Ceil the beat total because we never want time signatures with fractional parts.
		// Using ceil here instead of floor because LilyPond won't change the time signature until the current bar is completed.
		// If a bar ends before we want it to we may be stuck without a bar reset (for accidentals and such) for a very long time
		// depending on the length of the previous measure. It seems better to overshoot a little so any accidental engraving errors may be
		// kept to a minimum and hopefully occur only at the start of a measure.
		int roundedBeats = (int) Math.ceil(measure_duration);
		// We never want a 0/4 time signature (hangs Lilypond)
		int finalBeats = roundedBeats == 0 ? 1 : roundedBeats;
		// Add time signature information before each verse line.

		return String.format(Locale.US, "\\time %d/4", finalBeats);
	}

	// Takes two notes in LilyPond syntax (which ought to have the same pitch) and returns their combination as if next to each other on a single syllable.
	// Returns either a single note whose duration is the sum of the two given notes' durations with their original pitch, or returns the two notes combined with a tie.
	private static String combineNotes(String curr, String next) {
		// This string replaces duration information from the current note with %s so we can "format in" its new duration (if the duration combination method is chosen later).
		String noteFormat = curr.replaceAll("[0-9]+", "%s").replace(".", "");
		// The duration of the current note.
		float durCurrent = Float.parseFloat(curr.replaceAll("[\\D]", ""));
		// The duration of the next note.
		float durNext = Float.parseFloat(next.replaceAll("[\\D]", ""));

		// Inverse durations (so they read as fractions of a whole note in x/4 time).
		durCurrent = 1 / durCurrent;
		durNext = 1 / durNext;

		// Increase the duration by half if the note is dotted.
		if (curr.contains(".")) durCurrent += (durCurrent / 2);
		if (next.contains(".")) durNext += (durNext / 2);

		// Add the note durations and inverse again to return to LilyPond's duration format.
		float computedDur = 1 / (durCurrent + durNext);

		String newDur;

		// If the note combination process yielded a whole number...
		if (computedDur % 1 == 0) {
			// We just take it as the new duration and continue to the return statement.
			newDur = String.valueOf((int) computedDur);
		} else { // If a fractional number resulted it may be covered by a special case.
			// Invert the computed duration.
			float inverse = 1 / computedDur;

			if (inverse == 0.75) // Dotted half
				newDur = "2.";
			else if (inverse == 0.375) // Dotted quarter
				newDur = "4.";
			else // If no definition exists, we simply tie the notes, unless they are rests. In this case, return "".
				return noteFormat.contains("r") ? "" : curr + "~ " + next;
		}

		// If we got this far, the notes will be combined into one. We just format the new duration string into the note and return it.
		return String.format(Locale.US, noteFormat, newDur);
	}

	// Returns the number of beats for which the given note (or notes, if tied) last(s).
	private static float getBeatDuration(String note) {
		// If there is a tie...
		if (note.contains("~")) {
			String[] tiedNotes = note.split("~ ");

			float totalDuration = 0;

			for (String tNote : tiedNotes)
				// Make one recursive call for each tied note
				totalDuration += getBeatDuration(tNote);

			return totalDuration;

		} else {
			// Calculate the duration of the note.
			float beats = 4 / Float.parseFloat(note.replaceAll("[\\D]", ""));

			if (note.contains(".")) beats += (beats / 2);

			return beats;
		}
	}

	// Returns reformatted version of input such that double quotes display correctly and
	// straight apostrophes are replaced with curly ones in LilyPond output.
	private static String reformatTextForNotation(String input) {
		StringBuilder outputBuffer = new StringBuilder();

		// Delimiters are included to enable rebuilding the entire string with whitespace
		StringTokenizer tokenizer = new StringTokenizer(
				TWUtils.applySmartQuotes(input), " \t\n\r\f", true);

		while (tokenizer.hasMoreTokens()) {
			String current_token = tokenizer.nextToken();

			if (current_token.contains("\"")) {

				StringBuilder working_token = new StringBuilder(current_token);

				// Insert the escape character before each occurrence of a double quote in the syllable.
				for (int index = working_token.indexOf("\""); index >= 0; index = working_token.indexOf("\"", index + 2)) {
					working_token.insert(index, "\\");
				}

				// Surround the syllable without a leading space with quotes.
				outputBuffer.append("\"").append(working_token).append("\"");
			} else {
				outputBuffer.append(current_token);
			}
		}

		return outputBuffer.toString().replace("'", "\u2019");
	}
	private static String reformatTextForHeaders(String input) {
		return TWUtils.applySmartQuotes(input)
				.replace("\"", "\\\"").replace("'", "\u2019");
	}

	// Adjusts the octave of the given note or note group according to the octave_data string.
	// octave_data should be formatted "''", "'", ",", "", etc.
	// Each ' shifts the user's input up one octave and each , shifts down one octave.
	public static String parseNoteRelative(String note_data, String octave_data) {
		int adjustment = octaveToNumeric(octave_data);
		String[] notes = note_data.split(" ");
		boolean multi = notes.length > 1;
		return Arrays.stream(notes)
				.map(s -> s.replaceAll("[<>',]", "")
						+ numericToOctave(octaveToNumeric(s) + adjustment))
				.collect(Collectors.joining(" ", multi ? "<" : "", multi ? ">" : ""));
	}
	private static int octaveToNumeric(String note) {
		long up = note.chars().filter(c -> c == '\'').count();
		long down = note.chars().filter(c -> c == ',').count();
		return Math.toIntExact(up - down);
	}
	private static String numericToOctave(int octave) {
		return StringUtils.repeat(octave < 0 ? "," : "'", Math.abs(octave));
	}

	// Converts the UI string for the selected key signature into the format LilyPond expects.
	private static String keySignatureToLilyPond(String key_sig) {
		String keySigString = "";

		// Splitting up the two parts of the key string...
		String[] keySigParts = new String[2];
		keySigParts[1] = key_sig.substring(key_sig.length() - 5); // ...major or minor...
		keySigParts[0] = key_sig.replace(keySigParts[1], "").trim(); // ...and the key name itself.
		// Add the key's note letter.
		keySigString += keySigParts[0].substring(0, 1).toLowerCase(Locale.ROOT);
		// Add sharp or flat, if any.
		if (keySigParts[0].contains("\u266F")) {
			keySigString += "s";
		} else if (keySigParts[0].contains("\u266D")) {
			keySigString += "f";
		}
		// Add " \major" or " \minor".
		keySigString += (" \\" + keySigParts[1]);

		return keySigString;
	}

	public static void executeLilyPondRender(File lilypondFile, boolean renderPNG, Runnable exitingActions) throws IOException {

		// First, clear old logfiles
		TWUtils.cleanUpTempFiles("-logfile");

		ProcessBuilder prb = new ProcessBuilder(MainApp.getLilyPondPath() + MainApp.getPlatformSpecificLPExecutable(),
				renderPNG ? "-dpixmap-format=pngalpha" : "", renderPNG ? "-dresolution=300" : "", renderPNG ? "--png" : "-dlog-file=" +
				FilenameUtils.removeExtension(TWUtils.createTWTempFile("render", "logfile.log").getAbsolutePath()),
				"-o", lilypondFile.getAbsolutePath().replace(".ly", ""), lilypondFile.getAbsolutePath());

		lastExportProcess = prb.start();

		ProcessExitDetector prExitDetector = new ProcessExitDetector(lastExportProcess);
		prExitDetector.addProcessListener(process -> new Thread(exitingActions).start());
		prExitDetector.start();
	}

	public static File createTempLYChordFile(String toneFileName) {
		File tempFile = null;
		try {
			// Create the temporary file to hold the lilypond markup
			tempFile = TWUtils.createTWTempFile(FilenameUtils.removeExtension(toneFileName), "chord.ly");

			TWUtils.exportFSResource("chordTemplate.ly", tempFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return tempFile;
	}

	public static void cancelExportProcess() {
		lastExportProcess.destroy();
		exportCancelled = true;
	}
	public static void openLastExportFolder() {
		DesktopInterface.openFile(lastLilypondFile.getParentFile());
	}
	public static void openLastExportPDF() {
		DesktopInterface.openFile(new File(lastLilypondFile.getAbsolutePath().replace(".ly", ".pdf")));
	}
}
