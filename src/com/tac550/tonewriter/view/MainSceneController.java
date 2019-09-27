package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.FXMLLoaderIO;
import com.tac550.tonewriter.io.LilyPondWriter;
import com.tac550.tonewriter.io.Syllables;
import com.tac550.tonewriter.io.ToneReaderWriter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/*
 * TEST VERSES:
 * 

Lord, I call upon You, hear me!
Hear me O Lord!
Lord, I call upon You, hear me!
Receive the voice of my prayer
when I call upon You.//
Hear me O Lord!

Let my prayer arise
in Your sight as incense.
And let the lifting up of my hands
be an evening sacrifice.//
Hear me O Lord!

 */

public class MainSceneController {

	private Stage mainStage;

	private File toneFile;

	private String currentKey = "C major";
	private String headerText = "";
	private String paperSize = "";

	@FXML private MenuItem newToneMenuItem;
	@FXML private MenuItem openToneMenuItem;
	@FXML private MenuItem saveToneMenuItem;
	@FXML private MenuItem saveToneAsMenuItem;
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

	@FXML private ChoiceBox<String> verseTopChoice;
	@FXML private TextField verseTopField;
	@FXML private Button verseTopButton;
	@FXML private TextField titleTextField;
	@FXML private TextField subtitleTextField;
	@FXML private ChoiceBox<String> verseBottomChoice;
	@FXML private TextField verseBottomField;
	@FXML private Button verseBottomButton;

	static boolean LoadingTone = false;
	static String copiedChord = "";

	private boolean verseSet = false;
	private boolean askToOverwrite = false;
	private File builtInDir = new File(System.getProperty("user.dir") + File.separator + "Built-in Tones");
	private String currentRenderFileName = MainApp.APP_NAME + " Render";
	private File currentSavingDirectory = new File(System.getProperty("user.home"));

	@FXML VBox chantLineBox;
	private ArrayList<ChantLineViewController> chantLineControllers = new ArrayList<>();

	private List<ChantLineViewController> mainChantLines = new ArrayList<>();

	@FXML VBox verseLineBox;
	private ArrayList<VerseLineViewController> verseLineControllers = new ArrayList<>();
	@FXML TextArea verseArea;

	private Map<String, File[]> renderResultMap = new HashMap<>();

