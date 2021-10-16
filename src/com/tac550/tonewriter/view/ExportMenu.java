package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.LilyPondInterface;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.util.Objects;

public class ExportMenu extends Menu {

    private static final double INDICATOR_SIZE = 16.0;

    private final ImageView exportCompleteImage = new ImageView(Objects.requireNonNull(TopSceneController.class.getResource("/media/sign-check.png")).toExternalForm());
    private final ImageView exportFailedImage = new ImageView(Objects.requireNonNull(TopSceneController.class.getResource("/media/sign-delete.png")).toExternalForm());
    private final ImageView exportCancelledImage = new ImageView(Objects.requireNonNull(TopSceneController.class.getResource("/media/sign-ban.png")).toExternalForm());
    private final ImageView repeatExportImage = new ImageView(Objects.requireNonNull(TopSceneController.class.getResource("/media/sign-sync.png")).toExternalForm());
    private final ImageView cancelExportImage = new ImageView(Objects.requireNonNull(TopSceneController.class.getResource("/media/sign-ban.png")).toExternalForm());

    private final ProgressIndicator exportProgressIndicator = new ProgressIndicator();
    private final MenuItem cancelExportMenuItem = new MenuItem("Cancel Export");
    private final MenuItem openPDFMenuItem = new MenuItem("Open Exported PDF");
    private final MenuItem openFolderMenuItem = new MenuItem("Open Folder");
    private final CheckMenuItem openWhenCompletedItem = new CheckMenuItem("Automatically Open When Completed");

    private TopSceneController parentScene;

    public ExportMenu(double menu_icon_size) {
        // Sizing for switching menu icons
        repeatExportImage.setFitHeight(menu_icon_size);
        repeatExportImage.setFitWidth(menu_icon_size);
        cancelExportImage.setFitHeight(menu_icon_size);
        cancelExportImage.setFitWidth(menu_icon_size);

        TopSceneController.setMenuIcon(openPDFMenuItem, "/media/file-pdf.png");
        TopSceneController.setMenuIcon(openFolderMenuItem, "/media/folder-document.png");

        // Status icon/indicator settings and sizing
        exportProgressIndicator.setMouseTransparent(true);
        exportProgressIndicator.setPrefWidth(INDICATOR_SIZE);
        exportProgressIndicator.setPrefHeight(INDICATOR_SIZE);
        exportCompleteImage.setFitHeight(INDICATOR_SIZE);
        exportCompleteImage.setFitWidth(INDICATOR_SIZE);
        exportFailedImage.setFitHeight(INDICATOR_SIZE);
        exportFailedImage.setFitWidth(INDICATOR_SIZE);
        exportCancelledImage.setFitHeight(INDICATOR_SIZE);
        exportCancelledImage.setFitWidth(INDICATOR_SIZE);

        // Set up menu structure
        setGraphic(exportProgressIndicator);
        getItems().addAll(cancelExportMenuItem, new SeparatorMenuItem(), openPDFMenuItem, openFolderMenuItem,
                new SeparatorMenuItem(), openWhenCompletedItem);

        // Menu item behaviors
        cancelExportMenuItem.setOnAction((e) -> parentScene.cancelExport());
        openPDFMenuItem.setOnAction((e) -> LilyPondInterface.openLastExportPDF());
        openFolderMenuItem.setOnAction((e) -> LilyPondInterface.openLastExportFolder());
        openWhenCompletedItem.setSelected(MainApp.prefs.getBoolean(MainApp.PREFS_AUTO_OPEN_EXPORT, true));
        openWhenCompletedItem.selectedProperty().addListener((ov, oldVal, newVal) ->
                MainApp.prefs.putBoolean(MainApp.PREFS_AUTO_OPEN_EXPORT, newVal));

        // Menu initial state
        setText("No recent export");
        setDisable(true);
    }

    void setParentScene(TopSceneController parent) {
        parentScene = parent;
    }

    public void exportWorking() {
        parentScene.setCurrentlyExporting(true);
        setText("E_xport in Progress...");
        setGraphic(exportProgressIndicator);

        setDisable(false);
        showCancelExportOption();
        openPDFMenuItem.setDisable(true);
        openFolderMenuItem.setDisable(true);
    }
    public void exportSuccess() {
        parentScene.setCurrentlyExporting(false);
        setText("E_xport Complete");
        setGraphic(exportCompleteImage);

        setDisable(false);
        showRepeatExportOption();
        openPDFMenuItem.setDisable(!MainApp.lilyPondAvailable());
        openFolderMenuItem.setDisable(false);
    }
    public void exportCancelled() {
        parentScene.setCurrentlyExporting(false);
        setText("E_xport Cancelled");
        setGraphic(exportCancelledImage);

        setDisable(false);
        showRepeatExportOption();
        openPDFMenuItem.setDisable(true);
        openFolderMenuItem.setDisable(false);
    }
    public void exportFailure() {
        parentScene.setCurrentlyExporting(false);
        setText("E_xport Failed");
        setGraphic(exportFailedImage);

        setDisable(false);
        showRepeatExportOption();
        openPDFMenuItem.setDisable(true);
        openFolderMenuItem.setDisable(false);
    }
    public void exportReset() {
        parentScene.setCurrentlyExporting(false);
        setText("No recent export");
        setGraphic(exportProgressIndicator);

        setDisable(true);
        openPDFMenuItem.setDisable(true);
        openFolderMenuItem.setDisable(true);
    }

    private void showRepeatExportOption() {
        cancelExportMenuItem.setGraphic(repeatExportImage);
        cancelExportMenuItem.setText("Repeat Last Export");
    }
    private void showCancelExportOption() {
        cancelExportMenuItem.setGraphic(cancelExportImage);
        cancelExportMenuItem.setText("Cancel Export");
    }

    public boolean openWhenCompleted() {
        return openWhenCompletedItem.isSelected();
    }

}
