package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.FXMLLoaderIO;
import com.tac550.tonewriter.io.LilyPondInterface;
import com.tac550.tonewriter.io.Syllables;
import com.tac550.tonewriter.io.ToneIO;
import com.tac550.tonewriter.model.ToneMenuState;
import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Platform;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MainSceneController {

	enum ExportMode {
		NONE,
		ITEM,
		PROJECT
	}

	private Stage parentStage;
	private TopSceneController topSceneController;

	@FXML private SplitPane mainSplitPane;

	@FXML private StackPane openToneHintPane;
	@FXML private Button openToneHintButton;

	@FXML private ChoiceBox<String> topVerseChoice;
	@FXML private TextField topVerseField;
	@FXML private Button topVerseButton;
	@FXML private TextField titleTextField;
	@FXML private TextField subtitleTextField;
	@FXML private TextArea verseArea;
	@FXML private ChoiceBox<String> bottomVerseChoice;
	@FXML private TextField bottomVerseField;
	@FXML private Button bottomVerseButton;
	@FXML private StackPane setVersePane;
	@FXML private Button setVerseButton;
	@FXML private HBox setVerseProgressBox;
	@FXML private StackPane verseTextHintPane;

	@FXML private MenuButton optionsButton;
	@FXML private ToggleGroup titleOptions;
	@FXML private RadioMenuItem largeTitleOption;
	@FXML private RadioMenuItem hiddenTitleOption;

	@FXML private CheckMenuItem hideToneHeaderOption;
	@FXML private CheckMenuItem pageBreakOption;

	@FXML private CheckMenuItem extendTextTopOption;
	@FXML private CheckMenuItem extendTextBottomOption;
	@FXML private CheckMenuItem breakOnlyOnBlankOption;

	private final ToneMenuState toneMenuState = new ToneMenuState();

	private Consumer<MainSceneController> pendingLoadActions;

	private File toneFile;
	private int originalIndex = -1;
	private String cachedItemSource = "";
	private String cachedToneHash = "";

	private String keySignature = "C major";
	private String leftText = "";
	private String rightText = "";
	private boolean manualCLAssignment = false;

	static boolean LoadingTone = false;
	static String copiedChord = "";

	private boolean setVerseCancelled = false;
	private String lastVerseSet = "";

	private boolean toneEdited = false;

	private boolean verseEdited = false;

	private ExportMode exportMode = ExportMode.NONE;
	// File names and directories are kept separately to make exporting multiple items with the same name
	// and different extensions easier.
	private String itemExportFileName = MainApp.APP_NAME + " Render";
	private File itemSavingDirectory = MainApp.developerMode ? new File(System.getProperty("user.home") + File.separator + "Downloads")
		: MainApp.getPlatformSpecificInitialChooserDir();

	@FXML private ScrollPane toneScrollPane;
	@FXML private VBox chantLineBox;
	private final List<ChantLineViewController> chantLineControllers = new ArrayList<>();
	private boolean loadingTone = false;

	private final List<ChantLineViewController> mainChantLines = new ArrayList<>();

	@FXML private VBox verseLineBox;
	private final List<VerseLineViewController> verseLineControllers = new ArrayList<>();
	final List<Task<FXMLLoader>> verseLineLoaders = new ArrayList<>();

	@FXML private void initialize() {

		// Set up behavior for reader verse text completion buttons and fields
		topVerseButton.setOnAction((ae) -> showQuickVerseStage(topVerseField));
		bottomVerseButton.setOnAction((ae) -> showQuickVerseStage(bottomVerseField));

		Stream.of(topVerseChoice, bottomVerseChoice).forEach(box -> {
			box.getItems().add("Reader:");
			box.getItems().add("Sing:");
			box.getItems().add("Clergy:");
			box.getItems().add("Priest:");
			box.getItems().add("Deacon:");
			box.getItems().add("v:");
			box.getSelectionModel().select(0);
		});

		// Behavior when verseArea content changes
		verseArea.textProperty().addListener((ov, oldVal, newVal) -> {
			verseTextHintPane.setVisible(newVal.isEmpty());

			boolean showSetVerse = !newVal.equals(lastVerseSet) || verseEdited;
			setVerseButton.setVisible(showSetVerse);
			setVersePane.setMouseTransparent(!showSetVerse);

			topSceneController.projectEdited();
		});
		AnchorPane.setLeftAnchor(verseArea, 0.0); AnchorPane.setRightAnchor(verseArea, 0.0);
		AnchorPane.setTopAnchor(verseArea, 0.0); AnchorPane.setBottomAnchor(verseArea, 0.0);

		setVerseButton.setVisible(false);

		// Title and subtitle field tooltip info reflects current output mode
		titleTextField.getTooltip().setOnShown(event -> {
			if (exportMode == ExportMode.ITEM || topSceneController.getTabCount() == 1)
				((Tooltip) event.getTarget()).setText("Appears on every page (Single-item export mode)");
			else
				((Tooltip) event.getTarget()).setText("Centered above subtitle, appears once (Project export mode)");
		});
		subtitleTextField.getTooltip().setOnShown(event -> {
			if (exportMode == ExportMode.ITEM || topSceneController.getTabCount() == 1)
				((Tooltip) event.getTarget()).setText("First page only (Single-item export mode)");
			else
				((Tooltip) event.getTarget()).setText("Centered below title, appears once (Project export mode)");
		});

		// Set up the stack pane to behave like the whole thing extends the clickable area of the button.
		openToneHintPane.addEventFilter(MouseEvent.MOUSE_ENTERED, ev -> openToneHintButton.setStyle("-fx-background-color: linear-gradient(#57969c, #61a2b1);"));
		openToneHintPane.addEventFilter(MouseEvent.MOUSE_EXITED, ev -> openToneHintButton.setStyle("-fx-background-color: linear-gradient(#61a2b1, #61b0b1);"));
		openToneHintPane.addEventFilter(MouseEvent.MOUSE_CLICKED, ev -> openToneHintButton.fire());

		// Selecting either extended text option deselects the other and highlights verse field it's replacing.
		extendTextTopOption.selectedProperty().addListener((ov, oldVal, newVal) -> {
			if (newVal) {
				extendTextBottomOption.setSelected(false);
				topVerseField.setStyle("-fx-base: #FF0000");
			} else {
				topVerseField.setStyle("");
			}
		});
		extendTextBottomOption.selectedProperty().addListener((ov, oldVal, newVal) -> {
			if (newVal) {
				extendTextTopOption.setSelected(false);
				bottomVerseField.setStyle("-fx-base: #FF0000");
			} else {
				bottomVerseField.setStyle("");
			}
		});

		verseArea.setTextFormatter(new TWUtils.inputFormatter());
		titleTextField.setTextFormatter(new TWUtils.inputFormatter());
		subtitleTextField.setTextFormatter(new TWUtils.inputFormatter());
		topVerseField.setTextFormatter(new TWUtils.inputFormatter());
		bottomVerseField.setTextFormatter(new TWUtils.inputFormatter());

		// listeners for triggering project edited status
		titleTextField.textProperty().addListener(change -> topSceneController.projectEdited());
		subtitleTextField.textProperty().addListener(change -> topSceneController.projectEdited());
		topVerseChoice.valueProperty().addListener(change -> topSceneController.projectEdited());
		topVerseField.textProperty().addListener(change -> topSceneController.projectEdited());
		bottomVerseChoice.valueProperty().addListener(change -> topSceneController.projectEdited());
		bottomVerseField.textProperty().addListener(change -> topSceneController.projectEdited());

		for (Toggle item : titleOptions.getToggles())
			item.selectedProperty().addListener(change -> topSceneController.projectEdited());

		for (MenuItem item : optionsButton.getItems()) {
			if (item instanceof CheckMenuItem checkItem)
				checkItem.selectedProperty().addListener(change -> topSceneController.projectEdited());
		}

	}

	void setStageAndTopScene(Stage stage, TopSceneController top_scene) {
		parentStage = stage;
		topSceneController = top_scene;
	}

	public void setKeySignature(String key) {
		keySignature = key;
		refreshChordKeySignatures(keySignature);
	}

	public void setHeaderStrings(String left, String right) {
		leftText = left;
		rightText = right;
	}

	public Task<FXMLLoader> createVerseLine(String line) {

		Task<FXMLLoader> loaderTask = FXMLLoaderIO.loadFXMLLayoutAsync("verseLineView.fxml", loader -> {
			VerseLineViewController controller = loader.getController();
			controller.setParentControllers(this, topSceneController);

			controller.setVerseLine(line);
		});
		verseLineLoaders.add(loaderTask);

		return loaderTask;
	}
	public Task<FXMLLoader> createChantLine(int index, boolean recalculateNames) {

		return FXMLLoaderIO.loadFXMLLayoutAsync("chantLineView.fxml", loader -> {

			ChantLineViewController controller = loader.getController();
			GridPane chantLineLayout = loader.getRoot();
			controller.setMainController(this);

			chantLineControllers.add(index, controller);
			Platform.runLater(() -> {
				chantLineBox.getChildren().add(index, chantLineLayout);
				if (recalculateNames) recalcCLNames();
			});
		});
	}

	public void applyLoadedVerses(boolean project_edited_after) {
		Platform.runLater(() -> {
			for (Task<FXMLLoader> loader : verseLineLoaders) {
				try {
					verseLineControllers.add(loader.get().getController());
					verseLineBox.getChildren().add(loader.get().getRoot());
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}

			verseLineLoaders.clear();

			syncCVLMapping();

			if (project_edited_after)
				topSceneController.projectEdited();
			else
				topSceneController.resetProjectEditedStatus();
		});
	}

	public void recalcCLNames() {
		boolean previousWasPrime = false;
		int nextAlternate = 1;
		char currentLetter = 65;
		ChantLineViewController prevMainLine = null;
		mainChantLines.clear();

		for (ChantLineViewController chantLine : chantLineControllers) {
			if (chantLineControllers.get(chantLineControllers.size()-1) != chantLine) { // If not the last
				chantLine.setName(currentLetter, previousWasPrime, nextAlternate);
				previousWasPrime = false;

				chantLine.setNumAlts(0);
				chantLine.setHasPrime(false);

				if (chantLine.getIsPrime()) { // Prime chant line
					if (prevMainLine != null) {
						prevMainLine.setHasPrime(true);
					}

					previousWasPrime = true;
				} else if (chantLine.getIsAlternate()) { // Alternate chant line
					nextAlternate++;
				} else { // Normal chant line
					if (prevMainLine != null) {
						prevMainLine.setNumAlts(nextAlternate - 1);
					}
					prevMainLine = chantLine;
					mainChantLines.add(chantLine);

					nextAlternate = 1;
					currentLetter++;
				}
			} else {
				chantLine.makeFinal();
				// If this is not the only chant line...
				if (prevMainLine != null) {
					prevMainLine.setNumAlts(nextAlternate - 1);
				}
			}
		}

		toneEdited();
		syncCVLMapping();
	}

	void syncCVLMapping() {
		// First, update barlines and refresh their display
		VerseLineViewController prev = null;
		for (VerseLineViewController verseLine : verseLineControllers) {
			if (verseLine.notFirstInItem() && !Objects.requireNonNull(prev).isSeparator())
				verseLine.linkBeforeBarLine(Objects.requireNonNull(prev).afterBarProperty());
			else if (Arrays.asList(VLineEditViewController.barStrings).indexOf(verseLine.getBeforeBar())
					>= VLineEditViewController.firstBarOptionsLimit)
				verseLine.setBarlines(" ", "unchanged");

			if (isLastVerseLineOfSection(verseLine))
				verseLine.setBarlines("unchanged", "||");

			prev = verseLine;
		}

		if (toneFile == null) return; // No tone is loaded; don't continue to phrase assignment.

		// If manual mode is selected, allow user to choose all chant line assignments.
		if (manualCLAssignmentEnabled()) {

			for (VerseLineViewController verseLine : verseLineControllers) {
				if (verseLine.isSeparator()) continue;

				// Default last chant line selection to Final Phrase.
				if (isLastVerseLineOfSection(verseLine)) {
					verseLine.setChantLines(chantLineControllers.toArray(new ChantLineViewController[0]),
							chantLineControllers.size() - 1);
				} else {
					verseLine.setChantLines(chantLineControllers.toArray(new ChantLineViewController[0]));
				}
			}

		} else {

			int firstRepeated = 0;
			int CLNum = 0; // For retrieving proper chant line
			for (int VLNum = 0; VLNum < verseLineControllers.size(); VLNum++) {
				// Correct counter overflow for the chant line list (there will usually be more verse lines than chant lines)
				if (CLNum == mainChantLines.size()) {
					CLNum = firstRepeated;

					// If there are no main chant lines, use the Final Phrase.
					if (mainChantLines.isEmpty()) {
						verseLineControllers.get(VLNum).setChantLines(new ChantLineViewController[] {chantLineControllers.get(chantLineControllers.size()-1)});
						continue;
					}
				}

				ChantLineViewController currentChantLine = mainChantLines.get(CLNum);

				// If it's the last line before the end or a separator, it gets the Final Phrase.
				if (isLastVerseLineOfSection(verseLineControllers.get(VLNum))) {
					verseLineControllers.get(VLNum).setChantLines(new ChantLineViewController[] {chantLineControllers.get(chantLineControllers.size()-1)});
					CLNum = 0; // Resets back to the first chant line. Only matters if this was a separator ending.
					VLNum++; // Skips over separator. If it's the final line overall, has no effect because loop stops anyway.
					continue;
					// If it's the second-to-last line before the end or a separator, it gets prime, if any.
				} else if ((VLNum + 2 == verseLineControllers.size() || verseLineControllers.get(VLNum + 2).isSeparator()) && currentChantLine.getHasPrime()) {
					verseLineControllers.get(VLNum).setChantLines(new ChantLineViewController[] {chantLineControllers.get(chantLineControllers.indexOf(currentChantLine) + 1 + currentChantLine.getNumAlts())});
					continue;
				}

				// Save the index of the first-repeated chant line, on the first encounter only.
				if (firstRepeated == 0 && currentChantLine.getFirstRepeated()) {
					firstRepeated = CLNum;
				}

				// For normal cases do this.
				if (!currentChantLine.getIsPrime() && !currentChantLine.getIsAlternate()) {
					ChantLineViewController[] associatedControllers = new ChantLineViewController[currentChantLine.getNumAlts() + 1];
					associatedControllers[0] = currentChantLine;
					for (int i = 1; i < associatedControllers.length; i++) {
						associatedControllers[i] = chantLineControllers.get(chantLineControllers.indexOf(currentChantLine) + i);
					}
					verseLineControllers.get(VLNum).setChantLines(associatedControllers);
				} else {
					// Do another go-around on the same verse line but with the next chant line.
					VLNum--;
				}
				CLNum++;
			}
		}

	}

	boolean isLastVerseLineOfSection(VerseLineViewController line) {
		return (verseLineControllers.indexOf(line) == verseLineControllers.size() - 1
				|| verseLineControllers.get(verseLineControllers.indexOf(line) + 1).isSeparator());
	}

	public void clearChantLines() {
		chantLineControllers.clear();
		chantLineBox.getChildren().clear();
	}

	private void clearVerseLines() {
		verseLineControllers.clear();
		verseLineBox.getChildren().clear();
	}

	@FXML private void handleOpenToneHint() {
		handleOpenTone(null, false, false);
	}

	@FXML private void handleSetVerse() {

		if (!verseLineControllers.isEmpty()) {
			Optional<ButtonType> result = TWUtils.showAlert(AlertType.CONFIRMATION, "Set Verse Confirmation",
					"Are you sure you want to set this verse text? (changes and chord assignments in the current text will be lost)", true);
			if (result.isPresent() && result.get() == ButtonType.CANCEL) return;
			if (exportMode == ExportMode.ITEM)
				exportMode = ExportMode.NONE;
		}

		clearVerseLines();
		verseEdited = false;

		lastVerseSet = verseArea.getText();

		if (lastVerseSet.isEmpty()) {
			setVerseButton.setVisible(false);
			setVersePane.setMouseTransparent(true);

			topSceneController.projectEdited();
			return;
		}

		// Show working indicator
		setVerseButton.setVisible(false);
		setVerseProgressBox.setVisible(true);
		setVersePane.setMouseTransparent(false);

		// Sends off the contents of the verse field (trimmed, and with any multi-spaces reduced to one) to be broken into syllables.
		Task<Void> syllabificationTask = new Task<>() {

			@Override
			protected Void call() {

				String[] lines = Syllables.getSyllabificationLines(lastVerseSet, parentStage);

				if (setVerseCancelled) {
					setVerseCancelled = false;
					return null;
				}

				for (String line : lines) {
					createVerseLine(line);
				}

				applyLoadedVerses(true);

				// Hide working indicator
				setVerseProgressBox.setVisible(false);
				setVersePane.setMouseTransparent(true);

				return null;
			}
		};

		Thread syllableThread = new Thread(syllabificationTask);
		syllableThread.start();

	}

	@FXML private void handleCancelSetVerse() {
		setVerseCancelled = true;

		lastVerseSet = "";

		setVerseButton.setVisible(true);
		setVerseProgressBox.setVisible(false);
		setVersePane.setMouseTransparent(false);
	}

	/*
	 * Returns false if the user chooses cancel or closes. Doing that should halt any impending file related functions.
	 */
	boolean checkSaveTone() {
		if (toneFile == null || !toneEdited || !isToneSavable())
			return true;

		ButtonType saveButton = new ButtonType("Save");
		ButtonType dontSaveButton = new ButtonType("Don't Save");
		ButtonType cancelButton = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

		Optional<ButtonType> result = TWUtils.showAlert(AlertType.CONFIRMATION, "Tone Save Confirmation",
				"Save changes to tone \"" + toneFile.getName() + "\"?", true, parentStage,
				new ButtonType[] {saveButton, dontSaveButton, cancelButton}, cancelButton);

		if (result.isPresent()) {
			if (result.get() == saveButton) {
				handleSaveTone();
				return true;
			} else return result.get() == dontSaveButton;
		} else {
			// Not returning true, so no save will occur and the prompt will appear again next time.
			return false;
		}
	}
	private boolean isToneSavable() {
		return (nonInternalToneLoaded() && !TWUtils.isBuiltinTone(toneFile)) || MainApp.developerMode;
	}

	private boolean createNewTone() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save Tone As");
		fileChooser.setInitialFileName(".tone");

		if (toneFile != null && isToneSavable() && nonInternalToneLoaded())
			fileChooser.setInitialDirectory(toneFile.getParentFile());
		else
			fileChooser.setInitialDirectory(MainApp.getPlatformSpecificInitialChooserDir());

		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TONE file (*.tone)", "*.tone"));
		File saveFile = fileChooser.showSaveDialog(parentStage);
		if (saveFile == null) return false;

		if (!saveFile.getName().endsWith(".tone")) {
			saveFile = new File(saveFile.getAbsolutePath() + ".tone");
		}

		if (ToneIO.createToneFile(saveFile)) {
			toneFile = saveFile;

			toneMenuState.saveToneMenuItemDisabled = false;
			toneMenuState.saveToneAsMenuItemDisabled = false;
			toneMenuState.editMenuDisabled = false;
			applyToneMenuState();

			return true;
		} else {

			TWUtils.showAlert(AlertType.ERROR, "Error", "An error occurred while creating the tone!",
					true, parentStage);

			return false;
		}
	}
	private boolean loadTone(File selected_file, boolean selectHideToneHeader) {
		if (selected_file == null) {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Open Tone");
			if (toneFile != null && nonInternalToneLoaded()) {
				if (TWUtils.isBuiltinTone(toneFile)) fileChooser.setInitialDirectory(MainApp.BUILT_IN_TONE_DIR);
				else fileChooser.setInitialDirectory(toneFile.getParentFile());
			} else {
				if (MainApp.BUILT_IN_TONE_DIR.exists())
					fileChooser.setInitialDirectory(MainApp.BUILT_IN_TONE_DIR);
				else
					fileChooser.setInitialDirectory(MainApp.getPlatformSpecificInitialChooserDir());
			}
			fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TONE file (*.tone)", "*.tone"));
			selected_file = fileChooser.showOpenDialog(parentStage);
		}
		if (selected_file == null) return false;

		if (selected_file.exists()) {
			toneFile = selected_file;

			ToneIO toneReader = getToneReader();

			loadingTone = true;
			if (toneReader.loadTone(toneFile, this)) {
				hideToneHeaderOption.setSelected(selectHideToneHeader);

				loadingTone = false;
				return true;
			} else {
				TWUtils.showAlert(AlertType.ERROR, "Error", "Error loading tone!", true, parentStage);
				// Since a tone was not loaded (or at least, not correctly),
				toneFile = null;

				loadingTone = false;
				return false;
			}

		} else {
			TWUtils.showAlert(AlertType.ERROR, "Error", "Requested tone file doesn't exist!", true, parentStage);
			return false;
		}
	}

	public void swapToneFile(File tone_file) {
		toneFile = tone_file;
		toneMenuState.saveToneMenuItemDisabled = !isToneSavable();
		applyToneMenuState();
	}

	void updateStageTitle() {
		if (topSceneController.isActiveTab(this)) {
			// Update stage title to show loaded tone name and edit status
			parentStage.setTitle((topSceneController.getProjectEdited() ? "*" : "")
					+ topSceneController.getProjectFileName() + " - "
					+ MainApp.APP_NAME + (toneFile != null ? " - " + toneFile.getName() : "")
					+ (toneEdited ? "*" : ""));
		}
	}

	void applyToneMenuState() {
		topSceneController.setMenuState(toneMenuState);

		openToneHintPane.setVisible(toneMenuState.editMenuDisabled);
		openToneHintPane.setMouseTransparent(!toneMenuState.editMenuDisabled);
	}

	/*
	 * Project Menu Actions
	 */
	void handleExport() {
		// First make sure a filename and output mode are selected and that the target location is ready.
		if (exportMode != ExportMode.NONE) { // If an output mode is already selected for this tab...
			// Ask whether to keep settings and overwrite or reset export settings
			Optional<ButtonType> result = TWUtils.showAlert(AlertType.CONFIRMATION,
					"Overwrite " + (exportMode == ExportMode.ITEM ? "(Single-item mode)" : "(Project mode)"),
					"Overwrite the previous output? (Cancel to change output settings)", true,
					parentStage);
			if (result.isPresent() && result.get() == ButtonType.CANCEL) {
				try {
					setNewRenderFilename();
				} catch (RenderFormatException e) { return; }
			} else if (!deletePreviousRender()) {
				TWUtils.showAlert(AlertType.ERROR, "Error",
						"An error occurred while overwriting the previous files, attempting to output anyway...",
						true, parentStage);
			}
		} else {
			try {
				setNewRenderFilename();
			} catch (RenderFormatException e) { return; }
		}

		try {
			if (exportMode == ExportMode.ITEM) {
				if (!LilyPondInterface.exportItems(itemSavingDirectory, itemExportFileName,
						hiddenTitleOption.isSelected() ? "" : titleTextField.getText(),
						new MainSceneController[] {this}, topSceneController.getPaperSize(), topSceneController.getNoHeader(),
						topSceneController.getEvenSpread())) {
					TWUtils.showAlert(AlertType.ERROR, "Error", "An error occurred while exporting!",
							true, parentStage);
				}
			} else if (exportMode == ExportMode.PROJECT) {
				topSceneController.exportProject();
			}
		} catch (IOException e) {
			e.printStackTrace();
			TWUtils.showAlert(AlertType.ERROR, "Error", "There was an IO error while saving!",
					true, parentStage);
		}
	}

	/*
	 * Tone Menu Actions
	 */
	void handleNewTone() {
		if (checkSaveTone() && createNewTone()) {
			clearChantLines();
			toneMenuState.editMenuDisabled = false;
			toneMenuState.saveToneAsMenuItemDisabled = false;

			// Reset settings pertaining to any previously-loaded tone
			setHeaderStrings("", "");
			keySignature = "C major";
			toneMenuState.manualCLAssignmentSelected = false;

			Task<FXMLLoader> loaderTask = createChantLine(0, true);
			loaderTask.setOnSucceeded(event -> handleSaveTone()); // So that the tone is loadable
		}
	}
	public void handleOpenTone(File tone_file, boolean auto_load, boolean selectHideToneHeader) {
		LoadingTone = MainApp.lilyPondAvailable(); // Don't block re-renders during loading if there's no lilypond
		if ((auto_load || checkSaveTone()) && loadTone(tone_file, selectHideToneHeader)) {
			toneMenuState.editMenuDisabled = false;
			toneMenuState.saveToneMenuItemDisabled = false;
			toneMenuState.saveToneAsMenuItemDisabled = false;
			toneMenuState.saveToneMenuItemDisabled = !isToneSavable();

			resetToneEditedStatus();
			applyToneMenuState();

			if (exportMode == ExportMode.ITEM)
				exportMode = ExportMode.NONE;
		}

		LoadingTone = false;
		refreshToneChordPreviews();
	}
	void handleSaveTone() {
		if (toneFile == null || !isToneSavable()) return;

		ToneIO toneWriter = getToneWriter();
		if (!toneWriter.saveToneToFile(toneFile)) {
			TWUtils.showAlert(AlertType.ERROR, "Error", "Saving error!", true, parentStage);
		} else { // Save successful
			resetToneEditedStatus();
			topSceneController.refreshToneInstances(toneFile, this);
		}

	}
	void handleSaveToneAs() {
		if (createNewTone()) handleSaveTone();
	}

	public ToneIO getToneWriter() {
		return new ToneIO(chantLineControllers, this, keySignature, leftText, rightText);
	}
	private ToneIO getToneReader() {
		return new ToneIO(chantLineControllers, this);
	}

	/*
	 * Edit Menu Actions
	 */
	void handleSetKeySignature() {

		new Thread(() -> Platform.runLater(() -> {
			List<String> choices = new ArrayList<>(List.of("C major", "G major", "D major", "A major", "E major", "B major",
					"F\u266F major", "C\u266F major", "F major", "B\u266Dmajor", "E\u266Dmajor", "A\u266Dmajor", "D\u266Dmajor",
					"G\u266Dmajor", "C\u266Dmajor", "A minor", "E minor", "B minor", "F\u266F minor", "C\u266F minor", "G\u266F minor",
					"D\u266F minor", "A\u266F minor", "D minor", "G minor", "C minor", "F minor", "B\u266Dminor", "E\u266Dminor",
					"A\u266Dminor"));

			ChoiceDialog<String> dialog = new ChoiceDialog<>(keySignature, choices);
			dialog.setTitle("Key Choice");
			dialog.setHeaderText("Choose a key");
			ImageView keyIcon = new ImageView(getClass().getResource(TopSceneController.keyIconPath).toExternalForm());
			keyIcon.setFitHeight(50);
			keyIcon.setFitWidth(50);
			dialog.setGraphic(keyIcon);
			dialog.initOwner(parentStage);
			Optional<String> result = dialog.showAndWait();

			result.ifPresent(key -> {
				if (!key.equals(keySignature)) {
					toneEdited();
					setKeySignature(key);
				}
			});
		})).start();

	}
	void handleEditHeaderInfo() {

		new Thread(() -> Platform.runLater(() -> {
			Dialog<Pair<String, String>> dialog = new Dialog<>();
			dialog.setTitle("Header Info");
			dialog.setHeaderText("Input header info for first page");
			ImageView headerIcon = new ImageView(getClass().getResource(TopSceneController.headerIconPath).toExternalForm());
			headerIcon.setFitHeight(50);
			headerIcon.setFitWidth(50);
			dialog.setGraphic(headerIcon);
			dialog.initOwner(parentStage);

			dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

			GridPane grid = new GridPane();
			grid.setHgap(10);
			grid.setVgap(10);
			grid.setPadding(new Insets(20, 20, 10, 10));

			TextField leftField = new TextField();
			leftField.setPromptText("Tone #");
			leftField.setTextFormatter(new TWUtils.inputFormatter());
			leftField.setText(leftText);

			TextField rightField = new TextField();
			rightField.setPromptText("Composer - System");
			rightField.setTextFormatter(new TWUtils.inputFormatter());
			rightField.setText(rightText);
			rightField.setPrefWidth(200);

			grid.add(new Label("Left side:"), 0, 0);
			grid.add(leftField, 1, 0);
			grid.add(new Label("Right side:"), 2, 0);
			grid.add(rightField, 3, 0);

			dialog.getDialogPane().setContent(grid);

			dialog.setResultConverter(dialogButton -> {
				if (dialogButton == ButtonType.OK) {
					return new Pair<>(leftField.getText(), rightField.getText());
				}
				return null;
			});

			Optional<Pair<String, String>> result = dialog.showAndWait();

			result.ifPresent(leftRightText -> {
				String tempLeftText = leftRightText.getKey();
				String tempRightText = leftRightText.getValue();
				if (leftRightText.getKey().matches("[0-9]")) tempLeftText = "Tone " + tempLeftText;

				if (!(tempLeftText.equals(leftText) && tempRightText.equals(rightText))) {
					toneEdited();

					setHeaderStrings(tempLeftText, tempRightText);
				}
			});
		})).start();

	}
	void handleToggleManualCLAssignment() {
		manualCLAssignment = topSceneController.getManualCLAssignmentStatus();

		toneEdited();
		syncCVLMapping();
	}
	public boolean manualCLAssignmentEnabled() {
		return manualCLAssignment;
	}
	public void setManualCLAssignmentSilently(boolean enable) { // Doesn't trigger tone edited status
		manualCLAssignment = enable;
		toneMenuState.manualCLAssignmentSelected = enable;
		topSceneController.setMenuState(toneMenuState);
	}

	private void refreshChordKeySignatures(String key) {
		for (ChantLineViewController chantLineController : chantLineControllers) {
	    	chantLineController.setKeySignature(key);
	    }
	}
	void refreshToneChordPreviews() {
		if (!MainApp.lilyPondAvailable()) return;

		for (ChantLineViewController chantLineController : chantLineControllers) {
			chantLineController.refreshAllChordPreviews();
	    }

	}

	public void removeChantLine(ChantLineViewController chantLineViewController) {
		chantLineBox.getChildren().remove(chantLineViewController.getMainPane());
		chantLineControllers.remove(chantLineViewController);
		recalcCLNames();
	}
	void removeVerseLine(VerseLineViewController verseLineViewController) {

		// If this is a separator line, and the previous line ends with a double bar, change it to a single bar.
		if (verseLineControllers.indexOf(verseLineViewController) > 0) {
			VerseLineViewController previousController = verseLineControllers.get(
					verseLineControllers.indexOf(verseLineViewController) - 1);

			if (verseLineViewController.isSeparator() && previousController.getAfterBar().equals("||"))
				previousController.setBarlines("unchanged", "|");
		}

		verseLineBox.getChildren().remove(verseLineViewController.getRootPane());
		verseLineControllers.remove(verseLineViewController);

		verseEdited();

		syncCVLMapping();
	}
	void chantLineUp(ChantLineViewController chantLineViewController) {
		int i = chantLineControllers.indexOf(chantLineViewController);
		int j = i-1;

		ObservableList<Node> workingCollection = FXCollections.observableArrayList(chantLineBox.getChildren());
		Collections.swap(workingCollection, i, j);
		chantLineBox.getChildren().setAll(workingCollection);
		Collections.swap(chantLineControllers, i, j);

		recalcCLNames();
	}
	void chantLineDown(ChantLineViewController chantLineViewController) {
		int i = chantLineControllers.indexOf(chantLineViewController);
		int j = i + 1;

		ObservableList<Node> workingCollection = FXCollections.observableArrayList(chantLineBox.getChildren());
		Collections.swap(workingCollection, i, j);
		chantLineBox.getChildren().setAll(workingCollection);
		Collections.swap(chantLineControllers, i, j);

		recalcCLNames();
	}

	void clearFirstRepeated() {
		for (ChantLineViewController controller : chantLineControllers) {
			controller.resetFRState();
		}
	}
	public void setFirstRepeated(String chant_line) {
		for (ChantLineViewController chantLine : chantLineControllers) {
			if (chantLine.getName().equals(chant_line)) {
				chantLine.toggleFirstRepeated();
			}
		}
	}

	void scrollCLineIntoView(GridPane cline) {
		double viewportHeight = toneScrollPane.getViewportBounds().getHeight();
		double scrollPaneHeight = toneScrollPane.getContent().getBoundsInLocal().getHeight();
		double maxY = cline.getBoundsInParent().getMaxY();

		if (maxY < (viewportHeight / 2)) {
			toneScrollPane.setVvalue(0);
		} else if ((maxY >= (viewportHeight / 2)) & (maxY <= (scrollPaneHeight - viewportHeight / 2))) {
			toneScrollPane.setVvalue((maxY - (viewportHeight / 2)) / (scrollPaneHeight - viewportHeight));
		} else if (maxY >= (scrollPaneHeight - (viewportHeight / 2))) {
			toneScrollPane.setVvalue(1);
		}
	}

	private boolean nonInternalToneLoaded() {
		return !toneFile.getAbsolutePath().startsWith(new File(System.getProperty("java.io.tmpdir")).getAbsolutePath());
	}

	private void setNewRenderFilename() throws RenderFormatException {
		ExportMode tempExportMode;

		if (topSceneController.getTabCount() > 1) {
			ButtonType projectBT = new ButtonType("Entire project");
			ButtonType itemBT = new ButtonType("Current item only");

			Optional<ButtonType> result = TWUtils.showAlert(AlertType.CONFIRMATION, "Export Type",
					"Which type of export do you want? (Both result in 1 PDF file)", true, parentStage,
					new ButtonType[] {projectBT, itemBT, ButtonType.CANCEL}, projectBT);

			if (result.isPresent()) {
				if (result.get() == projectBT) {
					tempExportMode = ExportMode.PROJECT;
				} else if (result.get() == itemBT) {
					tempExportMode = ExportMode.ITEM;
				} else throw new RenderFormatException();
			} else throw new RenderFormatException();

		} else {
			tempExportMode = ExportMode.ITEM;
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Export As");
		fileChooser.setInitialFileName(TWUtils.replaceInvalidFileChars(String.format("%s.pdf",
				tempExportMode == ExportMode.ITEM ? titleTextField.getText()
						: FilenameUtils.removeExtension(topSceneController.getProjectFileName())), "_"));
		fileChooser.setInitialDirectory(tempExportMode == ExportMode.ITEM ? itemSavingDirectory :
				topSceneController.getProjectFile() != null ? topSceneController.getProjectFile().getParentFile() : topSceneController.defaultProjectDirectory);
		if (!fileChooser.getInitialDirectory().exists())
			fileChooser.setInitialDirectory(MainApp.getPlatformSpecificInitialChooserDir());
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF file (*.pdf)", "*.pdf"));
		File PDFFile = fileChooser.showSaveDialog(parentStage);
		if (PDFFile == null) {
			throw new RenderFormatException();
		} else {

			if (tempExportMode == ExportMode.PROJECT) {
				topSceneController.projectOutputFileName = FilenameUtils.removeExtension(PDFFile.getName());
				topSceneController.defaultProjectDirectory = PDFFile.getParentFile();

				topSceneController.propagateProjectOutputSetting();
			} else {
				itemExportFileName = FilenameUtils.removeExtension(PDFFile.getName());
				itemSavingDirectory = PDFFile.getParentFile();
			}

			exportMode = tempExportMode;
		}
	}

	ExportMode getExportMode() {
		return exportMode;
	}
	void setProjectOutputMode() {
		exportMode = ExportMode.PROJECT;
	}

	private boolean deletePreviousRender() {
		boolean item = exportMode == ExportMode.ITEM;

		File lyFile = new File((item ? itemSavingDirectory : topSceneController.defaultProjectDirectory) +
				File.separator + (item ? itemExportFileName : topSceneController.projectOutputFileName) + ".ly");
		File pdfFile = new File(FilenameUtils.removeExtension(lyFile.getAbsolutePath()) + ".pdf");

        return pdfFile.delete() || lyFile.delete();
	}

	private void showQuickVerseStage(TextField targetField) {

		FXMLLoaderIO.loadFXMLLayoutAsync("quickVerseView.fxml", loader -> {
			BorderPane rootLayout = loader.getRoot();
			QuickVerseController controller = loader.getController();

			controller.setTargetField(targetField);

			Platform.runLater(() -> {
				Stage syllableStage = new Stage();
				syllableStage.setTitle("Verse Finder");
				syllableStage.getIcons().add(MainApp.APP_ICON);
				syllableStage.setScene(new Scene(rootLayout));
				syllableStage.initModality(Modality.APPLICATION_MODAL);
				syllableStage.setResizable(false);
				syllableStage.initOwner(parentStage);
				syllableStage.show();

				controller.focusFilterField();
			});
		});

	}

	void refreshVerseTextStyle() {
		for (VerseLineViewController verseLine : verseLineControllers) {
			verseLine.refreshTextStyle();
		}
	}

	void toneEdited() {
		toneEdited(true);
	}
	public void toneEdited(boolean project_edited) {
		if (!toneEdited && isToneSavable() && !loadingTone) {
			toneEdited = true;
			updateStageTitle();
		}

		if (project_edited)
			topSceneController.projectEdited();
	}
	void resetToneEditedStatus() {
		toneEdited = false;
		updateStageTitle();
	}
	public boolean getToneEdited() {
		return toneEdited;
	}

	void verseEdited() {
		verseEdited = true;

		setVerseButton.setVisible(true);
		setVersePane.setMouseTransparent(false);

		topSceneController.projectEdited();
	}

	public void runPendingLoadActions() {
		if (pendingLoadActions != null) {
			pendingLoadActions.accept(this);
			pendingLoadActions = null;
		}
	}
	public Consumer<MainSceneController> getPendingLoadActions() {
		return pendingLoadActions;
	}
	public void setPendingLoadActions(Consumer<MainSceneController> pendingLoadActions) {
		this.pendingLoadActions = pendingLoadActions;
	}
	public boolean fullyLoaded() {
		return getPendingLoadActions() == null;
	}

	public File getToneFile() {
		return toneFile;
	}
	public int getOriginalIndex() {
		return originalIndex;
	}
	public void setOriginalIndex(int index) {
		originalIndex = index;
	}
	public String getLilyPondSource() {
		if (fullyLoaded())
			cachedItemSource = LilyPondInterface.generateItemSource(this);

		return cachedItemSource;
	}
	// Also removes any leading and trailing whitespace, keeping only one newline at the end.
	void setLilyPondSource(String source) {
		cachedItemSource = source.strip() + "\n";
	}
	public String getCachedToneHash() {
		return cachedToneHash;
	}
	public void setCachedToneHash(String hash) {
		cachedToneHash = hash;
	}

	ObservableStringValue getTitleTextProperty() {
		return titleTextField.textProperty();
	}

	void setDividerPosition(double position) {
		mainSplitPane.setDividerPosition(0, position);
	}

	double getDividerPosition() {
		return mainSplitPane.getDividerPositions()[0];
	}

	public String getTopVerseChoice() {
		return topVerseChoice.getValue();
	}
	public void setTopVerseChoice(String choice) {
		if (topVerseChoice.getItems().contains(choice)) topVerseChoice.setValue(choice);
	}
	public String getTopVerse() {
		return topVerseField.getText();
	}
	public void setTopVerse(String verse) {
		topVerseField.setText(verse);
	}
	public String getVerseAreaText() {
		return verseArea.getText();
	}
	public void setVerseAreaText(String text) {
		verseArea.setText(text);
	}
	public String getBottomVerseChoice() {
		return bottomVerseChoice.getValue();
	}
	public void setBottomVerseChoice(String choice) {
		if (bottomVerseChoice.getItems().contains(choice)) bottomVerseChoice.setValue(choice);
	}
	public String getBottomVerse() {
		return bottomVerseField.getText();
	}
	public void setBottomVerse(String verse) {
		bottomVerseField.setText(verse);
	}
	public boolean getLargeTitle() {
		return largeTitleOption.isSelected();
	}
	public RadioMenuItem getSelectedTitleOption() {
		return (RadioMenuItem) titleOptions.getSelectedToggle();
	}
	public boolean getHideToneHeader() {
		return hideToneHeaderOption.isSelected();
	}
	public boolean getPageBreak() {
		return pageBreakOption.isSelected();
	}
	public int getExtendTextSelection() { // Only one is selected at a time, so 3 = both is not expected.
		return (extendTextTopOption.isSelected() ? 1 : 0) + (extendTextBottomOption.isSelected() ? 2 : 0);
	}
	public boolean getBreakOnlyOnBlank() {
		return breakOnlyOnBlankOption.isSelected();
	}
	public String getFinalTitleContent() {
		return hiddenTitleOption.isSelected() || exportMode == ExportMode.ITEM ?
				"" : titleTextField.getText();
	}
	public String getTitle() {
		return titleTextField.getText();
	}
	public void setTitle(String title) {
		titleTextField.setText(title);
	}
	public String getSubtitle() {
		return subtitleTextField.getText();
	}
	public void setSubtitle(String subtitle) {
		subtitleTextField.setText(subtitle);
	}
	public void setOptions(String title_format, boolean hide_header, boolean page_break, int extended_text, boolean break_only_on_blank) {
		titleOptions.selectToggle(titleOptions.getToggles().stream()
				.filter(toggle -> ((RadioMenuItem) toggle).getText().equals(title_format)).toArray(Toggle[]::new)[0]);
		hideToneHeaderOption.setSelected(hide_header);
		pageBreakOption.setSelected(page_break);
		switch (extended_text) {
			case 1 -> extendTextTopOption.setSelected(true);
			case 2 -> extendTextBottomOption.setSelected(true);
		}
		breakOnlyOnBlankOption.setSelected(break_only_on_blank);
	}
	public String getLeftHeaderText() {
		return hideToneHeaderOption.isSelected() ? "" : leftText;
	}
	public String getRightHeaderText() {
		return hideToneHeaderOption.isSelected() ? "" : rightText;
	}
	public List<VerseLineViewController> getVerseLineControllers() {
		return verseLineControllers;
	}
	public String getKeySignature() {
		return keySignature;
	}
	boolean isLoadingTone() {
		return loadingTone;
	}

	private static class RenderFormatException extends Exception {}
}
