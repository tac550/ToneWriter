package com.tac550.tonewriter.io;

import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.MainSceneController;
import com.tac550.tonewriter.view.TopSceneController;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ProjectIO {

	public static boolean saveFile(File project_file, TopSceneController project_scene) {
		// Delete previous save file, if one exists.
		if (project_file.exists()) {
			if (!project_file.delete()) {
				TWUtils.showError("Failed to overwrite previous save file!"
						+ "Do you have write permission in that location?", true);
				return false;
			}
		}

		// Create temp directory in which to construct the final compressed project file.
		File tempDirectory;
		try {
			tempDirectory = TWUtils.createTWTempDir("ProjectSave-" + project_scene.getProjectTitle());
		} catch (IOException e) {
			TWUtils.showError("Failed to create folder for constructing project save!", true);
			return false;
		}

		// Collect tone strings and hashes.
		Map<String, ToneReaderWriter> MD5ToToneFiles;
		try {
			MD5ToToneFiles = collectUniqueOpenToneData(project_scene.getTabControllers());
		} catch (NoSuchAlgorithmException e) {
			TWUtils.showError("Platform does not support MD5 algorithm!", true);
			return false;
		}
		// Save each unique tone file into project directory.
		MD5ToToneFiles.forEach((hash, readerWriter) -> {
			File toneFile = new File(tempDirectory.getAbsolutePath() + File.separator + "tones"
					+ File.separator + hash + File.separator + "Unsaved.tone");

			if (ToneReaderWriter.createToneFile(toneFile)) {
				readerWriter.saveToneToFile(toneFile);
			}

		});

		return true;
	}

	private static Map<String, ToneReaderWriter> collectUniqueOpenToneData(Collection<MainSceneController> tab_controllers)
			throws NoSuchAlgorithmException {
		Map<String, ToneReaderWriter> MD5ToToneString = new HashMap<>();

		// Iterate through all open tabs.
		for (MainSceneController controller : tab_controllers) {
			File toneFile = controller.getToneFile();
			if (toneFile != null) { // If the tab has a tone loaded...
				MessageDigest md = MessageDigest.getInstance("MD5");

				String toneString = controller.getToneString();

				// Record MD5 hash of the current tone data (what its file would contain if saved)
				byte[] hashBytes = md.digest(toneString.getBytes());
				StringBuilder hashBuilder = new StringBuilder();
				for (byte b : hashBytes)
					hashBuilder.append(String.format("%02x", b));

				// Map hash to the controller's ToneWriter object for easier saving later.
				MD5ToToneString.put(hashBuilder.toString(), controller.getToneWriter());

			}
		}

		return MD5ToToneString;
	}

	public static boolean openFile(File project_file) {

		return true;
	}

}
