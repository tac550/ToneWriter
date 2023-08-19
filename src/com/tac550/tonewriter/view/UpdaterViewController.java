package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.AutoUpdater;
import com.tac550.tonewriter.util.DesktopInterface;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

import java.util.List;

public class UpdaterViewController {

	@FXML private BorderPane mainPane;

	private WebView webView;
	@FXML private TextFlow releasesLinkText;

	@FXML private Text updateStatusText;
	@FXML private ChoiceBox<String> versionChoiceBox;

	@FXML private Button updateButton;
	@FXML private Button laterButton;

	@FXML private CheckBox updateOnStartupBox;

	@FXML private void initialize() {
		// Checkbox initial state and behavior
		updateOnStartupBox.setSelected(MainApp.prefs.getBoolean(MainApp.PREFS_CHECK_UPDATE_STARTUP, true));
		updateOnStartupBox.selectedProperty().addListener((ov, oldVal, newVal) ->
				MainApp.prefs.putBoolean(MainApp.PREFS_CHECK_UPDATE_STARTUP, newVal));

		mainPane.getChildren().remove(releasesLinkText);

		webView = new WebView();
		mainPane.setCenter(webView);

		// Open any Web links in the system's default Web browser.
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

						DesktopInterface.browseURI(href);

						event.preventDefault();
					}, false);
				}

			}
		});
	}

	@FXML private void handleUpdate() {
		getStage().close();
		AutoUpdater.downloadUpdate(versionChoiceBox.getSelectionModel().getSelectedItem());
	}

	@FXML private void handleLater() {
		getStage().close();
	}

	@FXML private void handleReleasesLink() {
		DesktopInterface.browseURI("https://github.com/tac550/ToneWriter/releases/");
	}

	public void setWebViewContent(String page_text) {
		webView.getEngine().loadContent(page_text);
	}

	public void setVersionChoices(List<String> versions) {
		if (!versions.isEmpty()) {
			versionChoiceBox.getItems().setAll(versions);
			versionChoiceBox.getSelectionModel().select(0);
			updateStatusText.setText("Update available: Version " + versionChoiceBox.getSelectionModel().getSelectedItem()
					+ " (current version: " + MainApp.APP_VERSION + ")");
		} else {
			updateStatusText.setText("Already up to date! (Version " + MainApp.APP_VERSION + ")");
			updateButton.setDisable(true);
			laterButton.setText("Close");
		}
	}

	private Stage getStage() {
		return (Stage) mainPane.getScene().getWindow();
	}

}
