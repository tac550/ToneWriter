package com.tac550.tonewriter.util;

import com.tac550.tonewriter.io.LilyPondInterface;
import com.tac550.tonewriter.view.MainApp;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextFormatter;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TWUtils {

	// Color

	// For converting Color objects to strings used to style UI elements.
    public static String toRGBCode(Color color) {
        return String.format(Locale.US,
		        "#%02X%02X%02X",
		        (int) (color.getRed() * 255),
		        (int) (color.getGreen() * 255),
		        (int) (color.getBlue() * 255));
    }

    public static Color getUIBaseColor() {
    	return MainApp.isDarkModeEnabled() ? new Color(0.345, 0.361, 0.373, 1)
			    : new Color(0.957, 0.957, 0.957, 1);
    }

	// Strings

	private static final String LEFT_DOUBLE_QUOTE = "\u201C";
	private static final String RIGHT_DOUBLE_QUOTE = "\u201D";
	public static final String LEFT_APOSTROPHE = "\u2018";
	public static final String APOSTROPHE = "\u2019";
	public static final String SHARP = "\u266F";
	public static final String FLAT = "\u266D";

	public static String truncateVersionNumber(String version, int len_limit) {
		return Stream.of(version.split("\\.")).limit(len_limit).collect(Collectors.joining("."));
	}

	public static String convertAccidentalSymbols(String string) {
		// The extra space in the replacement for TWUtils.FLAT ("f ") in the key signature is necessary
		// because, for purposes of display, the original string doesn't include a space after the symbol.
		return string.replace(SHARP, "s").replace(FLAT, "f ");
	}

	/**
	 * Compares two version strings.
	 * <p> Use this instead of String.compareTo() for a non-lexicographical
	 * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
	 *
	 * @param v1 a string of alpha numerals separated by decimal points.
	 * @param v2 a string of alpha numerals separated by decimal points.
	 * @param len_limit Maximum number of decimal-separated numerals to compare. Negative value signals no limit.
	 * @return The result is 0 if the versions are equal up to len_limit or len_limit is 0.
	 *         The result is 1 if v1 is greater than v2 up to len_limit.
	 *         The result is 2 if v2 is greater than v1 up to len_limit.
	 *         The result is -1 if the version format is unrecognized.
	 */
	public static int versionCompare(String v1, String v2, int len_limit) {

		if (len_limit > -1) {
			v1 = truncateVersionNumber(v1, len_limit);
			v2 = truncateVersionNumber(v2, len_limit);
		}

		int v1Len = StringUtils.countMatches(v1,".");
		int v2Len = StringUtils.countMatches(v2,".");

		if (v1Len != v2Len) {
			int count = Math.abs(v1Len-v2Len);
			if (v1Len > v2Len) {
				v2 = v2 + ".0".repeat(Math.max(0, count));
			} else {
				v1 = v1 + ".0".repeat(Math.max(0, count));
			}
		}

		if (v1.equals(v2)) return 0;

		String[] v1Str = StringUtils.split(v1, ".");
		String[] v2Str = StringUtils.split(v2, ".");
		for (int i = 0; i < v1Str.length; i++) {
			StringBuilder str1 = new StringBuilder();
			StringBuilder str2 = new StringBuilder();
			for (char c : v1Str[i].toCharArray()) {
				if (Character.isLetter(c)) {
					int u = c - 'a' + 1;
					if (u < 10)
						str1.append("0").append(u);
					else str1.append(u);
				} else str1.append(c);
			}
			for (char c : v2Str[i].toCharArray()) {
				if (Character.isLetter(c)) {
					int u = c - 'a' + 1;
					if (u < 10)
						str2.append("0").append(u);
					else str2.append(u);
				} else str2.append(c);
			}
			v1Str[i] = "1" + str1;
			v2Str[i] = "1" + str2;

			int num1 = Integer.parseInt(v1Str[i]);
			int num2 = Integer.parseInt(v2Str[i]);

			if (num1 != num2) {
				if (num1 > num2) return 1;
				else return 2;
			}
		}

		return -1;
	}
	/**
	 * Overloaded {@link #versionCompare(String,String,int)} which always compares the entirety of the version strings.
	 * @see TWUtils#versionCompare(String,String,int)
	 */
	public static int versionCompare(String v1, String v2) {
		return versionCompare(v1, v2, -1);
	}

	public static String encodeNewLines(String original) {
		return original.replace("/", "<%47>").replace("\n", "/n");
	}
	public static String decodeNewLines(String encoded) {
		return encoded.replace("/n", "\n").replace("<%47>", "/");
	}

	public static String replaceInvalidFileChars(String text, String replacement) {
		return text.replaceAll("[\\\\/:*?\"<>|]", replacement);
	}

	public static String replaceLast(String text, String regex, String replacement) {
		return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
	}

	public static String applySmartQuotes(String text) {
		Pattern pattern = Pattern.compile("\"|'");
		Matcher matcher = pattern.matcher(text);

		List<Character> punctuation = List.of('.', ',', '!', '?', ';', '\'',
				RIGHT_DOUBLE_QUOTE.charAt(0), APOSTROPHE.charAt(0));

		int lastPos;
		while (matcher.find()) {
			lastPos = matcher.end();

			char matching_char = text.charAt(lastPos - 1);

			if (matching_char == '\"') {
				if (lastPos < 2 || lastPos - 1 > text.length())
					text = text.replaceFirst("\"", LEFT_DOUBLE_QUOTE);
				else if (!Character.isLetterOrDigit(text.charAt(lastPos - 2))
						&& !punctuation.contains(text.charAt(lastPos - 2)))
					text = text.replaceFirst("\"", LEFT_DOUBLE_QUOTE);
				else
					text = text.replaceFirst("\"", RIGHT_DOUBLE_QUOTE);
			} else if (matching_char == '\'') {
				if (lastPos < 2 || lastPos - 1 > text.length())
					text = text.replaceFirst("'", LEFT_APOSTROPHE);
				else if (!Character.isLetterOrDigit(text.charAt(lastPos - 2))
						&& !punctuation.contains(text.charAt(lastPos - 2)))
					text = text.replaceFirst("'", LEFT_APOSTROPHE);
				else
					text = text.replaceFirst("'", APOSTROPHE);
			}
		}

		text = text.replaceAll("\\\\[\"\u201C\u201D]", "\""); // \u201C\u201D = “”

		return text;
	}
	public static String reverseSmartQuotes(String text) {
		return text.replace("\"", "\\\"").replace("\\\\", "\\")
				.replaceAll("[\u201C\u201D]", "\""); // \u201C\u201D = “”
	}

	// Notation

	public static String addDurations(String curr, String next) {
		float durCurrent = Float.parseFloat(curr.replaceAll("\\D", ""));
		float durNext = Float.parseFloat(next.replaceAll("\\D", ""));

		// Inverse durations (so they read as fractions of a whole note in x/4 time).
		durCurrent = 1 / durCurrent;
		durNext = 1 / durNext;

		// Increase the duration by half if the note is dotted.
		if (curr.contains(".")) durCurrent += (durCurrent / 2);
		if (next.contains(".")) durNext += (durNext / 2);

		// Add the note durations and inverse again to return to LilyPond's duration format.
		float newDurFloat = 1 / (durCurrent + durNext);

		String newDur;

		// If the note combination process yielded a whole number...
		if (newDurFloat % 1 == 0) {
			// We just take it as the new duration and continue to the return statement.
			newDur = String.valueOf((int) newDurFloat);
		} else { // If a fractional number resulted it may be covered by a special case.
			// Invert the computed duration.
			float inverse = 1 / newDurFloat;

			if (inverse == 0.75) // Dotted half
				newDur = "2.";
			else if (inverse == 0.375) // Dotted quarter
				newDur = "4.";
			else
				newDur = null;
		}

		return newDur;
	}

	// Filesystem

	public static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));

		return new String(encoded, encoding);
	}

	// Creates and returns a temp file which will be recognized by the automatic temp cleaner
	public static File createTWTempFile(String prefix, String suffix) throws IOException {
		return Files.createTempFile(MainApp.APP_NAME + "-" +
				(prefix.isEmpty() ? "" : prefix + "-"),
				suffix.isEmpty() ? "" : (suffix.startsWith(".") ? "" : "-") + suffix).toFile();
	}
	public static File createTWTempDir(String prefix) throws IOException {
		return Files.createTempDirectory(MainApp.APP_NAME + "-" +
						(prefix.isEmpty() ? "" : prefix + "-")).toFile();
	}

	// Cleans up all ToneWriter temp files, but only if this is the only active instance
	public static void cleanUpAllTempFiles() {
		if (noOtherAppInstanceRunning())
			cleanUpTempFiles("");
	}
	public static void cleanUpAutosaves() {
		cleanUpTempFiles("-Autosave");
	}
	public static void cleanUpTempFiles(String with_postfix) {
		File[] files = new File(System.getProperty("java.io.tmpdir")).listFiles();
		if (files == null) return;
		for (File file : files) {
			String fileNameNoExtension = FilenameUtils.removeExtension(file.getName());
			if ((!with_postfix.equals("-Autosave") && fileNameNoExtension.endsWith("-Autosave") && !file.isDirectory())
					|| (with_postfix.equals("-Autosave") && file.isDirectory())) continue;
			if (file.getName().startsWith(MainApp.APP_NAME) && fileNameNoExtension.endsWith(with_postfix)) {
				try {
					if (file.isDirectory())
						FileUtils.deleteDirectory(file);
					else
						if (!file.delete())
							throw new IOException("(TW) File deletion failed for file " + file.getAbsolutePath());
				} catch (IOException e) {
					e.printStackTrace();
					TWUtils.showError("Failed to delete temp file " + file.getName(), false);
				}
			}
		}
	}

	// Copies file from io package to an external location.
	public static void exportFSResource(String resource_name, File out_file) throws Exception {
		try (InputStream inputStream = LilyPondInterface.class.getResourceAsStream(resource_name);
		     OutputStream outputStream = new FileOutputStream(out_file)) {

			if (inputStream == null)
				throw new Exception("Cannot get resource \"" + resource_name + "\" from Jar file.");

			int readBytes;
			byte[] buffer = new byte[4096];
			while ((readBytes = inputStream.read(buffer)) > 0)
				outputStream.write(buffer, 0, readBytes);

		}
	}

	// Recursively traverse directories to return a list of all (non-directory) files contained within root_dir.
	public static List<String> generateFileList(File root_dir) {
		List<String> fileList = new ArrayList<>();
		generateFileList(fileList, root_dir);
		return fileList;
	}
	private static void generateFileList(List<String> file_list, File node) {
		if (node.isFile())
			file_list.add(node.getAbsolutePath().replace("\\", "/"));

		if (node.isDirectory()) {
			String[] subNode = node.list();
			for (String filename : Objects.requireNonNull(subNode))
				generateFileList(file_list, new File(node, filename));
		}
	}

	public static boolean isBuiltinTone(File tone_file) {
		return tone_file.getAbsolutePath().contains(File.separator + MainApp.BUILT_IN_TONE_DIR.getName() + File.separator);
	}

	// Creates a temp file which contains this process's pid to register it as a running instance of the app.
	public static void establishFileLock() {
		String pid = String.valueOf(ProcessHandle.current().pid());

		try {
			Files.write(TWUtils.createTWTempFile("", "FileLock").toPath(), List.of(pid));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static boolean noOtherAppInstanceRunning() {
		boolean instanceRunning = false;

		Set<Long> livePIDs = ProcessHandle.allProcesses()
				.filter(ProcessHandle::isAlive)
				.map(ProcessHandle::pid)
				.collect(Collectors.toSet());

		try (Stream<Path> fileLocksStream = Files.list(Path.of(System.getProperty("java.io.tmpdir")))
				.filter(Files::isRegularFile)
				.filter(path -> FilenameUtils.removeExtension(path.getFileName().toString()).endsWith("-FileLock"))) {

			Set<Path> fileLocks = fileLocksStream.collect(Collectors.toSet());
			for (Path path : fileLocks) {
				long pid = Long.parseLong(Files.readString(path).strip());
				if (!livePIDs.contains(pid))
					Files.delete(path);
				else if (pid != ProcessHandle.current().pid())
					instanceRunning = true;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return !instanceRunning;
	}

	// UI

	public static class inputFormatter extends TextFormatter<String> {
		public inputFormatter() {
			super(c -> {
				String changeText = c.getText();

				if (changeText.contains("\t"))
					c.setText(changeText.replace("\t", " "));

				return c;
			});
		}
	}

	public static String shortenPhraseName(String name) {
		return name.replace("alternate", "alt").replace("Phrase", "").trim();
	}

	public static void showError(String message, boolean wait) {
		showAlert(AlertType.ERROR, "Error", message, wait);
	}
	public static Optional<ButtonType> showAlert(AlertType alert_type, String title_text, String header_text, boolean wait) {
		return showAlert(alert_type, title_text, header_text, wait, null, null, null);
	}
	public static Optional<ButtonType> showAlert(AlertType alert_type, String title_text, String header_text, boolean wait,
												 Stage owner) {
		return showAlert(alert_type, title_text, header_text, wait, owner, null, null);
	}
	public static Optional<ButtonType> showAlert(AlertType alert_type, String title_text, String header_text, boolean wait,
												 Stage owner, ButtonType[] button_types, ButtonType default_button) {
		Alert alert = new Alert(alert_type);
		alert.initOwner(owner);
		((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(MainApp.APP_ICON);
		alert.setTitle(title_text);
		alert.setHeaderText(header_text);
		if (button_types != null)
			alert.getButtonTypes().setAll(button_types);
		if (default_button != null)
			((Button) alert.getDialogPane().lookupButton(default_button)).setDefaultButton(true);

		if (wait)
			return alert.showAndWait();
		else
			alert.show();

		return Optional.empty();
	}

}