package com.tac550.tonewriter.view;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.util.List;

public class UpdaterViewController {

	@FXML private WebView webView;

	@FXML private Text updateStatusText;
	@FXML private ChoiceBox<Float> versionChoiceBox;

	@FXML private Button updateButton;
	@FXML private Button laterButton;

	private String result = "";

	@FXML private void initialize() {

	}

	@FXML private void handleUpdate() {
		result = versionChoiceBox.getSelectionModel().getSelectedItem().toString();
		getStage().close();
	}

	@FXML private void handleLater() {
		result = "";
		getStage().close();
	}

	public void setWebViewContent(String page_text) {
		webView.getEngine().loadContent(page_text);
	}

	public void setVersionChoices(List<Float> versions) {
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
