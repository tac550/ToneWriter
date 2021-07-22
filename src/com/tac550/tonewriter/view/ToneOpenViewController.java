package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.ToneIO;
import com.tac550.tonewriter.model.ToneTreeCell;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.comparator.NameFileComparator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ToneOpenViewController {

    MainSceneController mainController;

    @FXML TreeView<File> builtinTonesView;
    @FXML TreeView<File> recentTonesView;

    void setMainController(MainSceneController controller) {
        mainController = controller;
    }

    @FXML private void initialize() {
        // Built-in view initialization
        builtinTonesView.setCellFactory(p -> new ToneTreeCell());
        builtinTonesView.focusedProperty().addListener(new treeFocusListener(recentTonesView));
        builtinTonesView.setRoot(new TreeItem<>(MainApp.BUILT_IN_TONE_DIR));
        populateBuiltinTones(builtinTonesView.getRoot());
        // Default top-level directories to expanded position
        for (TreeItem<File> item : builtinTonesView.getRoot().getChildren())
            item.setExpanded(true);

        // Recent view initialization
        recentTonesView.setCellFactory(p -> new ToneTreeCell());
        recentTonesView.focusedProperty().addListener(new treeFocusListener(builtinTonesView));
        recentTonesView.setRoot(new TreeItem<>());
        try {
            List<File> recentTones = ToneIO.getRecentTones();
            recentTonesView.getRoot().getChildren().addAll(recentTones.stream().distinct()
                    .map(TreeItem::new).collect(Collectors.toList()));
        } catch (IOException e) {
            recentTonesView.getRoot().getChildren().add(new TreeItem<>(new File("No recent tones")));
        }
    }

    private static class treeFocusListener implements ChangeListener<Boolean> {
        TreeView<File> otherTreeView;

        public treeFocusListener(TreeView<File> view) {
            otherTreeView = view;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) {
            if (newVal) // Gained focus -> Clear other tree's selection
                otherTreeView.getSelectionModel().clearSelection();
        }
    }

    @FXML private void handleCancel() {
        closeWindow();
    }

    @FXML private void handleOpen() {
        TreeItem<File> selectedBuiltin = builtinTonesView.getSelectionModel().getSelectedItem();
        TreeItem<File> selectedCustom = recentTonesView.getSelectionModel().getSelectedItem();
        if (selectedBuiltin != null && selectedBuiltin.isLeaf()) {
            closeWindow();
            mainController.requestOpenTone(selectedBuiltin.getValue(), false, false);
        } else if (selectedCustom != null && selectedCustom.isLeaf()) {
            closeWindow();
            mainController.requestOpenTone(selectedCustom.getValue(), false, false);
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
            ToneIO.bumpRecentTone(selectedFile);
            mainController.requestOpenTone(selectedFile, false, false);
        }
    }

    private void populateBuiltinTones(TreeItem<File> root_node) {
        File[] files = root_node.getValue().listFiles();
        if (files == null) return;

        Arrays.sort(files, NameFileComparator.NAME_COMPARATOR);
        for (File file : files) {
            TreeItem<File> item = new TreeItem<>(file);

            if (file.isDirectory() || file.getName().endsWith(".tone"))
                root_node.getChildren().add(item);

            if (file.isDirectory())
                populateBuiltinTones(item);
        }
    }

    private void closeWindow() {
        ((Stage) builtinTonesView.getScene().getWindow()).close();
    }
}
