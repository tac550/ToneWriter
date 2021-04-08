package com.tac550.tonewriter.io;

import com.tac550.tonewriter.view.MainApp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class QuickVerseIO {

	public static List<String> getBuiltinVerses() throws IOException {
		List<String> finalList = new ArrayList<>();

		try (InputStream stream = LilyPondInterface.class.getResourceAsStream(MainApp.prefs.getBoolean(MainApp.PREFS_THOU_THY, false) ? "quickVersesTT.txt" : "quickVersesYY.txt");
			 InputStreamReader inputReader = new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8);
			 BufferedReader bufferedReader = new BufferedReader(inputReader)) {
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				if (!line.isEmpty() && !line.startsWith("#")) {
					finalList.add(line);
				}
			}
		}

		return finalList;
	}

	public static List<String> getCustomVerses() throws IOException {
		List<String> finalList = new ArrayList<>();

		File verseFile = getPlatformSpecificVerseFile();

		// If verse file doesn't exist, just return the empty final list.
		if (verseFile == null || !verseFile.exists()) {
			return finalList;
		}

		try (FileReader fileReader = new FileReader(verseFile, StandardCharsets.UTF_8);
		     BufferedReader bufferedReader = new BufferedReader(fileReader)) {
			String line;

			while ((line = bufferedReader.readLine()) != null) {
				if (!line.isEmpty() && !line.startsWith("#")) {
					finalList.add(line);
				}
			}
		}

		return finalList;
	}

	public static void addCustomVerse(String verse) throws IOException {
		File verseFile = getPlatformSpecificVerseFile();

		if (verseFile == null) throw new IOException("Failed to get verse storage file!");

		// Create custom verse file if it doesn't already exist.
		if (!verseFile.getParentFile().exists()) {
			if (!(verseFile.getParentFile().mkdirs())) {
				throw new IOException("Failed to create directory for new verse storage file!");
			}
		}

		if (!verseFile.exists()) {
			if (!verseFile.createNewFile()) {
				throw new IOException("Failed to create new verse storage file!");
			}
		}

		// Write the new custom verse to the file.
		Files.write(verseFile.toPath(), Collections.singletonList(verse), StandardOpenOption.APPEND);
	}

	public static boolean removeCustomVerse(String verse) throws IOException {
		File verseFile = getPlatformSpecificVerseFile();

		if (verseFile == null || !verseFile.exists()) {
			return false;
		}

		File tempFile = new File(verseFile.getParent() + File.separator + "TEMP");

		boolean removed = false;
		try (BufferedReader reader = new BufferedReader(new FileReader(verseFile, StandardCharsets.UTF_8));
		     BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
			// Only skip the removed line once (only one is removed even if there are multiple lines with same text).
			String currentLine;
			while ((currentLine = reader.readLine()) != null) {
				if (currentLine.trim().equals(verse.trim()) && !removed) {
					removed = true;
					continue; // Skips the following writer.write() line for the item to be removed (removes only one line)
				}
				writer.write(currentLine + System.getProperty("line.separator"));
			}
		}

		if (!(verseFile.delete() && tempFile.renameTo(verseFile))) {
			throw new IOException("Failed to delete or rename verse file!");
		}

		// Only return true if a line was removed (otherwise it must have been a built-in verse)
		return removed;
	}

	private static File getPlatformSpecificVerseFile() {
		String fileNameString = File.separator + MainApp.APP_NAME + File.separator +
				(MainApp.prefs.getBoolean(MainApp.PREFS_THOU_THY, false) ? "CustomVersesTT.txt" : "CustomVersesYY.txt");

		if (MainApp.OS_NAME.startsWith("win")) {
			return new File(System.getenv("APPDATA") + fileNameString);
		} if (MainApp.OS_NAME.startsWith("mac")) {
			return new File(System.getProperty("user.home") + "/Library/Preferences" + fileNameString);
		} if (MainApp.OS_NAME.startsWith("lin")) {
			return new File(System.getProperty("user.home") + "/.config" + fileNameString);
		} else return null;
	}

}
