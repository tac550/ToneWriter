package com.tac550.tonewriter.io;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.MainApp;
import com.tac550.tonewriter.view.UpdaterViewController;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AutoUpdater {

	private static WebClient webClient;

	private static Stage updaterStage = new Stage();

	private static Alert downloadAlert;

	public static void updateCheck(Window owner, boolean startup) {

		Task<Void> updateTask = new Task<>() {
			@Override
			protected Void call() {
				try {
					webClient = new WebClient();
					webClient.getOptions().setJavaScriptEnabled(false);
					webClient.getOptions().setCssEnabled(false);

					// Get the GitHub Releases page
					final HtmlPage releasesPage = webClient.getPage("https://github.com/tac550/ToneWriter/releases");

					// Generate the changelog display HTML and store version numbers.
					List<HtmlDivision> releaseHeaders = releasesPage.getByXPath("//div[@class='release-header']");
					List<HtmlDivision> releaseNotes = releasesPage.getByXPath("//div[@class='markdown-body']");
					ArrayList<Float> releaseNumbers = new ArrayList<>();

					StringBuilder finalHTMLBuilder = new StringBuilder();

					finalHTMLBuilder.append("<body bgcolor=\"").append(TWUtils.toRGBCode(TWUtils.getUIBaseColor())).append("\">");

					for (int i = 0; i < releaseHeaders.size(); i++) {
						HtmlDivision header = releaseHeaders.get(i);
						String releaseTitle = header.getElementsByTagName("a").get(0).getTextContent();
						float releaseNumber = Float.parseFloat(releaseTitle.replaceAll("[^\\d.]+|\\.(?!\\d)", ""));

						if (releaseNumber >= Float.parseFloat(MainApp.APP_VERSION)) {
							// Add the version heading to the output
							finalHTMLBuilder.append("<h1>").append(releaseTitle).append("</h1>");

							// Add the associated changelog to the output
							String body = releaseNotes.get(i).asXml();
							int afterFirstHeading = body.indexOf("</h1>") + 5;
							String changelog = body.substring(afterFirstHeading, body.indexOf("<h1>", afterFirstHeading));
							finalHTMLBuilder.append(changelog);
						}

						if (releaseNumber > Float.parseFloat(MainApp.APP_VERSION)) {
							releaseNumbers.add(releaseNumber);
						}

					}

					finalHTMLBuilder.append("</body>");

					// If there's no update and this is the startup check, stop here.
					if (startup && releaseNumbers.size() == 0) {
						return null;
					}

					Platform.runLater(() -> {
						FXMLLoader loader = FXMLLoaderIO.loadFXMLLayout("updaterView.fxml");
						UpdaterViewController updaterController = loader.getController();

						updaterStage.setTitle(String.format("%s Automatic Updater", MainApp.APP_NAME));
						updaterStage.getIcons().add(MainApp.APP_ICON);
						updaterStage.setScene(new Scene(loader.getRoot()));
						updaterStage.setOnShown(event -> {
							updaterStage.setMinWidth(updaterStage.getWidth());
							updaterStage.setMinHeight(updaterStage.getHeight());
						});

						updaterController.setWebViewContent(finalHTMLBuilder.toString());
						updaterController.setVersionChoices(releaseNumbers);

						if (updaterStage.getOwner() == null) {
							updaterStage.initOwner(owner);
							updaterStage.initModality(Modality.APPLICATION_MODAL);
						}

						updaterStage.show();

						if (Taskbar.isTaskbarSupported()) {
							if (Taskbar.getTaskbar().isSupported(Taskbar.Feature.USER_ATTENTION)) {
								Taskbar.getTaskbar().requestUserAttention(true, true);
							}
						}

					});


				} catch (FailingHttpStatusCodeException | IOException e) {
					e.printStackTrace();

					Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.WARNING, "Warning",
							"Internet connection failure! Unable to check for updates.", true));


				}
				return null;
			}
		};

		Thread updateThread = new Thread(updateTask);
		updateThread.start();

	}

	public static void downloadUpdate(String version) {

		showDownloadAlert(version);

		// Create temp download file
		File downloadFile;
		try {
			downloadFile = TWUtils.createTWTempFile("Update", MainApp.OS_NAME.startsWith("win") ? ".exe" : ".zip");
		} catch (IOException e) {
			e.printStackTrace();
			Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
					"Failed to create download file! Update will be cancelled.", true));
			return;
		}

		// Copy download stream into temp file
		String sourceFileName;

		if (MainApp.OS_NAME.startsWith("win")) {
			sourceFileName = String.format(Locale.US, "ToneWriter%s_Setup.exe", version);
		} else if (MainApp.OS_NAME.startsWith("mac")) {
			sourceFileName = String.format(Locale.US, "ToneWriter%s.app.zip", version);
		} else if (MainApp.OS_NAME.startsWith("lin")) {
			sourceFileName = String.format(Locale.US, "ToneWriter%s-Linux.zip", version);
		} else {
			TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
					"Unknown platform. Unable to download update.", true);
			return;
		}

		Task<Boolean> downloadTask = new Task<>() {
			@Override
			protected Boolean call() {
				try (FileOutputStream foStream = new FileOutputStream(downloadFile)) {
					final WebResponse response = webClient.getPage(String.format(Locale.US,
							"https://github.com/tac550/ToneWriter/releases/download/%s/%s", version, sourceFileName)).getWebResponse();
					IOUtils.copy(response.getContentAsStream(), foStream);
				} catch (FailingHttpStatusCodeException e) {
					e.printStackTrace();
					Platform.runLater(() -> {
						TWUtils.showAlert(Alert.AlertType.WARNING, "Connection Error",
								"Internet connection failure! Unable to download update.", true);
						hideDownloadAlert();
					});
					return false;
				} catch (IOException e) {
					e.printStackTrace();
					Platform.runLater(() -> {
						TWUtils.showAlert(Alert.AlertType.WARNING, "I/O Error",
								"An error occurred while processing the download.", true);
						hideDownloadAlert();
					});
					return false;
				}

				return true;
			}
		};

		downloadTask.setOnSucceeded(wsevent -> {
			if (downloadTask.getValue()) executeInstaller(downloadFile);
		});
		Thread downloadThread = new Thread(downloadTask);
		downloadThread.start();

	}

	private static void executeInstaller(File downloaded_file) {

		File userDir = new File(System.getProperty("user.dir"));

		if (MainApp.OS_NAME.startsWith("win")) {

			try {
				Runtime.getRuntime().exec("cmd /c " + downloaded_file.getAbsolutePath() + " /D=" + userDir);
			} catch (IOException e) {
				e.printStackTrace();
				Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
						"I/O error occurred while running installer!", true));

				hideDownloadAlert();
				return;
			}

		} if (MainApp.OS_NAME.startsWith("mac")) {

			File scriptFile;
			try {
				scriptFile = TWUtils.createTWTempFile("updateScript",
						"version" + MainApp.APP_VERSION + ".sh");
			} catch (IOException e) {
				e.printStackTrace();
				Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
						"I/O error occurred while generating temp files!", true));

				hideDownloadAlert();
				return;
			}

			try {
				TWUtils.exportIOResource("autoupdate-macOS.sh", scriptFile);
			} catch (Exception e) {
				e.printStackTrace();
				Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
						"Error while exporting installer script!", true));

				hideDownloadAlert();
				return;
			}

			// Get PID representing this process's JVM.
			RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
			String pid = bean.getName().split("@")[0];

			try {
				Runtime.getRuntime().exec(new String[] {"chmod", "+x", scriptFile.getAbsolutePath()});
				Runtime.getRuntime().exec(new String[] {scriptFile.getAbsolutePath(), pid, downloaded_file.getAbsolutePath(), userDir.getParentFile().getParentFile().getAbsolutePath()});
			} catch (IOException e) {
				e.printStackTrace();
				Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
						"Failed to run installer script!", true));

				hideDownloadAlert();
				return;
			}

		} if (MainApp.OS_NAME.startsWith("lin")) {

			File scriptFile;
			try {
				scriptFile = TWUtils.createTWTempFile("updateScript",
						"version" + MainApp.APP_VERSION + ".sh");
			} catch (IOException e) {
				e.printStackTrace();
				Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
						"I/O error occurred while generating temp files!", true));

				hideDownloadAlert();
				return;
			}

			try {
				TWUtils.exportIOResource("autoupdate-Linux.sh", scriptFile);
			} catch (Exception e) {
				e.printStackTrace();
				Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
						"Error while exporting installer script!", true));

				hideDownloadAlert();
				return;
			}

			// Get PID representing this process's JVM.
			RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
			String pid = bean.getName().split("@")[0];

			try {
				Runtime.getRuntime().exec(new String[] {"chmod", "+x", scriptFile.getAbsolutePath()});
				Runtime.getRuntime().exec(new String[] {scriptFile.getAbsolutePath(), pid, downloaded_file.getAbsolutePath(), userDir.getParentFile().getAbsolutePath()});
			} catch (IOException e) {
				e.printStackTrace();
				Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
						"Failed to run installer script!", true));

				hideDownloadAlert();
				return;
			}

		}

		System.out.println("Now exiting for installation!");
		Platform.exit();
	}

	private static void showDownloadAlert(String version) {
		if (downloadAlert == null) {
			downloadAlert = new Alert(AlertType.INFORMATION);
			downloadAlert.setTitle("Download");
			downloadAlert.setHeaderText("Downloading version " + version + ". This may take some time.");
			((Stage) downloadAlert.getDialogPane().getScene().getWindow()).getIcons().add(MainApp.APP_ICON);
			downloadAlert.getButtonTypes().clear();
			downloadAlert.initModality(Modality.APPLICATION_MODAL);
		}

		downloadAlert.show();

	}

	private static void hideDownloadAlert() {
		Stage stage = (Stage) downloadAlert.getDialogPane().getScene().getWindow();
		stage.close();
	}

}
