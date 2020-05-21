package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.*;
import com.tac550.tonewriter.model.MenuState;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.MainSceneController.OutputMode;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TopSceneController {

	private Stage parentStage;

	@FXML private MenuItem projectTitleMenuItem;
	@FXML private MenuItem exportPDFMenuItem;
	@FXML private MenuItem exitMenuItem;

	@FXML private MenuItem newToneMenuItem;
	@FXML private MenuItem openToneMenuItem;
	@FXML private MenuItem saveToneMenuItem;
	@FXML private MenuItem saveToneAsMenuItem;

	@FXML private Menu editMenu;
	@FXML private MenuItem addCLMenuItem;
	@FXML private MenuItem setKeyMenuItem;
	@FXML private MenuItem editHeaderInfoMenuItem;
	@FXML private CheckMenuItem manualCLAssignmentMenuItem;

	@FXML private CheckMenuItem playMidiMenuItem;
	@FXML private CheckMenuItem hoverHighlightMenuItem;
	@FXML private CheckMenuItem saveLPMenuItem;
	@FXML private MenuItem setLilyPondLocationItem;
	@FXML private MenuItem resetLilyPondLocationItem;
	@FXML private CheckMenuItem darkModeMenuItem;

	@FXML private MenuItem aboutMenuItem;
	@FXML private MenuItem updateMenuitem;

	@FXML private TabPane tabPane;
	private final HashMap<Tab, MainSceneController> tabControllerMap = new HashMap<>();

	@FXML private Button addTabButton;

	static final String headerIconPath = "/media/profile.png";
	static final String keyIconPath = "/media/key.png";
	static final String bookIconPath = "/media/book.png";

	String projectOutputFileName;
	File projectSavingDirectory = MainApp.developerMode ? new File(System.getProperty("user.home") + File.separator + "Downloads")
			: new File(FileSystemView.getFileSystemView().getDefaultDirectory().getPath());

	String projectTitle = "";

	String paperSize = "";

	@FXML private void initialize() {

		// Menu icons
		setMenuIcon(projectTitleMenuItem, bookIconPath);
		setMenuIcon(exportPDFMenuItem, "/media/box-out.png");
		setMenuIcon(exitMenuItem, "/media/sign-error.png");
		setMenuIcon(newToneMenuItem, "/media/file-text.png");
		setMenuIcon(openToneMenuItem, "/media/folder.png");
		setMenuIcon(saveToneMenuItem, "/media/floppy.png");
		setMenuIcon(saveToneAsMenuItem, "/media/floppy-add.png");
		setMenuIcon(addCLMenuItem, "/media/sign-add.png");
		setMenuIcon(setKeyMenuItem, keyIconPath);
		setMenuIcon(editHeaderInfoMenuItem, headerIconPath);
		setMenuIcon(manualCLAssignmentMenuItem, "/media/tag-alt.png");
		setMenuIcon(updateMenuitem, "/media/cloud-sync.png");
		setMenuIcon(aboutMenuItem, "/media/sign-info.png");

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
	 * Project Menu Actions
	 */
	@FXML private void handleExport() {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleExport();
	}
	@FXML private void handleExit() {
		Event event = new Event(null, null, null);
		requestExit(event);
		if (!event.isConsumed()) Platform.exit();
	}

	/*
	 * Tone Menu Actions
	 */
	@FXML void handleNewTone() {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleNewTone();
	}
	@FXML private void handleOpenTone() {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleOpenTone(null, false);
	}
	@FXML void handleSaveTone() {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleSaveTone();
	}
	@FXML private void handleSaveToneAs() {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleSaveToneAs();
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
	 * Settings Menu Actions
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
	@FXML private void handleResetMidi() {
		MidiInterface.resetMidiSystem();
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
		// Load layout from fxml file
		FXMLLoaderIO.loadFXMLLayoutAsync("MainScene.fxml", loader -> {

			SplitPane mainLayout = loader.getRoot();
			MainSceneController newTabController = loader.getController();
			newTabController.setStageAndTopScene(parentStage, this);

			Tab tab = new Tab();

			tab.textProperty().bind(newTabController.getTitleTextProperty());

			tabControllerMap.put(tab, newTabController);

			AnchorPane anchorPane = new AnchorPane();
			anchorPane.getChildren().add(mainLayout);
			AnchorPane.setTopAnchor(mainLayout, 0d);
			AnchorPane.setBottomAnchor(mainLayout, 0d);
			AnchorPane.setLeftAnchor(mainLayout, 0d);
			AnchorPane.setRightAnchor(mainLayout, 0d);

			tab.setOnCloseRequest(event -> {
				// This is necessary to avoid a bug where tabs may be left unable to respond to UI events.
				tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);

				Optional<ButtonType> result = TWUtils.showAlert(Alert.AlertType.CONFIRMATION, "Deleting Item",
						"Are you sure you want to remove \"" + tab.getText() + "\" from your project?", true, parentStage);
				if (result.isPresent() && result.get() == ButtonType.CANCEL || !newTabController.checkSave()) {
					event.consume();
				} else {
					cleanUpTabForRemoval(tab);
				}

				// This is necessary to avoid a bug where tabs may be left unable to respond to UI events.
				Platform.runLater(() -> tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER));
			});

			// Null if this is the first tab being created
			Tab prevTab = tabPane.getSelectionModel().getSelectedItem();
			MainSceneController prevTabController = tabControllerMap.get(prevTab);

			Platform.runLater(() -> {
				tab.setContent(anchorPane);

				if (prevTabController != null) {
					newTabController.setDividerPosition(prevTabController.getDividerPosition());
					if (prevTabController.getToneFile() != null) TWUtils.showAlert(Alert.AlertType.CONFIRMATION, "New Tab",
							"Open tone \"" + prevTabController.getToneFile().getName() + "\" for new item?",
							true, parentStage, new ButtonType[]{ButtonType.YES, ButtonType.NO}, ButtonType.YES).ifPresent(buttonType -> {
						if (buttonType == ButtonType.YES) newTabController.handleOpenTone(prevTabController.getToneFile(), true);
					});

					// Propagate project output mode if it's active on the previous tab.
					if (prevTabController.getOutputMode() == OutputMode.PROJECT)
						newTabController.setProjectOutputMode();

					// Increment any verses used from the built-in verse finder data.
					try {
						ArrayList<String> verses = QuickVerseIO.getBuiltinVerses();

						if (!prevTabController.getTopVerse().isBlank()) {
							newTabController.setTopVerseChoice(prevTabController.getTopVerseChoice());

							String prevTopVerse = prevTabController.getTopVerse();

							if (verses.contains(prevTopVerse) && verses.indexOf(prevTopVerse) < verses.size() - 1)
								newTabController.setTopVerse(
										verses.get(verses.indexOf(prevTopVerse) + 1));
						}
						if (!prevTabController.getBottomVerse().isBlank()) {
							newTabController.setBottomVerseChoice(prevTabController.getBottomVerseChoice());

							String prevBottomVerse = prevTabController.getBottomVerse();

							if (verses.contains(prevBottomVerse) && verses.indexOf(prevBottomVerse) < verses.size() - 1)
								newTabController.setBottomVerse(
										verses.get(verses.indexOf(prevBottomVerse) + 1));
						}

					} catch (IOException e) {
						e.printStackTrace();
					}

					// Increment the number at the end of the previous tab's title, if any.
					// Otherwise give the new tab a generic title
					String prevTitle = prevTabController.getTitle();
					if (!prevTitle.isEmpty() && Character.isDigit(prevTitle.charAt(prevTitle.length() - 1))) {
						Pattern lastIntPattern = Pattern.compile("([0-9]+)$");
						Matcher matcher = lastIntPattern.matcher(prevTitle);
						if (matcher.find()) {
							String prevNum = matcher.group();
							int numIndex = matcher.start();
							int nextNum = Integer.parseInt(prevNum) + 1;

							newTabController.setTitleText(prevTitle.substring(0, numIndex) + nextNum);
						}
					} else {
						newTabController.setTitleText("Item " + (tabPane.getTabs().size() + 1));
					}

				} else {
					// Title text for the first tab created (at startup)
					newTabController.setTitleText("Item 1");
				}

				// Add the tab after the selected one, if any.
				if (prevTabController != null) {
					tabPane.getTabs().add(tabPane.getTabs().indexOf(prevTab) + 1, tab);
				} else {
					tabPane.getTabs().add(tab);
				}
				tabPane.getSelectionModel().select(tab);
				tab.getContent().requestFocus();
			});

		});

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

	void cleanUpTabForRemoval(Tab tab) { // TODO: We seem to have a memory leak (closing tabs doesn't fully free RAM)
		tab.textProperty().unbind();
		tabControllerMap.remove(tab);

		System.gc();
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

	void openParameterFile(File file) {
		tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleOpenTone(file, true);
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

	@FXML boolean handleSetProjectTitle() {
		TextInputDialog dialog = new TextInputDialog(projectTitle);
		dialog.setTitle("Project Title");
		dialog.setHeaderText("Enter project title");
		dialog.setContentText("This appears on the top of every page of a project export");
		dialog.getEditor().setPrefWidth(250);
		ImageView bookIcon = new ImageView(getClass().getResource(bookIconPath).toExternalForm());
		bookIcon.setFitHeight(50);
		bookIcon.setFitWidth(50);
		dialog.setGraphic(bookIcon);
		dialog.initOwner(parentStage);

		Optional<String> result = dialog.showAndWait();

		if (result.isPresent()) {
			projectTitle = result.get();
			return true;
		} else return false;
	}

	void exportProject() throws IOException {

		MainSceneController[] mainControllers = new MainSceneController[tabPane.getTabs().size()];
		for (int i = 0; i < tabPane.getTabs().size(); i++) {
			mainControllers[i] = tabControllerMap.get(tabPane.getTabs().get(i));
		}

		LilyPondInterface.exportItems(projectSavingDirectory, projectOutputFileName, projectTitle,
				mainControllers, paperSize);

	}

	void checkProjectName() {
		while (true) {
			if (!projectTitle.isBlank() || !handleSetProjectTitle()) break;
		}
	}

	void propagateProjectOutputSetting() {
		for (Tab tab : tabPane.getTabs()) {
			tabControllerMap.get(tab).setProjectOutputMode();
		}
	}

	void propagateDarkModeSetting() {
		for (Tab tab : tabPane.getTabs()) {
			MainSceneController controller = tabControllerMap.get(tab);
			controller.refreshVerseTextStyle();
		}
		refreshAllChordPreviews();
	}

	void refreshAllChordPreviews() {
		for (Tab tab : tabPane.getTabs()) {
			tabControllerMap.get(tab).refreshAllChordPreviews();
		}
	}

	// Notifies other tabs that a tone file was saved to synchronize changes or avoid repeating save requests
	void refreshToneInstances(File toneFile, MainSceneController caller) {
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

	public int tabCount() {
		return tabPane.getTabs().size();
	}

}
