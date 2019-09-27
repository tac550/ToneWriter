package com.tac550.tonewriter.io;

import com.tac550.tonewriter.model.ChordData;
import com.tac550.tonewriter.util.ProcessExitDetector;
import com.tac550.tonewriter.view.MainApp;
import com.tac550.tonewriter.view.MainSceneController;
import com.tac550.tonewriter.view.SyllableText;
import com.tac550.tonewriter.view.VerseLineViewController;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.commons.io.FilenameUtils;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LilyPondWriter {

	// Constants for how much to adjust notes for each part when applying relative octaves.
	// Each ' shifts the user's input up one octave and each , shifts the same down one octave.
	// Changing these will cause previously-saved tone data to be read with incorrect octaves.
	public static final String ADJUSTMENT_SOPRANO = "'";
	public static final String ADJUSTMENT_ALTO = "'";
	public static final String ADJUSTMENT_TENOR = "'";
	public static final String ADJUSTMENT_BASS = "";

	// Constants to make some array indices easier to read.
	private static final int PART_SOPRANO = 0;
	private static final int PART_ALTO = 1;
	private static final int PART_TENOR = 2;
	private static final int PART_BASS = 3;

	// The function that handles final output.
	public static boolean writeToLilypond(File saving_dir, String file_name, ArrayList<VerseLineViewController> verse_lines, String keySignature,
	                                      String title, String subtitle, String header,
	                                      String topReaderType, String topReader, String bottomReaderType, String bottomReader, String paperSize) throws IOException {

		// Create the LilyPond output file, and if it already exists, delete the old one.
		File lilypondFile = new File(saving_dir.getAbsolutePath() + File.separator + file_name + ".ly");

		if (lilypondFile.exists()) {
			// Have to do this because MacOS doesn't like overwriting existing files
			if (!lilypondFile.delete()) {
				System.out.println("Error deleting existing LilyPond file. Continuing anyway...");
			}
		}

		if (!lilypondFile.createNewFile()) {
			System.out.println("Filed to create new file");
			return false;
		}

		// Copy the chord template file, the basis for all our LilyPond output files, to the output path.
		try {
			exportResource("renderTemplate.ly", lilypondFile.getAbsolutePath());
		} catch (Exception e2) {
			e2.printStackTrace();
			return false;
		}

		// The buffer in which we'll store the output file as we build it.
		List<String> lines = Files.readAllLines(lilypondFile.toPath(), StandardCharsets.UTF_8);

		// Adding paper size, title, and header info.
		lines.set(2, "#(set-default-paper-size \"" + paperSize.split(" \\(")[0] + "\")");
		lines.set(7, "  subtitle = \"" + title + "\"");
		lines.set(8, "  subsubtitle = \"" + (subtitle.isEmpty() ? " " : subtitle) + "\"");
		String[] headerParts = header.split("-", 2);
		lines.set(9, "  poet = \"" + headerParts[0].trim() + "\"");
		lines.set(10, "  composer = \"" + (headerParts.length > 1 ? headerParts[1].trim() : "") + "\"");

		lines.set(12, lines.get(12).replace("$VERSION", MainApp.APP_VERSION)
				.replace("$APPNAME", MainApp.APP_NAME));

		// Adding key signature info.
		lines.set(16, keySignatureToLilyPond(keySignature));

		// Note buffers for the piece. S   A   T   B
		String[] parts = new String[]{"", "", "", ""};
		// Buffer for the piece's text.
		StringBuilder verseText = new StringBuilder();

		// For every verse line...
		for (VerseLineViewController verseLineController : verse_lines) {
			// Store the line's syllables.
			List<SyllableText> syllableList = Arrays.asList(verseLineController.getSyllables());
			// If this is a verse separator line, add the double bar line and continue to the next one.
			if (syllableList.isEmpty()) {
				parts[PART_SOPRANO] += " \\bar \"||\"";
				continue;
			}

			// Buffer for the line's text.
			StringBuilder verseLine = new StringBuilder();
			// Number of beats in the line.
			float lineBeats = 0;

			// For each syllable in the line...
			for (SyllableText syllableData : syllableList) {
				// Note buffers for this syllable.           S   A   T   B
				String[] syllableNoteBuffers = new String[]{"", "", "", ""};
				// Buffer for the syllable's text.
				StringBuilder syllableTextBuffer = new StringBuilder();

				// Whether or not notes were combined for each part in the previous chord.
				// Note that notes being combined means either being made into one note with a
				// duration which is the sum of the two, or that the notes were tied (because there can be no single note with such a duration)
				boolean[] noteCombined = new boolean[]{false, false, false, false};
				// True if the corresponding part needed a combination 2 chords ago.
				boolean[] previousNoteCombined = new boolean[]{false, false, false, false};
				// Notes for each part to consider "current" when testing whether to combine.
				// Gets set if a note was combined on the last chord.
				String[] tempCurrentNotes = new String[]{"", "", "", ""};

				List<ChordData> chordList = Arrays.asList(syllableData.getAssociatedChords());
				// For each chord assigned to the syllable...
				for (ChordData chordData : chordList) {

					String syllable = chordData.getSyllable();

					// SYLLABLE TEXT PROCESSING

					// If this is the first chord associated with the syllable, then we do text parsing right now and only once.
					// The reason we do this now instead of in the enclosing scope is because we want to skip
					// adding to the final text any syllable that has no associated chords.
					if (chordList.indexOf(chordData) == 0) {

						// Make any double quotes in the text understandable to LilyPond.
						if (syllable.contains("\"")) { // If the syllable contains a double quote...
							// Store the syllable without the leading hyphen, if any (we always throw this hyphen away in the end).
							StringBuilder cleanSyllable = new StringBuilder(syllable.replace("-", ""));

							// Insert the escape character before each occurrence of a double quote in the syllable.
							for (int index = cleanSyllable.indexOf("\""); index >= 0; index = cleanSyllable.indexOf("\"", index + 2)) {
								cleanSyllable.insert(index, "\\");
							}

							// If the syllable contains a leading space... (Most do, to separate them from the previous word or syllable)
							if (cleanSyllable.toString().startsWith(" ")) {
								// Surround just the syllable (not the leading space) with double quotes
								syllableTextBuffer.append(cleanSyllable.toString().replace(" ", " \"")).append("\"");
							} else {
								// Otherwise just surround the syllable without a leading space with quotes.
								syllableTextBuffer.append("\"").append(cleanSyllable.toString()).append("\"");
							}

						} else { // If there are no double quotes, add the syllable normally (throwing away the leading hyphen, if any).
							syllableTextBuffer.append(syllable.replace("-", ""));
						}

						// If this is not the last syllable in the text... (we're just avoiding an index out of bounds-type error)
						if (syllableList.indexOf(syllableData) < syllableList.size() - 1) {
							// If the next syllable starts with a hyphen, it is part of the same word, so we need to add these dashes immediately after the current syllable.
							// This ensures that syllables belonging to one word split across a distance are engraved correctly by LilyPond.
							if (syllableList.get(syllableList.indexOf(syllableData) + 1).getText().startsWith("-")) {
								syllableTextBuffer.append(" -- ");
							}
						}

						// If this is not the first chord associated with the syllable...
					} else {
						// If the soprano part was combined two chords ago, we skip the following addition to the text buffer for the syllable.
						// We only check the soprano part because that is the only part to which the text is actually mapped by LilyPond.
						if (!previousNoteCombined[PART_SOPRANO]) {
							// For chords subsequent to the first for each syllable, we add this to tell Lilypond this syllable has an additional chord attached to it.
							syllableTextBuffer.append(" _ ");
						}
					}

					// CHORD DATA PROCESSING

					// Flag to keep track of whether this chord will be hidden.
					// Chord hiding cleans up repeated quarter notes for extended periods of recitative.
					// The default behavior is to hide the chord after the below processing is done,
					// but there will be many opportunities for the chord to remain visible based upon what's going on around it.
					// We must check the notes in each part to see if the chord shouldn't be hidden.
					boolean hideThisChord = true;
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
							if (syllableList.indexOf(syllableData) == 0) {
								// Then we know we need to show the chord. It'll be the first chord on the page!
								hideThisChord = false;
							} else {
								// The previous note is the note from the last chord from the previous syllable.
								ChordData[] previousSyllableChords = syllableList.get(syllableList.indexOf(syllableData) - 1).getAssociatedChords();
								previousNote = previousSyllableChords[previousSyllableChords.length - 1].getPart(i);
							}
						} else {
							// The previous note is the note just before this one on this same syllable.
							previousNote = chordList.get(chordList.indexOf(chordData) - 1).getPart(i);
						}

						// CURRENT NOTE

						// If there is an alternate current note... (the previous one was combined)
						if (!tempCurrentNotes[i].isEmpty()) {
							// Then the current note will be the temporarily designated one.
							currentNote = tempCurrentNotes[i];
						} else {
							currentNote = chordData.getPart(i);
						}

						// NEXT NOTE

						// If this is the last chord associated with this syllable...
						if (chordList.indexOf(chordData) == chordList.size() - 1) {
							// And if this isn't the last syllable with chords on it...
							if (syllableList.indexOf(syllableData) < syllableList.size() - 1
									&& syllableList.get(syllableList.indexOf(syllableData) + 1).getAssociatedChords().length > 0) {
								// Then the next note is the note from the first chord of the next syllable.
								nextNote = syllableList.get(syllableList.indexOf(syllableData) + 1).getAssociatedChords()[0].getPart(i);
							} else {
								// This is the last chord associated with the last syllable with chords on it,
								// so we definitely do not want to hide it (it's the last chord on the page!)
								hideThisChord = false;
							}
							// If there's another chord associated with this syllable...
						} else {
							// Then the next note is the note from the next chord on this same syllable.
							nextNote = chordList.get(chordList.indexOf(chordData) + 1).getPart(i);

							// Since we have determined that there is at least one more chord on this syllable,
							// we have to decide whether the current note should be combined with the next note.

							// If the current and next notes both have the same pitch... (they must if they are to be combined)
							if (currentNote.replaceAll("[^A-Za-z',]+", "").equals(nextNote.replaceAll("[^A-Za-z',]+", ""))) {

								// The addNotes function handles adding the notes together.
								String addedNotes = combineNotes(currentNote, nextNote);

								// If the previous note was also combined, remove it (and any tokens after it).
								if (!tempCurrentNotes[i].isEmpty()) {
									String[] tokens = syllableNoteBuffers[i].split(" ");
									// This flag gets set if the previous note was a note group.
									boolean noteGroup = false;
									// Work backward through the tokens.
									for (int i1 = tokens.length - 1; i1 >= 0; i1--) {
										if (tokens[i1].contains("a") || tokens[i1].contains("b") || tokens[i1].contains("c") ||
												tokens[i1].contains("d") || tokens[i1].contains("e") || tokens[i1].contains("f") || tokens[i1].contains("g")) {
											if (noteGroup) {
												// If we hit the beginning of the note group...
												if (tokens[i1].contains("<")) {
													// remove it and we're done.
													tokens[i1] = "";
													break;
												}

												// If the note we're trying to remove is actually a note group...
											} else if (tokens[i1].contains(">")) {
												// Set the flag.
												noteGroup = true;
											}

											tokens[i1] = "";

											if (!noteGroup) {
												// Stop here because we just removed the previous note.
												break;
											}

										} else {
											// Remove tokens that aren't notes from the end.
											tokens[i1] = "";
										}
									}

									// Remove any excess spaces from the syllable note buffer
									StringBuilder editedString = new StringBuilder();
									for (String token : tokens) {
										if (token.isEmpty()) continue;
										editedString.append(" ").append(token);
									}
									// Save back to the buffer.
									syllableNoteBuffers[i] = editedString.toString();

								}

								// Add the combined note(s) to the buffer.
								syllableNoteBuffers[i] += " " + addedNotes;
								// Add duration of this/these note(s) to the beat total but only if we're on the soprano part (we only need to count beats for 1 part).
								if (i == 0) lineBeats += getBeatDuration(addedNotes);

								// If the notes were combined into one... (not tied)
								if (!addedNotes.contains("~")) {
									// The new note becomes the temporary current note for the current part.
									tempCurrentNotes[i] = addedNotes;
								} else {
									// If the combination resulted in a tie, the temporary current note is the second of the two tied notes.
									tempCurrentNotes[i] = addedNotes.split(" ")[1];
								}

								// Remember that we just did a note combination for the current part.
								noteCombined[i] = true;
							}

						}

						// This is just protection against some kind of error resulting in empty notes. Just don't hide the chord in this case.
						if (previousNote.equals("") || currentNote.equals("") || nextNote.equals("")) {
							hideThisChord = false;
						}

						// If the previous, current, and next notes are not all quarters and/or not all the same pitch...
						if (!previousNote.equals(currentNote) || !currentNote.equals(nextNote) || !currentNote.contains("4")) {
							// Don't hide the current chord because neighboring chords are different.
							hideThisChord = false;
						}

					}

					// If hideThisChord remained true after all the checks for all the parts in the chord...
					if (hideThisChord) {
						for (int i = 0; i < 4; i++) {
							// Add the flag that hides the note for each part of the chord before adding the notes themselves.
							syllableNoteBuffers[i] += " \\noteHide";
						}
					}

					// Add note data for each part to the buffers...
					for (int i = 0; i < 4; i++) {
						// ...but only if the note was not combined... (in which case it will already have been added)
						if (!noteCombined[i]) {
							// ...and the one before that was not combined.
							if (!previousNoteCombined[i]) {
								syllableNoteBuffers[i] += " " + chordData.getPart(i);
								// Add duration of this note to the beat total but only if we're on the soprano part (we only need to count beats for 1 part).
								if (i == 0) lineBeats += getBeatDuration(chordData.getPart(i));
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

				}
				// That's it for each chord in the syllable.

				// SLUR PROCESSING

				// Flags for whether we needed a slur on the syllable for each part.
				boolean[] addedSlur = new boolean[]{false, false, false, false};

				// For each part...
				for (int i = 0; i < 4; i++) {
					// If the current part doesn't have any hidden notes on this syllable... (which would indicate that it's recitative)
					if (!syllableNoteBuffers[i].contains("noteHide")) {

						String[] tokens = syllableNoteBuffers[i].trim().split(" ");

						// If there are only two notes in this syllable and they're tied, skip adding slurs.
						if (tokens.length == 2 && tokens[0].contains("~")) {
							continue;
						}

						// Reconstruct the syllable note buffer, adding the beginning slur parenthesis after the first note (as LilyPond syntax dictates).
						StringBuilder finalString = new StringBuilder();
						// For each token in the buffer...
						for (int i1 = 0; i1 < tokens.length; i1++) {
							// If it's not the first token, we haven't added the slur yet, and the previous token was not the beginning of a
							// note group... (two notes occurring in one part, which will be split across two tokens)
							if (i1 > 0 && !addedSlur[i] && !tokens[i1 - 1].contains("<")) {
								// Add the beginning slur parenthesis and the current token.
								finalString.append(" \\( ").append(tokens[i1]);
								addedSlur[i] = true;
							} else {
								// Otherwise skip empty tokens or add the current one.
								if (tokens[i1].isEmpty()) continue;
								finalString.append(" ").append(tokens[i1]);
							}
						}

						// Save the new string to the note buffer.
						syllableNoteBuffers[i] = finalString.toString();
					}
				}

				// Close parentheses to complete the slur for the syllable.
				// For each part...
				for (int i = 0; i < 4; i++) {
					// If a slur was begun to be added for the part...
					if (addedSlur[i]) {
						// Complete the slur by adding a closing parenthesis.
						syllableNoteBuffers[i] += ("\\)");
					}
				}

				// SYLLABLE BUFFER SAVE-OUTS

				// Add the text buffer for the syllable to that of the line.
				verseLine.append(syllableTextBuffer);

				// Add the note data for the syllable from the buffers to the final part strings.
				for (int i = 0; i < 4; i++) {
					parts[i] += syllableNoteBuffers[i];
				}

			}

			// Ceil the beat total because we never want time signatures with fractional parts.
			// Using ceil here instead of floor because LilyPond won't change the time signature until the current bar is completed.
			// If a bar ends before we want it to we may be stuck without a bar reset (for accidentals and such) for a very long time
			// depending on the length of the previous line. It seems better to overshoot a little so any accidental engraving errors may be
			// kept to a minimum and hopefully occur only at the start of a line.
			int roundedBeats = (int) Math.ceil(lineBeats);
			// We never want a 0/4 time signature (hangs Lilypond)
			int finalBeats = roundedBeats == 0 ? 1 : roundedBeats;
			// Add time signature information before each verse line except the first one.
			if (verse_lines.indexOf(verseLineController) != 0) {
				verseText.append(String.format(Locale.US, " \\time %d/4 ", finalBeats)).append(verseLine);
			} else {
				// If it's the first line, put its time signature in the header instead.
				verseText.append(verseLine);
				lines.set(17, String.format(Locale.US, "  \\time %d/4", finalBeats));
			}

			// Add a barline after each verse line
			parts[PART_SOPRANO] += " \\bar \"|\"";

		}
		// That's it for each verse line.

		// WRITING LINES OUT TO FINAL BUFFER

		// Add a double barline at the end of the page.
		lines.set(24, parts[PART_SOPRANO] + " \\bar \"||\"");
		lines.set(30, parts[PART_ALTO]);
		lines.set(36, parts[PART_TENOR]);
		lines.set(42, parts[PART_BASS]);
		lines.set(48, verseText.toString());
		// Add markup for readers' parts, if any.
		if (!topReader.isEmpty()) {
			lines.set(57, "\\markup {");
			lines.set(58, "  \\vspace #2 \\justify { \\halign #-1 \\bold {" + topReaderType + "} " + topReader + "}");
			lines.set(59, "}");
		}
		if (!bottomReader.isEmpty()) {
			lines.set(94, "\\markup {");
			lines.set(95, "  \\vspace #2 \\justify { \\halign #-1 \\bold {" + bottomReaderType + "} " + bottomReader + "}");
			lines.set(96, "}");
		}

		// Write the file back out.
		Files.write(lilypondFile.toPath(), lines, StandardCharsets.UTF_8);

		if (MainApp.lilyPondAvailable()) {
			executePlatformSpecificLPRender(lilypondFile, false, () -> {
				try {
					// After the render is complete, ask the OS to open the resulting PDF file.
					Desktop.getDesktop().open(new File(lilypondFile.getAbsolutePath().replace(".ly", ".pdf")));

					// Delete the lilypond file if the option to save it isn't set
					if (!MainApp.prefs.getBoolean(MainApp.PREFS_SAVE_LILYPOND_FILE, false)) {
						if (!lilypondFile.delete()) {
							System.out.println("Failed to delete LilyPond file, continuing...");
						}
					}

				} catch (Exception e) {
					// If the final rendered PDF can't be opened, open the folder instead (.ly file should be there even
					// if it's not set to be saved).
//					e.printStackTrace();
					try {
						Desktop.getDesktop().open(new File(lilypondFile.getParent()));
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});
		} else {
			try {
				Desktop.getDesktop().open(new File(lilypondFile.getParent()));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}


		return true;
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
			// Inverse the computed duration.
			float inverse = 1 / computedDur;
			if (inverse == 0.75) { // Dotted half
				newDur = "2.";
			} else if (inverse == 0.375) { // Dotted quarter
				newDur = "4.";
			} else if (inverse == 1.5) { // Dotted whole; but we'll return the more common whole tied to a half.
				return String.format(Locale.US, noteFormat, "1") + "~ " + String.format(Locale.US, noteFormat, "2");
			} else { // If the non-whole computed value didn't have a definition, we just return the notes as they came in, but tied.
				return curr + "~ " + next;
			}
		}

		// If we got this far, the notes will be combined into one. We just format the new duration string into the note and return it.
		return String.format(Locale.US, noteFormat, newDur);
	}

	// Returns the number of beats for which the given note (or notes, if tied) last(s).
	private static float getBeatDuration(String note) {

		// If there is a tie...
		if (note.contains("~")) {
			String[] tiedNotes = note.split(" ");

			float totalDuration = 0;

			for (String tNote : tiedNotes) {
				// Make recursive calls if there are multiple notes combined with a tie or ties.
				System.out.println("Recursing!! (probably bad news)");
				totalDuration += getBeatDuration(tNote); // TODO: Investigate this, seems to always StackOverFlow
			}

			return totalDuration;

			// If no tie is present...
		} else {

			// Calculate the duration of the note.
			float beats = 4 / Float.parseFloat(note.replaceAll("[\\D]", ""));

			if (note.contains(".")) beats += (beats / 2);

			return beats;
		}

	}

	// TODO: Seems to have bugs with note groups of more than 2 notes
	// Adjusts the octave of the given note according to the octave_data string.
	// octave_data should be formatted "''", "'", ",", "", etc.
	// Each ' shifts the user's input up one octave and each , shifts the same down one octave.
	public static String parseNoteRelative(String note_data, String octave_data) {

		// If there is no note, no adjustment is needed, but we do need to return a rest.
		if (note_data.trim().isEmpty()) {
			return "r";
		}

		// We construct the final adjusted note here.
		StringBuilder finalNoteData = new StringBuilder();

		// This pattern looks for note names in the note_data.
		Pattern p = Pattern.compile("[abcdefg]");
		Matcher m = p.matcher(note_data);
		// Set until we encounter the first match (first note letter).
		boolean first = true;
		// Set when we encounter the end of the note group (if that's what we were given)
		boolean closeGroupFlag;
		// For each note name found...
		while (m.find()) {
			closeGroupFlag = false;
			int position = m.start();

			/*
			 * Continue to the next match if this is an add-on character after a note name,
			 * such as an "f" to indicate flat.
			 */
			if (!first && note_data.substring(position - 1, position).matches("[abcdefg]")) {
				continue;
			}
			first = false;

			String workingOctave = octave_data;
			StringBuilder workingSection;
			// If the note data contains its first space after the position of the match...
			if (note_data.contains(" ")
					&& note_data.indexOf(" ") > position) {
				// The working section goes from the match to the next space.
				workingSection = new StringBuilder(note_data.substring(position - 1, note_data.indexOf(" ")));
				// If the note data contains no spaces but does contain a note group ending...
			} else if (note_data.contains(">")) {
				// Working section ends before end of group.
				workingSection = new StringBuilder(note_data.substring(position, note_data.indexOf(">")));
				closeGroupFlag = true;
				// Otherwise...
			} else {
				// Working section goes to the end.
				workingSection = new StringBuilder(note_data.substring(position));
			}

			// For each quote in the octave data...
			while (workingOctave.contains("'")) {
				// Remove one quote.
				workingOctave = workingOctave.replaceFirst("'", "");
				if (workingSection.toString().contains(",")) { // If we've got comma(s) in the note data...
					// Remove a comma from the note.
					workingSection = new StringBuilder(workingSection.toString().replaceFirst(",", ""));
				} else { // If we don't have commas in the note data...
					// Add a quote to the note.
					workingSection.append("'");
				}
			}

			// for each comma in the octave data...
			while (workingOctave.contains(",")) {
				// Remove one comma.
				workingOctave = workingOctave.replaceFirst(",", "");
				if (workingSection.toString().contains("'")) { // If we've got quote(s) in the note data...
					// Remove a quote from the note.
					workingSection = new StringBuilder(workingSection.toString().replaceFirst("'", ""));
				} else { // If we don't have quotes in the note data...
					// Add a comma to the note.
					workingSection.append(",");
				}
			}

			if (closeGroupFlag) {
				// Re-add the grouping syntax if this was the last note of a note group.
				finalNoteData.append(" ").append(workingSection).append(workingOctave).append(">");
			} else {
				finalNoteData.append(workingSection).append(workingOctave);
			}

		}

		return finalNoteData.toString();
	}

	// Copies internal file on the classpath to an external location.
	public static void exportResource(String resourceName, String outFilePath) throws Exception {
		InputStream stream = null;
		OutputStream resStreamOut = null;
		try {
			stream = LilyPondWriter.class.getResourceAsStream(resourceName);
			if (stream == null) {
				throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
			}

			int readBytes;
			byte[] buffer = new byte[4096];
			resStreamOut = new FileOutputStream(outFilePath);
			while ((readBytes = stream.read(buffer)) > 0) {
				resStreamOut.write(buffer, 0, readBytes);
			}
		} finally {
			if (stream != null) {
				stream.close();
			}
			if (resStreamOut != null) {
				resStreamOut.close();
			}
		}

	}

	// Converts the UI string for the selected key signature into the format LilyPond expects.
	private static String keySignatureToLilyPond(String key_sig) {
		String keySigString = "  \\key ";

		// Splitting up the two parts of the key string...
		String[] keySigParts = new String[2];
		keySigParts[1] = key_sig.substring(key_sig.length() - 5); // ...major or minor...
		keySigParts[0] = key_sig.replace(keySigParts[1], "").trim(); // ...and the key name itself.
		// Add the key's note letter.
		keySigString += keySigParts[0].substring(0, 1).toLowerCase();
		// Add sharp or flat, if any.
		if (keySigParts[0].contains("♯")) {
			keySigString += "s";
		} else if (keySigParts[0].contains("♭")) {
			keySigString += "f";
		}
		// Add " \major" or " \minor".
		keySigString += (" \\" + keySigParts[1]);

		return keySigString;
	}

	public static void executePlatformSpecificLPRender(File lilypondFile, boolean renderPNG, Runnable exitingActions) throws IOException {

		Runtime rt = Runtime.getRuntime();
		Process pr;

		if (MainApp.OS_NAME.startsWith("win")) {
			pr = rt.exec(String.format(Locale.US, "%s %s -o \"%s\" \"%s\"", MainApp.getLilyPondPath() + MainApp.getPlatformSpecificLPExecutable(), renderPNG ? "--png" : "", lilypondFile.getAbsolutePath().replace(".ly", ""), lilypondFile.getAbsolutePath()));
		} else {
			pr = rt.exec(new String[]{MainApp.getLilyPondPath() + MainApp.getPlatformSpecificLPExecutable(), renderPNG ? "--png" : "", "-o", lilypondFile.getAbsolutePath().replace(".ly", ""), lilypondFile.getAbsolutePath()});
		}

		ProcessExitDetector prExitDetector = new ProcessExitDetector(pr);
		prExitDetector.addProcessListener((process) -> new Thread(exitingActions).start());
		prExitDetector.start();
	}

	public static File[] renderChord(File lilypondFile, String fields, String keySignature, MainSceneController mainSceneView) throws IOException {
		return renderChord(lilypondFile, fields, keySignature, null, mainSceneView);
	}
	public static File[] renderChord(File lilypondFile, String fields, String keySignature, ImageView chordView) throws IOException {
		return renderChord(lilypondFile, fields, keySignature, chordView, null);
	}
	private static File[] renderChord(File lilypondFile, String fields, String keySignature, ImageView chordView, MainSceneController mainSceneView) throws IOException {

		String[] parts = fields.split("-");

		List<String> lines = Files.readAllLines(lilypondFile.toPath(), StandardCharsets.UTF_8);

		lines.set(10, LilyPondWriter.keySignatureToLilyPond(keySignature));
		lines.set(18, LilyPondWriter.parseNoteRelative(parts[PART_SOPRANO], LilyPondWriter.ADJUSTMENT_SOPRANO));
		lines.set(24, "\\with-color #(rgb-color " + (MainApp.darkModeEnabled() ?
				"0.345 0.361 0.373)" : "0.957 0.957 0.957)"));
		lines.set(34, LilyPondWriter.parseNoteRelative(parts[PART_ALTO], LilyPondWriter.ADJUSTMENT_ALTO));
		lines.set(40, LilyPondWriter.parseNoteRelative(parts[PART_TENOR], LilyPondWriter.ADJUSTMENT_TENOR));
		lines.set(46, LilyPondWriter.parseNoteRelative(parts[PART_BASS], LilyPondWriter.ADJUSTMENT_BASS));
		Files.write(lilypondFile.toPath(), lines, StandardCharsets.UTF_8);

		File outputFile = new File(lilypondFile.getAbsolutePath().replace(".ly", ".png"));
		outputFile.deleteOnExit();
		File midiFile = new File(lilypondFile.getAbsolutePath().replace(".ly",
				Objects.requireNonNull(MainApp.getPlatformSpecificMidiExtension())));
		midiFile.deleteOnExit();
		// In case of a rendering failure that leaves .ps files in the temp location, delete those files.
		File psFile = new File(lilypondFile.getAbsolutePath().replace(".ly", ".ps"));
		psFile.deleteOnExit();

		if (chordView != null) {
			LilyPondWriter.executePlatformSpecificLPRender(lilypondFile, true, () ->
					chordView.setImage(new Image(outputFile.toURI().toString())));
		} else {
			LilyPondWriter.executePlatformSpecificLPRender(lilypondFile, true, () ->
					mainSceneView.chordRendered(fields));
		}

		return new File[] {outputFile, midiFile};
	}

	public static File createTempLYChordFile(String toneFileName) {
		File tempFile = null;
		try {
			// Create the temporary file to hold the lilypond markup
			tempFile = File.createTempFile(MainApp.APP_NAME + "--"
							+ FilenameUtils.removeExtension(toneFileName) + "-",
					"-chord.ly");
			tempFile.deleteOnExit();

			LilyPondWriter.exportResource("chordTemplate.ly", tempFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return tempFile;
	}

}
