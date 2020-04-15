package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.AutoUpdater;
import com.tac550.tonewriter.io.FXMLLoaderIO;
import com.tac550.tonewriter.model.MenuState;
import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class TopSceneController {

	private Stage parentStage;

	@FXML private MenuItem newToneMenuItem;
	@FXML private MenuItem openToneMenuItem;
	@FXML private MenuItem saveToneMenuItem;
	@FXML private MenuItem saveToneAsMenuItem;
	@FXML private MenuItem exportPDFMenuItem;
	@FXML private MenuItem exitMenuItem;

	@FXML private MenuItem addCLMenuItem;
	@FXML private MenuItem setKeyMenuItem;
	@FXML private MenuItem editHeaderInfoMenuItem;

	@FXML private Menu editMenu;
	@FXML private CheckMenuItem manualCLAssignmentMenuItem;
	@FXML private CheckMenuItem playMidiMenuItem;
	@FXML private CheckMenuItem hoverHighlightMenuItem;
	@FXML private CheckMenuItem saveLPMenuItem;
	@FXML private MenuItem setLilyPondLocationItem;
	@FXML private MenuItem resetLilyPondLocationItem;
	@FXML private CheckMenuItem darkModeMenuItem;

	@FXML private MenuItem combinePDFsMenuItem;

	@FXML private MenuItem aboutMenuItem;
	@FXML private MenuItem updateMenuitem;

	@FXML private TabPane tabPane;
	private final HashMap<Tab, MainSceneController> tabControllerMap = new HashMap<>();

	@FXML private Button addTabButton;

	static final String composerIconPath = "/media/profile.png";
	static final String keyIconPath = "/media/key.png";

	File currentSavingDirectory = new File(FileSystemView.getFileSystemView().getDefaultDirectory().getPath());

	String paperSize = "";

	@FXML private void initialize() {

		// Menu icons
		setMenuIcon(newToneMenuItem, "/media/file-text.png");
		setMenuIcon(openToneMenuItem, "/media/folder.png");
		setMenuIcon(saveToneMenuItem, "/media/floppy.png");
		setMenuIcon(saveToneAsMenuItem, "/media/floppy-add.png");
		setMenuIcon(exitMenuItem, "/media/sign-error.png");
		setMenuIcon(exportPDFMenuItem, "/media/box-out.png");
		setMenuIcon(aboutMenuItem, "/media/sign-info.png");
		setMenuIcon(addCLMenuItem, "/media/sign-add.png");
		setMenuIcon(setKeyMenuItem, keyIconPath);
		setMenuIcon(editHeaderInfoMenuItem, composerIconPath);
		setMenuIcon(manualCLAssignmentMenuItem, "/media/tag-alt.png");
		setMenuIcon(combinePDFsMenuItem, "/media/file-pdf.png");
		setMenuIcon(updateMenuitem, "/media/cloud-sync.png");

		// Modify LilyPond location editing menu items on Mac
		if (MainApp.OS_NAME.startsWith("mac")) {
			setLilyPondLocationItem.setText("Locate LilyPond.app");
		} if (MainApp.OS_NAME.startsWith("lin")) {
			resetLilyPondLocationItem.setText("Reset LilyPond Location (use /usr/bin/lilypond)");
		}

		// If Lilypond isn't present, disable option to play midi as chords are assigned and to not save LilyPond files.
		if (!MainApp.lilyPondAvailable()) {
			playMidiMenuItem.setSelected(false);
			playMidiMenuItem.setDisable(true);
			saveLPMenuItem.setSelected(true);
			saveLPMenuItem.setDisable(true);
		}

		// Initial state and behavior for "Save LilyPond file" option
		saveLPMenuItem.setSelected(MainApp.prefs.getBoolean(MainApp.PREFS_SAVE_LILYPOND_FILE, false));
		saveLPMenuItem.selectedProperty().addListener((ov, oldVal, newVal) ->
				MainApp.prefs.putBoolean(MainApp.PREFS_SAVE_LILYPOND_FILE, newVal));
		// Set initial state for paper size, which may have been saved in preferences.
		paperSize = MainApp.prefs.get(MainApp.PREFS_PAPER_SIZE, "letter (8.5 x 11.0 in)");

		// Hover Highlight menu item behavior and initial state
		hoverHighlightMenuItem.setSelected(MainApp.prefs.getBoolean(MainApp.PREFS_HOVER_HIGHLIGHT, true));
		hoverHighlightMenuItem.selectedProperty().addListener((ov, oldVal, newVal) ->
				MainApp.prefs.putBoolean(MainApp.PREFS_HOVER_HIGHLIGHT, newVal));

		// Dark Mode menu item behavior and initial state
		darkModeMenuItem.setSelected(MainApp.isDarkModeEnabled());
		darkModeMenuItem.selectedProperty().addListener((ov, oldVal, newVal) -> {
			MainApp.prefs.putBoolean(MainApp.PREFS_DARK_MODE, newVal);
			MainApp.setDarkModeEnabled(newVal);
		});

		// Tab pane initialization

		tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
		tabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
			if (tabPane.getTabs().size() == 1) {
				tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
			} else {
				tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
			}
		});
		tabPane.getSelectionModel().selectedItemProperty().addListener(observable ->
				tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).updateTopLevelInfo());

		// Add button initialization

		ImageView addImageView = new ImageView(new Image(getClass().getResource("/media/sign-add.png").toExternalForm(),
				16, 16, false, true));
		addTabButton.setGraphic(addImageView);
		if (MainApp.OS_NAME.startsWith("mac")) addTabButton.getTooltip().setText("Add item (\u2318T)");

	}

	void setParentStage(Stage parent_stage) {
		parentStage = parent_stage;

		// Concluding setup process
		addTab();
	}

	private void setMenuIcon(MenuItem menu_item, String imagePath) {
		ImageView saveIcon = new ImageView(getClass().getResource(imagePath).toExternalForm());
		double menuIconSize = 30;
		saveIcon.setFitHeight(menuIconSize);
		saveIcon.setFitWidth(menuIconSize);
		menu_item.setGraphic(saveIcon);
	}

	private void setPaperSize(String size) {
		paperSize = size;

		MainApp.prefs.put(MainApp.PREFS_PAPER_SIZE, paperSize);
	}

	/*
	 * File Menu Actions
	 */
	@FXML void handleNewTone() {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleNewTone();
	}
	@FXML private void handleOpenTone() {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleOpenTone(null, false);
	}
	@FXML void handleSave() {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleSave();
	}
	@FXML private void handleSaveAs() {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleSaveAs();
	}
	@FXML private void handleExport() {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleExport();
	}
	@FXML private void handleExit() {
		Event event = new Event(null, null, null);
		requestExit(event);
		if (!event.isConsumed()) Platform.exit();
	}

	/*
	 * Edit Menu Actions
	 */
	@FXML private void handleCreateChantLine() {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).createChantLine(true);
	}
	@FXML private void handleSetKeySignature() {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleSetKeySignature();
	}
	@FXML private void handleEditHeaderInfo() {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleEditHeaderInfo();
	}
	@FXML private void handleToggleManualCLAssignment() {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleToggleManualCLAssignment();
	}

	/*
	 * Tools Menu Actions
	 */
	@FXML private void handleCombinePDFs() {

		FXMLLoaderIO.loadFXMLLayoutAsync("pdfCombineView.fxml", loader -> {
			BorderPane rootLayout = loader.getRoot();
			PDFCombineViewController controller = loader.getController();
			controller.setDefaultDirectory(currentSavingDirectory);

			Platform.runLater(() -> {
				Stage pdfStage = new Stage();
				pdfStage.setTitle("Combine PDFs");
				pdfStage.getIcons().add(MainApp.APP_ICON);
				pdfStage.setScene(new Scene(rootLayout));
				pdfStage.setResizable(false);
				pdfStage.show();
			});
		});

	}

	/*
	 * Options Menu Actions
	 */
	@FXML private void handleSetLilyPondDir() {
		MainApp.setLilyPondDir(parentStage, false);
	}
	@FXML private void handleResetLilyPondDir() {
		MainApp.resetLilyPondDir(false);
	}
	@FXML private void handleSetPaperSize() {
		List<String> choices = new ArrayList<>();

		choices.add("a4 (210 x 297 mm)");
		choices.add("junior-legal (8.0 x 5.0 in)");
		choices.add("legal (8.5 x 14.0 in)");
		choices.add("ledger (17.0 x 11.0 in)");
		choices.add("letter (8.5 x 11.0 in)");
		choices.add("tabloid (11.0 x 17.0 in)");
		choices.add("11x17 (11.0 x 17.0 in)");
		choices.add("17x11 (17.0 x 11.0 in)");

		ChoiceDialog<String> dialog = new ChoiceDialog<>(paperSize, choices);
		dialog.setTitle("Paper sizes");
		dialog.setHeaderText("Choose a paper size");
		dialog.initOwner(parentStage);
		Optional<String> result = dialog.showAndWait();

		result.ifPresent(this::setPaperSize);
	}

	/*
	 * Help Menu Actions
	 */
	@FXML private void handleAbout() {
		FXMLLoaderIO.loadFXMLLayoutAsync("AboutScene.fxml", loader -> {
			BorderPane aboutLayout = loader.getRoot();

			Platform.runLater(() -> {
				Stage aboutStage = new Stage();
				aboutStage.setScene(new Scene(aboutLayout));

				aboutStage.setTitle("About " + MainApp.APP_NAME);
				aboutStage.setResizable(false);
				aboutStage.initOwner(parentStage);
				aboutStage.initModality(Modality.APPLICATION_MODAL);
				aboutStage.getIcons().add(MainApp.APP_ICON);
				aboutStage.show();
			});
		});
	}
	@FXML private void handleUpdateCheck() {
		AutoUpdater.updateCheck(parentStage, false);
	}

	@FXML void addTab() {
		try {
			// Load layout from fxml file TODO: Why not Thread this?
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("MainScene.fxml"));
			SplitPane mainLayout = loader.load();
			MainSceneController mainController = loader.getController();
			mainController.setStageAndTopScene(parentStage, this);

			Tab tab = new Tab();

			tab.textProperty().bind(mainController.getTitleTextProperty());
			mainController.setTitleText("Item " + (tabPane.getTabs().size() + 1));

			tabControllerMap.put(tab, mainController);

			AnchorPane anchorPane = new AnchorPane();
			anchorPane.getChildren().add(mainLayout);
			AnchorPane.setTopAnchor(mainLayout, 0d);
			AnchorPane.setBottomAnchor(mainLayout, 0d);
			AnchorPane.setLeftAnchor(mainLayout, 0d);
			AnchorPane.setRightAnchor(mainLayout, 0d);

			tab.setContent(anchorPane);
			tabPane.getTabs().add(tab);

			tab.setOnCloseRequest(event -> {
				// This is necessary to avoid a bug where tabs may be left unable to respond to UI events.
				tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);

				Optional<ButtonType> result = TWUtils.showAlert(Alert.AlertType.CONFIRMATION, "Deleting Item",
						"Are you sure you want to remove \"" + tab.getText() + "\" from your project?", true, parentStage);
				if (result.isPresent() && result.get() == ButtonType.CANCEL || !tabControllerMap.get(tab).checkSave()) {
					event.consume();
				} else {
					tabControllerMap.remove(tab);
				}

				// This is necessary to avoid a bug where tabs may be left unable to respond to UI events.
				Platform.runLater(() -> tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER));
			});

			MainSceneController previousTab = tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem());

			mainController.setDividerPosition(previousTab.getDividerPosition());
			if (previousTab.getToneFile() != null) TWUtils.showAlert(Alert.AlertType.CONFIRMATION, "New Tab",
					"Open tone \"" + previousTab.getToneFile().getName() + "\" for new item?",
					true, parentStage, new ButtonType[]{ButtonType.YES, ButtonType.NO}).ifPresent(buttonType -> {
				if (buttonType == ButtonType.YES) mainController.handleOpenTone(previousTab.getToneFile(), true);
			});

			tabPane.getSelectionModel().selectLast();
			tabPane.getSelectionModel().getSelectedItem().getContent().requestFocus();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void closeTab(Tab tab) {
		if (tabPane.getTabClosingPolicy() == TabPane.TabClosingPolicy.UNAVAILABLE) return;

		EventHandler<Event> handler = tab.getOnCloseRequest();
		if (handler != null) {
			Event event = new Event(null, null, null);
			handler.handle(event);
			if (event.isConsumed()) return;
		}

		tabPane.getTabs().remove(tab);
	}
	void closeSelectedTab() {
		closeTab(tabPane.getSelectionModel().getSelectedItem());
	}

	void setMenuState(MenuState menu_state) {
		editMenu.setDisable(menu_state.editMenuDisabled);
		saveToneMenuItem.setDisable(menu_state.saveToneMenuItemDisabled);
		saveToneAsMenuItem.setDisable(menu_state.saveToneAsMenuItemDisabled);
		manualCLAssignmentMenuItem.setSelected(menu_state.manualCLAssignmentSelected);
	}

	boolean manualCLAssignmentEnabled() {
		return manualCLAssignmentMenuItem.isSelected();
	}

	boolean playMidiAsAssigned() {
		return playMidiMenuItem.isSelected();
	}
	boolean hoverHighlightEnabled() {
		return hoverHighlightMenuItem.isSelected();
	}

	void openParameterTone(File tone) {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleOpenTone(tone, true);
	}

	void requestExit(Event ev) {
		Tab prevTab = tabPane.getSelectionModel().getSelectedItem();

		for (Tab tab : tabPane.getTabs()) {
			MainSceneController controller = tabControllerMap.get(tab);
			if (controller.isToneUnedited()) continue;

			tabPane.getSelectionModel().select(tab);
			double prevPosition = controller.getDividerPosition();
			controller.setDividerPosition(1.0);

			boolean saveCancelled = !controller.checkSave();

			controller.setDividerPosition(prevPosition);

			if (saveCancelled) {
				ev.consume();
				break;
			} else if (controller.isToneUnedited()) {
				refreshToneInstances(controller.getToneFile(), controller);
			}

		}

		tabPane.getSelectionModel().select(prevTab);
	}

	protected void propagateDarkModeSetting() {
		for (Tab tab : tabPane.getTabs()) {
			MainSceneController controller = tabControllerMap.get(tab);
			controller.refreshVerseTextStyle();
		}
		refreshAllChordPreviews();
	}

	protected void refreshAllChordPreviews() {
		for (Tab tab : tabPane.getTabs()) {
			tabControllerMap.get(tab).refreshAllChordPreviews();
		}
	}

	// Notifies other tabs that a tone file was saved to synchronize changes or avoid repeating save requests
	protected void refreshToneInstances(File toneFile, MainSceneController caller) {
		for (Tab tab : tabPane.getTabs()) {
			MainSceneController controller = tabControllerMap.get(tab);
			if (controller != caller && controller.getToneFile() != null && controller.getToneFile().equals(toneFile)) {
				controller.handleOpenTone(toneFile, true);
			}
		}
	}

	public boolean isActiveTab(MainSceneController controller) {
		return tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()) == controller;
	}

}
