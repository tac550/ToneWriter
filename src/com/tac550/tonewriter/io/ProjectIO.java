package com.tac550.tonewriter.io;

import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.MainSceneController;
import com.tac550.tonewriter.view.TopSceneController;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ProjectIO {

	public static boolean saveProject(File project_file, TopSceneController project_scene) {
		// Create temp directory in which to construct the final compressed project file
		File tempDirectory;
		try {
			tempDirectory = TWUtils.createTWTempDir("ProjectSave-" + project_scene.getProjectTitle());
		} catch (IOException e) {
			TWUtils.showError("Failed to create temp directory for project save!", true);
			return false;
		}

		// Add info file and save project metadata into it
		File projectInfoFile = new File(tempDirectory.getAbsolutePath() + File.separator + "project");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(projectInfoFile))) {

			writeLine(writer, project_scene.getProjectTitle());
			writeLine(writer, project_scene.getTabCount());

		} catch (IOException e) {
			TWUtils.showError("Failed to create project metadata file!", true);
			return false;
		}

		Set<String> uniqueHashes = new HashSet<>();

		// Iterate through all the tabs
		int index = 0;
		for (MainSceneController controller : project_scene.getTabControllers()) {
			File toneFile = controller.getToneFile();
			String toneHash = "";
			if (toneFile != null) { // If the tab has a tone loaded...

				ToneReaderWriter toneWriter = controller.getToneWriter();
				toneHash = toneWriter.getToneHash();

				// Save each unique tone file into "tones" directory
				if (!uniqueHashes.contains(toneHash)) {
					uniqueHashes.add(toneHash);

					File toneSaveFile = new File(tempDirectory.getAbsolutePath() + File.separator + "tones"
							+ File.separator + toneHash + File.separator + "unsaved.tone");

					if (ToneReaderWriter.createToneFile(toneSaveFile)) {
						toneWriter.saveToneToFile(toneSaveFile);
					}
				}
			}

			// Place each item in a file in "items" directory and named by tab index
			File itemSaveFile = new File(tempDirectory.getAbsolutePath() + File.separator + "items"
					+ File.separator + index);

			saveItemToFile(itemSaveFile, controller, toneHash);

			index++;
		}

		// Delete previous save file, if one exists
		if (project_file.exists()) {
			if (!project_file.delete()) {
				TWUtils.showError("Failed to overwrite previous save file!"
						+ "Do you have write permission in that location?", true);
				return false;
			}
		}

		// Compress the temp directory and save to the final location
		try (FileOutputStream fos = new FileOutputStream(project_file);
		     ZipOutputStream zos = new ZipOutputStream(fos)) {
			byte[] buffer = new byte[1024];

			for (String filePath : TWUtils.generateFileList(tempDirectory)) {
				ZipEntry ze = new ZipEntry(filePath.substring(tempDirectory.getAbsolutePath().length() + 1));
				zos.putNextEntry(ze);
				try (FileInputStream in = new FileInputStream(filePath)) {
					int len;
					while ((len = in.read(buffer)) > 0) {
						zos.write(buffer, 0, len);
					}
				}
			}

		} catch (IOException e) {
			TWUtils.showError("Failed to compress and save project file!", true);
			e.printStackTrace();
			return false;
		}

		// Try to delete the temporary directory created in the process
		try {
			FileUtils.deleteDirectory(tempDirectory);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	private static void saveItemToFile(File save_file, MainSceneController controller, String tone_hash) {
		try {
			// Create new file
			if (save_file.getParentFile().mkdirs() || save_file.getParentFile().exists()) {
				if (!save_file.createNewFile())
					throw new IOException("File creation failed");
			} else {
				throw new IOException("Directory creation failed");
			}

			// Save to the file
			try (FileWriter fileWriter = new FileWriter(save_file)) {
				saveItemTo(fileWriter, controller, tone_hash);
			}

		} catch (IOException e) {
			e.printStackTrace();
			TWUtils.showError("Failed to save item \"" + controller.getTitle() + "\"", false);
		}
	}

	private static void saveItemTo(Writer destination, MainSceneController controller, String tone_hash) throws IOException {
		try (PrintWriter writer = new PrintWriter(destination)) {

			// General item data
			File toneFile = controller.getToneFile();
			writeLine(writer, toneFile != null ? controller.getToneFile().getAbsolutePath() : "");
			writeLine(writer, tone_hash); // Tone hash (may be empty if no tone loaded)
			writeLine(writer, controller.getTitle(), controller.getSubtitle()); // Title + subtitle
			writeLine(writer, controller.getSelectedTitleOption().getText(),
					controller.getHideToneHeader(), controller.getPageBreak()); // Options line
			writeLine(writer, controller.getTopVerseChoice(), controller.getTopVerse()); // Top verse
			writeLine(writer, controller.getVerseAreaText()); // Verse area text
			writeLine(writer, controller.getBottomVerseChoice(), controller.getBottomVerse()); // Bottom verse

			// Syllables and assignment data

		}
	}

	public static boolean openProject(File project_file, TopSceneController project_scene) {

		// Create temp directory to unzip project into
		File tempDirectory;
		try {
			tempDirectory = TWUtils.createTWTempDir("ProjectLoad-" + project_scene.getProjectTitle());
		} catch (IOException e) {
			TWUtils.showError("Failed to create temp directory for project load!", true);
			return false;
		}

		// Unzip the project file
		try (FileInputStream fis = new FileInputStream(project_file);
		     ZipInputStream zis = new ZipInputStream(fis)) {
			byte[] buffer = new byte[1024];

			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				File unzippedFile = checkExtractedFile(tempDirectory, zipEntry);
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

		int numItems;

		// Gather project metadata from info file
		File projectInfoFile = new File(tempDirectory.getAbsolutePath() + File.separator + "project");
		try (BufferedReader reader = new BufferedReader(new FileReader(projectInfoFile))) {

			project_scene.setProjectTitle(readLine(reader).get(0));
			numItems = Integer.parseInt(readLine(reader).get(0));

		} catch (IOException e) {
			TWUtils.showError("Failed to read project metadata file!", true);
			return false;
		}

		// Load however many items are in the save file
		for (int i = 0; i < numItems; i++) {
			int finalI = i;
			project_scene.addTab(null, i, ctr -> ctr.setTitleText("Loaded " + (finalI + 1)));
		}

		// Try to delete the temporary directory created in the process
		try {
			FileUtils.deleteDirectory(tempDirectory);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	// Guards against a known Zip vulnerability
	private static File checkExtractedFile(File dest_dir, ZipEntry zip_entry) throws IOException {
		File destFile = new File(dest_dir, zip_entry.getName());

		String destDirPath = dest_dir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry is outside of the target dir: " + zip_entry.getName());
		}

		return destFile;
	}

	private static List<String> readLine(BufferedReader reader) throws IOException {
		String line;
		if ((line = reader.readLine()) != null)
			return List.of(line.split("\t", -1));
		else
			return List.of("");
	}

	private static void writeLine(Writer writer, Object... items) throws IOException {
		writer.write(String.join("\t", Arrays.stream(items).map(String::valueOf).toArray(String[]::new))
				+ "\n");
	}

}
