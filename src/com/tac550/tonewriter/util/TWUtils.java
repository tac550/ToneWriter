package com.tac550.tonewriter.util;

import com.tac550.tonewriter.io.LilyPondWriter;
import com.tac550.tonewriter.view.MainApp;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

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
    	return MainApp.darkModeEnabled() ? new Color(0.345, 0.361, 0.373, 1)
			    : new Color(0.957, 0.957, 0.957, 1);
    }

	// Strings
//	public static int countOccurrences(String string, String single_character) {
//		return string.length() - string.replace(single_character, "").length();
//	}

	// I/O

	public static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));

		return new String(encoded, encoding);
	}

	// Creates and returns a temp file which will be recognized by the automatic temp cleaner
	public static File createTWTempFile(String prefix, String suffix) throws IOException {
		return File.createTempFile(MainApp.APP_NAME + "-" +
				(prefix.isEmpty() ? "" : prefix + "-"),
				suffix.isEmpty() ? "" : (suffix.startsWith(".") ? "" : "-") + suffix);
	}
	public static void cleanUpTempFiles() {
		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		File[] files = tempDir.listFiles();
		for (File file : Objects.requireNonNull(files)) {
			if (file.getName().startsWith(MainApp.APP_NAME)) {
				if (!file.delete()) {
					System.out.println("Failed to delete temp file " + file.getName());
				}
			}
		}
	}

	// Copies file from io package to an external location.
	public static void exportIOResource(String resource_name, File out_file) throws Exception {
		InputStream stream = null;
		OutputStream resStreamOut = null;
		try {
			stream = LilyPondWriter.class.getResourceAsStream(resource_name);
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

	public static Optional<ButtonType> showAlert(Alert.AlertType alert_type, String title_text, String header_text, boolean wait) {
		return showAlert(alert_type, title_text, header_text, wait, null, null);
	}
	public static Optional<ButtonType> showAlert(Alert.AlertType alert_type, String title_text, String header_text, boolean wait,
												 Stage owner) {
		return showAlert(alert_type, title_text, header_text, wait, owner, null);
	}
	public static Optional<ButtonType> showAlert(Alert.AlertType alert_type, String title_text, String header_text, boolean wait,
												 Stage owner, ButtonType[] button_types) {
		Alert alert = new Alert(alert_type);
		alert.setTitle(title_text);
		alert.setHeaderText(header_text);
		alert.initOwner(owner);
		((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(MainApp.APP_ICON);
		if (button_types != null) {
			alert.getButtonTypes().setAll(button_types);
		}

		if (wait) {
			return alert.showAndWait();
		} else {
			alert.show();
			return Optional.empty();
		}
	}

}