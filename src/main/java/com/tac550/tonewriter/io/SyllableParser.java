package com.tac550.tonewriter.io;

import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Platform;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSubmitInput;
import org.htmlunit.html.HtmlTextArea;

import java.io.IOException;

public class SyllableParser {

	// Sends text off to the syllabification engine and returns the resulting lines as an array of strings.
	public static String[] getSyllabificationLines(String full_verse, Stage main_stage) {
		try (final WebClient webClient = new WebClient()) {
			webClient.setIncorrectnessListener((a0, a1) -> {});

	        final HtmlPage page = webClient.getPage("https://www.juiciobrennan.com/syllables/");

	        final HtmlTextArea textField = page.getHtmlElementById("inputText");
	        final HtmlSubmitInput submitButton = page.getHtmlElementById("inputTextButton");
	        
	        textField.setText(full_verse);
	        final HtmlPage resultPage = submitButton.click();

			return resultPage.getHtmlElementById("inputText").getTextContent().split("\\r?\\n");
		} catch (FailingHttpStatusCodeException | IOException e) {
			Platform.runLater(() -> TWUtils.showAlert(AlertType.WARNING, "Warning",
					"Failed to connect to online syllabification service! " +
							"Use Edit buttons to break words into syllables.", false, main_stage));
			// Return the provided text without modification if there's a failure.
			return full_verse.split("\\r?\\n");
		}
	}
}
