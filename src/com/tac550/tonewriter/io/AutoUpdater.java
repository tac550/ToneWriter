package com.tac550.tonewriter.io;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.MainApp;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class AutoUpdater {

	private static Stage updaterStage = new Stage();

	public static void AutoUpdate() {

		String fileName = "C:\\Users\\Thomas\\Downloads\\Test.zip";

		try (final WebClient webClient = new WebClient()) {
			webClient.setCssErrorHandler(new SilentCssErrorHandler());

			// Start by getting the GitHub Releases page
			final HtmlPage releasesPage = webClient.getPage("https://github.com/tac550/ToneWriter/releases");

			List<HtmlDivision> releaseHeaders = releasesPage.getByXPath("//div[@class='release-header']");
			List<HtmlDivision> releaseNotes = releasesPage.getByXPath("//div[@class='markdown-body']");

			System.out.println(releaseHeaders.size());

			for (HtmlDivision element : releaseHeaders) {
				System.out.println(element.getElementsByTagName("a").get(0).getTextContent());
			}
//			for (HtmlDivision element : releaseNotes) {
//				System.out.println(element.getElementsByTagName("ul").get(0).getTextContent());
//			}

			FXMLLoader loader = FXMLLoaderIO.loadFXMLLayout("updaterView.fxml");
			updaterStage.setTitle("Automatic Updater");
			updaterStage.getIcons().add(MainApp.APP_ICON);
			updaterStage.setScene(new Scene(loader.getRoot()));
			updaterStage.showAndWait();


//			final WebResponse response = webClient.getPage("https://github.com/tac550/ToneWriter/releases/download/0.5/ToneWriter.app.zip").getWebResponse();
//			IOUtils.copy(response.getContentAsStream(), new FileOutputStream(fileName));

		} catch (FailingHttpStatusCodeException | IOException e) {

			Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
					"Internet connection failure! Unable to check for updates.", true));

		}

	}

}