	@FXML private void initialize() {

		// Interface icons
		ImageView newIcon = new ImageView(getClass().getResource("/media/file-text.png").toExternalForm());
		// Menus
		double iconSize = 30;
		newIcon.setFitHeight(iconSize);
		newIcon.setFitWidth(iconSize);
		newToneMenuItem.setGraphic(newIcon);

		ImageView openIcon = new ImageView(getClass().getResource("/media/folder.png").toExternalForm());
		openIcon.setFitHeight(iconSize);
		openIcon.setFitWidth(iconSize);
		openToneMenuItem.setGraphic(openIcon);

		ImageView saveIcon = new ImageView(getClass().getResource("/media/floppy.png").toExternalForm());
		saveIcon.setFitHeight(iconSize);
		saveIcon.setFitWidth(iconSize);
		saveToneMenuItem.setGraphic(saveIcon);

		ImageView saveAsNewIcon = new ImageView(getClass().getResource("/media/floppy-add.png").toExternalForm());
		saveAsNewIcon.setFitHeight(iconSize);
		saveAsNewIcon.setFitWidth(iconSize);
		saveToneAsMenuItem.setGraphic(saveAsNewIcon);

		ImageView exitIcon = new ImageView(getClass().getResource("/media/sign-error.png").toExternalForm());
		exitIcon.setFitHeight(iconSize);
		exitIcon.setFitWidth(iconSize);
		exitMenuItem.setGraphic(exitIcon);

		ImageView aboutIcon = new ImageView(getClass().getResource("/media/sign-info.png").toExternalForm());
		aboutIcon.setFitHeight(iconSize);
		aboutIcon.setFitWidth(iconSize);
		aboutMenuItem.setGraphic(aboutIcon);

		ImageView addIcon = new ImageView(getClass().getResource("/media/sign-add.png").toExternalForm());
		addIcon.setFitHeight(iconSize);
		addIcon.setFitWidth(iconSize);
		addCLMenuItem.setGraphic(addIcon);

		ImageView keyIcon = new ImageView(getClass().getResource("/media/key.png").toExternalForm());
		keyIcon.setFitHeight(iconSize);
		keyIcon.setFitWidth(iconSize);
		setKeyMenuItem.setGraphic(keyIcon);

		ImageView composerIcon = new ImageView(getClass().getResource("/media/profile.png").toExternalForm());
		composerIcon.setFitHeight(iconSize);
		composerIcon.setFitWidth(iconSize);
		editHeaderInfoMenuItem.setGraphic(composerIcon);

		ImageView manualAssignIcon = new ImageView(getClass().getResource("/media/tag-alt.png").toExternalForm());
		manualAssignIcon.setFitHeight(iconSize);
		manualAssignIcon.setFitWidth(iconSize);
		manualCLAssignmentMenuItem.setGraphic(manualAssignIcon);

		ImageView pdfIcon = new ImageView(getClass().getResource("/media/file-pdf.png").toExternalForm());
		pdfIcon.setFitHeight(iconSize);
		pdfIcon.setFitWidth(iconSize);
		combinePDFsMenuItem.setGraphic(pdfIcon);

		// Modify LilyPond location editing menu items on Mac
		if (MainApp.OS_NAME.startsWith("mac")) {
			setLilyPondLocationItem.setText("Locate LilyPond.app");
			resetLilyPondLocationItem.setText("Reset LilyPond.app Location (use /Applications)");
		}

		// If Lilypond isn't present, disable option to play midi as chords are assigned and to not save LilyPond files.
		if (!MainApp.lilyPondAvailable()) {
			playMidiMenuItem.setSelected(false);
			playMidiMenuItem.setDisable(true);
			saveLPMenuItem.setSelected(true);
			saveLPMenuItem.setDisable(true);
		}

		// Behavior for "Save LilyPond file" option
		saveLPMenuItem.selectedProperty().addListener((ov, oldVal, newVal) ->
				MainApp.prefs.putBoolean(MainApp.PREFS_SAVE_LILYPOND_FILE, newVal));
		// Set initial state for "Save LilyPond file" option and paper size, which may have been saved in preferences.
		saveLPMenuItem.setSelected(MainApp.prefs.getBoolean(MainApp.PREFS_SAVE_LILYPOND_FILE, false));
		paperSize = MainApp.prefs.get(MainApp.PREFS_PAPER_SIZE, "letter (8.5 x 11.0 in)");

		// Hover Highlight menu item behavior and initial state
		hoverHighlightMenuItem.setSelected(MainApp.prefs.getBoolean(MainApp.PREFS_HOVER_HIGHLIGHT, true));
		hoverHighlightMenuItem.selectedProperty().addListener((ov, oldVal, newVal) ->
				MainApp.prefs.putBoolean(MainApp.PREFS_HOVER_HIGHLIGHT, newVal));

		// Dark Mode menu item behavior and initial state
		darkModeMenuItem.setSelected(MainApp.darkModeEnabled());
		setDarkModeEnabled(MainApp.darkModeEnabled());
		darkModeMenuItem.selectedProperty().addListener((ov, oldVal, newVal) -> {
			MainApp.prefs.putBoolean(MainApp.PREFS_DARK_MODE, newVal);
			setDarkModeEnabled(newVal);
		});

		// Set up behavior for reader verse text completion buttons and fields
		verseTopButton.setOnAction((ae) -> showQuickVerseStage(verseTopField));
		verseBottomButton.setOnAction((ae) -> showQuickVerseStage(verseBottomField));

		ArrayList<ChoiceBox<String>> verseChoices = new ArrayList<>();
		verseChoices.add(verseTopChoice);
		verseChoices.add(verseBottomChoice);
		for (ChoiceBox<String> box : verseChoices) {
			box.getItems().add("Reader:");
			box.getItems().add("Sing:");
			box.getSelectionModel().select(0);
		}

	}
	void setStage(Stage stage) {
		mainStage = stage;
	}
	File getToneFile() {
		return toneFile;
	}
	String getCurrentKey() {
		return currentKey;
	}
	public void setCurrentKey(String key) {
		currentKey = key;
		refreshChordKeySignatures(currentKey);
	}
	public void setHeaderText(String text) {
		headerText = text;
	}

