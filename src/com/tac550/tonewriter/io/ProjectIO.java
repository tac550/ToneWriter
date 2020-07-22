package com.tac550.tonewriter.io;

import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.MainSceneController;
import com.tac550.tonewriter.view.TopSceneController;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class ProjectIO {

	public static boolean saveProject(File project_file, TopSceneController project_scene) {
		// Create temp directory in which to construct the final compressed project file.
		File tempDirectory;
		try {
			tempDirectory = TWUtils.createTWTempDir("ProjectSave-" + project_scene.getProjectTitle());
		} catch (IOException e) {
			TWUtils.showError("Failed to create folder for constructing project save!", true);
			return false;
		}

		// Add info file to save project metadata
		File projectInfoFile = new File(tempDirectory.getAbsolutePath() + File.separator + "project");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(projectInfoFile))) {

			writer.write(project_scene.getProjectTitle() + "\n");
			writer.write(project_scene.getTabCount() + "\n");

		} catch (IOException e) {
			TWUtils.showError("Failed to create project metadata file!", true);
			return false;
		}

		// Collect tone strings and hashes.
		Set<String> uniqueHashes = new HashSet<>();

		// Iterate through all the tabs
		int index = 0;
		for (MainSceneController controller : project_scene.getTabControllers()) {
			File toneFile = controller.getToneFile();
			if (toneFile != null) { // If the tab has a tone loaded...

				ToneReaderWriter toneWriter = controller.getToneWriter();
				String toneHash = toneWriter.getToneHash();

				// Save each tone file into project directory if unique.
				if (!uniqueHashes.contains(toneHash)) {
					uniqueHashes.add(toneHash);

					File toneSaveFile = new File(tempDirectory.getAbsolutePath() + File.separator + "tones"
							+ File.separator + toneHash + File.separator + "unsaved.tone");

					if (ToneReaderWriter.createToneFile(toneSaveFile)) {
						toneWriter.saveToneToFile(toneSaveFile);
					}

				}

				// Place each item in a file in "items" directory named by index.
				File itemSaveFile = new File(tempDirectory.getAbsolutePath() + File.separator + "items"
						+ File.separator + index);

				saveItemToFile(itemSaveFile, controller);

			}

			index++;
		}

		// Compress the temp directory. (and rename it?)

		// Delete previous save file, if one exists.
		if (project_file.exists()) {
			if (!project_file.delete()) {
				TWUtils.showError("Failed to overwrite previous save file!"
						+ "Do you have write permission in that location?", true);
				return false;
			}
		}

		// Move (and rename?) temp project file to final location.

		return true;
	}

	private static void saveItemToFile(File save_file, MainSceneController controller) {
		try {
			// Create new file
			if (save_file.getParentFile().mkdirs() || save_file.getParentFile().exists()) {
				if (!save_file.createNewFile())
					throw new IOException("File creation failed");
			} else {
				throw new IOException("Directory creation failed");
			}

			// Save to the file
			FileWriter fileWriter = new FileWriter(save_file);
			saveItemTo(fileWriter, controller);
			fileWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
			TWUtils.showError("Failed to save item \"" + controller.getTitle() + "\"", false);
		}
	}

	private static void saveItemTo(Writer destination, MainSceneController controller) {
		PrintWriter printWriter = new PrintWriter(destination);

		// General item metadata
		printWriter.println(controller.getTitle() + "\t" + controller.getSubtitle());

		printWriter.close();
	}

	public static boolean openProject(File project_file) {

		return true;
	}

}
