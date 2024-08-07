package com.tac550.tonewriter.io;

import com.tac550.tonewriter.model.*;
import com.tac550.tonewriter.util.DesktopInterface;
import com.tac550.tonewriter.util.ProcessExitDetector;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.util.ToneChordRenderAdapter;
import com.tac550.tonewriter.view.ExportMenu;
import com.tac550.tonewriter.view.MainApp;
import javafx.application.Platform;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LilyPondInterface {

	public static final String[] barStrings = new String[] {" ", ".|:", "[|:", "|", "||", ":|.|:", ":|][|:", "|.", "'", ":|.", ":|]", "!"};
	public static final String BAR_UNCHANGED = "unchanged";

	// Constants for how much to adjust notes for each part ({S, A, T, B}) when applying relative octaves.
	// Each `'` shifts the user's input up one octave and each `,` shifts the same down one octave.
	// Changing these will cause previously-saved tone data to be read with incorrect octaves.
	public static final String[] PART_ADJUSTMENTS = {"'", "'", "'", ""};

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

	private static File lastLilypondFile;
	private static Process lastExportProcess;
	private static boolean exportCancelled = false;

	public static void renderChord(Chord chord, String key_sig, Consumer<File[]> exit_tasks) throws IOException {
		final String chordID = ToneChordRenderAdapter.generateChordId(chord, key_sig);
		File lilypondFile = LilyPondInterface.createTempLYChordFile(chordID);

		buildLilyPondSourceForChordRender(chord, key_sig, lilypondFile);

		File outputFile = new File(lilypondFile.getAbsolutePath().replace(".ly", ".png"));
		File midiFile = new File(lilypondFile.getAbsolutePath().replace(".ly",
				MainApp.getPlatformSpecificMidiExtension()));

		executeLilyPondRender(lilypondFile, true, () -> exit_tasks.accept(new File[] {outputFile, midiFile}));
	}

	private static void buildLilyPondSourceForChordRender(Chord chord, String key_sig, File lilypond_file) throws IOException {
		String[] parts = chord.getFields().split("-");

		List<String> lines = Files.readAllLines(lilypond_file.toPath(), StandardCharsets.UTF_8);

		lines.set(14, "  \\key " + keySignatureToLilyPond(key_sig));
		lines.set(20, adjustOctave(parts[PART_SOPRANO], PART_ADJUSTMENTS[PART_SOPRANO]));
		lines.set(25, adjustOctave(parts[PART_ALTO], PART_ADJUSTMENTS[PART_ALTO]));
		lines.set(30, adjustOctave(parts[PART_TENOR], PART_ADJUSTMENTS[PART_TENOR]));
		lines.set(35, adjustOctave(parts[PART_BASS], PART_ADJUSTMENTS[PART_BASS]));
		Files.write(lilypond_file.toPath(), lines, StandardCharsets.UTF_8);
	}

	// The function that handles final output.
	public static boolean exportItems(File saving_dir, String file_name, String output_title, List<ProjectItem> items,
	                                  Project project, ExportMenu export_menu, boolean gen_midi, int midi_tempo) throws IOException {
		exportCancelled = false;
		lastLilypondFile = new File(saving_dir.getAbsolutePath() + File.separator + file_name + ".ly");

		if (!saveToLilyPondFile(lastLilypondFile, output_title, items, project, gen_midi, midi_tempo)) {
			if (export_menu != null) export_menu.exportFailure();
			return false;
		}

		if (MainApp.lilyPondAvailable()) {
			if (export_menu != null) export_menu.exportWorking();

			executeLilyPondRender(lastLilypondFile, false, () -> {
				if (exportCancelled)
					return; // If user cancelled before we got here, stop. // TODO: Potential bug where a LP temp file is left behind?
				if (lastExportProcess.exitValue() != 0) {
					if (export_menu != null) Platform.runLater(export_menu::exportFailure);
					return;
				}

				try {
					if (export_menu != null) {
						// After the render is complete, ask the OS to open the resulting PDF file.
						Platform.runLater(export_menu::exportSuccess);
						if (export_menu.openWhenCompleted())
							openLastExportPDF();
					}

					// Delete the lilypond file if the option to save it isn't set
					if (!MainApp.prefs.getBoolean(MainApp.PREFS_SAVE_LILYPOND_FILE, false)) {
						if (!lastLilypondFile.delete()) {
							TWUtils.showError("Failed to delete LilyPond file, continuing...", false);
						}
					} // TODO: Consider making sure this has a chance to happen in event of cancellation or failure.
				} catch (Exception e) {
					if (export_menu != null) {
						// If the final rendered PDF can't be opened, open the folder instead (.ly file should be there even
						// if it's not set to be saved).
						Platform.runLater(export_menu::exportFailure);
						if (export_menu.openWhenCompleted())
							openLastExportFolder();
					}
				}
			});
		} else {
			if (export_menu != null) {
				export_menu.exportSuccess();
				if (export_menu.openWhenCompleted())
					openLastExportFolder();
			}
		}

		return true;
	}

	private static boolean saveToLilyPondFile(File lilypond_file, String output_title, List<ProjectItem> items, Project project,
	                                          boolean gen_midi, int midi_tempo) throws IOException {

		// Create the LilyPond output file, and if it already exists, delete the old one.
		if (lilypond_file.exists()) {
			// Have to do this because macOS doesn't like overwriting existing files
			if (!lilypond_file.delete()) {
				TWUtils.showError("Error deleting existing LilyPond file. Continuing anyway...", false);
			}
		}

		if (!lilypond_file.createNewFile()) {
			TWUtils.showError("Failed to create new LilyPond file", true);
			return false;
		}

		// If no items were passed, use the items contained in the project model for the export
		if (items == null || items.isEmpty())
			items = project.getItems();

		// Copy the render template file to the output path.
		try {
			TWUtils.exportFSResource(project.isNoHeader() && items.size() > 1 ? "/lilypond/exportTemplateNoHeader.ly"
					: "/lilypond/exportTemplate.ly", lilypond_file);
		} catch (Exception e2) {
			e2.printStackTrace();
			return false;
		}

		// The buffer in which we'll store the output file as we build it.
		List<String> lines = new ArrayList<>(Files.readAllLines(lilypond_file.toPath(), StandardCharsets.UTF_8));

		boolean generateHeader = !project.isNoHeader() || items.size() == 1;

		// Replacing paper size, title, and tagline info.
		lines.set(2, "#(set-default-paper-size \"" + project.getPaperSize().split(" \\(")[0] + "\")");
		if (generateHeader) {
			lines.set(7, lines.get(7).replace("$PROJECT_TITLE",
					items.size() == 1 ? (items.getFirst().getTitleType() == ProjectItem.TitleType.LARGE ? "\\fontsize #3 \"" : "\"")
							+ reformatHeaderText(items.getFirst().getTitleText()) + "\"" : "\"" + reformatHeaderText(output_title) + "\""));
			lines.set(9, lines.get(9).replace("$VERSION", MainApp.APP_VERSION)
					.replace("$APPNAME", MainApp.APP_NAME));
			if (items.size() == 1 && items.getFirst().getTitleType() == ProjectItem.TitleType.LARGE) {
				lines.set(15, lines.get(15).replace("\\fromproperty #'header:instrument", "\\fontsize #-3 \\fromproperty #'header:instrument"));
				lines.set(16, lines.get(16).replace("\\fromproperty #'header:instrument", "\\fontsize #-3 \\fromproperty #'header:instrument"));
			}

			if (!project.isEvenSpread()) {
				lines.set(15, lines.get(15).replace("  evenHeaderMarkup =", "  oddHeaderMarkup ="));
				lines.set(16, lines.get(16).replace("  oddHeaderMarkup =", "  evenHeaderMarkup ="));
			}
		}
		lines.set(generateHeader ? 18 : 15, "  top-margin = %s\\%s".formatted(project.getMarginInfo()[0], project.getMarginInfo()[1]));
		lines.set(generateHeader ? 19 : 16, "  bottom-margin = %s\\%s".formatted(project.getMarginInfo()[2], project.getMarginInfo()[3]));
		lines.set(generateHeader ? 20 : 17, "  left-margin = %s\\%s".formatted(project.getMarginInfo()[4], project.getMarginInfo()[5]));
		lines.set(generateHeader ? 21 : 18, "  right-margin = %s\\%s".formatted(project.getMarginInfo()[6], project.getMarginInfo()[7]));

		// Add a blank line before scores begin
		lines.add("");

		int index = 0;
		for (ProjectItem item : items) {

			lines.add(generateItemSource(item, gen_midi, midi_tempo, items.size() == 1));

			// Remove page break at beginning of item listing, if present.
			if (index == 0)
				lines.set(lines.size() - 1, lines.getLast().replaceFirst("\n\\\\pageBreak\n", ""));

			index++;
		}

		// Remove extra newline at end of file (result is one blank line)
		lines.set(lines.size() - 1, lines.getLast().replaceAll("\n$", ""));

		// Write the file back out.
		Files.write(lilypond_file.toPath(), lines, StandardCharsets.UTF_8);

		return true;
	}

	private static String generateItemSource(ProjectItem item, boolean generate_midi, int midi_tempo, boolean single_item) {
		List<String> lines = new ArrayList<>();

		// Comment for purposes of future parsing of output (for internal project file renders)
		lines.add("% " + item.getTitleText() + "\n");

		// Page break if requested, but only if this item is not the first.
		if (item.isPageBreakBeforeItem())
			lines.add("\\pageBreak\n");

		// Perform layout procedure
		String[] results = generateNotationAndLyrics(item.getAssignmentLines());

		// Pattern which matches valid LilyPond notes
		Pattern noteDataPattern = Pattern.compile("[a-g]\\S*\\d");

		// Check all four parts for any note data - if any exists, we need to include a staff or staves.
		boolean createStaff = Stream.of(results).limit(4).anyMatch(part -> noteDataPattern.matcher(part).find());

		// Manual title markup goes here, if any.
		// This allows displaying title and subtitle before top text.
		if ((item.getTitleType() != ProjectItem.TitleType.HIDDEN && !item.getTitleText().isEmpty())
				|| !item.getSubtitleText().isEmpty()) {
			Collections.addAll(lines, "\\markup \\column {");

			// Title, if not hidden and not the only item...
			if (!single_item && item.getTitleType() != ProjectItem.TitleType.HIDDEN && !item.getTitleText().isEmpty())
				Collections.addAll(lines, "  \\fill-line \\bold %s{\\justify { %s } }".formatted(item.getTitleType() == ProjectItem.TitleType.LARGE ?
						"\\fontsize #3 " : "\\fontsize #1 ", reformatBodyText(item.getTitleText())));
			// ...and subtitle, if present
			if (!item.getSubtitleText().isEmpty())
				Collections.addAll(lines, "  \\fill-line %s{\\justify { %s } } \\vspace #0.5".formatted(
						"\\fontsize #0.5 ", reformatBodyText(item.getSubtitleText())));

			Collections.addAll(lines, "  \\vspace #0.25", "}\n", "\\noPageBreak\n");
		}

		// Top verse, if any
		if (item.getExtendedTextSelection() == 1) {
			lines.add(reformatBodyText(generateExtendedText(item.getTopVersePrefix(),
					item.getVerseAreaText(), item.isBreakExtendedTextOnlyOnBlank())) + (createStaff ? "\\noPageBreak\n" : ""));
		} else if (!item.getTopVerse().isEmpty()) {
			Collections.addAll(lines, "\\markup \\column {",
					String.format("  \\vspace #0.5 \\justify { \\halign #-1 %s%s \\vspace #0.5",
							formatVersePrefixSelection(item.getTopVersePrefix()), reformatBodyText(item.getTopVerse()) + " } "),
					"}\n",
					createStaff ? "\\noPageBreak\n" : "");
		}

		if (createStaff) {
			// Score header
			Collections.addAll(lines, "\\score {\n", "  \\header {",
					String.format("    piece = \"%s\"", item.isHideToneHeader() ? "" : reformatHeaderText(item.getAssociatedTone().getToneText())),
					String.format("    opus = \"%s\"", item.isHideToneHeader() ? "" : reformatHeaderText(item.getAssociatedTone().getComposerText())),
					"    instrument = \"\"",
					"  }\n");

			Collections.addAll(lines, "  \\new ChoirStaff <<", "    \\new Staff \\with {",
					"      \\once \\override Staff.TimeSignature.stencil = ##f % Hides the time signatures in the upper staves");
			if (generate_midi) lines.add("      midiInstrument = #\"choir aahs\"");
			Collections.addAll(lines, "    } <<", "      \\key " + keySignatureToLilyPond(item.getAssociatedTone().getKeySignature()),
					"      \\new Voice = \"soprano\" { \\voiceOne {" + results[PART_SOPRANO] + " } }",
					"      \\new Voice = \"alto\" { \\voiceTwo {" + results[PART_ALTO] + " } }",
					"    >>", "    \\new Lyrics \\with {", "      \\override VerticalAxisGroup.staff-affinity = #CENTER",
					"    } \\lyricsto \"soprano\" { \\lyricmode {" + results[4] + " } }\n");

			// If the tenor and bass parts are not empty, include a lower staff.
			if (noteDataPattern.matcher(results[PART_TENOR]).find() || noteDataPattern.matcher(results[PART_BASS]).find()) {
				Collections.addAll(lines, "    \\new Staff \\with {",
						"      \\once \\override Staff.TimeSignature.stencil = ##f % Hides the time signatures in the lower staves");
				if (generate_midi) lines.add("      midiInstrument = #\"choir aahs\"");
				Collections.addAll(lines, "    } <<", "      \\clef bass",
						"      \\key " + keySignatureToLilyPond(item.getAssociatedTone().getKeySignature()),
						"      \\new Voice = \"tenor\" { \\voiceOne {" + results[PART_TENOR] + " } }",
						"      \\new Voice = \"bass\" { \\voiceTwo {" + results[PART_BASS] + " } }",
						"    >>");
			}

			Collections.addAll(lines, "  >>\n", "  \\layout {", "    \\context {", "      \\Score",
					"      \\remove \"Bar_number_engraver\" % removes the bar numbers at the start of each system",
					"      \\accidentalStyle neo-modern-voice-cautionary",
					"    }", "  }");
			if (generate_midi) lines.add("  \\midi {\n    \\tempo 4 = %d\n  }".formatted(midi_tempo));
			lines.add("}\n");

		} else {
			lines.add("\\markup \\column { \\vspace #0.5 }\n");
		}

		// Bottom verse, if any
		if (item.getExtendedTextSelection() == 2) {
			lines.add((createStaff ? "\\noPageBreak\n" : "") + reformatBodyText(generateExtendedText(item.getBottomVersePrefix(),
					item.getVerseAreaText(), item.isBreakExtendedTextOnlyOnBlank())));
		} else if (!item.getBottomVerse().isEmpty()) {
			Collections.addAll(lines, createStaff ? "\\noPageBreak\n" : "",
					"\\markup \\column {",
					String.format("  \\justify { \\halign #-1 %s%s \\vspace #1",
							formatVersePrefixSelection(item.getBottomVersePrefix()), reformatBodyText(item.getBottomVerse()) + " } "),
					"}\n");
		}

		return String.join("\n", lines);
	}

	private static String formatVersePrefixSelection(String prefix) {
		return prefix.equals("(None)") ? "" : String.format("\\bold {%s} ", prefix);
	}

	private static String generateExtendedText(String verse_choice, String extended_text, boolean break_only_on_blank) {
		StringBuilder resultText = new StringBuilder();

		String[] lines = extended_text.split("\n");

		resultText.append(String.format("\\markup \\column { \\vspace #1 \\justify { \\halign #-1 %s%s } \\vspace #0.125 }%s\n",
				formatVersePrefixSelection(verse_choice), lines[0],
				break_only_on_blank && lines.length > 1 && !lines[1].isBlank() ? " \\noPageBreak" : ""));

		int blankLineCounter = 0;
		for (int i = 1; i < lines.length; i++) {
			if (lines[i].isBlank()) {
				blankLineCounter++;
			} else {
				resultText.append(String.format("\\markup \\column {%s \\justify { %s } \\vspace #0.125 }%s\n",
						blankLineCounter > 0 ? " \\vspace #" + blankLineCounter : "", lines[i],
						break_only_on_blank && lines.length > i + 1 && !lines[i + 1].isBlank() ? " \\noPageBreak" : ""));
				blankLineCounter = 0;
			}
		}

		return resultText.toString();
	}

	private static String[] generateNotationAndLyrics(List<AssignmentLine> assignment_lines) {
		// Part buffers for the item  { S  A    T  B }
		String[] parts = new String[]{"", "", "", ""};
		// Buffer for the piece's text.
		StringBuilder verseText = new StringBuilder();
		// Add initial bar line, if there are any vlines.
		if (!assignment_lines.isEmpty()) {
			String bar = assignment_lines.getFirst().getBeforeBar();
			verseText.append(String.format("\\bar \"%s\"", bar.equals(BAR_UNCHANGED) ? " " : bar));
		}

		for (int i = 0; i < assignment_lines.size(); i++)
			generateLine(parts, verseText, assignment_lines.get(i), i < assignment_lines.size() - 1 ? assignment_lines.get(i + 1) : null);

		// Insert invisible barlines where indicated, after reversing possible incorrect orderings of bars/slur starts
		parts[PART_SOPRANO] = parts[PART_SOPRANO].replace("$bar  \\(", "\\( $bar")
				.replace("$bar", "\\bar \"\"");

		return new String[]{parts[0], parts[1], parts[2], parts[3], verseText.toString()};
	}

	private static void generateLine(String[] parts, StringBuilder verseText, AssignmentLine assignmentLine, AssignmentLine nextALine) {
		// Do nothing if this is a verse separator line or an empty line.
		if (!assignmentLine.isSeparator() && assignmentLine.getSelectedChantPhrase() != null) {
			StringBuilder verseLine = new StringBuilder();
			// Number of beats in the line. This determines where the visible barline goes.
			float measureBeats = generateNotatedLine(parts, assignmentLine.getSyllables(), verseLine,
					assignmentLine.isSystemBreakingDisabled(), assignmentLine);
			// Add barline style indicator to the soprano part.
			String bar = assignmentLine.getAfterBar();

			String defaultBar = nextALine == null || nextALine.isSeparator() ? "||" : "|";
			parts[PART_SOPRANO] += String.format(" \\bar \"%s\"", bar.equals(BAR_UNCHANGED) ? defaultBar : bar);

			// Insert time signature and the line's text into the lyrics buffer.
			verseText.append(String.format(" %s %s", generateTimeSignature(measureBeats), verseLine));
		}
	}

	private static float generateNotatedLine(String[] parts, List<AssignmentSyllable> syllableList, StringBuilder verseLine,
	                                         boolean disableLineBreaks, AssignmentLine al) {
		List<Chord> inOrderChords = al.getSelectedChantPhrase().getChordsMelodyOrder(); // TODO: This seems inefficient. It gets done for every verse line but concerns only chant lines.

		float measureBeats = 0;
		int breakCount = 0;

		// For each syllable in the line...
		for (AssignmentSyllable syllable : syllableList) {
			// Note buffers for this syllable.          { S  A    T  B }
			String[] syllableNoteBuffers = new String[]{"", "", "", ""};
			// Buffer for the syllable's text.
			StringBuilder syllableTextBuffer = new StringBuilder();

			// True if notes were combined for each part in the previous chord.
			// Note that notes being combined means either being made into one note with a
			// duration which is the sum of the two, or that the notes were tied (because there can be no single note with such a duration)
			boolean[] noteCombined = new boolean[]{false, false, false, false};
			// True if the corresponding part needed a combination 2 chords ago.
			boolean[] previousNoteCombined = new boolean[]{false, false, false, false};
			// Notes for each part to consider "current" when testing whether to combine.
			// Gets set if a note was combined on the last chord.
			String[] tempCurrentNotes = new String[]{"", "", "", ""};

			List<AssignedChordData> chordList = syllable.getAssignedChords();
			// For each chord assigned to the syllable...

			if (!chordList.isEmpty())
				addSyllableToLyrics(syllableList, syllable, syllableTextBuffer);

			for (AssignedChordData chordData : chordList) {

				// Add another chord indicator for the syllable, unless the soprano part was combined two chords ago,
				// or it contains a rest. We only check the soprano part because the text is mapped to it.
				if (chordList.indexOf(chordData) != 0 && !previousNoteCombined[PART_SOPRANO]
						&& !getNoteAndDuration(chordData, inOrderChords, PART_SOPRANO).contains("r"))
					// For chords after the first for each syllable, we add this to the lyric line
					// to tell Lilypond this syllable has an additional chord attached to it.
					syllableTextBuffer.append(" _ ");

				// CHORD DATA PROCESSING

				// Flag to keep track of whether this chord will be hidden. Chord hiding cleans up repeated quarter
				// notes for extended periods of recitative. We assume the chord will be hidden by default, but there
				// will be many opportunities for the chord to remain visible based upon what's going on around it.
				// We must check the notes in each part to see if the chord shouldn't be hidden.
				boolean hideThisChord = true;

				boolean lastChordInLine = false;
				boolean currentNoteIsEighth = false;

				// First, don't hide the chord if the next syllable (if any) has more than one assigned chord.
				AssignmentSyllable nextSyllable;
				if (syllableList.indexOf(syllable) < syllableList.size() - 1
						&& (nextSyllable = syllableList.get(syllableList.indexOf(syllable) + 1)) != null)
					if (nextSyllable.getAssignedChords().size() > 1)
						hideThisChord = false;

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
							// Then we know we need to show the chord.
							hideThisChord = false;
						} else {
							List<AssignedChordData> previousSyllableChords = syllableList.get(syllableList.indexOf(syllable) - 1).getAssignedChords();
							if (previousSyllableChords.isEmpty())
								// If the previous syllable has no chords assigned, then we know we shouldn't be hiding this one.
								hideThisChord = false;
							else
								// Otherwise, the previous note is the note from the last chord from the previous syllable.
								previousNote = getLastNote(parts[i]);
						}
					} else {
						// The previous note is the note just before this one on this same syllable.
						previousNote = getLastNote(syllableNoteBuffers[i]);
					}

					// CURRENT NOTE

					// If there is an alternate current note... (the previous one was combined)
					if (!tempCurrentNotes[i].isEmpty())
						// Then the current note will be the temporarily designated one.
						currentNote = tempCurrentNotes[i];
					else
						currentNote = getNoteAndDuration(chordData, inOrderChords, i);

					currentNoteIsEighth = currentNoteIsEighth || currentNote.contains("8");

					// NEXT NOTE

					// If this is the last chord associated with this syllable...
					if (chordList.indexOf(chordData) == chordList.size() - 1) {
						// And if this isn't the last syllable with chords on it...
						if (syllableList.indexOf(syllable) < syllableList.size() - 1
								&& !syllableList.get(syllableList.indexOf(syllable) + 1).getAssignedChords().isEmpty()) {
							// Then the next note is the note from the first chord of the next syllable.
							nextNote = getNoteAndDuration(syllableList.get(syllableList.indexOf(syllable) + 1).getAssignedChords().getFirst(), inOrderChords, i);
						} else {
							// This is the last chord associated with the last syllable with chords on it,
							// so we definitely do not want to hide it.
							hideThisChord = false;
							lastChordInLine = true;
						}
						// If there's another chord associated with this syllable...
					} else {
						// Then the next note is the note from the next chord on this same syllable.
						nextNote = getNoteAndDuration(chordList.get(chordList.indexOf(chordData) + 1), inOrderChords, i);

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
									tempCurrentNotes[i] = getLastNote(addedNotes);

								// Remember that we just did a note combination for the current part.
								noteCombined[i] = true;
								// Don't try to hide chords with combined notes.
								hideThisChord = false;
							}
						}
					}

					// If the previous, current, and next notes are not all quarters and/or not all the same pitch...
					if (!previousNote.equals(currentNote) || !currentNote.equals(nextNote) || !currentNote.contains("4"))
						// Don't hide the current chord because neighboring chords are different.
						hideThisChord = false;

					// This is just protection against some kind of error resulting in empty notes. Just don't hide the chord in this case.
					if (previousNote.isEmpty() || currentNote.isEmpty() || nextNote.isEmpty())
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
							syllableNoteBuffers[i] += " " + getNoteAndDuration(chordData, inOrderChords, i);
							// Add duration of this note to the beat total only if we're on the soprano part (we only need to count beats for one part).
							if (i == 0)
								measureBeats += getBeatDuration(getNoteAndDuration(chordData, inOrderChords, i));
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
				// Individual lines can disable this. Also, don't try subdividing if this is the last chord in the
				// phrase, we haven't reached the beat threshold for adding an optional break, the next syllable is the
				// last and has only one chord, or the note which would precede the possible break point is an eighth note.
				if (!disableLineBreaks && !lastChordInLine && measureBeats > measureBreakBeatThreshold
						* breakCount + measureBreakBeatThreshold && (syllableList.indexOf(syllable) != syllableList.size() - 2
						|| syllableList.getLast().getAssignedChords().size() != 1) && !currentNoteIsEighth)
					breakCount += trySubdividing(syllableNoteBuffers, syllableTextBuffer);

			}
			// That's it for each chord in the syllable.

			// SLUR AND BEAM PROCESSING
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

	private static String getNoteAndDuration(AssignedChordData chord, List<Chord> chords_in_order, int part) {
		return adjustOctave(chords_in_order.get(chord.getChordIndex()).getPart(part),
				PART_ADJUSTMENTS[part]) + chord.getDuration();
	}

	private static String removeLastNote(String notes) {
		String[] tokens = notes.split(" ");
		// This flag gets set if the previous token removed was in a note group.
		boolean noteGroup = false;
		// Work backward through the tokens.
		for (int i = tokens.length - 1; i >= 0; i--) {
			// Skip barline token
			if (tokens[i].equals("$bar"))
				continue;

			if (tokens[i].matches(".*[a-gr].*")) {
				if (noteGroup) {
					// If we hit the beginning of the note group (last token to remove)...
					if (tokens[i].contains("<")) {
						// remove it and we're done.
						tokens[i] = "";
						break;
					}
				} else if (tokens[i].contains(">")) { // If the note we're trying to remove is part of a note group...
					// Set the flag.
					noteGroup = true;
				}

				tokens[i] = "";

				if (!noteGroup) // Stop here because we just removed the previous note.
					break;

			} else {
				// Remove tokens that aren't notes from the end.
				tokens[i] = "";
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

	private static String getLastNote(String notes) {
		StringBuilder lastNoteBuffer = new StringBuilder();

		String[] tokens = notes.split(" ");
		// This flag gets set if the previous token selected was in a note group.
		boolean noteGroup = false;
		// Work backward through the tokens.
		for (int i = tokens.length - 1; i >= 0; i--) {
			// Skip barline token
			if (tokens[i].equals("$bar"))
				continue;

			if (tokens[i].matches(".*[a-gr].*")) {
				lastNoteBuffer.append(new StringBuilder(tokens[i]).insert(0, " ").reverse());
				if (noteGroup) {
					// If we hit the beginning of the note group (last token to remove)...
					if (tokens[i].contains("<")) {
						// We found the last token in the note group and are done.
						break;
					}
				} else if (tokens[i].contains(">")) { // If the note we're trying to remove is part of a note group...
					// Set the flag.
					noteGroup = true;
				}

				if (!noteGroup) // Stop here because we just removed the previous note.
					break;

			}
		}

		return lastNoteBuffer.reverse().toString().trim();
	}

	private static String applySlursAndBeams(String syllable_notes) {
		boolean addedSlur = false;

		String[] tokens = syllable_notes.trim().split(" ");

		// If this part has any hidden notes, or all notes are tied, or the part contains any rests, skip adding a slur.
		if (syllable_notes.contains("noteHide")
				|| (tokens.length > 1 && Arrays.stream(tokens).limit(tokens.length - 1).allMatch(val -> val.contains("~")))
				|| Arrays.stream(tokens).anyMatch(val -> Pattern.matches("r\\S", val)))
			return syllable_notes;

		String startSymbol = "\\(";
		String endSymbol = "\\)";

		// If the syllable contains nothing but eighth notes, apply beam along with slur
		// (also prevents auto-connecting beam to note(s) from previous syllable).
		int eighthNotes = (int) Arrays.stream(tokens).filter(t -> t.contains("8")).count();
		int totalNotes = (int) Arrays.stream(tokens).filter(t -> t.matches(".*\\d.*")).count();
		if (eighthNotes == totalNotes) {
			startSymbol = "\\([";
			endSymbol = "]\\)";

			// If there's only one note in the syllable, disable any beam connecting to another syllable.
			if (totalNotes == 1)
				tokens[0] += "\\noBeam";
		}

		// Reconstruct the syllable note buffer, adding the beginning slur parenthesis after the first note (as LilyPond syntax dictates).
		StringBuilder finalString = new StringBuilder();
		// For each token in the buffer...
		boolean inAGroup = false;
		for (int i = 0; i < tokens.length; i++) {
			if (!inAGroup && i > 0 && tokens[i - 1].contains("<")) inAGroup = true;
			if (inAGroup && tokens[i - 1].contains(">")) inAGroup = false;

			// If the token is a barline indicator, append it and the following as if one token
			if (tokens[i].equals("$bar")) {
				finalString.append(" ").append(tokens[i])
						.append(" ").append(i + 1 < tokens.length ? tokens[i + 1] : "");
				i++; // Don't try to append i + 1 again
				continue;
			}
			// If it's not the first token, we haven't added the slur yet, and we're not in a
			// note group... (>1 note occurring in one part, which will be split across >1 token)
			if (i > 0 && !addedSlur && !inAGroup) {
				// Add the beginning slur parenthesis and the current token.
				finalString.append(" %s ".formatted(startSymbol)).append(tokens[i]);
				addedSlur = true;
			} else {
				// Otherwise, skip empty tokens or add the current one.
				if (tokens[i].isEmpty()) continue;
				finalString.append(" ").append(tokens[i]);
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

	private static void addSyllableToLyrics(List<AssignmentSyllable> syllableList, AssignmentSyllable syllable, StringBuilder syllableTextBuffer) {
		// Add any formatting flags for the syllable first.
		syllableTextBuffer.append(syllable.isBold() ? " \\lyricBold " : "")
				.append(syllable.isItalic() ? " \\lyricItalic " : "")
				.append(syllableList.indexOf(syllable) < syllableList.size() - 1 && syllableList.get(syllableList.indexOf(syllable) + 1).isForcingHyphen() ? " \\forceHyphen " : "");

		// Add syllable to the text buffer, throwing away any (presumably leading) hyphens beforehand.
		syllableTextBuffer.append(reformatBodyText(syllable.getSyllableText().replace("-", "")));

		// If this is not the last syllable in the text,
		if (syllableList.indexOf(syllable) < syllableList.size() - 1)
			// If the next syllable doesn't begin a new word, we need to add these dashes immediately after the current syllable.
			// This ensures correct hyphenation formatting in lyric engraving.
			if (!syllableList.get(syllableList.indexOf(syllable) + 1).isFirstSyllableInWord())
				syllableTextBuffer.append(" -- ");

		// Add closing formatting flag if necessary.
		if (syllable.isBold() || syllable.isItalic())
			syllableTextBuffer.append(" \\lyricRevert ");
	}

	private static int trySubdividing(String[] partBuffers, StringBuilder syllableTextBuffer) {
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
		String noteFormat = curr.replaceAll("\\d+", "%s").replace(".", "");
		String durSum = TWUtils.addDurations(curr, next);

		if (durSum == null) // Combination failed, so we tie the notes, unless they are rests. In that case, return "".
			return noteFormat.contains("r") ? "" : curr + "~ " + next;
		else // Combination succeeded. We just format the new duration string into the note and return it.
			return String.format(Locale.US, noteFormat, durSum);
	}

	// Returns the number of beats for which the given note (or notes, if tied) last(s).
	private static float getBeatDuration(String note) {
		// If there is a tie...
		if (note.contains("~")) {
			String[] tiedNotes = note.split("~ ");

			float totalDuration = 0;

			for (String tNote : tiedNotes) // Make one recursive call for each tied note
				totalDuration += getBeatDuration(tNote);

			return totalDuration;

		} else {
			// Calculate the duration of the note.
			float beats = 4 / Float.parseFloat(note.replaceAll("\\D", ""));

			if (note.contains(".")) beats += (beats / 2);

			return beats;
		}
	}

	// Returns reformatted version of input such that double quotes display correctly and
	// straight apostrophes are replaced with curly ones in LilyPond output.
	private static String reformatBodyText(String input) {
		StringBuilder outputBuffer = new StringBuilder();

		// Delimiters are included to enable rebuilding the entire string with whitespace
		StringTokenizer tokenizer = new StringTokenizer(
				TWUtils.applySmartQuotes(input), " \t\n\r\f", true);

		while (tokenizer.hasMoreTokens()) {
			String current_token = tokenizer.nextToken();

			if (current_token.contains("\"")) {

				StringBuilder working_token = new StringBuilder(current_token);

				// Insert the escape character before each occurrence of a double quote in the syllable.
				for (int index = working_token.indexOf("\""); index >= 0; index = working_token.indexOf("\"", index + 2))
					working_token.insert(index, "\\");

				// Surround the syllable without a leading space with quotes.
				outputBuffer.append("\"").append(working_token).append("\"");
			} else {
				outputBuffer.append(current_token);
			}
		}

		return outputBuffer.toString().replace("'", TWUtils.APOSTROPHE);
	}

	private static String reformatHeaderText(String input) {
		return TWUtils.applySmartQuotes(input)
				.replace("\"", "\\\"").replace("'", TWUtils.APOSTROPHE);
	}

	// Adjusts the octave of the given note or note group according to the octave_data string.
	// octave_data should be formatted "''", "'", ",", "", etc.
	// Each ' shifts the user's input up one octave and each , shifts down one octave.
	private static String adjustOctave(String note_data, String octave_data) {
		if (note_data.isBlank()) return "r";
		int adjustment = octaveToNumeric(octave_data);
		String[] notes = note_data.split(" ");
		boolean multi = notes.length > 1;
		return Arrays.stream(notes)
				.map(s -> s.replaceAll("[<>',]", "") + numericToOctave(octaveToNumeric(s) + adjustment))
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

	// Converts the provided UI string for a key signature into the format LilyPond expects.
	private static String keySignatureToLilyPond(String key_sig) {
		String keySigString = "";

		// Splitting up the two parts of the key string...
		String[] keySigParts = new String[2];
		keySigParts[1] = key_sig.substring(key_sig.length() - 5); // ...major or minor...
		keySigParts[0] = key_sig.replace(keySigParts[1], "").trim(); // ...and the key name itself.
		// Add the key's note letter.
		keySigString += keySigParts[0].substring(0, 1).toLowerCase(Locale.ROOT);
		// Add sharp or flat, if any.
		if (keySigParts[0].contains(TWUtils.SHARP))
			keySigString += "s";
		else if (keySigParts[0].contains(TWUtils.FLAT))
			keySigString += "f";
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

			TWUtils.exportFSResource("/lilypond/chordTemplate.ly", tempFile);
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
		DesktopInterface.highlightFile(lastLilypondFile);
	}

	public static void openLastExportPDF() {
		DesktopInterface.openFile(new File(lastLilypondFile.getAbsolutePath().replace(".ly", ".pdf")));
	}
}