	private void setPaperSize(String size) {
		paperSize = size;

		MainApp.prefs.put(MainApp.PREFS_PAPER_SIZE, paperSize);
	}

	private Task<FXMLLoader> createVerseLine(String line) {

		Task<FXMLLoader> loaderTask = FXMLLoaderIO.loadFXMLLayout("verseLineView.fxml", loader -> {
			VerseLineViewController controller = loader.getController();
			controller.setParentController(this);

			controller.setVerseLine(line);

		});

		Thread loaderThread = new Thread(loaderTask);
		loaderThread.start();

		return loaderTask;
	}
	public Task<FXMLLoader> createChantLine(boolean recalculateNames) {

		Task<FXMLLoader> loaderTask = FXMLLoaderIO.loadFXMLLayout("chantLineView.fxml", loader -> {

			ChantLineViewController controller = loader.getController();
			GridPane chantLineLayout = loader.getRoot();
			controller.setMainController(this);

			chantLineControllers.add(controller);
			Platform.runLater(() -> chantLineBox.getChildren().add(chantLineLayout));

			if (recalculateNames) {
				Platform.runLater(this::recalcCLNames);
			}
		});

		Thread loaderThread = new Thread(loaderTask);
		loaderThread.start();

		return loaderTask;
	}

	public void recalcCLNames() {
		boolean previousWasPrime = false;
		int alternateCount = 1;
		char currentLetter = 65;
		ChantLineViewController prevMainLine = null;
		mainChantLines.clear();

		for (ChantLineViewController chantLine : chantLineControllers) {
			if (chantLineControllers.get(chantLineControllers.size()-1) != chantLine) { // If not the last
				chantLine.setName(currentLetter, previousWasPrime, alternateCount);
				previousWasPrime = false;

				chantLine.setNumAlts(0);
				chantLine.setHasPrime(false);

				if (chantLine.getIsPrime()) { // Prime chant line
					if (prevMainLine != null) {
						prevMainLine.setHasPrime(true);
					}

					previousWasPrime = true;
				} else if (chantLine.getIsAlternate()) { // Alternate chant line
					alternateCount++;
				} else { // Normal chant line
					if (prevMainLine != null) {
						prevMainLine.setNumAlts(alternateCount - 1);
					}
					prevMainLine = chantLine;
					mainChantLines.add(chantLine);

					alternateCount = 1;
					currentLetter++;
				}
			} else {
				chantLine.makeCadence(); // TODO: Investigate why this isn't working if the cadence line was removed
				// If this is not the only chant line...
				if (prevMainLine != null) {
					prevMainLine.setNumAlts(alternateCount - 1);
				}
			}
		}
		syncCVLMapping();
	}

