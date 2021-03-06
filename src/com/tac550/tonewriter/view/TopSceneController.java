package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.*;
import com.tac550.tonewriter.model.ToneMenuState;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.MainSceneController.ExportMode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TopSceneController {

	private Stage parentStage;

	private final ProjectIO projectIO = new ProjectIO();

	@FXML private MenuItem addItemMenuItem;
	@FXML private MenuItem projectTitleMenuItem;
	@FXML private MenuItem pageSetupMenuItem;
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
	private static final String bookIconPath = "/media/book.png";

	private boolean projectEdited = true;

	// File names and directories are kept separately to make exporting multiple items with the same name
	// and different extensions easier.
	String projectOutputFileName;
	File defaultProjectDirectory = MainApp.getPlatformSpecificInitialChooserDir();

	private File projectFile;
	private String projectTitle = "Unnamed Project";

	private String defaultPaperSize = "";
	private String projectPaperSize = "";

	static final List<String> PAPER_SIZES = List.of("junior-legal (8.0 x 5.0 in)",
			"half letter (5.5 x 8.5 in)", "a5 (148 x 210 mm)", "letter (8.5 x 11.0 in)", "a4 (210 x 297 mm)",
			"legal (8.5 x 14.0 in)", "ledger (17.0 x 11.0 in)", "tabloid (11.0 x 17.0 in)");

	private boolean noHeader = false;
	private boolean evenSpread = true;

	private final ObservableMap<Integer, Tab> tabsToAdd = FXCollections.observableHashMap();

	// Note duration context menus
	private static final RadioMenuItem eighthNoteMenuItem = new RadioMenuItem("eighth note");
	private static final RadioMenuItem quarterNoteMenuItem = new RadioMenuItem("quarter note");
	private static final RadioMenuItem dottedQuarterNoteMenuItem = new RadioMenuItem("dotted quarter note");
	private static final RadioMenuItem halfNoteMenuItem = new RadioMenuItem("half note");
	private static final RadioMenuItem dottedHalfNoteMenuItem = new RadioMenuItem("dotted half note");
	private static final RadioMenuItem wholeNoteMenuItem = new RadioMenuItem("whole note");
	private static final List<RadioMenuItem> clickItems = new ArrayList<>();
	private static final ContextMenu noteMenu = new ContextMenu(eighthNoteMenuItem, quarterNoteMenuItem, dottedQuarterNoteMenuItem, halfNoteMenuItem, dottedHalfNoteMenuItem, wholeNoteMenuItem);
	private static final ToggleGroup durationGroup = new ToggleGroup();
	private static final ImageView eighthNoteTouchItem = new ImageView(TopSceneController.class.getResource("/media/notes/eighth.png").toExternalForm());
	private static final ImageView quarterNoteTouchItem = new ImageView(TopSceneController.class.getResource("/media/notes/quarter.png").toExternalForm());
	private static final ImageView dottedQuarterNoteTouchItem = new ImageView(TopSceneController.class.getResource("/media/notes/dotted-quarter.png").toExternalForm());
	private static final ImageView halfNoteTouchItem = new ImageView(TopSceneController.class.getResource("/media/notes/half.png").toExternalForm());
	private static final ImageView dottedHalfNoteTouchItem = new ImageView(TopSceneController.class.getResource("/media/notes/dotted-half.png").toExternalForm());
	private static final ImageView wholeNoteTouchItem = new ImageView(TopSceneController.class.getResource("/media/notes/whole.png").toExternalForm());
	private static final List<ImageView> touchItems = new ArrayList<>();
	private static final Text touchDescriptionText = new Text("Note info");
	private static final ColorAdjust touchSelectionEffect = new ColorAdjust(1, 1, 1, 1);
	private static final Stage durationTouchStage = new Stage(StageStyle.UNDECORATED);
	private static final List<String> durationMapping = new ArrayList<>();

	// Syllable formatting context menus
	private static final CheckMenuItem boldMenuItem = new CheckMenuItem("Bold");
	private static final CheckMenuItem italicMenuItem = new CheckMenuItem("Italic");
	private static final ContextMenu syllableMenu = new ContextMenu(boldMenuItem, italicMenuItem);

	static {
		Collections.addAll(durationMapping, LilyPondInterface.NOTE_EIGHTH, LilyPondInterface.NOTE_QUARTER,
				LilyPondInterface.NOTE_DOTTED_QUARTER, LilyPondInterface.NOTE_HALF, LilyPondInterface.NOTE_DOTTED_HALF,
				LilyPondInterface.NOTE_WHOLE);

		Collections.addAll(clickItems, eighthNoteMenuItem, quarterNoteMenuItem, dottedQuarterNoteMenuItem,
				halfNoteMenuItem, dottedHalfNoteMenuItem, wholeNoteMenuItem);

		Platform.runLater(() -> {
			for (RadioMenuItem item : clickItems) {
				item.setToggleGroup(durationGroup);
			}

			// Removes drop shadow from note menu. The drop shadow blocks mouse click events,
			// making it impossible to double click a note button near the bottom of the screen.
			noteMenu.setStyle("-fx-effect: null");
		});

		Collections.addAll(touchItems, eighthNoteTouchItem, quarterNoteTouchItem, dottedQuarterNoteTouchItem,
				halfNoteTouchItem, dottedHalfNoteTouchItem, wholeNoteTouchItem);

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
		setMenuIcon(pageSetupMenuItem, "/media/file-exe.png");
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
		defaultPaperSize = MainApp.prefs.get(MainApp.PREFS_PAPER_SIZE, "letter (8.5 x 11.0 in)");
		projectPaperSize = defaultPaperSize;

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
			if (getTabCount() == 1) {
				tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
			} else {
				tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
			}
		});
		tabPane.getSelectionModel().selectedItemProperty().addListener(observable -> {
			MainSceneController selectedController = getSelectedTabScene();
			if (selectedController == null) return;

			// Finish loading the tab if not already done.
			selectedController.runPendingLoadActions();

			updateStageTitle();
			selectedController.applyToneMenuState();
		});

		// Add button initialization

		ImageView addImageView = new ImageView(new Image(getClass().getResource("/media/sign-add.png").toExternalForm(),
				16, 16, false, true));
		addTabButton.setGraphic(addImageView);
		if (MainApp.OS_NAME.startsWith("mac")) addTabButton.getTooltip().setText("Add item (\u2318T)");

		// Behavior for adding asynchronously loading tabs
		tabsToAdd.addListener((MapChangeListener<Integer, Tab>) change -> {
			if (change.wasAdded())
				addNextPendingTabs();
		});

		// listener which triggers project edited status when tabs reordered or removed
		tabPane.getTabs().addListener((ListChangeListener<? super Tab>) change -> {
			while (change.next()) {
				if (change.wasPermutated() || change.wasRemoved())
					projectEdited();
			}
		});

	}

	void performSetup(Stage parent_stage, File arg_file) {
		parentStage = parent_stage;

		// Check type of file in arguments
		if (arg_file != null) {
			if (FilenameUtils.isExtension(arg_file.getName(), "tone"))
				addTab(null, 0, null, null,
						ctr -> ctr.handleOpenTone(arg_file, true, false), false);
			else {
				addTab(null, 0, null, null, ctr -> openProject(arg_file), true);
			}
		} else {
			addTab(null, 0, null, null, null, true);
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
		defaultPaperSize = size;

		MainApp.prefs.put(MainApp.PREFS_PAPER_SIZE, defaultPaperSize);
	}

	private void addNextPendingTabs() {
		// Add the next pending tabs in an uninterrupted sequence.
		int addingIndex = getTabCount();
		while (tabsToAdd.containsKey(addingIndex)) {
			tabPane.getTabs().add(addingIndex, tabsToAdd.get(addingIndex));

			addingIndex++;
		}

		// Clean up current + lower index pending tabs.
		for (int key : new HashSet<>(tabsToAdd.keySet())) {
			if (key <= addingIndex)
				tabsToAdd.remove(key);
		}
	}

	/*
	 * Project Menu Actions
	 */
	@FXML private void addTab() {
		addTab(null, -1, null, null, null, false);
		projectEdited();
	}
	@FXML private void handleSetProjectTitle() {
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

		if (result.isPresent() && !result.get().equals(projectTitle)) {
			setProjectTitle(result.get());
			projectEdited();
		}
	}
	@FXML private void handlePageSetup() {
		FXMLLoaderIO.loadFXMLLayoutAsync("PageSetupView.fxml", loader -> {
			VBox rootLayout = loader.getRoot();
			PageSetupViewController controller = loader.getController();

			controller.setParentController(this);
			controller.setPaperSize(projectPaperSize);
			controller.setNoHeader(noHeader);
			controller.setSpreadSetting(evenSpread);

			Platform.runLater(() -> {
				Stage syllableStage = new Stage();
				syllableStage.setTitle("Page Setup");
				syllableStage.getIcons().add(MainApp.APP_ICON);
				syllableStage.setScene(new Scene(rootLayout));
				syllableStage.initModality(Modality.APPLICATION_MODAL);
				syllableStage.setResizable(false);
				syllableStage.initOwner(parentStage);
				syllableStage.show();
			});
		});
	}

	@FXML private void handleNewProject() {
		// Create new project in existing window
		Event event = new Event(null, null, null);
		requestClose(event);
		if (event.isConsumed())
			return;

		clearAllTabs();

		setProjectTitle("Unnamed Project");
		projectPaperSize = defaultPaperSize;
		projectFile = null;

		addTab();

	}
	@FXML private void handleOpenProject() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Project");
		if (projectFile != null)
			fileChooser.setInitialDirectory(projectFile.getParentFile());
		else
			fileChooser.setInitialDirectory(MainApp.getPlatformSpecificInitialChooserDir());
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ToneWriter Project file (*.twproj)", "*.twproj"));
		File selectedFile = fileChooser.showOpenDialog(parentStage);
		if (selectedFile == null) return;

		// Open project in existing window
		Event event = new Event(null, null, null);
		requestClose(event);
		if (event.isConsumed())
			return;

		openProject(selectedFile);
	}
	@FXML private void handleSaveProject() {
		if (projectFile != null) {
			if (projectIO.saveProject(projectFile, this))
				resetProjectEditedStatus();
		} else {
			handleSaveProjectAs();
		}
	}
	@FXML private void handleSaveProjectAs() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Project As");
		fileChooser.setInitialFileName(TWUtils.replaceInvalidFileChars(projectTitle, "_") + ".twproj");
		if (projectFile != null)
			fileChooser.setInitialDirectory(projectFile.getParentFile());
		else
			fileChooser.setInitialDirectory(MainApp.getPlatformSpecificInitialChooserDir());
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ToneWriter Project file (*.twproj)", "*.twproj"));
		File saveFile = fileChooser.showSaveDialog(parentStage);
		if (saveFile == null) return;

		if (!saveFile.getName().endsWith(".twproj")) {
			saveFile = new File(saveFile.getAbsolutePath() + ".twproj");
		}

		if (projectIO.saveProject(saveFile, this)) {
			projectFile = saveFile;
			resetProjectEditedStatus();
		}
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
	@FXML private void handleNewTone() {
		getSelectedTabScene().handleNewTone();
	}
	@FXML private void handleOpenTone() {
		getSelectedTabScene().handleOpenTone(null, false, false);
	}
	@FXML private void handleSaveTone() {
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
	@FXML private void handleSetHeaderInfo() {
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
		ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultPaperSize, PAPER_SIZES);
		dialog.setTitle("Default Paper Size");
		dialog.setHeaderText("Set the default spaper size");
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

	public void addTab(String with_title, int at_index, String precomp_source, String tone_hash,
					   Consumer<MainSceneController> loading_actions, boolean reset_edited) {
		// Load layout from fxml file
		FXMLLoaderIO.loadFXMLLayoutAsync("MainScene.fxml", loader -> {

			SplitPane mainLayout = loader.getRoot();
			MainSceneController newTabController = loader.getController();
			newTabController.setStageAndTopScene(parentStage, this);

			newTabController.setOriginalIndex(at_index);

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
					if (loading_actions == null && prevTabController.getToneFile() != null) TWUtils.showAlert(Alert.AlertType.CONFIRMATION, "New Tab",
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

							newTabController.setTitle(prevTitle.substring(0, numIndex) + nextNum);
						}
					} else {
						newTabController.setTitle("Item " + (getTabCount() + 1));
					}
				} else {
					newTabController.setTitle("Item 1");
				}

				// Title text for the first tab created (at startup)
				if (with_title != null)
					newTabController.setTitle(with_title);

				// If there is a specified index...
				if (at_index != -1) {
					if (at_index >= getTabCount())
						tabsToAdd.put(at_index, tab);
					else
						tabPane.getTabs().add(at_index, tab);
				} else {
					// Add the tab after the selected one, if any.
					if (prevTabController != null) {
						tabPane.getTabs().add(tabPane.getTabs().indexOf(prevTab) + 1, tab);
					} else {
						tabPane.getTabs().add(tab);
					}
					tabPane.getSelectionModel().select(tab);
					tab.getContent().requestFocus();
				}

				if (precomp_source != null)
					newTabController.setLilyPondSource(precomp_source);

				newTabController.setCachedToneHash(tone_hash);

				// Save any loading operations for later (when the user switches to the tab or exports the project).
				newTabController.setPendingLoadActions(loading_actions);
				// If this is the first tab, run them now (since this tab will be autoselected).
				if (at_index == 0)
					newTabController.runPendingLoadActions();

				// Reset project edited status if indicated
				// (usually because this tab is being added as part of a project load).
				if (reset_edited)
					resetProjectEditedStatus();
			});
		});
	}

	private void closeTab(Tab tab) {
		if (tabPane.getTabClosingPolicy() == TabPane.TabClosingPolicy.UNAVAILABLE) return;

		EventHandler<Event> handler = tab.getOnCloseRequest();
		Event event = new Event(null, null, null);
		handler.handle(event);
		if (event.isConsumed())
			return;

		prepareTabForRemoval(tab);
		tabPane.getTabs().remove(tab);
	}
	void closeSelectedTab() {
		closeTab(tabPane.getSelectionModel().getSelectedItem());
	}
	void clearAllTabs() {
		List<Tab> tabs = new ArrayList<>(tabPane.getTabs());
		Collections.reverse(tabs);

		// Prevents tabs from automatically being sequentially selected (and loaded) after the selected one closes
		tabPane.getSelectionModel().select(0);

		for (Tab tab : tabs)
			prepareTabForRemoval(tab);

		tabPane.getTabs().clear();
	}
	void prepareTabForRemoval(Tab tab) {
		tab.textProperty().unbind();
		tabControllerMap.remove(tab);
	}

	MainSceneController getSelectedTabScene() {
		return tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem());
	}

	public String getProjectTitle() {
		return projectTitle;
	}

	public void setProjectTitle(String title) {
		projectTitle = title;
	}

	public File getProjectFile() {
		return projectFile;
	}
	public String getProjectFileName() {
		return projectFile != null ? projectFile.getName() : "Unsaved Project";
	}

	void autoSaveProjectIfUnsaved() {
		TWUtils.cleanUpAutosaves();

		try {
			String titleFileName = TWUtils.replaceInvalidFileChars(getProjectTitle(), "_") + "-";
			projectIO.saveProject(
					TWUtils.createTWTempFile(titleFileName + new SimpleDateFormat("yyyy,MM,dd 'at' HH.mm.ss z")
							.format(new Date(System.currentTimeMillis())), "Autosave.twproj"),
					this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void openProject(File selected_file) {
		clearAllTabs();

		if (selected_file.exists()) {
			if (projectIO.openProject(selected_file, this)) {
				projectFile = selected_file;
			} else {
				clearAllTabs();
				addTab();
			}
		}
	}

	void recoverProject(File selected_file) {
		openProject(selected_file);

		projectFile = null;
	}

	void projectEdited() {
		if (!projectEdited) {
			projectEdited = true;
			updateStageTitle();
		}
	}
	public void resetProjectEditedStatus() {
		projectEdited = false;
		updateStageTitle();
	}
	public boolean getProjectEdited() {
		return projectEdited;
	}

	private void updateStageTitle() {
		MainSceneController selectedTabScene = getSelectedTabScene();
		if (selectedTabScene != null)
			selectedTabScene.updateStageTitle();
	}

	void setMenuState(ToneMenuState menu_state) {
		editMenu.setDisable(menu_state.editMenuDisabled);
		saveToneMenuItem.setDisable(menu_state.saveToneMenuItemDisabled);
		saveToneAsMenuItem.setDisable(menu_state.saveToneAsMenuItemDisabled);
		manualCLAssignmentMenuItem.setSelected(menu_state.manualCLAssignmentSelected);
	}

	boolean getManualCLAssignmentStatus() {
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

			// Don't check for tabs that haven't been edited
			if (!controller.getToneEdited()) continue;

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
		LilyPondInterface.exportItems(defaultProjectDirectory, projectOutputFileName, projectTitle,
				getTabControllers(), projectPaperSize, noHeader, evenSpread);
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
	}

	void refreshAllChordPreviews() {
		for (Tab tab : tabPane.getTabs()) {
			tabControllerMap.get(tab).refreshToneChordPreviews();
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

	public String getPaperSize() {
		return projectPaperSize;
	}
	public void setPaperSize(String size) {
		if (size.isBlank()) {
			if (!projectPaperSize.equals(defaultPaperSize)) {
				projectPaperSize = defaultPaperSize;
				projectEdited();
			}
		} else if (!size.equals(projectPaperSize)) {
			projectPaperSize = size;
			projectEdited();
		}
	}

	public boolean getNoHeader() {
		return noHeader;
	}
	public void setNoHeader(boolean no_header) {
		if (noHeader != no_header) {
			noHeader = no_header;
			projectEdited();
		}
	}

	public boolean getEvenSpread() {
		return evenSpread;
	}
	public void setEvenSpread(boolean even_spread) {
		if (evenSpread != even_spread) {
			evenSpread = even_spread;
			projectEdited();
		}
	}

	public MainSceneController[] getTabControllers() {
		int tabCount = getTabCount();

		MainSceneController[] mainControllers = new MainSceneController[tabCount];
		for (int i = 0; i < tabCount; i++) {
			mainControllers[i] = tabControllerMap.get(tabPane.getTabs().get(i));
		}

		return mainControllers;
	}

	void showNoteMenu(SyllableText syllable, Button noteButton) {
		int noteButtonIndex = syllable.getAssociatedButtons().indexOf(noteButton);

		// Behavior
		for (RadioMenuItem item : clickItems) {
			item.setOnAction(event -> {
				syllable.setNoteDuration(durationMapping.get(clickItems.indexOf(item)), noteButtonIndex);
				projectEdited();
			});
		}

		// Initial state
		clickItems.get(durationMapping.indexOf(syllable.getNoteDuration(noteButtonIndex))).setSelected(true);

		// Showing / Positioning
		noteMenu.show(noteButton, Side.BOTTOM, 0,
				(syllable.getAssociatedButtons().size() - syllable.getAssociatedButtons().indexOf(noteButton) - 1)
						* VerseLineViewController.NOTE_BUTTON_HEIGHT.get());
	}

	void showSyllableMenu(SyllableText syllable) {
		// Behavior
		boldMenuItem.setOnAction(event -> {
			syllable.setBold(boldMenuItem.isSelected());
			projectEdited();
		});
		italicMenuItem.setOnAction(event -> {
			syllable.setItalic(italicMenuItem.isSelected());
			projectEdited();
		});

		// Initial state
		boldMenuItem.setSelected(syllable.getBold());
		italicMenuItem.setSelected(syllable.getItalic());

		// Showing / Positioning
		syllableMenu.show(syllable, Side.TOP, 0, 0);
	}

	void showTouchNoteMenu(SyllableText syllable, Button noteButton, TouchEvent touchEvent) {
		int noteButtonIndex = syllable.getAssociatedButtons().indexOf(noteButton);

		// Behavior
		durationTouchStage.setOnHiding(event -> {
			for (ImageView item : touchItems) {
				if (item.getEffect() != null) {
					syllable.setNoteDuration(durationMapping.get(touchItems.indexOf(item)), noteButtonIndex);
					projectEdited();
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
