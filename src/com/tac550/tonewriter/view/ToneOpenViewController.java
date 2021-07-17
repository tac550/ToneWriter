package com.tac550.tonewriter.view;

import com.tac550.tonewriter.model.ToneTreeCell;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ToneOpenViewController {

    MainSceneController mainController;

    @FXML TreeView<File> builtinTonesView;
    @FXML ListView<File> recentTonesView;

    void setMainController(MainSceneController controller) {
        mainController = controller;
    }

    @FXML private void initialize() {
        builtinTonesView.setCellFactory(p -> new ToneTreeCell());

        builtinTonesView.setRoot(new TreeItem<>(MainApp.BUILT_IN_TONE_DIR));
        populateBuiltinView(builtinTonesView.getRoot());
        // Default top-level directories to expanded position
        for (TreeItem<File> item : builtinTonesView.getRoot().getChildren())
            item.setExpanded(true);
    }

    @FXML private void handleCancel() {
        closeWindow();
    }

    @FXML private void handleOpen() {
        TreeItem<File> selected = builtinTonesView.getSelectionModel().getSelectedItem();
        if (selected.isLeaf()) {
            closeWindow();
            mainController.requestOpenTone(selected.getValue(), false, false);
        }
    }

    @FXML private void handleBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Tone");
        fileChooser.setInitialDirectory(MainApp.getPlatformSpecificInitialChooserDir()); // TODO: Default to dir containing open tone?
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TONE file (*.tone)", "*.tone"));

        File selectedFile = fileChooser.showOpenDialog(builtinTonesView.getScene().getWindow());

        if (selectedFile != null) {
            closeWindow();
            mainController.requestOpenTone(selectedFile, false, false);
        }
    }

    private void populateBuiltinView(TreeItem<File> root_node) {
        File[] files = root_node.getValue().listFiles();
        if (files == null) return;

        for (File file : files) {
            TreeItem<File> item = new TreeItem<>(file);

            if (file.isDirectory() || file.getName().endsWith(".tone"))
                root_node.getChildren().add(item);

            if (file.isDirectory())
                populateBuiltinView(item);
        }
    }

    private void closeWindow() {
        ((Stage) builtinTonesView.getScene().getWindow()).close();
    }
}