	@FXML public void syncCVLMapping() {
		if (toneFile == null) return; // No tone is loaded; don't do anything

		// If manual mode is selected, allow user to choose all chant line assignments.
		if (manualCLAssignmentMenuItem.isSelected()) {
			for (VerseLineViewController verseLine : verseLineControllers) {
				verseLine.setChantLines(chantLineControllers.toArray(new ChantLineViewController[0]));
			}
			return;
		}

		int firstRepeated = 0;
		int CLNum = 0; // For retrieving proper chant line
		for (int VLNum = 0; VLNum < verseLineControllers.size(); VLNum++) {
			// Correct counter overflow for the chant line list (there will usually be more verse lines than chant lines)
			if (CLNum == mainChantLines.size()) {
				CLNum = firstRepeated;
			}

			ChantLineViewController currentChantLine = mainChantLines.get(CLNum);

			// If it's the last line before the end or a separator, it gets Cadence.
			if (VLNum + 1 == verseLineControllers.size() || verseLineControllers.get(VLNum + 1).isSeparator()) {
				verseLineControllers.get(VLNum).setChantLines(new ChantLineViewController[] {chantLineControllers.get(chantLineControllers.size()-1)});
				CLNum = 0; // Resets back to the first chant line. Only matters if this was a separator ending.
				VLNum++; // Skips over separator. If it's the final line overall, has no effect because loop stops anyway.
				continue;
			// If it's the second-to-last line before the end or a separator, it gets prime, if any.
			} else if ((VLNum + 2 == verseLineControllers.size() || verseLineControllers.get(VLNum + 2).isSeparator()) && currentChantLine.hasPrime()) {
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

	public void clearChantLines() {
		chantLineControllers.clear();
		chantLineBox.getChildren().clear();
	}

	private void clearVerseLines() {
		verseLineControllers.clear();
		verseLineBox.getChildren().clear();
	}

	@FXML private void handleSetVerse() {

		if (verseSet) {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Set Verse Confirmation");
			alert.setHeaderText("Are you sure you want to set this verse text? (changes and chord assignments in the current text will be lost)");
			alert.initOwner(mainStage);
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get() == ButtonType.CANCEL) return;
			else askToOverwrite = false;
		}

		clearVerseLines();

		if (verseArea.getText().isEmpty()) return;

		// Sends off the contents of the verse field (trimmed, and with any multi-spaces reduced to one) to be broken into syllables.
		Task<Void> syllabificationTask = new Task<>() {

			@Override
			protected Void call() {
				String[] lines = Syllables.getSyllabificationLines(verseArea.getText());

				ArrayList<Task<FXMLLoader>> lineLoaders = new ArrayList<>();

				for (String line : lines) {
					lineLoaders.add(createVerseLine(line));
				}

				Platform.runLater(() -> {
					for (Task<FXMLLoader> loader : lineLoaders) {
						try {
							verseLineControllers.add(loader.get().getController());
							verseLineBox.getChildren().add(loader.get().getRoot());
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
						}
					}

					verseSet = true;
					syncCVLMapping();
				});
				return null;
			}
		};

		Thread syllableThread = new Thread(syllabificationTask);
		syllableThread.start();

	}


	boolean checkSave() {
		if (toneFile == null ||
				(toneFile.getAbsolutePath().startsWith(builtInDir.getAbsolutePath()) && !MainApp.developerMode)) {
			return true;
		}
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Save Confirmation");
		alert.setHeaderText("Do you want to save tone \"" + toneFile.getName() + "\"?");
		alert.initOwner(mainStage);
		ButtonType saveButton = new ButtonType("Save");
		ButtonType dontSaveButton = new ButtonType("Don't Save");
		ButtonType cancelButton = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
		alert.getButtonTypes().setAll(saveButton, dontSaveButton, cancelButton);

		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent()) {
			if (result.get() == saveButton) {
				handleSave();
				return true;
			} else return result.get() == dontSaveButton;
		} else return false;
	}
	private boolean createNewTone() {
		FileChooser fileChooser = new FileChooser();
		// The second condition is there to make sure the user can't create a new tone in the built-in tones directory.
		if (toneFile != null && !saveDisabled()) {
			fileChooser.setInitialDirectory(toneFile.getParentFile());
		} else {
			fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		}
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TONE files (*.tone)", "*.tone"));
		File saveFile = fileChooser.showSaveDialog(mainStage);
		if (saveFile == null) return false;

		if (ToneReaderWriter.createToneFile(saveFile)) {
			toneFile = saveFile;

			return true;
		} else {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("An error occurred creating the tone!");
			alert.initOwner(mainStage);
			alert.showAndWait();
			return false;
		}
	}
	private boolean loadTone(File selectedFile) {
		if (selectedFile == null) {
			FileChooser fileChooser = new FileChooser();
			if (toneFile != null) {
				fileChooser.setInitialDirectory(toneFile.getParentFile());
			} else {
				if (builtInDir.exists()) {
					fileChooser.setInitialDirectory(builtInDir);
				} else {
					fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
				}
			}
			fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TONE files (*.tone)", "*.tone"));
			selectedFile = fileChooser.showOpenDialog(mainStage);
		}
		if (selectedFile == null) return false;

		if (selectedFile.exists()) {
			toneFile = selectedFile;

			ToneReaderWriter toneReader = new ToneReaderWriter(chantLineControllers, manualCLAssignmentMenuItem);

			if (toneReader.loadTone(this, toneFile)) {
				return true;
			} else {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("");
				alert.setHeaderText("Error loading tone!");
				alert.initOwner(mainStage);
				alert.showAndWait();

				// Since a tone was not loaded (or at least, not correctly),
				toneFile = null;

				return false;
			}

		} else {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("That file doesn't exist!");
			alert.initOwner(mainStage);
			alert.showAndWait();
			return false;
		}
	}

	private void resetStageTitle() {
		mainStage.setTitle(MainApp.APP_NAME);
	}
	private void updateStageTitle() {
		mainStage.setTitle(MainApp.APP_NAME + " - " + toneFile.getName());
	}

	/*
	 * File Menu Actions
	 */
	@FXML void handleNewTone() {
		if (checkSave() && createNewTone()) {
			resetStageTitle();
			clearChantLines();
			editMenu.setDisable(false);
			saveToneMenuItem.setDisable(false);
			saveToneAsMenuItem.setDisable(false);

			// Reset settings pertaining to any previously-loaded tone
			headerText = "";
			currentKey = "C major";
			manualCLAssignmentMenuItem.setSelected(false);

			createChantLine(false);
			createChantLine(true);
			updateStageTitle();
			handleSave(); // So that the tone is loadable (will be empty)
		}
	}
	void handleOpenTone(File selectedFile) {
		LoadingTone = MainApp.lilyPondAvailable(); // Don't block re-renders during loading if there's no lilypond
		if (checkSave() && loadTone(selectedFile)) {
			resetStageTitle();
			editMenu.setDisable(false);
			saveToneMenuItem.setDisable(false);
			saveToneAsMenuItem.setDisable(false);
			updateStageTitle();

			saveToneMenuItem.setDisable(saveDisabled());

			askToOverwrite = false;
		}
		LoadingTone = false;

		refreshAllChords();
	}
	@FXML private void handleOpenTone() {
		handleOpenTone(null);
	}
	@FXML void handleSave() {
		if (toneFile == null || saveDisabled()) return;

		ToneReaderWriter toneWriter = new ToneReaderWriter(chantLineControllers, manualCLAssignmentMenuItem, currentKey,
				headerText);
		if (!toneWriter.saveTone(toneFile)) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Saving error!");
			alert.initOwner(mainStage);
			alert.showAndWait();
		} else {
			saveToneMenuItem.setDisable(false);
		}

	}
	@FXML private void handleSaveAs() {
		if (createNewTone()) {
			handleSave();
			updateStageTitle();
		}

	}
	@FXML private void handleExit() {
		Platform.exit();
	}

