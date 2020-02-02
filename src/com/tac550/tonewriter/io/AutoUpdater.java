package com.tac550.tonewriter.io;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.MainApp;
import com.tac550.tonewriter.view.UpdaterViewController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AutoUpdater {

	private static Stage updaterStage = new Stage();

	public static void AutoUpdate() {

		String fileName = "C:\\Users\\Thomas\\Downloads\\Test.zip";

		try (final WebClient webClient = new WebClient()) {
			webClient.setCssErrorHandler(new SilentCssErrorHandler());

			// Get the GitHub Releases page
			final HtmlPage releasesPage = webClient.getPage("https://github.com/tac550/ToneWriter/releases");

			// Generate the changelog display HTML and store version numbers.
			List<HtmlDivision> releaseHeaders = releasesPage.getByXPath("//div[@class='release-header']");
			List<HtmlDivision> releaseNotes = releasesPage.getByXPath("//div[@class='markdown-body']");
			ArrayList<Float> releaseNumbers = new ArrayList<>();

			StringBuilder finalHTMLString = new StringBuilder();

			System.out.println(releaseNotes.size());
			for (int i = 0; i < releaseHeaders.size(); i++) {
				HtmlDivision header = releaseHeaders.get(i);
				String body = releaseNotes.get(i).asXml();

				String releaseTitle = header.getElementsByTagName("a").get(0).getTextContent();
				finalHTMLString.append("<h1>").append(releaseTitle).append("</h1>");

				String result = body.substring(body.indexOf("</h1>") + 5, body.indexOf("<h1>", body.indexOf("</h1>") + 5));
				finalHTMLString.append(result);

				float releaseNumber = Float.parseFloat(releaseTitle.replaceAll("[^\\d.]+|\\.(?!\\d)", ""));
				if (releaseNumber > Float.parseFloat(MainApp.APP_VERSION)) {
					releaseNumbers.add(releaseNumber);
				}

			}


//			for (HtmlDivision element : releaseNotes) {
//				System.out.println(element.getElementsByTagName("ul").get(0).getTextContent());
//			}

			FXMLLoader loader = FXMLLoaderIO.loadFXMLLayout("updaterView.fxml");
			UpdaterViewController updaterController = loader.getController();

			updaterStage.setTitle("Automatic Updater");
			updaterStage.getIcons().add(MainApp.APP_ICON);
			updaterStage.setScene(new Scene(loader.getRoot()));
			updaterStage.setOnShown(event -> {
				updaterStage.setMinWidth(updaterStage.getWidth());
				updaterStage.setMinHeight(updaterStage.getHeight());
			});

			updaterController.setWebViewContent(finalHTMLString.toString());
			updaterController.setVersionChoices(releaseNumbers);

			updaterStage.showAndWait();

//			final WebResponse response = webClient.getPage("https://github.com/tac550/ToneWriter/releases/download/0.5/ToneWriter.app.zip").getWebResponse();
//			IOUtils.copy(response.getContentAsStream(), new FileOutputStream(fileName));

		} catch (FailingHttpStatusCodeException | IOException e) {

			Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
					"Internet connection failure! Unable to check for updates.", true));

		}

	}

}
