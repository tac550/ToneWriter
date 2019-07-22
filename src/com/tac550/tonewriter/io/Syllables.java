package com.tac550.tonewriter.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Syllables {

	// Sends text off to the syllabification engine and returns the resulting lines as an array of strings.
	public static String[] getSyllabificationLines(String full_verse) {
		
		ArrayList<String> lines;
		
		try (final WebClient webClient = new WebClient()) {
			
	        final HtmlPage page = webClient.getPage("http://www.juiciobrennan.com/syllables/");
	        
	        final HtmlTextArea textField = page.getHtmlElementById("inputText");
	        final HtmlSubmitInput submitButton = page.getHtmlElementById("inputTextButton");
	        
	        textField.setText(full_verse);
	        final HtmlPage resultPage = submitButton.click();
	        lines = new ArrayList<>(
	        		Arrays.asList(resultPage.getHtmlElementById("inputText").getTextContent().split("\\r?\\n")));

		} catch (FailingHttpStatusCodeException | IOException e) {
	    	Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Internet connection failure! Use Edit buttons to break up syllables.");

			alert.showAndWait();
			// Return the provided text without modification if there's a failure.
			return full_verse.split("\\r?\\n");
		}
		
		return lines.toArray(new String[0]);
		
	}
	
}