	/*
	 * Edit Menu Actions
	 */
	@FXML private void handleCreateChantLine() {
		createChantLine(true);
	}
	@FXML private void handleSetKeySignature() {
		List<String> choices = new ArrayList<>();
		choices.add("C major");
		choices.add("G major");
		choices.add("D major");
		choices.add("A major");
		choices.add("E major");
		choices.add("B major");
		choices.add("F\u266F major");
		choices.add("C\u266F major");
		choices.add("F major");
		choices.add("B\u266Dmajor");
		choices.add("E\u266Dmajor");
		choices.add("A\u266Dmajor");
		choices.add("D\u266Dmajor");
		choices.add("G\u266Dmajor");
		choices.add("C\u266Dmajor");

		choices.add("A minor");
		choices.add("E minor");
		choices.add("B minor");
		choices.add("F\u266F minor");
		choices.add("C\u266F minor");
		choices.add("G\u266F minor");
		choices.add("D\u266F minor");
		choices.add("A\u266F minor");
		choices.add("D minor");
		choices.add("G minor");
		choices.add("C minor");
		choices.add("F minor");
		choices.add("B\u266Dminor");
		choices.add("E\u266Dminor");
		choices.add("A\u266Dminor");


		ChoiceDialog<String> dialog = new ChoiceDialog<>(currentKey, choices);
		dialog.setTitle("Key Choice");
		dialog.setHeaderText("Choose a key");
		dialog.initOwner(mainStage);
		Optional<String> result = dialog.showAndWait();

		result.ifPresent(this::setCurrentKey);
	}

