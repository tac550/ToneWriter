package com.tac550.tonewriter.util;

import com.tac550.tonewriter.io.LilyPondInterface;
import com.tac550.tonewriter.view.MainApp;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

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
	public static String toNormalizedRGBCode(Color color) {
		return String.format(Locale.US,
				"%f %f %f",
				color.getRed(),
				color.getGreen(),
				color.getBlue());
	}

    public static Color getUIBaseColor() {
    	return MainApp.isDarkModeEnabled() ? new Color(0.345, 0.361, 0.373, 1)
			    : new Color(0.957, 0.957, 0.957, 1);
    }

	// Strings
//	public static int countOccurrences(String string, String single_character) {
//		return string.length() - string.replace(single_character, "").length();
//	}

	/**
	 * Compares two version strings.
	 *
	 * Use this instead of String.compareTo() for a non-lexicographical
	 * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
	 *
	 * @param v1 a string of alpha numerals separated by decimal points.
	 * @param v2 a string of alpha numerals separated by decimal points.
	 * @return The result is 1 if v1 is greater than v2.
	 *         The result is 2 if v2 is greater than v1.
	 *         The result is -1 if the version format is unrecognized.
	 *         The result is zero if the strings are equal.
	 */

	public static int versionCompare(String v1, String v2) {

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

	// I/O

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

	public static void cleanUpTempFiles() {
		cleanUpTempFiles("");
	}
	public static void cleanUpTempFiles(String with_postfix) {
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		File[] files = tempDir.listFiles();
		for (File file : Objects.requireNonNull(files)) {
			if (file.getName().startsWith(MainApp.APP_NAME) && FilenameUtils.removeExtension(file.getName()).endsWith(with_postfix)) {
				try {
					if (file.isDirectory())
						FileUtils.deleteDirectory(file);
					else
						if (!file.delete())
							throw new IOException("(TW) File deletion failed");
				} catch (IOException e) {
					e.printStackTrace();
					TWUtils.showError("Failed to delete temp file " + file.getName(), false);
				}
			}
		}
	}

	// Copies file from io package to an external location.
	public static void exportIOResource(String resource_name, File out_file) throws Exception {
		InputStream stream = null;
		OutputStream resStreamOut = null;
		try {
			stream = LilyPondInterface.class.getResourceAsStream(resource_name);
			if (stream == null) {
				throw new Exception("Cannot get resource \"" + resource_name + "\" from Jar file.");
			}

			int readBytes;
			byte[] buffer = new byte[4096];
			resStreamOut = new FileOutputStream(out_file);
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

	// UI

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

		if (wait) {
			return alert.showAndWait();
		} else {
			alert.show();
			return Optional.empty();
		}
	}

}