package com.tac550.tonewriter.io;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Platform;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Syllables {

	// Sends text off to the syllabification engine and returns the resulting lines as an array of strings.
	public static String[] getSyllabificationLines(String full_verse, Stage main_stage) {
		
		ArrayList<String> lines;
		
		try (final WebClient webClient = new WebClient()) {

			webClient.setIncorrectnessListener((a0, a1) -> {});

	        final HtmlPage page = webClient.getPage("http://www.juiciobrennan.com/syllables/");

	        final HtmlTextArea textField = page.getHtmlElementById("inputText");
	        final HtmlSubmitInput submitButton = page.getHtmlElementById("inputTextButton");
	        
	        textField.setText(full_verse);
	        final HtmlPage resultPage = submitButton.click();
	        lines = new ArrayList<>(
	        		Arrays.asList(resultPage.getHtmlElementById("inputText").getTextContent().split("\\r?\\n")));

		} catch (FailingHttpStatusCodeException | IOException e) {

			Platform.runLater(() -> TWUtils.showAlert(AlertType.WARNING, "Warning",
					"Internet connection failure! Use Edit buttons to break words up into syllables.", true, main_stage));

			// Return the provided text without modification if there's a failure.
			return full_verse.split("\\r?\\n");
		}
		
		return lines.toArray(new String[0]);
		
	}
	
}