	@FXML private void handleEditHeaderInfo() {

		new Thread(() -> Platform.runLater(() -> {
			TextInputDialog dialog = new TextInputDialog(headerText); // Initial text is existing composer text, if any.
			dialog.setTitle("Header Info");
			dialog.setHeaderText("Input header info (formatted \"Tone # - Composer/System\")");
			dialog.initOwner(mainStage);
			Optional<String> result = dialog.showAndWait();

			result.ifPresent(text -> headerText = text);
		})).start();

	}

	/*
	 * Tools Menu Actions
	 */
	@FXML private void handleCombinePDFs() {
		// Load layout from fxml file
		Task<FXMLLoader> loaderTask = FXMLLoaderIO.loadFXMLLayout("pdfCombineView.fxml", loader -> {
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

		Thread loaderThread = new Thread(loaderTask);
		loaderThread.start();
	}

	/*
	 * Options Menu Actions
	 */
	@FXML private void handleSetLilyPondDir() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Please select the folder which contains lilypond.exe");
		directoryChooser.setInitialDirectory(new File(MainApp.prefs.get(MainApp.PREFS_LILYPOND_LOCATION, System.getProperty("user.home"))));
		File savingDirectory = directoryChooser.showDialog(mainStage);
		if (savingDirectory == null) return;

		if (new File(savingDirectory.getAbsolutePath() + File.separator + MainApp.getPlatformSpecificLPExecutable()).exists()) {
			MainApp.prefs.put(MainApp.PREFS_LILYPOND_LOCATION, savingDirectory.getAbsolutePath());
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Restart");
			alert.setHeaderText(String.format(Locale.US, "This change will take effect the next time you restart %s.", MainApp.APP_NAME));
			alert.initOwner(mainStage);
			alert.showAndWait();
		} else {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("That directory does not contain a valid LilyPond executable.");
			alert.initOwner(mainStage);
			alert.showAndWait();
		}

	}
	@FXML private void handleResetLilyPondDir() {
		MainApp.prefs.remove(MainApp.PREFS_LILYPOND_LOCATION);
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Restart");
		alert.setHeaderText(String.format(Locale.US, "This change will take effect the next time you restart %s.", MainApp.APP_NAME));
		alert.initOwner(mainStage);
		alert.showAndWait();
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
		dialog.initOwner(mainStage);
		Optional<String> result = dialog.showAndWait();

		result.ifPresent(this::setPaperSize);
	}

	/*
	 * Help Menu Actions
	 */
	@FXML private void handleAbout() {

		Task<FXMLLoader> loaderTask = FXMLLoaderIO.loadFXMLLayout("AboutScene.fxml", loader -> {
			BorderPane aboutLayout = loader.getRoot();

			Platform.runLater(() -> {
				Stage aboutStage = new Stage();
				aboutStage.setScene(new Scene(aboutLayout));

				aboutStage.setTitle("About " + MainApp.APP_NAME);
				aboutStage.setResizable(false);
				aboutStage.initOwner(mainStage);
				aboutStage.initModality(Modality.APPLICATION_MODAL);
				aboutStage.getIcons().add(MainApp.APP_ICON);
				aboutStage.show();
			});
		});

		Thread loaderThread = new Thread(loaderTask);
		loaderThread.start();
	}

	private void refreshChordKeySignatures(String key) {
		for (ChantLineViewController chantLineController : chantLineControllers) {
	    	chantLineController.setKeySignature(key);
	    }
	}
	private void refreshAllChords() {
		if (!MainApp.lilyPondAvailable()) return;

		File tempDir = new File(System.getProperty("java.io.tmpdir"));
		File[] files = tempDir.listFiles();
		for (File file : Objects.requireNonNull(files)) {
			if (file.getName().startsWith(MainApp.APP_NAME)) {
				if (!file.delete()) {
					System.out.println("Failed to delete temp file " + file.getName());
				}
			}
		}

		Set<String> allFieldsSet = new HashSet<>();

		for (ChantLineViewController chantLineController : chantLineControllers) {
			allFieldsSet.addAll(chantLineController.getAllFields());
	    }

		for (String fields : allFieldsSet) {
			try {
				renderResultMap.put(fields, LilyPondWriter.renderChord(LilyPondWriter.createTempLYChordFile(getToneFile().getName()),
						fields, getCurrentKey(), this));
			} catch (IOException e) {
				System.out.println("Chord image creation failed");
				e.printStackTrace();
			}
		}
	}
	public void chordRendered(String fields) {
		for (ChantLineViewController chantLineController : chantLineControllers) {
			chantLineController.chordRendered(fields, renderResultMap.get(fields));
		}
	}

	void removeChantLine(ChantLineViewController chantLineViewController) {
		chantLineBox.getChildren().remove(chantLineViewController.getRootLayout());
		chantLineControllers.remove(chantLineViewController);
		recalcCLNames();
	}
	void removeVerseLine(VerseLineViewController verseLineViewController) {
		verseLineBox.getChildren().remove(verseLineViewController.getRootLayout());
		verseLineControllers.remove(verseLineViewController);
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
		int j = i+1;

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
				chantLine.makeFirstRepeated();
			}
		}
	}

	@FXML private void handleFinalRender() { // TODO: Needs improvement, especially in error reporting!

		if (askToOverwrite) {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Overwrite");
			alert.setHeaderText("Do you want to overwrite the previous render? (Choose cancel to create a new file)");
			alert.initOwner(mainStage);
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get() == ButtonType.CANCEL) {
				if (!getNewRenderFilename()) return;
			} else if (!deletePreviousRender()) {
				Alert alert2 = new Alert(AlertType.ERROR);
				alert2.setTitle("Error");
				alert2.setHeaderText("An error occurred while overwriting the previous files, attempting to render anyway...");
				alert2.initOwner(mainStage);
				alert2.showAndWait();
			}
		} else {
			if (getNewRenderFilename()) {
				askToOverwrite = true;
			} else {
				return;
			}
		}

		try {
			if (!LilyPondWriter.writeToLilypond(currentSavingDirectory, currentRenderFileName, verseLineControllers, currentKey,
					titleTextField.getText(), subtitleTextField.getText(), headerText, verseTopChoice.getValue(),
					verseTopField.getText(), verseBottomChoice.getValue(), verseBottomField.getText(), paperSize)) {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("An error occurred while saving!");
				alert.initOwner(mainStage);
				alert.showAndWait();
			}
		} catch (IOException e) {
			e.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("There was an IO error while saving!");
			alert.initOwner(mainStage);
			alert.showAndWait();
		}

	}

	private boolean saveDisabled() {
		return toneFile.getAbsolutePath().contains(builtInDir.getAbsolutePath())
				&& !MainApp.developerMode;
	}

	private boolean getNewRenderFilename() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialFileName(titleTextField.getText());
		fileChooser.setInitialDirectory(currentSavingDirectory);
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));
		File PDFFile = fileChooser.showSaveDialog(mainStage);
		if (PDFFile == null) return false;
		else currentSavingDirectory = PDFFile.getParentFile();

		currentRenderFileName = FilenameUtils.removeExtension(PDFFile.getName());

		return true;
	}

	private boolean deletePreviousRender() {
		File lyFile = new File(currentSavingDirectory + File.separator + currentRenderFileName + ".ly");
		File pdfFile = new File(currentSavingDirectory + File.separator + currentRenderFileName + ".pdf");


        return pdfFile.delete() || lyFile.delete();
	}

	private void showQuickVerseStage(TextField targetField) {

		Task<FXMLLoader> loaderTask = FXMLLoaderIO.loadFXMLLayout("quickVerseView.fxml", loader -> {
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
				syllableStage.initOwner(mainStage);
				syllableStage.show();

				controller.focusFilterField();
			});
		});

		Thread loaderThread = new Thread(loaderTask);
		loaderThread.start();
	}

	boolean playMidiAsAssigned() {
		return playMidiMenuItem.isSelected();
	}
	boolean hoverHighlightEnabled() {
		return hoverHighlightMenuItem.isSelected();
	}

	private void setDarkModeEnabled(boolean value) {
		MainApp.setDarkMode(value);

		if (value) MainApp.setUserAgentStylesheet("/styles/modena-dark/modena-dark.css");
		else MainApp.setUserAgentStylesheet(Application.STYLESHEET_MODENA);

		for (VerseLineViewController verseLine : verseLineControllers) {
			verseLine.refreshTextStyle();
		}

		refreshAllChords();
	}

}
