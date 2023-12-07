package com.tac550.tonewriter.io;

import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.MainApp;
import com.tac550.tonewriter.view.UpdaterViewController;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.io.IOUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.WebClient;
import org.htmlunit.WebResponse;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoUpdater {

	private static WebClient webClient;

	private static final Stage updaterStage = new Stage();
	private static Alert updateAlert;
	private static boolean checkCancelled;

	private static Alert downloadAlert;

	public static void updateCheck(Window owner, boolean startup) {

		Task<Void> updateCheckTask = new Task<>() {
			@Override
			protected Void call() {
				try {
					webClient = new WebClient();
					webClient.getOptions().setJavaScriptEnabled(false);
					webClient.getOptions().setCssEnabled(false);

					// Get the available versions and their associated Markdown changelogs.
					HashMap<String, String> versionInfo = retrieveReleaseInfo();

					// The HTML that appears in the updater window's WebView
					StringBuilder finalHTMLBuilder = new StringBuilder();
					finalHTMLBuilder.append("<body bgcolor=\"").append(TWUtils.toRGBCode(TWUtils.getUIBaseColor())).append("\">");

                    List<String> versionOptions = new ArrayList<>();
					for (String version : versionInfo.keySet().stream().sorted(Comparator.reverseOrder()).toList()) {
						int versionDiff = TWUtils.versionCompare(version, MainApp.APP_VERSION);

						if (versionDiff == 1 || versionDiff == 0) {
							// Add the version heading to the page
							finalHTMLBuilder.append("<h1>").append("Version ").append(version).append("</h1>");

							// Add the associated changelog to the page
							Node document = Parser.builder().build().parse(versionInfo.get(version));
							String body = HtmlRenderer.builder().build().render(document);
							int afterFirstHeading = body.indexOf("</h1>") + 5;
							String changelog = body.substring(afterFirstHeading, body.indexOf("<h1>", afterFirstHeading));
							finalHTMLBuilder.append(changelog);
						}

						if (versionDiff == 1)
							versionOptions.add(version);
					}

					finalHTMLBuilder.append("</body>");

					// If there's no update and this is the startup check, or the check has been cancelled, stop here.
					if ((startup && versionOptions.isEmpty()) || checkCancelled)
						return null;

					Platform.runLater(() -> {
						FXMLLoader loader = FXMLLoaderIO.loadFXMLLayout("/fxml/UpdaterView.fxml");
						UpdaterViewController updaterController = loader.getController();

						updaterStage.setTitle("%s Automatic Updater".formatted(MainApp.APP_NAME));
						updaterStage.getIcons().add(MainApp.APP_ICON);
						updaterStage.setScene(new Scene(loader.getRoot()));
						updaterStage.setOnShown(event -> {
							updaterStage.setMinWidth(updaterStage.getWidth());
							updaterStage.setMinHeight(updaterStage.getHeight());
						});

						updaterController.setWebViewContent(finalHTMLBuilder.toString());
						updaterController.setVersionChoices(versionOptions);

						if (updaterStage.getOwner() == null) {
							updaterStage.initOwner(owner);
							updaterStage.initModality(Modality.APPLICATION_MODAL);
						}

						updaterStage.show();

						if (Taskbar.isTaskbarSupported()) {
							if (Taskbar.getTaskbar().isSupported(Taskbar.Feature.USER_ATTENTION))
								Taskbar.getTaskbar().requestUserAttention(true, true);
						}

					});


				} catch (FailingHttpStatusCodeException | IOException | URISyntaxException e) {
					Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.WARNING, "Warning",
							"Internet connection failure! Unable to check for updates.", true));
				}

				Platform.runLater(() -> {
					if (updateAlert != null && updateAlert.isShowing())
						updateAlert.close();
				});
				return null;
			}
		};

		updateCheckTask.setOnFailed(e -> {
			TWUtils.showError("Update check failed.", false);
			if (updateAlert != null && updateAlert.isShowing()) updateAlert.close();
		});
		Thread updateCheckThread = new Thread(updateCheckTask);

		Platform.runLater(() -> {
			if (!startup) {
				if (updateAlert == null) {
					updateAlert = new Alert(AlertType.INFORMATION);
					updateAlert.initModality(Modality.NONE);
					((Stage) updateAlert.getDialogPane().getScene().getWindow()).getIcons().add(MainApp.APP_ICON);
					updateAlert.setTitle("Update");
					updateAlert.setHeaderText("Checking for updates...");
					updateAlert.getButtonTypes().set(0, ButtonType.CANCEL);
					((Button) updateAlert.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);

					updateAlert.setOnHiding(event -> checkCancelled = true);
					updateAlert.setOnShowing(event -> checkCancelled = false);

				}
				updateAlert.show();
			}
		});

		updateCheckThread.start();
	}

    private static HashMap<String, String> retrieveReleaseInfo() throws IOException, URISyntaxException {
        HashMap<String, String> versionInfo = new HashMap<>();
        StringBuilder content = new StringBuilder();
        URI uri = new URI("https://api.github.com/repos/tac550/tonewriter/releases");
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("GET");
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String respLine;
        while ((respLine = br.readLine()) != null)
            content.append(respLine);
        Pattern versionPattern = Pattern.compile("\"tag_name\":\"");
        Matcher versionMatcher = versionPattern.matcher(content);
        while (versionMatcher.find()) {
            String choppedVersion = content.substring(versionMatcher.end());

			Pattern notesPattern = Pattern.compile("\"body\":\"");
			Matcher notesMatcher = notesPattern.matcher(choppedVersion);
			if (notesMatcher.find()) {
				String choppedNotes = choppedVersion.substring(notesMatcher.end())
						.replace("\\r\\n", "\n").replace("\\\"", "\"");
				versionInfo.put(choppedVersion.substring(0, choppedVersion.indexOf("\"")),
						choppedNotes.substring(0, choppedNotes.indexOf("\"}")));
			}
        }
        return versionInfo;
    }

	public static void downloadUpdate(String version) {

		showDownloadAlert(version);

		// Create temp download file
		File downloadFile;
		try {
			downloadFile = TWUtils.createTWTempFile("Update-" + version + "-",
					MainApp.OS_NAME.startsWith("win") ? ".exe" : ".zip");
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

		Thread downloadThread = buildDownloadThread(version, downloadFile, sourceFileName);
		downloadThread.start();

	}

	private static Thread buildDownloadThread(String version, File downloadFile, String sourceFileName) {
		Task<Boolean> downloadTask = new Task<>() {
			@Override
			protected Boolean call() {
				try (FileOutputStream foStream = new FileOutputStream(downloadFile)) {
					final WebResponse response = webClient.getPage(String.format(Locale.US,
							"https://github.com/tac550/ToneWriter/releases/download/%s/%s", version, sourceFileName)).getWebResponse();
					IOUtils.copy(response.getContentAsStream(), foStream);
				} catch (FailingHttpStatusCodeException e) {
					e.printStackTrace();
					Platform.runLater(() -> TWUtils.showAlert(AlertType.WARNING, "Connection Error",
							"Internet connection failure! Unable to download the update.", true));
					return false;
				} catch (IOException e) {
					e.printStackTrace();
					Platform.runLater(() -> TWUtils.showAlert(AlertType.WARNING, "I/O Error",
							"An error occurred while processing the download.", true));
					return false;
				}
				return true;
			}
		};

		downloadTask.setOnSucceeded(event -> {
			executeInstaller(downloadFile);
			TWUtils.showError("Failed to install update!", true);
			hideDownloadAlert();
		});
		downloadTask.setOnFailed(event -> {
			TWUtils.showError("Failed to download update!", true);
			hideDownloadAlert();
		});
		return new Thread(downloadTask);
	}

	private static void executeInstaller(File downloaded_file) {

		File userDir = new File(System.getProperty("user.dir"));

		if (MainApp.OS_NAME.startsWith("win")) {

			try {
				new ProcessBuilder("cmd", "/c", downloaded_file.getAbsolutePath(), "/D=" + userDir).start();
			} catch (IOException e) {
				e.printStackTrace();
				Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
						"I/O error occurred while running installer!", true));
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
				return;
			}

			try {
				TWUtils.exportFSResource("/updater/autoupdate-macOS.sh", scriptFile);
			} catch (Exception e) {
				e.printStackTrace();
				Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
						"Error while exporting installer script!", true));
				return;
			}

			long pid = ProcessHandle.current().pid();

			try {
				String installDest = userDir.getParentFile().getParentFile().getAbsolutePath();
				if (TWUtils.versionCompare(System.getProperty("os.version"), "10.15.0") < 2)
					installDest = "/System/Volumes/Data" + installDest;

				Runtime.getRuntime().exec(new String[] {"chmod", "+x", scriptFile.getAbsolutePath()});
				String[] cmdlist = new String[] {"osascript", "-e", String.format("do shell script \"%s\" with administrator privileges", String.join(" ",
						new String[] {scriptFile.getAbsolutePath(), String.valueOf(pid), downloaded_file.getAbsolutePath(), installDest}))};
				Runtime.getRuntime().exec(cmdlist);
			} catch (IOException e) {
				e.printStackTrace();
				Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
						"Failed to run installer script!", true));
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
				return;
			}

			try {
				TWUtils.exportFSResource("/updater/autoupdate-Linux.sh", scriptFile);
			} catch (Exception e) {
				e.printStackTrace();
				Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
						"Error while exporting installer script!", true));
				return;
			}

			long pid = ProcessHandle.current().pid();

			try {
				Runtime.getRuntime().exec(new String[] {"chmod", "+x", scriptFile.getAbsolutePath()}).waitFor();
				Runtime.getRuntime().exec(new String[] {scriptFile.getAbsolutePath(), String.valueOf(pid), downloaded_file.getAbsolutePath(), userDir.getParentFile().getAbsolutePath()});
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
				Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
						"Failed to run installer script!", true));
				return;
			}

		}

		System.out.println("Now exiting for update installation!");
		System.exit(2);
	}

	private static void showDownloadAlert(String version) {
		if (downloadAlert == null) {
			downloadAlert = new Alert(AlertType.INFORMATION);
			downloadAlert.setTitle("Download");
			downloadAlert.setHeaderText("Downloading version " + version + ". This may take some time."
					+ (MainApp.OS_NAME.startsWith("mac") ? " You will be prompted to enter your password upon completion." : ""));
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
