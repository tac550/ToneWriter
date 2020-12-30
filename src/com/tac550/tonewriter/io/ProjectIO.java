package com.tac550.tonewriter.io;

import com.tac550.tonewriter.model.AssignedChordData;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.*;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import org.apache.commons.io.FileUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ProjectIO {

	private static final String RC4Key = "0123456789abcdef";

	private File tempProjectDirectory;

	public boolean saveProject(File project_file, TopSceneController top_controller) {
		TWUtils.cleanUpAutosaves();

		// Create temp directory in which to construct the final compressed project file
		if (tempProjectDirectory == null || !tempProjectDirectory.exists()) {
			try {
				tempProjectDirectory = TWUtils.createTWTempDir("ProjectSave-" + project_file.getName());
			} catch (IOException e) {
				TWUtils.showError("Failed to create temp directory for project save!", true);
				return false;
			}
		} else {
			try {
				FileUtils.copyDirectory(new File(tempProjectDirectory.getAbsolutePath() + File.separator + "items"),
						new File(tempProjectDirectory.getAbsolutePath() + File.separator + "items_old"));
			} catch (IOException e) {
				TWUtils.showError("Failed to copy old items!", true);
				return false;
			}
		}

		// Add info file and save project metadata into it
		File projectInfoFile = new File(tempProjectDirectory.getAbsolutePath() + File.separator + "project");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(projectInfoFile, StandardCharsets.UTF_8))) {

			writeLine(writer, MainApp.APP_VERSION);
			writeLine(writer, top_controller.getProjectTitle());
			writeLine(writer, top_controller.getTabCount());

		} catch (IOException e) {
			TWUtils.showError("Failed to create project metadata file!", true);
			return false;
		}

		Set<String> uniqueHashes = new HashSet<>();

		// Iterate through all the tabs, saving their configurations and saving tones if unique
		int index = 0;
		for (MainSceneController controller : top_controller.getTabControllers()) {
			File toneFile = controller.getToneFile();
			String toneHash = "";
			if (toneFile != null) { // If the tab has a tone loaded...

				ToneReaderWriter toneWriter = controller.getToneWriter();
				toneHash = toneWriter.getCurrentToneHash();

				// Save each unique tone file into "tones" directory
				if (!uniqueHashes.contains(toneHash)) {
					uniqueHashes.add(toneHash);

					File toneSaveFile = new File(tempProjectDirectory.getAbsolutePath() + File.separator + "tones"
							+ File.separator + toneHash + File.separator + "Unsaved Tone.tone");

					if (ToneReaderWriter.createToneFile(toneSaveFile)) {
						toneWriter.saveToneToFile(toneSaveFile);
					}
				}
			} else if (controller.getCachedToneHash() != null) {
				uniqueHashes.add(controller.getCachedToneHash());
			}

			// Place each item in a file in "items" directory and named by tab index, but only if the item
			// has been loaded in the UI (otherwise copy its old entry into the file instead)
			File itemSaveFile = new File(tempProjectDirectory.getAbsolutePath() + File.separator + "items"
					+ File.separator + index);

			if (controller.fullyLoaded()) {
				saveItemToFile(itemSaveFile, controller, toneHash, project_file.getParent());
			} else {
				File oldItem = new File(tempProjectDirectory.getAbsolutePath() + File.separator + "items_old"
						+ File.separator + controller.getOriginalIndex());

				saveCachedItem(itemSaveFile.toPath(), oldItem.toPath());

				// Update old item index to current index value
				controller.setOriginalIndex(index);
			}

			index++;
		}

		// Delete any leftover tones (tones required to open the project that are no longer in use)
		File tonesDir = new File(tempProjectDirectory.getAbsolutePath() + File.separator + "tones");
		if (tonesDir.exists())
			try (Stream<Path> dirs = Files.list(tonesDir.toPath())) {
				dirs.filter(path -> !uniqueHashes.contains(path.getFileName().toString()))
						.forEach(path -> {
							try { FileUtils.deleteDirectory(path.toFile()); } catch (IOException e) { e.printStackTrace();
								TWUtils.showError("Failed to delete leftover tone " + path, false); }
						});
			} catch (IOException e) {
				e.printStackTrace();
				TWUtils.showError("Failed to delete leftover tone entries!", false);
			}

		// Delete any leftover items (necessary if items have been removed since last save/load)
		try (Stream<Path> files = Files.list(new File(tempProjectDirectory.getAbsolutePath()
				+ File.separator + "items").toPath())) {
			int finalIndex = index;
			files.filter(path -> Integer.parseInt(path.getFileName().toString()) >= finalIndex)
					.forEach(path -> {
						try { Files.delete(path); } catch (IOException e) { e.printStackTrace();
							TWUtils.showError("Failed to delete leftover item " + path, false); }
					});
		} catch (IOException e) {
			e.printStackTrace();
			TWUtils.showError("Failed to delete leftover item entries!", false);
		}

		// Delete items_old directory as it is no longer needed and doesn't belong in the final project file
		try {
			File oldItemsDir = new File(tempProjectDirectory.getAbsolutePath()
					+ File.separator + "items_old");
			if (oldItemsDir.exists())
				FileUtils.deleteDirectory(oldItemsDir);
		} catch (IOException e) {
			TWUtils.showError("Failed to remove old items directory!", false);
		}

		// Generate a full project source render and save it inside the project
		File lilypondFile = new File(tempProjectDirectory.getAbsolutePath() + File.separator + "render.ly");
		try {
			LilyPondInterface.saveToLilyPondFile(lilypondFile, top_controller.getProjectTitle(),
					top_controller.getTabControllers(), top_controller.getPaperSize());
		} catch (IOException e) {
			TWUtils.showError("Failed to save project render!", true);
			return false;
		}

		// Compress the temp directory and save to a temp zip file
		File tempZip;
		try {
			tempZip = TWUtils.createTWTempFile("Saving", "in-progress.zip");
		} catch (IOException e) {
			TWUtils.showError("Failed to create temporary zip file!", true);
			return false;
		}
		try (FileOutputStream fos = new FileOutputStream(tempZip);
		     ZipOutputStream zos = new ZipOutputStream(fos)) {
			byte[] buffer = new byte[1024];

			for (String filePath : TWUtils.generateFileList(tempProjectDirectory)) {
				ZipEntry ze = new ZipEntry(filePath.substring(tempProjectDirectory.getAbsolutePath().length() + 1));
				zos.putNextEntry(ze);
				try (FileInputStream in = new FileInputStream(filePath)) {
					int len;
					while ((len = in.read(buffer)) > 0) {
						zos.write(buffer, 0, len);
					}
				}
			}
		} catch (IOException e) {
			TWUtils.showError("Failed to compress project file!", true);
			e.printStackTrace();
			return false;
		}

		// Delete previous save file, if one exists
		if (project_file.exists()) {
			if (!project_file.delete()) {
				TWUtils.showError("Failed to overwrite previous save file! "
						+ "Do you have write permission in that location?", true);
				return false;
			}
		}

		// Encrypt temp zip file, outputting to final location. This is not meant to be secure, and simply prevents
		// other programs from detecting that the file is a zip archive and messing with it.
		if (applyCipherFailed(tempZip, project_file)) {
			TWUtils.showError("Failed to output final project file!", true);
			return false;
		}

		// Delete temp zip file
		if (!tempZip.delete())
			TWUtils.showError("Failed to delete temporary zip file!", false);

		return true;
	}

	private void saveCachedItem(Path save_file, Path source_file) {
		try {
			Files.copy(source_file, save_file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
			TWUtils.showError("Failed to save item from cache!", false);
		}
	}
	private void saveItemToFile(File save_file, MainSceneController controller, String tone_hash,
								String projectSavePath) {
		try {
			// Create new file
			if (!save_file.exists()) {
				if (save_file.getParentFile().mkdirs() || save_file.getParentFile().exists()) {
					if (!save_file.createNewFile())
						throw new IOException("File creation failed");
				} else {
					throw new IOException("Directory creation failed");
				}
			} else {
				if (!save_file.delete())
					throw new IOException("Failed to replace old item save file");
			}

			// Save to the file
			try (OutputStream os = new FileOutputStream(save_file)) {
				saveItemTo(new OutputStreamWriter(os, StandardCharsets.UTF_8), controller, tone_hash, projectSavePath);
			}

		} catch (IOException e) {
			e.printStackTrace();
			TWUtils.showError("Failed to save item \"" + controller.getTitle() + "\"", false);
		}
	}

	private void saveItemTo(Writer destination, MainSceneController controller, String tone_hash,
							String parent_path) throws IOException {
		try (PrintWriter writer = new PrintWriter(destination)) {

			// General item data
			File toneFile = controller.getToneFile();

			// Version info
			writeLine(writer, MainApp.APP_VERSION);

			// Original tone location; relative path if built-in or in a subdirectory to project file.
			String tonePath = toneFile != null ? toneFile.getAbsolutePath() : "";
			String builtInPath = MainApp.BUILT_IN_TONE_DIR.getAbsolutePath();
			if (tonePath.startsWith(builtInPath))
				tonePath = tonePath.replace(builtInPath, "$BUILT_IN_DIR");
			else if (tonePath.startsWith(parent_path))
				tonePath = tonePath.replace(parent_path, "$PROJECT_DIR");
			writeLine(writer, tonePath);

			writeLine(writer, tone_hash); // Tone hash (may be empty if no tone loaded)
			writeLine(writer, controller.getToneEdited()); // Tone edited status
			writeLine(writer, controller.getTitle(), controller.getSubtitle()); // Title + subtitle
			writeLine(writer, controller.getSelectedTitleOption().getText(), // Options
					controller.getHideToneHeader(), controller.getPageBreak(), controller.getExtendTextSelection());
			writeLine(writer, controller.getTopVerseChoice(), controller.getTopVerse()); // Top verse
			writeLine(writer, TWUtils.encodeNewLines(controller.getVerseAreaText())); // Verse area text
			writeLine(writer, controller.getBottomVerseChoice(), controller.getBottomVerse()); // Bottom verse

			// Syllables, assignment, and formatting data
			for (VerseLineViewController vLine : controller.getVerseLineControllers()) {
				StringBuilder line = new StringBuilder("+");

				if (vLine.isSeparator()) {
					line.append("--------");
				} else {
					line.append(vLine.getTonePhraseChoice());
				}

				for (SyllableText syllable : vLine.getSyllables()) {
					line.append("|").append(syllable.getText().strip());

					String formatData = syllable.getFormatData();
					if (formatData.length() > 0)
						line.append("&").append(formatData);

					line.append(" ");

					for (AssignedChordData chordData : syllable.getAssociatedChords()) {
						line.append(chordData.getChordIndex()).append("-").append(chordData.getDuration()).append(";");
					}
				}

				writeLine(writer, line);
			}

		}
	}

	public boolean openProject(File project_file, TopSceneController top_controller) {
		// Decrypt project file, outputting to a temp zip. This is not meant to be secure, and simply prevents
		// other programs from detecting that the file is a zip archive and displaying it as such.
		File tempZip;
		try {
			tempZip = TWUtils.createTWTempFile("Loading", "in-progress.zip");
		} catch (IOException e) {
			TWUtils.showError("Failed to create temporary zip file!", true);
			return false;
		}
		if (applyCipherFailed(project_file, tempZip)) {
			TWUtils.showError("Failed to import raw project file!", true);
			return false;
		}

		deleteTempDir();

		// Create temp directory to unzip project into
		try {
			tempProjectDirectory = TWUtils.createTWTempDir("ProjectLoad-" + project_file.getName());
		} catch (IOException e) {
			TWUtils.showError("Failed to create temp directory for project load!", true);
			return false;
		}

		// Unzip the project file
		try (FileInputStream fis = new FileInputStream(tempZip);
		     ZipInputStream zis = new ZipInputStream(fis)) {
			byte[] buffer = new byte[1024];

			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				File unzippedFile = checkExtractionDestination(tempProjectDirectory, zipEntry);
				if (!unzippedFile.getParentFile().mkdirs() && !unzippedFile.getParentFile().exists()) {
					TWUtils.showError("Failed to construct internal temp directory!", true);
					return false;
				}
				try (FileOutputStream fos = new FileOutputStream(unzippedFile)) {
					int len;
					while ((len = zis.read(buffer)) > 0)
						fos.write(buffer, 0, len);
				} catch (IOException e) {
					e.printStackTrace();
					TWUtils.showError("Failed to extract file " + zipEntry.getName(), true);
				}

				zipEntry = zis.getNextEntry();
			}

		} catch (IOException e) {
			TWUtils.showError("Failed to extract project file!", true);
			return false;
		}

		// Delete temp zip file
		if (!tempZip.delete())
			TWUtils.showError("Failed to delete temporary zip file!", false);

		// Gather project metadata from info file
		int numItems;
		String projectVersion;
		File projectInfoFile = new File(tempProjectDirectory.getAbsolutePath() + File.separator + "project");
		try (BufferedReader reader = new BufferedReader(new FileReader(projectInfoFile, StandardCharsets.UTF_8))) {

			projectVersion = readLine(reader).get(0);
			top_controller.setProjectTitle(readLine(reader).get(0));
			numItems = Integer.parseInt(readLine(reader).get(0));

		} catch (IOException e) {
			TWUtils.showError("Failed to read project metadata file!", true);
			return false;
		}

		// Opening project files from future versions is unsupported because the lazy saving system is likely to corrupt
		// project files for users of newer versions.
		if (TWUtils.versionCompare(projectVersion, MainApp.APP_VERSION) == 1) {
			TWUtils.showAlert(Alert.AlertType.INFORMATION, "Warning", String.format(Locale.US,
					"This project can only be opened in %s version %s or newer.",
					MainApp.APP_NAME, projectVersion), true);
			return false;
		}

		// Gather references to tone files
		Map<String, File> hashtoToneFile = new HashMap<>();
		File tonesDir = new File(tempProjectDirectory.getAbsolutePath() + File.separator + "tones");
		File[] toneDirs = tonesDir.listFiles();
		if (tonesDir.exists() && toneDirs != null) {
			for (File toneDir : toneDirs) {
				hashtoToneFile.put(toneDir.getName(), new File(toneDir.getAbsolutePath()
						+ File.separator + "Unsaved Tone.tone"));
			}
		}

		// Collect each item's LilyPond source output
		File lilyPondFile = new File(tempProjectDirectory.getAbsolutePath() + File.separator + "render.ly");
		String LPSource;
		try {
			LPSource = Files.readString(lilyPondFile.toPath());
		} catch (IOException e) {
			TWUtils.showError("Failed to read saved LilyPond render!", true);
			return false;
		}
		String[] itemSources = LPSource.split("(?=\n%.*\n)");

		// Adjust loaded cached item sources for changes in output rules
		// Pre-0.9: Remove negative vspace from bottom verse, if any.
		if (TWUtils.versionCompare(projectVersion, "0.9") == 2)
			itemSources = Arrays.stream(itemSources)
					.map(item -> item.replace("  \\vspace #-1", ""))
					.toArray(String[]::new);

		// Load however many items are in the save file
		for (int i = 0; i < numItems; i++) {
			File itemFile = new File(tempProjectDirectory.getAbsolutePath() + File.separator + "items"
					+ File.separator + i);
			try (BufferedReader reader = new BufferedReader((new FileReader(itemFile, StandardCharsets.UTF_8)))) {

				String itemVersion;
				String origToneFilePath;

				// If the first entry in the item is not a version number, we have a pre-1.0 project file.
				String firstLine = readLine(reader).get(0);
				if (Pattern.compile("^(\\d+\\.)?(\\d+\\.)?(\\*|\\d+)$").matcher(firstLine).matches()) {
					itemVersion = firstLine;
					origToneFilePath = readLine(reader).get(0);
				} else {
					itemVersion = "0.9";
					origToneFilePath = firstLine;
				}

				File originalToneFile = new File(origToneFilePath
						.replace("$BUILT_IN_DIR", MainApp.BUILT_IN_TONE_DIR.getAbsolutePath())
						.replace("$PROJECT_DIR", project_file.getParent()));
				String toneHash = readLine(reader).get(0);
				boolean edited = Boolean.parseBoolean(readLine(reader).get(0));
				List<String> titleSubtitle = readLine(reader);
				List<String> options = readLine(reader);
				List<String> topVerse = readLine(reader);
				String verseAreaText = TWUtils.decodeNewLines(readLine(reader).get(0));
				List<String> bottomVerse = readLine(reader);

				List<String> assignedPhrases = new ArrayList<>();
				List<List<String>> syllableLines = new ArrayList<>();
				List<List<String>> formatLines = new ArrayList<>();
				List<List<String>> assignmentLines = new ArrayList<>();

				String assignmentLine;
				while ((assignmentLine = readLine(reader).get(0)).startsWith("+")) {
					List<String> lineSyllables = new ArrayList<>();
					List<String> lineFormatting = new ArrayList<>();
					List<String> lineAssignments = new ArrayList<>();

					String[] data = assignmentLine.split("\\|");

					// data[0] contains meta info about the line.
					assignedPhrases.add(data[0].substring(1));

					// Iterate through data[1] -> data[data.length - 1]
					for (int j = 1; j < data.length; j++) {
						String[] parts = data[j].split(" ");

						String syllable;
						if (parts.length == 0)
							syllable = data[j];
						else
							syllable = parts[0];

						if (syllable.contains("&")) { // There is appended formatting data
							String[] syllData = syllable.split("&");
							syllable = syllData[0];
							lineFormatting.add(syllData[1]);
						} else {
							lineFormatting.add("");
						}

						if (!syllable.startsWith("-"))
							syllable = " " + syllable;
						String assignment = parts.length > 1 ? parts[1] : "";

						lineSyllables.add(syllable);
						lineAssignments.add(assignment);
					}

					syllableLines.add(lineSyllables);
					formatLines.add(lineFormatting);
					assignmentLines.add(lineAssignments);
				}

				// Create and set up item tab
				int finalI = i;
				top_controller.addTab(titleSubtitle.get(0), i, itemSources[i + 1], toneHash, ctr -> {
					final boolean projectEditedState = top_controller.getProjectEdited();

					if (!toneHash.isEmpty())
						ctr.handleOpenTone(hashtoToneFile.get(toneHash), true, false);

					try {
						if (originalToneFile.exists() && (ctr.getToneWriter().loadedToneSimilarTo(originalToneFile)
								|| edited)) {
							ctr.swapToneFile(originalToneFile);
						}
					} catch (IOException e) {
						TWUtils.showError("Failed to compare original tone file", false);
						e.printStackTrace();
					}

					if (edited)
						ctr.toneEdited(false);

					ctr.setSubtitle(titleSubtitle.get(1));

					// Before 1.0: No extended text option.
					int extText = TWUtils.versionCompare("1.0", itemVersion) == 1 ? 0 : Integer.parseInt(options.get(3));

					ctr.setOptions(options.get(0), Boolean.parseBoolean(options.get(1)),
							Boolean.parseBoolean(options.get(2)), extText);

					ctr.setTopVerseChoice(topVerse.get(0));
					ctr.setTopVerse(topVerse.get(1));
					ctr.setVerseAreaText(verseAreaText);
					ctr.setBottomVerseChoice(bottomVerse.get(0));
					ctr.setBottomVerse(bottomVerse.get(1));

					for (int j = 0; j < syllableLines.size(); j++) {
						List<String> sylls = syllableLines.get(j);
						List<String> formatting = formatLines.get(j);
						List<String> assigns = assignmentLines.get(j);

						// Create verse line with provided syllable data and save a reference to its controller
						Task<FXMLLoader> verseLineLoader = ctr.createVerseLine(String.join("", sylls));
						VerseLineViewController verseLine = null;
						try {
							verseLine = verseLineLoader.get().getController();
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
						assert verseLine != null;

						int finalJ = j;
						verseLine.setPendingActions(finalI == 0, vLine -> {
							List<String> durations = new ArrayList<>();

							vLine.setTonePhraseChoice(assignedPhrases.get(finalJ));

							// Apply syllable formatting.
							for (int k = 0; k < formatting.size(); k++) {
								vLine.getSyllables()[k].setBold(formatting.get(k).contains("b"));
								vLine.getSyllables()[k].setItalic(formatting.get(k).contains("i"));
							}

							// Assign and/or skip chords as would be done by a user.
							int startSyll = 0;
							int lastChordIndex = 0;
							boolean prevWasEmpty = true;

							outer:
							for (int k = 0; k < assigns.size(); k++) {
								String currAssigns = assigns.get(k);
								if (currAssigns.isEmpty()) {
									if (!prevWasEmpty) {
										vLine.assignChordSilently(startSyll, k - 1);
										startSyll = k;
										lastChordIndex++;
									}

									startSyll++;
									prevWasEmpty = true;
									continue;
								} else {
									prevWasEmpty = false;
								}

								String[] chords = currAssigns.split(";");
								int chordNum = 0;
								for (String chord : chords) {
									String[] ind_dur = chord.split("-");
									int chordIndex = Integer.parseInt(ind_dur[0]);
									durations.add(ind_dur[1]);
									if (chordIndex > lastChordIndex) {

										vLine.assignChordSilently(startSyll, chordNum - 1 >= 0
												&& Integer.parseInt(chords[chordNum - 1].split("-")[0])
												- lastChordIndex < 1 ? k : k - 1);

										while (lastChordIndex < chordIndex - 1) {
											vLine.skipChord();
											lastChordIndex++;
										}

										lastChordIndex++;
										startSyll = k;

									}
									if (chordNum == chords.length - 1 && assigns.stream()
											.skip(k + 1).allMatch(String::isEmpty)) {
										while (lastChordIndex < chordIndex) {
											vLine.skipChord();
											lastChordIndex++;
										}
										vLine.assignChordSilently(startSyll, k);
										break outer;
									}

									chordNum++;
								}
							}

							vLine.setAssignmentDurations(durations);
						});
					}

					ctr.applyLoadedVerses(finalI != 0 && projectEditedState);
				}, true);

			} catch (IOException e) {
				TWUtils.showError("Failed to read item file " + i, true);
				return false;
			}

		}

		TWUtils.cleanUpAutosaves();
		return true;
	}

	private static boolean applyCipherFailed(File input, File output) {
		try (FileInputStream inputStream = new FileInputStream(input);
		     FileOutputStream outputStream = new FileOutputStream(output)) {
			Key key = new SecretKeySpec(RC4Key.getBytes(), "RC4");
			Cipher cipher = Cipher.getInstance("RC4");
			cipher.init(Cipher.DECRYPT_MODE, key);

			byte[] inputBytes = new byte[(int) input.length()];
			//noinspection ResultOfMethodCallIgnored
			inputStream.read(inputBytes);

			byte[] outputBytes = cipher.doFinal(inputBytes);
			outputStream.write(outputBytes);

		} catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
				| BadPaddingException | IllegalBlockSizeException e) {
			return true;
		}

		return false;
	}

	// Guards against a known Zip vulnerability
	private static File checkExtractionDestination(File dest_dir, ZipEntry zip_entry) throws IOException {
		File destFile = new File(dest_dir, zip_entry.getName());

		String destDirPath = dest_dir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry is outside of the target dir: " + zip_entry.getName());
		}

		return destFile;
	}

	// Remove existing temp directory, if any.
	private void deleteTempDir() {
		if (tempProjectDirectory != null && tempProjectDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(tempProjectDirectory);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private List<String> readLine(BufferedReader reader) throws IOException {
		String line;
		if ((line = reader.readLine()) != null)
			return List.of(line.split("\t", -1));
		else
			return List.of("");
	}

	private void writeLine(Writer writer, Object... items) throws IOException {
		writer.write(Arrays.stream(items).map(String::valueOf).collect(Collectors.joining("\t")) + "\n");
	}

}
