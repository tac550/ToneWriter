package com.tac550.tonewriter.io;

import com.tac550.tonewriter.model.*;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.MainApp;
import javafx.scene.control.Alert;
import org.apache.commons.io.FileUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.apache.commons.io.FilenameUtils.separatorsToSystem;

public class ProjectIO {

	private static final String RC4_KEY = "0123456789abcdef";

	private static File tempProjectDirectory;

	public static boolean saveProject(File project_file, Project project) {
		TWUtils.cleanUpAutosaves();

		// Create temp directory in which to construct the final compressed project file
		try {
			tempProjectDirectory = TWUtils.createTWTempDir("ProjectSave-" + project_file.getName());
		} catch (IOException e) {
			TWUtils.showError("Failed to create temp directory for project save!", true);
			return false;
		}

		// Add info file and save project metadata into it
		File projectInfoFile = new File(tempProjectDirectory.getAbsolutePath() + File.separator + "project");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(projectInfoFile, StandardCharsets.UTF_8))) {

			writeLine(writer, MainApp.APP_VERSION);
			writeLine(writer, project.getTitle());
			writeLine(writer, project.getItems().size());
			writeLine(writer, project.getPaperSize(), project.isNoHeader(), project.isEvenSpread());
			writeLine(writer, (Object[]) project.getMarginInfo());

		} catch (IOException e) {
			TWUtils.showError("Failed to create project metadata file!", true);
			return false;
		}

		Set<String> uniqueHashes = new HashSet<>();

		// Iterate through all the tabs, saving their configurations and saving tones if unique
		int index = 0;
		for (ProjectItem item : project.getItems()) {
			String toneHash;
			if (item.getAssociatedTone() != null) { // If the tab has a tone loaded...
				toneHash = ToneIO.getToneHash(item.getAssociatedTone());

				// Save each unique tone file into "tones" directory
				if (!uniqueHashes.contains(toneHash)) {
					uniqueHashes.add(toneHash);

					File toneSaveFile = new File(tempProjectDirectory.getAbsolutePath() + File.separator + "tones"
							+ File.separator + toneHash + File.separator + "Unsaved Tone.tone");

					if (ToneIO.createToneFile(toneSaveFile))
						ToneIO.saveToneToFile(item.getAssociatedTone(), toneSaveFile);
				}
			}

			// Place each item in a file located in "items" directory and named by tab index
			File itemSaveFile = new File(tempProjectDirectory.getAbsolutePath() + File.separator + "items"
					+ File.separator + index);

			saveItemToFile(itemSaveFile, item, project_file.getParent());

			index++;
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

	private static void saveItemToFile(File save_file, ProjectItem item, String projectSavePath) {
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
				saveItemTo(new OutputStreamWriter(os, StandardCharsets.UTF_8), item, projectSavePath);
			}

		} catch (IOException e) {
			e.printStackTrace();
			TWUtils.showError("Failed to save item \"" + item.getTitleText() + "\"", false);
		}
	}

	private static void saveItemTo(Writer destination, ProjectItem item, String parent_path) throws IOException {
		try (PrintWriter writer = new PrintWriter(destination)) {
			// Version info
			writeLine(writer, MainApp.APP_VERSION);

			String tonePath = getTonePath(item, parent_path);
			writeLine(writer, tonePath);

			writeLine(writer, ToneIO.getToneHash(item.getAssociatedTone())); // Tone hash (empty if no tone loaded)
			writeLine(writer, item.isToneEdited()); // Tone edited status
			writeLine(writer, item.getTitleText(), item.getSubtitleText()); // Title + subtitle
			writeLine(writer, item.getTitleType(), // Options
					item.isHideToneHeader(), item.isPageBreakBeforeItem(), item.getExtendedTextSelection(),
					item.isBreakExtendedTextOnlyOnBlank());
			writeLine(writer, item.getTopVersePrefix(), item.getTopVerse()); // Top verse
			writeLine(writer, TWUtils.encodeNewLines(item.getVerseAreaText())); // Verse area text
			writeLine(writer, item.getBottomVersePrefix(), item.getBottomVerse()); // Bottom verse

			// Syllables, assignment, and formatting data
			for (AssignmentLine assnLine : item.getAssignmentLines()) {
				StringBuilder line = new StringBuilder("+");

				if (assnLine.isSeparator())
					line.append("--------");
				else if (assnLine.getSelectedChantPhrase() != null)
					line.append(TWUtils.shortenPhraseName(assnLine.getSelectedChantPhrase().getName()));

				for (AssignmentSyllable syllable : assnLine.getSyllables()) {
					line.append("|").append(syllable.getSyllableText().strip());

					String formatData = syllable.getFormatData();
					if (!formatData.isEmpty())
						line.append("&").append(formatData);

					line.append(" ");

					for (AssignedChordData chordData : syllable.getAssignedChords())
						line.append(chordData.getChordIndex()).append("-").append(chordData.getDuration()).append(";");
				}

				writeLine(writer, line, assnLine.getBeforeBar(), assnLine.getAfterBar(), assnLine.isSystemBreakingDisabled());
			}

		}
	}

	// Get original tone location; relative path if built-in or in a subdirectory to project file.
	private static String getTonePath(ProjectItem item, String parent_path) {
		File originalToneFile = item.getOriginalToneFile();
		String tonePath = originalToneFile != null && originalToneFile.isFile() ? originalToneFile.getAbsolutePath() : "";
		String builtInPath = MainApp.BUILT_IN_TONE_DIR.getAbsolutePath();
		if (tonePath.startsWith(builtInPath))
			tonePath = tonePath.replace(builtInPath, "$BUILT_IN_DIR");
		else if (tonePath.startsWith(parent_path))
			tonePath = tonePath.replace(parent_path, "$PROJECT_DIR");
		return tonePath;
	}

	public static Project loadProject(File project_file) {
		Project.ProjectBuilder projectBuilder = new Project.ProjectBuilder();

		// Decrypt project file, outputting to a temp zip. This is not meant to be secure, and simply prevents
		// other programs from detecting that the file is a zip archive and displaying it as such.
		File tempZip;
		try {
			tempZip = TWUtils.createTWTempFile("Loading", "in-progress.zip");
		} catch (IOException e) {
			TWUtils.showError("Failed to create temporary zip file!", true);
			return null;
		}
		if (applyCipherFailed(project_file, tempZip)) {
			TWUtils.showError("Failed to import raw project file!", true);
			return null;
		}

		// Create temp directory to unzip project into (delete any old one first)
		deleteTempDir();
		try {
			tempProjectDirectory = TWUtils.createTWTempDir("ProjectLoad-" + project_file.getName());
		} catch (IOException e) {
			TWUtils.showError("Failed to create temp directory for project load!", true);
			return null;
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
					return null;
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
			return null;
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

			// Version checking
			// Opening project files from future minor version increases is unsupported because the lazy saving system is
			// likely to corrupt project files for users of newer versions. Patch version differences do not trigger this.
			if (TWUtils.versionCompare(projectVersion, MainApp.APP_VERSION, 2) == 1) {
				TWUtils.showAlert(Alert.AlertType.ERROR, "Error", String.format(Locale.US,
						"This project can only be opened in %s version %s.0 or newer.",
						MainApp.APP_NAME, TWUtils.truncateVersionNumber(projectVersion, 2)), true);
				return null;
			}

			projectBuilder.title(readLine(reader).get(0));
			numItems = Integer.parseInt(readLine(reader).get(0));

			// Before 1.0: no project-level paper size, spread type, or no-header option.
			if (TWUtils.versionCompare("1.0", projectVersion) != 1) {
				List<String> pageSettings = readLine(reader);
				projectBuilder.paperSize(pageSettings.get(0));
				projectBuilder.noHeader(Boolean.parseBoolean(pageSettings.get(1)));
				projectBuilder.evenSpread(Boolean.parseBoolean(pageSettings.get(2)));
			}
			// Before 1.2: no margin size settings
			if (TWUtils.versionCompare("1.2", projectVersion) != 1) {
				List<String> margins = readLine(reader);
				projectBuilder.marginInfo(margins.toArray(String[]::new));
			}
		} catch (IOException e) {
			TWUtils.showError("Failed to read project metadata file!", true);
			return null;
		}

		// Gather references to tone files
		Map<String, File> hashtoToneFile = new HashMap<>();
		File tonesDir = new File(tempProjectDirectory.getAbsolutePath() + File.separator + "tones");
		File[] toneDirs = tonesDir.listFiles(); // Directory names are the hash of contained tone.
		if (tonesDir.exists() && toneDirs != null) {
			for (File toneDir : toneDirs)
				hashtoToneFile.put(toneDir.getName(), new File(toneDir.getAbsolutePath()
						+ File.separator + "Unsaved Tone.tone"));
		}

		List<ProjectItem> items = new ArrayList<>();
		// Load however many items are in the save file
		for (int i = 0; i < numItems; i++) {
			ProjectItem.ProjectItemBuilder itemBuilder = new ProjectItem.ProjectItemBuilder();
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

				itemBuilder.originalToneFile(new File(separatorsToSystem(origToneFilePath
						.replace("$BUILT_IN_DIR", MainApp.BUILT_IN_TONE_DIR.getAbsolutePath())
						.replace("$PROJECT_DIR", project_file.getParent()))));
				String toneHash = readLine(reader).get(0);
				Tone associatedTone = null;
				if (!toneHash.isEmpty()) {
					associatedTone = ToneIO.loadTone(hashtoToneFile.get(toneHash));
					itemBuilder.toneLoadedFrom(hashtoToneFile.get(toneHash)).associatedTone(associatedTone);
				}

				itemBuilder.toneEdited(Boolean.parseBoolean(readLine(reader).get(0)));
				List<String> titleSubtitle = readLine(reader);
				itemBuilder.titleText(titleSubtitle.get(0)).subtitleText(titleSubtitle.get(1));
				List<String> options = readLine(reader);
				itemBuilder.titleType(ProjectItem.TitleType.valueOf(options.get(0).toUpperCase(Locale.ROOT)));
				itemBuilder.hideToneHeader(Boolean.parseBoolean(options.get(1)));
				itemBuilder.breakBeforeItem(Boolean.parseBoolean(options.get(2)));

				// Before 1.0: No extended text options.
				itemBuilder.extendedTextSelection(TWUtils.versionCompare("1.0", itemVersion) == 1 ? 0 : Integer.parseInt(options.get(3)));
				itemBuilder.breakExtendedTextOnlyOnBlank(TWUtils.versionCompare("1.0", itemVersion) != 1 && Boolean.parseBoolean(options.get(4)));

				List<String> topVerseData = readLine(reader);
				itemBuilder.topVersePrefix(topVerseData.get(0)).topVerse(topVerseData.get(1));
				itemBuilder.verseAreaText(TWUtils.decodeNewLines(readLine(reader).get(0)));
				List<String> bottomVerseData = readLine(reader);
				itemBuilder.bottomVersePrefix(bottomVerseData.get(0)).bottomVerse(bottomVerseData.get(1));

				List<AssignmentLine> assignmentLines = new ArrayList<>();

				List<String> lineEntry;
				boolean previousWasSeparator = false;
				while ((lineEntry = readLine(reader)).get(0).startsWith("+")) {
					AssignmentLine.AssignmentLineBuilder lineBuilder = new AssignmentLine.AssignmentLineBuilder();

					String[] syllData = lineEntry.get(0).split("\\|");

					ChantPhrase selectedChantPhrase = null;
					// assignedPhraseName = assigned phrase name (or divider indicator) without leading "+"
					String assignedPhraseName = syllData[0].substring(1);
					// If this line is a separator, add it and continue to the next line.
					if (assignedPhraseName.contains("---")) {
						assignmentLines.add(lineBuilder.separator(true).buildAssignmentLine());
						previousWasSeparator = true;
						continue;
					} else if (associatedTone != null) {
						selectedChantPhrase = associatedTone.getChantPhrases().stream().filter(p ->
								TWUtils.shortenPhraseName(p.getName()).equals(assignedPhraseName)).toList().get(0);
						lineBuilder.selectedChantPhrase(selectedChantPhrase);
					}

					List<AssignmentSyllable> syllables = new ArrayList<>();
					int startJ = 1;
					for (int j = startJ; j < syllData.length; j++) {
						AssignmentSyllable.AssignmentSyllableBuilder syllableBuilder = new AssignmentSyllable.AssignmentSyllableBuilder();
						String[] syllAndAssgmnts = syllData[j].split(" ");

						String syllable;
						if (syllAndAssgmnts.length == 0)
							syllable = syllData[j];
						else
							syllable = syllAndAssgmnts[0];

						if (syllable.contains("&")) { // Syllable formatting information is present.
							String[] syllAndFormatting = syllable.split("&");
							syllable = syllAndFormatting[0];
							if (syllAndFormatting[1].contains("b"))
								syllableBuilder.bold(true);
							if (syllAndFormatting[1].contains("i"))
								syllableBuilder.italic(true);
							if (syllAndFormatting[1].contains("h"))
								syllableBuilder.forceHyphen(true);
						}

						if (!syllable.startsWith("-") && j > startJ)
							syllable = " " + syllable;

						syllableBuilder.syllableText(syllable);

						List<AssignedChordData> chordData = new ArrayList<>();
						if (syllAndAssgmnts.length > 1 && selectedChantPhrase != null) {
							String[] chords = syllAndAssgmnts[1].split(";");
							for (String chord : chords) {
								String[] ind_dur = chord.split("-");
								chordData.add(new AssignedChordData(Integer.parseInt(ind_dur[0]), ind_dur[1]));
							}
						}
						syllableBuilder.assignedChords(chordData);
						syllables.add(syllableBuilder.buildAssignmentSyllable());
					}
					lineBuilder.syllables(syllables);

					// Before 1.0: No custom barlines or line break disabling.
					if (TWUtils.versionCompare("1.0", itemVersion) != 1)
						lineBuilder.beforeBar(previousWasSeparator && lineEntry.get(1).equals(" ") ?
										LilyPondInterface.BAR_UNCHANGED : lineEntry.get(1))
								.afterBar(lineEntry.get(2)).systemBreakDisabled(Boolean.parseBoolean(lineEntry.get(3)));

					assignmentLines.add(lineBuilder.buildAssignmentLine());
					previousWasSeparator = false;
				}
				itemBuilder.assignmentLines(assignmentLines);
				items.add(itemBuilder.buildProjectItem());

			} catch (IOException e) {
				TWUtils.showError("Failed to read item file " + i, true);
				return null;
			}
		}
		projectBuilder.items(items);

		return projectBuilder.buildProject();
	}

	private static boolean applyCipherFailed(File input, File output) {
		try (FileInputStream inputStream = new FileInputStream(input);
		     FileOutputStream outputStream = new FileOutputStream(output)) {
			Key key = new SecretKeySpec(RC4_KEY.getBytes(), "RC4");
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

	// Remove existing temporary project directory, if any.
	private static void deleteTempDir() {
		if (tempProjectDirectory != null && tempProjectDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(tempProjectDirectory);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static List<String> readLine(BufferedReader reader) throws IOException {
		String line;
		if ((line = reader.readLine()) != null)
			return List.of(line.split("\t", -1));
		else
			return List.of("");
	}

	private static void writeLine(Writer writer, Object... items) throws IOException {
		writer.write(Arrays.stream(items).map(String::valueOf).collect(Collectors.joining("\t")) + "\n");
	}

}
