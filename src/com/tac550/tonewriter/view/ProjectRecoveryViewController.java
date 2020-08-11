package com.tac550.tonewriter.view;

import com.tac550.tonewriter.util.TWUtils;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectRecoveryViewController {

	@FXML private ListView<String> listView;
	private final List<File> autoSaveFiles = new ArrayList<>();

	private TopSceneController topController;

	void setTopController(TopSceneController controller) {
		topController = controller;
	}

	@FXML private void initialize() {
		listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

		File[] fileList = new File(System.getProperty("java.io.tmpdir")).listFiles();
		if (fileList == null) return;

		for (File file : fileList) {
			if (file.getName().endsWith("-Autosave.twproj")) {
				listView.getItems().add(file.getName());
				autoSaveFiles.add(file);
			}
		}

		listView.getSelectionModel().select(0);
	}

	@FXML private void handleCancel() {
		((Stage) listView.getScene().getWindow()).close();
	}

	@FXML private void handleRecover() {
		int selectedIndex = listView.getSelectionModel().getSelectedIndex();
		if (selectedIndex == -1) {
			TWUtils.showError("Please select a project to recover from the list first.", true);
			return;
		}

		topController.recoverProject(autoSaveFiles.get(selectedIndex));
		handleCancel();
	}

	boolean hasAutosavedProject() {
		return listView.getItems().size() > 0;
	}

}
