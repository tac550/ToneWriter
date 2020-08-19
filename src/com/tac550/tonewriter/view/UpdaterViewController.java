package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.AutoUpdater;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class UpdaterViewController {

	@FXML private WebView webView;

	@FXML private Text updateStatusText;
	@FXML private ChoiceBox<String> versionChoiceBox;

	@FXML private Button updateButton;
	@FXML private Button laterButton;

	@FXML private CheckBox updateOnStartupBox;

	private String result = "";

	@FXML private void initialize() {
		// Checkbox initial state and behavior
		updateOnStartupBox.setSelected(MainApp.prefs.getBoolean(MainApp.PREFS_CHECK_UPDATE_APPSTARTUP, true));
		updateOnStartupBox.selectedProperty().addListener((ov, oldVal, newVal) ->
				MainApp.prefs.putBoolean(MainApp.PREFS_CHECK_UPDATE_APPSTARTUP, newVal));

		// Open any webview links in the system's default Web browser.
		webView.getEngine().getLoadWorker().stateProperty().addListener((ov, oldVal, newVal) -> {
			if (newVal == Worker.State.SUCCEEDED) {

				NodeList nodeList = webView.getEngine().getDocument().getElementsByTagName("a");
				for (int i = 0; i < nodeList.getLength(); i++) {
					Node node = nodeList.item(i);
					EventTarget eventTarget = (EventTarget) node;
					eventTarget.addEventListener("click", event -> {
						EventTarget target = event.getCurrentTarget();
						HTMLAnchorElement anchorElement = (HTMLAnchorElement) target;
						String href = anchorElement.getHref();

						try {
							if (MainApp.OS_NAME.startsWith("lin")) {
								Runtime.getRuntime().exec("xdg-open " + href);
							} else {
								Desktop.getDesktop().browse(new URL(href).toURI());
							}
						} catch (IOException | URISyntaxException e) {
							e.printStackTrace();
						}

						event.preventDefault();
					}, false);
				}

			}
		});
	}

	@FXML private void handleUpdate() {
		result = versionChoiceBox.getSelectionModel().getSelectedItem();
		getStage().close();
		AutoUpdater.downloadUpdate(result);
	}

	@FXML private void handleLater() {
		result = "";
		getStage().close();
	}

	public void setWebViewContent(String page_text) {
		webView.getEngine().loadContent(page_text);
	}

	public void setVersionChoices(List<String> versions) {
		if (versions.size() > 0) {
			versionChoiceBox.getItems().setAll(versions);
			versionChoiceBox.getSelectionModel().select(0);
			updateStatusText.setText("Update available: Release " + versionChoiceBox.getSelectionModel().getSelectedItem()
					+ " (current version: " + MainApp.APP_VERSION + ")");
		} else {
			updateStatusText.setText("Already up to date! (version " + MainApp.APP_VERSION + ")");
			updateButton.setDisable(true);
			laterButton.setText("Close");
		}
	}

	public String getResult() {
		return result;
	}

	private Stage getStage() {
		return (Stage) webView.getScene().getWindow();
	}

}
