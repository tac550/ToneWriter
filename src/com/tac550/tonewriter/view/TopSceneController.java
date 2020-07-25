package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.*;
import com.tac550.tonewriter.model.ToneMenuState;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.MainSceneController.ExportMode;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.*;
import org.apache.commons.io.FilenameUtils;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TopSceneController {

	private Stage parentStage;

	@FXML private MenuItem addItemMenuItem;
	@FXML private MenuItem projectTitleMenuItem;
	@FXML private MenuItem newProjectMenuItem;
	@FXML private MenuItem openProjectMenuItem;
	@FXML private MenuItem saveProjectMenuItem;
	@FXML private MenuItem saveProjectAsMenuItem;
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
	@FXML private MenuItem updateMenuItem;

	@FXML private TabPane tabPane;
	private final HashMap<Tab, MainSceneController> tabControllerMap = new HashMap<>();

	@FXML private Button addTabButton;

	static final String headerIconPath = "/media/profile.png";
	static final String keyIconPath = "/media/key.png";
	static final String bookIconPath = "/media/book.png";

	private boolean projectEdited = true;

	// File names and directories are kept separately to make exporting multiple items with the same name
	// and different extensions easier.
	String projectOutputFileName;
	File projectSavingDirectory = MainApp.developerMode ? new File(System.getProperty("user.home") + File.separator + "Downloads")
			: new File(FileSystemView.getFileSystemView().getDefaultDirectory().getPath());

	private File projectFile;
	private String projectTitle = "Unnamed Project";

	String paperSize = "";

	// Note duration menu elements are re-used to save memory
	private static final RadioMenuItem eighthNoteMenuItem = new RadioMenuItem("eighth note");
	private static final RadioMenuItem quarterNoteMenuItem = new RadioMenuItem("quarter note");
	private static final RadioMenuItem dottedQuarterNoteMenuItem = new RadioMenuItem("dotted quarter note");
	private static final RadioMenuItem halfNoteMenuItem = new RadioMenuItem("half note");
	private static final RadioMenuItem wholeNoteMenuItem = new RadioMenuItem("whole note");
	private static final List<RadioMenuItem> clickItems = new ArrayList<>();
	private static final ContextMenu noteMenu = new ContextMenu(eighthNoteMenuItem, quarterNoteMenuItem, dottedQuarterNoteMenuItem, halfNoteMenuItem, wholeNoteMenuItem);
	private static final ToggleGroup durationGroup = new ToggleGroup();
	private static final ImageView eighthNoteTouchItem = new ImageView(TopSceneController.class.getResource("/media/eighth-note.png").toExternalForm());
	private static final ImageView quarterNoteTouchItem = new ImageView(TopSceneController.class.getResource("/media/quarter-note.png").toExternalForm());
	private static final ImageView dottedQuarterNoteTouchItem = new ImageView(TopSceneController.class.getResource("/media/dotted-quarter-note.png").toExternalForm());
	private static final ImageView halfNoteTouchItem = new ImageView(TopSceneController.class.getResource("/media/half-note.png").toExternalForm());
	private static final ImageView wholeNoteTouchItem = new ImageView(TopSceneController.class.getResource("/media/whole-note.png").toExternalForm());
	private static final List<ImageView> touchItems = new ArrayList<>();
	private static final Text touchDescriptionText = new Text("Note info");
	private static final ColorAdjust touchSelectionEffect = new ColorAdjust(0.5, 1, 0.5, 1);
	private static final Stage durationTouchStage = new Stage(StageStyle.UNDECORATED);
	private static final List<String> durationMapping = new ArrayList<>();

	static {
		Collections.addAll(durationMapping, LilyPondInterface.NOTE_EIGHTH, LilyPondInterface.NOTE_QUARTER,
				LilyPondInterface.NOTE_DOTTED_QUARTER, LilyPondInterface.NOTE_HALF, LilyPondInterface.NOTE_WHOLE);

		Collections.addAll(clickItems, eighthNoteMenuItem, quarterNoteMenuItem, dottedQuarterNoteMenuItem,
				halfNoteMenuItem, wholeNoteMenuItem);

		Platform.runLater(() -> {
			for (RadioMenuItem item : clickItems) {
				item.setToggleGroup(durationGroup);
			}

			// Removes drop shadow from note menu. The drop shadow blocks mouse click events,
			// making it impossible to double click a note button near the bottom of the screen.
			noteMenu.setStyle("-fx-effect: null");
		});

		Collections.addAll(touchItems, eighthNoteTouchItem, quarterNoteTouchItem, dottedQuarterNoteTouchItem,
				halfNoteTouchItem, wholeNoteTouchItem);

		VBox mainBox = new VBox();
		mainBox.setAlignment(Pos.CENTER);
		HBox durationTouchBox = new HBox();
		durationTouchBox.setAlignment(Pos.CENTER);
		durationTouchBox.setSpacing(10);
		mainBox.getChildren().addAll(touchDescriptionText, durationTouchBox);

		durationTouchBox.getChildren().addAll(touchItems);
		Scene durationTouchScene = new Scene(mainBox);

		for (ImageView item : touchItems) {
			item.setPreserveRatio(true);
			item.setFitWidth(40);
			item.setFitHeight(40);
		}

		durationTouchScene.setOnTouchMoved(TopSceneController::selectTouchDuration);

		durationTouchScene.setOnTouchReleased(event -> {
			selectTouchDuration(event);
			Platform.runLater(durationTouchStage::close);
		});


		durationTouchStage.focusedProperty().addListener((ov, oldVal, newVal) -> {
			if (!newVal)
				durationTouchStage.close();
		});

		durationTouchStage.setScene(durationTouchScene);

	}
	private static void selectTouchDuration(TouchEvent event) {
		for (ImageView release_item : touchItems) {
			release_item.setEffect(null);
		}

		double totalWidth = durationTouchStage.getWidth();
		double stepSize = totalWidth / touchItems.size();

		Point2D touchPoint = durationTouchStage.getScene().getRoot()
				.screenToLocal(event.getTouchPoint().getScreenX(), event.getTouchPoint().getScreenY());

		int index = Math.min(Math.max(0, (int) (touchPoint.getX() / stepSize)), touchItems.size() - 1);
		touchItems.get(index).setEffect(touchSelectionEffect);
	}

	@FXML private void initialize() {

		// Menu icons
		setMenuIcon(addItemMenuItem, "/media/sign-add.png");
		setMenuIcon(projectTitleMenuItem, bookIconPath);
		setMenuIcon(newProjectMenuItem, "/media/file-sound.png");
		setMenuIcon(openProjectMenuItem, "/media/folder.png");
		setMenuIcon(saveProjectMenuItem, "/media/floppy.png");
		setMenuIcon(saveProjectAsMenuItem, "/media/floppy-add.png");
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
		setMenuIcon(updateMenuItem, "/media/cloud-sync.png");
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
		tabPane.getSelectionModel().selectedItemProperty().addListener(observable -> {
			MainSceneController selectedController = getSelectedTabScene();
			if (selectedController == null) return;
			selectedController.updateStageTitle();
			selectedController.applyToneMenuState();
		});

		// Add button initialization

		ImageView addImageView = new ImageView(new Image(getClass().getResource("/media/sign-add.png").toExternalForm(),
				16, 16, false, true));
		addTabButton.setGraphic(addImageView);
		if (MainApp.OS_NAME.startsWith("mac")) addTabButton.getTooltip().setText("Add item (\u2318T)");

	}

	void performSetup(Stage parent_stage, File arg_file) {
		parentStage = parent_stage;

		// Check type of file in arguments
		if (arg_file != null) {
			if (FilenameUtils.isExtension(arg_file.getName(), "tone"))
				addTab(arg_file);
			else
				openProject(arg_file);
		} else {
			addTab();
		}
	}

	private static void setMenuIcon(MenuItem menu_item, String image_path) {
		ImageView imageView = new ImageView(TopSceneController.class.getResource(image_path).toExternalForm());
		double menuIconSize = 30;
		imageView.setFitHeight(menuIconSize);
		imageView.setFitWidth(menuIconSize);
		menu_item.setGraphic(imageView);
	}

	private void setDefaultPaperSize(String size) {
		paperSize = size;

		MainApp.prefs.put(MainApp.PREFS_PAPER_SIZE, paperSize);
	}

	/*
	 * Project Menu Actions
	 */
	@FXML void addTab() {
		addTab(null);
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
			getSelectedTabScene().updateStageTitle();
			return true;
		} else return false;
	}

	@FXML private void handleNewProject() {
		// Create new project in existing window
		Event event = new Event(null, null, null);
		requestClose(event);
		if (event.isConsumed())
			return;

		for (Tab tab : new ArrayList<>(tabPane.getTabs()))
			forceCloseTab(tab);

		projectTitle = "Unnamed Project";
		projectFile = null;

		addTab();

	}
	@FXML private void handleOpenProject() {
		FileChooser fileChooser = new FileChooser();
		if (projectFile != null)
			fileChooser.setInitialDirectory(projectFile.getParentFile());
		else
			fileChooser.setInitialDirectory(new File(FileSystemView.getFileSystemView().getDefaultDirectory().getPath()));
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ToneWriter Project file (*.twproj)", "*.twproj"));
		File selectedFile = fileChooser.showOpenDialog(parentStage);
		if (selectedFile == null) return;

		// Open project in existing window
		Event event = new Event(null, null, null);
		requestClose(event);
		if (event.isConsumed())
			return;

		for (Tab tab : new ArrayList<>(tabPane.getTabs()))
			forceCloseTab(tab);

		openProject(selectedFile);
	}
	@FXML private void handleSaveProject() {
		if (projectFile != null) {
			if (!ProjectIO.saveProject(projectFile, this))
				return;
		} else {
			if (!handleSaveProjectAs())
				return;
		}
		resetProjectEditedStatus();
	}
	@FXML private boolean handleSaveProjectAs() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialFileName(projectTitle + ".twproj");
		if (projectFile != null)
			fileChooser.setInitialDirectory(projectFile.getParentFile());
		else
			fileChooser.setInitialDirectory(projectSavingDirectory);
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ToneWriter Project file (*.twproj)", "*.twproj"));
		File saveFile = fileChooser.showSaveDialog(parentStage);
		if (saveFile == null) return false;

		if (!saveFile.getName().endsWith(".twproj")) {
			saveFile = new File(saveFile.getAbsolutePath() + ".twproj");
		}

		boolean success = ProjectIO.saveProject(saveFile, this);
		if (success) {
			projectFile = saveFile;
		}

		return success;
	}
	@FXML private void handleExport() {
		getSelectedTabScene().handleExport();
	}
	@FXML private void handleExit() {
		Window window = parentStage.getScene().getWindow();
		window.fireEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
	}

	/*
	 * Tone Menu Actions
	 */
	@FXML void handleNewTone() {
		getSelectedTabScene().handleNewTone();
	}
	@FXML private void handleOpenTone() {
		getSelectedTabScene().handleOpenTone(null, false, false);
	}
	@FXML void handleSaveTone() {
		getSelectedTabScene().handleSaveTone();
	}
	@FXML private void handleSaveToneAs() {
		getSelectedTabScene().handleSaveToneAs();
	}

	/*
	 * Edit Menu Actions
	 */
	@FXML private void handleCreateChantLine() {
		getSelectedTabScene().createChantLine(0, true);
	}
	@FXML private void handleSetKeySignature() {
		getSelectedTabScene().handleSetKeySignature();
	}
	@FXML private void handleEditHeaderInfo() {
		getSelectedTabScene().handleEditHeaderInfo();
	}
	@FXML private void handleToggleManualCLAssignment() {
		getSelectedTabScene().handleToggleManualCLAssignment();
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
	@FXML private void handleSetDefaultPaperSize() {
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
		dialog.setTitle("Default Paper Size");
		dialog.setHeaderText("Set the default paper size");
		dialog.initOwner(parentStage);
		Optional<String> result = dialog.showAndWait();

		result.ifPresent(this::setDefaultPaperSize);
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

	public void addTab(File with_tone) {
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
				if (result.isPresent() && result.get() == ButtonType.CANCEL || !newTabController.checkSaveTone()) {
					event.consume();
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
						if (buttonType == ButtonType.YES) newTabController.handleOpenTone(prevTabController.getToneFile(), true, true);
					});

					// Propagate project output mode if it's active on the previous tab.
					if (prevTabController.getExportMode() == ExportMode.PROJECT)
						newTabController.setProjectOutputMode();

					// Increment any verses used from the built-in verse finder data.
					try {
						List<String> verses = QuickVerseIO.getBuiltinVerses();

						if (!prevTabController.getTopVerse().isBlank()) {
							newTabController.setTopVerseChoice(prevTabController.getTopVerseChoice());

							String prevTopVerse = prevTabController.getTopVerse();

							if (verses.contains(prevTopVerse) && verses.indexOf(prevTopVerse) < verses.size() - 1) {
								String nextVerse = verses.get(verses.indexOf(prevTopVerse) + 1);
								if (nextVerse.startsWith("^"))
									newTabController.setTopVerse(verses.get(0));
								else if (!nextVerse.startsWith("-"))
									newTabController.setTopVerse(verses.get(verses.indexOf(prevTopVerse) + 1));
							}
						}
						if (!prevTabController.getBottomVerse().isBlank()) {
							newTabController.setBottomVerseChoice(prevTabController.getBottomVerseChoice());

							String prevBottomVerse = prevTabController.getBottomVerse();

							if (verses.contains(prevBottomVerse) && verses.indexOf(prevBottomVerse) < verses.size() - 1) {
								String nextVerse = verses.get(verses.indexOf(prevBottomVerse) + 1);
								if (nextVerse.startsWith("^"))
									newTabController.setBottomVerse(verses.get(0));
								else if (!nextVerse.startsWith("-"))
									newTabController.setBottomVerse(verses.get(verses.indexOf(prevBottomVerse) + 1));
							}
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

					if (with_tone != null) {
						newTabController.handleOpenTone(with_tone, true, false);
					}

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

	private void forceCloseTab(Tab tab) {
		cleanUpTabForRemoval(tab);
		tabPane.getTabs().remove(tab);
	}
	private void closeTab(Tab tab) {
		if (tabPane.getTabClosingPolicy() == TabPane.TabClosingPolicy.UNAVAILABLE) return;

		EventHandler<Event> handler = tab.getOnCloseRequest();
		Event event = new Event(null, null, null);
		handler.handle(event);
		if (event.isConsumed())
			return;

		cleanUpTabForRemoval(tab);
		tabPane.getTabs().remove(tab);
	}
	void closeSelectedTab() {
		closeTab(tabPane.getSelectionModel().getSelectedItem());
	}

	void cleanUpTabForRemoval(Tab tab) {
		tab.textProperty().unbind();
		tabControllerMap.remove(tab);

		System.gc();
	}

	MainSceneController getSelectedTabScene() {
		return tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem());
	}

	public String getProjectTitle() {
		return projectTitle;
	}

	void projectEdited() {
		projectEdited = true;
	}
	void resetProjectEditedStatus() {
		projectEdited = false;
		getSelectedTabScene().updateStageTitle();
	}

	void openProject(File selected_file) {
		if (selected_file.exists() && ProjectIO.openProject(selected_file, this))
			resetProjectEditedStatus();
	}

	boolean getProjectEdited() {
		return projectEdited;
	}

	void setMenuState(ToneMenuState menu_state) {
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

	void requestClose(Event ev) {
		if (!checkSaveProject() || !checkAllToneSaves()) {
			ev.consume();
		}
	}

	/*
	 * Returns false if the user chooses cancel or closes. Doing that should halt any impending file related functions.
	 */
	private boolean checkSaveProject() {
		if (!projectEdited)
			return true;

		ButtonType saveButton = new ButtonType("Save");
		ButtonType dontSaveButton = new ButtonType("Don't Save");
		ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

		Optional<ButtonType> result = TWUtils.showAlert(Alert.AlertType.CONFIRMATION, "Project Save Confirmation",
				"Save changes to project \"" + projectTitle + "\"?", true, parentStage,
				new ButtonType[] {saveButton, dontSaveButton, cancelButton}, cancelButton);

		if (result.isPresent()) {
			if (result.get() == saveButton) {
				handleSaveProject();
				return true;
			} else return result.get() == dontSaveButton;
		} else {
			// Not returning true, so no save will occur and the prompt will appear again next time.
			return false;
		}
	}

	boolean checkAllToneSaves() {
		Tab prevTab = tabPane.getSelectionModel().getSelectedItem();

		for (Tab tab : tabPane.getTabs()) {
			MainSceneController controller = tabControllerMap.get(tab);

			tabPane.getSelectionModel().select(tab);
			double prevPosition = controller.getDividerPosition();
			controller.setDividerPosition(1.0);

			boolean saveCancelled = !controller.checkSaveTone();

			controller.setDividerPosition(prevPosition);

			if (saveCancelled) {
				tabPane.getSelectionModel().select(prevTab);
				return false;
			} else if (!controller.getToneEdited()) { // The user selected Save; update other open instances.
				refreshToneInstances(controller.getToneFile(), controller);
			}
		}

		return true;
	}

	void exportProject() throws IOException {

		LilyPondInterface.exportItems(projectSavingDirectory, projectOutputFileName, projectTitle,
				getTabControllers(), paperSize);

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
				controller.handleOpenTone(toneFile, true, controller.getHideToneHeader());
			}
		}
	}

	public boolean isActiveTab(MainSceneController controller) {
		return getSelectedTabScene() == controller;
	}

	public int getTabCount() {
		return tabPane.getTabs().size();
	}

	public MainSceneController[] getTabControllers() {
		MainSceneController[] mainControllers = new MainSceneController[tabPane.getTabs().size()];
		for (int i = 0; i < tabPane.getTabs().size(); i++) {
			mainControllers[i] = tabControllerMap.get(tabPane.getTabs().get(i));
		}

		return mainControllers;
	}

	static void showNoteMenu(SyllableText syllable, Button noteButton) {
		int noteButtonIndex = syllable.getAssociatedButtons().indexOf(noteButton);

		// Behavior
		for (RadioMenuItem item : clickItems) {
			item.setOnAction(event ->
					syllable.setNoteDuration(durationMapping.get(clickItems.indexOf(item)), noteButtonIndex));
		}

		// Initial state
		clickItems.get(durationMapping.indexOf(syllable.getNoteDuration(noteButtonIndex))).setSelected(true);

		// Showing / Positioning
		noteMenu.show(noteButton, Side.BOTTOM, 0,
				(syllable.getAssociatedButtons().size() - syllable.getAssociatedButtons().indexOf(noteButton) - 1)
						* VerseLineViewController.NOTE_BUTTON_HEIGHT.get());
	}

	static void showTouchNoteMenu(SyllableText syllable, Button noteButton, TouchEvent touchEvent) {
		int noteButtonIndex = syllable.getAssociatedButtons().indexOf(noteButton);

		// Behavior
		durationTouchStage.setOnHiding(event -> {
			for (ImageView item : touchItems) {
				if (item.getEffect() != null) {
					syllable.setNoteDuration(durationMapping.get(touchItems.indexOf(item)), noteButtonIndex);
					item.setEffect(null);
				}
			}
		});

		// Initial state
		ImageView selectedItem = touchItems.get(durationMapping.indexOf(syllable.getNoteDuration(noteButtonIndex)));
		selectedItem.setEffect(touchSelectionEffect);
		touchDescriptionText.setText(noteButton.getText());
		durationTouchStage.getScene().getRoot().setStyle(noteButton.getStyle());

		// Showing / Positioning
		double totalWidth = durationTouchStage.getWidth();
		double stepSize = totalWidth / touchItems.size();

		touchEvent.getTouchPoint().ungrab();
		touchEvent.getTouchPoint().grab(durationTouchStage.getScene());

		durationTouchStage.show();
		durationTouchStage.setX(touchEvent.getTouchPoint().getScreenX() - (stepSize * touchItems.indexOf(selectedItem))
				- (stepSize / 2));
		durationTouchStage.setY(touchEvent.getTouchPoint().getScreenY() - durationTouchStage.getHeight() / 2);
	}

}
