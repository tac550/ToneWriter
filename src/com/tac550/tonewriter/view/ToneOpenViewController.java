package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.ToneIO;
import com.tac550.tonewriter.model.ToneTreeCell;
import com.tac550.tonewriter.util.DesktopInterface;
import com.tac550.tonewriter.util.TWUtils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.comparator.NameFileComparator;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ToneOpenViewController {

    private static final String NO_RECENTS_MESSAGE = "No recent tones";
    private static final ButtonType openFolderButton = new ButtonType("Open Folder");

    private MainSceneController mainController;

    private List<File> recentTones;

    private @FXML TreeView<File> builtinTonesView;
    private @FXML TreeView<File> recentTonesView;

    private final ContextMenu recentToneMenu = new ContextMenu();
    private final MenuItem openFolderItem = new MenuItem("Open Folder");
    private final MenuItem removeItem = new MenuItem("Remove From List");

    void setMainController(MainSceneController controller) {
        mainController = controller;
    }

    @FXML private void initialize() {
        // Built-in view initialization
        builtinTonesView.setCellFactory(p -> new ToneTreeCell());
        builtinTonesView.focusedProperty().addListener(new treeFocusListener(recentTonesView));
        applyCommonTVHandlers(builtinTonesView);
        builtinTonesView.setRoot(new TreeItem<>(MainApp.BUILT_IN_TONE_DIR));
        populateBuiltinTones(builtinTonesView.getRoot());
        // Default top-level directories to expanded position
        for (TreeItem<File> item : builtinTonesView.getRoot().getChildren())
            item.setExpanded(true);

        // Recent view initialization
        recentTonesView.setCellFactory(p -> new ToneTreeCell());
        recentTonesView.focusedProperty().addListener(new treeFocusListener(builtinTonesView));
        applyCommonTVHandlers(recentTonesView);
        recentTonesView.addEventHandler(MouseEvent.MOUSE_CLICKED, ev -> {
            TreeItem<File> selectedItem = recentTonesView.getSelectionModel().getSelectedItem();
            if (ev.getButton() == MouseButton.SECONDARY && selectedItem != null && selectedItem.isLeaf() && !recentTones.isEmpty())
                recentToneMenu.show(recentTonesView, ev.getScreenX(), ev.getScreenY());
        });
        refreshRecentTones();

        // Recent view context menu setup
        recentToneMenu.getItems().addAll(openFolderItem, removeItem);
        openFolderItem.setOnAction(ev ->
                DesktopInterface.openFile(recentTonesView.getSelectionModel().getSelectedItem().getValue().getParentFile()));
        removeItem.setOnAction(ev -> removeRecentTone(recentTonesView.getSelectionModel().getSelectedItem().getValue()));
    }

    private static class treeFocusListener implements ChangeListener<Boolean> {
        private final TreeView<File> otherTreeView;

        public treeFocusListener(TreeView<File> view) {
            otherTreeView = view;
        }

        @Override
        public void changed(ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) {
            if (newVal) // Gained focus -> Clear other tree's selection
                otherTreeView.getSelectionModel().clearSelection();
        }
    }

    private void refreshRecentTones() {
        recentTonesView.setRoot(new TreeItem<>());
        recentTones = ToneIO.getRecentTones();
        if (recentTones.isEmpty())
            recentTonesView.getRoot().getChildren().add(new TreeItem<>(new File(NO_RECENTS_MESSAGE)));
        else
            recentTonesView.getRoot().getChildren().addAll(recentTones.stream().distinct()
                    .map(TreeItem::new).collect(Collectors.toList()));
    }

    private void removeRecentTone(File tone_file) {
        recentTones.remove(tone_file);
        ToneIO.writeRecentTones(recentTones);
        refreshRecentTones();
    }

    private void applyCommonTVHandlers(TreeView<File> tree_view) {
        tree_view.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ENTER)
                handleOpen();
            else if (ev.getCode() == KeyCode.ESCAPE)
                handleCancel();
        });
        tree_view.addEventHandler(MouseEvent.MOUSE_CLICKED, ev -> {
            if (ev.getButton() == MouseButton.PRIMARY && ev.getClickCount() > 1)
                handleOpen();
        });
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
            if (selectedCustom.getValue().exists()) {
                closeWindow();
                ToneIO.bumpRecentTone(selectedCustom.getValue());
                mainController.requestOpenTone(selectedCustom.getValue(), false, false);
            } else if (!selectedCustom.getValue().getName().equals(NO_RECENTS_MESSAGE)) {
                TWUtils.showAlert(Alert.AlertType.INFORMATION, "Missing Tone",
                        "Recent tone \"" + selectedCustom.getValue().getName() + "\" is missing. Remove from list?",
                        true, (Stage)builtinTonesView.getScene().getWindow(), new ButtonType[]{ButtonType.YES, openFolderButton, ButtonType.NO}, ButtonType.YES).ifPresent(buttonType -> {
                    if (buttonType == ButtonType.YES) removeRecentTone(selectedCustom.getValue());
                    else if (buttonType == openFolderButton) DesktopInterface.openFile(selectedCustom.getValue().getParentFile());
                });
            }
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
