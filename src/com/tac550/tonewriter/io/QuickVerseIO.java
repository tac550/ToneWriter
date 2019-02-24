package com.tac550.tonewriter.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;

import com.tac550.tonewriter.view.MainApp;

public class QuickVerseIO {

	public static ArrayList<String> getBuiltinVerses() throws IOException {
		ArrayList<String> finalList = new ArrayList<>();
		
		InputStream stream = LilyPondWriter.class.getResourceAsStream(MainApp.prefs.getBoolean(MainApp.PREFS_THOU_THY_ENABLED, false) ? "quickVersesTT.txt" : "quickVersesYY.txt");
		InputStreamReader inputReader = new InputStreamReader(stream);
		BufferedReader bufferedReader = new BufferedReader(inputReader);
		
		String line;
		
		while ( (line = bufferedReader.readLine()) != null) {
			if (!line.isEmpty() && !line.startsWith("#")) {
				finalList.add(line);	
			}
		}
		
		bufferedReader.close();
		inputReader.close();
		stream.close();
		
		return finalList;
	}
	
	public static ArrayList<String> getCustomVerses() throws IOException {
		ArrayList<String> finalList = new ArrayList<>();
		
		File verseFile = new File("");
		
		String osName = System.getProperty("os.name").toLowerCase();
		String fileNameString = (MainApp.prefs.getBoolean(MainApp.PREFS_THOU_THY_ENABLED, false) ? "CustomVersesTT.txt" : "CustomVersesYY.txt");
		// Set the verse file path, which depends on the platform.
		if (osName.startsWith("win")) {
			verseFile = new File(System.getenv("APPDATA") + File.separator + MainApp.APPNAME + File.separator + fileNameString);
		} if (osName.startsWith("mac")) {
			verseFile = new File(System.getProperty("user.home") + "/Library/Preferences/" + MainApp.APPNAME + File.separator + fileNameString);
			System.out.println(verseFile.getAbsolutePath());
		} if (osName.startsWith("lin")) {
			// TODO: UNKNOWN
		}
		
		// If verse file doesn't exist, just return the empty final list.
		if (!verseFile.exists()) {
			return finalList;
		}
		
		FileReader fileReader = new FileReader(verseFile);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		
		String line;
		
		while ((line = bufferedReader.readLine()) != null) {
			if (!line.isEmpty() && !line.startsWith("#")) {
				finalList.add(line);	
			}
		}
		
		bufferedReader.close();
		fileReader.close();
		
		return finalList;
	}
	
	public static void addCustomVerse(String verse) throws IOException {
		File verseFile = new File("");
		
		String osName = System.getProperty("os.name").toLowerCase();
		String fileNameString = (MainApp.prefs.getBoolean(MainApp.PREFS_THOU_THY_ENABLED, false) ? "CustomVersesTT.txt" : "CustomVersesYY.txt");
		// Set the verse file path, which depends on the platform.
		if (osName.startsWith("win")) {
			verseFile = new File(System.getenv("APPDATA") + File.separator + MainApp.APPNAME + File.separator + fileNameString);
		} if (osName.startsWith("mac")) {
			verseFile = new File(System.getProperty("user.home") + "/Library/Preferences/" + MainApp.APPNAME + File.separator + fileNameString);
		} if (osName.startsWith("lin")) {
			// TODO: UNKNOWN
		}
		
		// Create custom verse file if it doesn't already exist.
		if (!verseFile.exists()) {
			verseFile.getParentFile().mkdirs();
			verseFile.createNewFile();
		}
		
		// Write the new custom verse to the file.
		Files.write(verseFile.toPath(), Arrays.asList(verse), StandardOpenOption.APPEND);
	}
	
	public static boolean removeCustomVerse(String verse) throws IOException {
		File verseFile = new File("");
		
		String osName = System.getProperty("os.name").toLowerCase();
		String fileNameString = (MainApp.prefs.getBoolean(MainApp.PREFS_THOU_THY_ENABLED, false) ? "CustomVersesTT.txt" : "CustomVersesYY.txt");
		// Set the verse file path, which depends on the platform.
		if (osName.startsWith("win")) {
			verseFile = new File(System.getenv("APPDATA") + File.separator + MainApp.APPNAME + File.separator + fileNameString);
		} if (osName.startsWith("mac")) {
			verseFile = new File(System.getProperty("user.home") + "/Library/Preferences/" + MainApp.APPNAME + File.separator + fileNameString);
		} if (osName.startsWith("lin")) {
			// TODO: UNKNOWN
		}
		
		if (!verseFile.exists()) {
			return false;
		}
		
		File tempFile = new File(verseFile.getParent() + File.separator + "TEMP");

		BufferedReader reader = new BufferedReader(new FileReader(verseFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

		// Only skip the removed line once (only one is removed even if there are multiple lines with same text).
		String currentLine;
		boolean removed = false;
		while ((currentLine = reader.readLine()) != null) {
		    if (currentLine.trim().equals(verse) && !removed) {
		    	removed = true;
		    	continue;
		    }
		    writer.write(currentLine + System.getProperty("line.separator"));
		}
		
		writer.close(); 
		reader.close();
		
		verseFile.delete();
		tempFile.renameTo(verseFile);
		
		// Only return true if a line was removed (otherise it must have been a built-in verse)
		return removed;
	}
	
}
