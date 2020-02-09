package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.*;
import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.robot.Robot;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.apache.commons.io.FilenameUtils;

import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

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
	@FXML private MenuItem updateMenuitem;

	@FXML private VBox bottomRightBox;
	@FXML private ChoiceBox<String> verseTopChoice;
	@FXML private TextField verseTopField;
	@FXML private Button verseTopButton;
	@FXML private CheckBox largeTitleCheckBox;
	@FXML private TextField titleTextField;
	@FXML private TextField subtitleTextField;
	@FXML TextArea verseArea;
	@FXML private ChoiceBox<String> verseBottomChoice;
	@FXML private TextField verseBottomField;
	@FXML private Button verseBottomButton;
	@FXML private Button setVerseButton;
	@FXML private HBox setVerseProgressBox;

	private Robot robot = new Robot();

	private static final String composerIconPath = "/media/profile.png";
	private static final String keyIconPath = "/media/key.png";

	private File toneFile;

	private String currentKey = "C major";
	private String poetText = "";
	private String composerText = "";
	private String paperSize = "";

	static boolean LoadingTone = false;
	static String copiedChord = "";

	private boolean verseSet = false;
	private boolean askToOverwriteOutput = false; // If false, save dialog always appears for final output.
	private boolean askToSaveTone = false;
	private File builtInDir = new File(System.getProperty("user.dir") + File.separator + "Built-in Tones");
	private String currentRenderFileName = MainApp.APP_NAME + " Render";
	private File currentSavingDirectory = new File(FileSystemView.getFileSystemView().getDefaultDirectory().getPath());

	@FXML VBox chantLineBox;
	private ArrayList<ChantLineViewController> chantLineControllers = new ArrayList<>();

	private List<ChantLineViewController> mainChantLines = new ArrayList<>();

	@FXML VBox verseLineBox;
	private ArrayList<VerseLineViewController> verseLineControllers = new ArrayList<>();

	private boolean setVerseCancelled = false;

	private Map<String, File[]> renderResultMap = new HashMap<>();

	@FXML private void initialize() {

		// Menu icons
		setMenuIcon(newToneMenuItem, "/media/file-text.png");
		setMenuIcon(openToneMenuItem, "/media/folder.png");
		setMenuIcon(saveToneMenuItem, "/media/floppy.png");
		setMenuIcon(saveToneAsMenuItem, "/media/floppy-add.png");
		setMenuIcon(exitMenuItem, "/media/sign-error.png");
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

		// Replace text area with a custom one.
		int index = bottomRightBox.getChildren().indexOf(verseArea);
		verseArea = new TextArea() {
			@Override
			public void paste() { // Intercept paste actions.
				String pastingText = null;
				try {
					pastingText = (String) Toolkit.getDefaultToolkit()
							.getSystemClipboard().getData(DataFlavor.stringFlavor);
				} catch (UnsupportedFlavorException | IOException e) {
					e.printStackTrace();
				}

				// Replace each tabbed-in newline in pasted text with a single space.
				if (pastingText == null || !Pattern.compile("\n\t+").matcher(pastingText).find()) { super.paste(); return; }
				String editedText = pastingText.replaceAll("\n\t+", " ").replaceAll(" +", " ");
				Toolkit.getDefaultToolkit().getSystemClipboard()
						.setContents(new StringSelection(editedText), null);

				super.paste();

				Toolkit.getDefaultToolkit().getSystemClipboard()
						.setContents(new StringSelection(pastingText), null);
			}
		};
		// Replace all typed tabs with spaces.
		verseArea.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.TAB) {
				robot.keyType(KeyCode.SPACE);
				event.consume();
			}
		});
		VBox.setVgrow(verseArea, Priority.ALWAYS);
		bottomRightBox.getChildren().remove(index);
		bottomRightBox.getChildren().add(index, verseArea);

	}

	private void setMenuIcon(MenuItem menu_item, String imagePath) {
		ImageView saveIcon = new ImageView(getClass().getResource(imagePath).toExternalForm());
		double menuIconSize = 30;
		saveIcon.setFitHeight(menuIconSize);
		saveIcon.setFitWidth(menuIconSize);
		menu_item.setGraphic(saveIcon);
	}

	void setStage(Stage stage) {
		mainStage = stage;

		resetStageTitle();

		mainStage.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN),
				this::handleExport);
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

	public void setHeaderText(String poet, String composer) {
		poetText = poet;
		composerText = composer;
	}

	private void setPaperSize(String size) {
		paperSize = size;

		MainApp.prefs.put(MainApp.PREFS_PAPER_SIZE, paperSize);
	}

	private Task<FXMLLoader> createVerseLine(String line) {

		return FXMLLoaderIO.loadFXMLLayoutAsync("verseLineView.fxml", loader -> {
			VerseLineViewController controller = loader.getController();
			controller.setParentController(this);

			controller.setVerseLine(line);

		});

	}
	public Task<FXMLLoader> createChantLine(boolean recalculateNames) {

		return FXMLLoaderIO.loadFXMLLayoutAsync("chantLineView.fxml", loader -> {

			ChantLineViewController controller = loader.getController();
			GridPane chantLineLayout = loader.getRoot();
			controller.setMainController(this);

			chantLineControllers.add(controller);
			Platform.runLater(() -> {
				chantLineBox.getChildren().add(chantLineLayout);
				if (recalculateNames) recalcCLNames();
			});

		});

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
				chantLine.makeCadence();
				// If this is not the only chant line...
				if (prevMainLine != null) {
					prevMainLine.setNumAlts(alternateCount - 1);
				}
			}
		}

		toneEdited();
		syncCVLMapping();
	}

	void syncCVLMapping() {
		if (toneFile == null) return; // No tone is loaded; don't do anything

		// If manual mode is selected, allow user to choose all chant line assignments.
		if (manualCLAssignmentMenuItem.isSelected()) {
			for (VerseLineViewController verseLine : verseLineControllers) {
				// Default last chant line selection to Cadence line.
				if (verseLineControllers.indexOf(verseLine) == verseLineControllers.size() - 1) {
					verseLine.setChantLines(chantLineControllers.toArray(new ChantLineViewController[0]),
							chantLineControllers.size() - 1);
				} else {
					verseLine.setChantLines(chantLineControllers.toArray(new ChantLineViewController[0]));
				}
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
			Optional<ButtonType> result = TWUtils.showAlert(AlertType.CONFIRMATION, "Set Verse Confirmation",
					"Are you sure you want to set this verse text? (changes and chord assignments in the current text will be lost)", true);
			if (result.isPresent() && result.get() == ButtonType.CANCEL) return;
			else askToOverwriteOutput = false;
		}

		clearVerseLines();

		if (verseArea.getText().isEmpty()) return;

		// Show working indicator
		setVerseButton.setVisible(false);
		setVerseProgressBox.setVisible(true);

		// Sends off the contents of the verse field (trimmed, and with any multi-spaces reduced to one) to be broken into syllables.
		Task<Void> syllabificationTask = new Task<>() {

			@Override
			protected Void call() {

				String[] lines = Syllables.getSyllabificationLines(verseArea.getText(), mainStage);

				if (setVerseCancelled) {
					setVerseCancelled = false;
					return null;
				}

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

					// Hide working indicator
					setVerseButton.setVisible(true);
					setVerseProgressBox.setVisible(false);
				});
				return null;
			}
		};

		Thread syllableThread = new Thread(syllabificationTask);
		syllableThread.start();

	}

	@FXML private void handleCancelSetVerse() {
		setVerseCancelled = true;

		setVerseButton.setVisible(true);
		setVerseProgressBox.setVisible(false);
	}

	/*
	 * Returns false if the user chooses cancel or closes. This should halt any impending file related functions.
	 */
	boolean checkSave() {
		if (toneFile == null ||
				!askToSaveTone ||
				!isToneSavable()) {
			return true;
		}

		ButtonType saveButton = new ButtonType("Save");
		ButtonType dontSaveButton = new ButtonType("Don't Save");
		ButtonType cancelButton = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

		Optional<ButtonType> result = TWUtils.showAlert(AlertType.CONFIRMATION, "Save Confirmation",
				"Do you want to save tone \"" + toneFile.getName() + "\"?", true, mainStage,
				new ButtonType[] {saveButton, dontSaveButton, cancelButton});

		if (result.isPresent()) {
			if (result.get() == saveButton) {
				handleSave();
				return true;
			} else return result.get() == dontSaveButton;
		} else {
			// Not returning true, so no save will occur and the prompt will appear again next time.
			return false;
		}
	}
	private boolean isToneSavable() {
		return !builtInToneLoaded() || MainApp.developerMode;
	}

	private boolean createNewTone() {
		FileChooser fileChooser = new FileChooser();
		// The second condition is there to make sure the user can't create a new tone in the built-in tones directory.
		if (toneFile != null && !saveDisabled()) {
			fileChooser.setInitialDirectory(toneFile.getParentFile());
		} else {
			fileChooser.setInitialDirectory(new File(FileSystemView.getFileSystemView().getDefaultDirectory().getPath()));
		}
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TONE file (*.tone)", "*.tone"));
		File saveFile = fileChooser.showSaveDialog(mainStage);
		if (saveFile == null) return false;

		if (ToneReaderWriter.createToneFile(saveFile)) {
			toneFile = saveFile;

			saveToneMenuItem.setDisable(false);

			return true;
		} else {

			TWUtils.showAlert(AlertType.ERROR, "Error", "An error occurred while creating the tone!",
					true, mainStage);

			return false;
		}
	}
	private boolean loadTone(File selectedFile) {
		if (selectedFile == null) {
			FileChooser fileChooser = new FileChooser();
			if (toneFile != null) {
				if (builtInToneLoaded()) fileChooser.setInitialDirectory(builtInDir);
				else fileChooser.setInitialDirectory(toneFile.getParentFile());
			} else {
				if (builtInDir.exists()) {
					fileChooser.setInitialDirectory(builtInDir);
				} else {
					fileChooser.setInitialDirectory(new File(FileSystemView.getFileSystemView().getDefaultDirectory().getPath()));
				}
			}
			fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TONE file (*.tone)", "*.tone"));
			selectedFile = fileChooser.showOpenDialog(mainStage);
		}
		if (selectedFile == null) return false;

		if (selectedFile.exists()) {
			toneFile = selectedFile;

			ToneReaderWriter toneReader = new ToneReaderWriter(chantLineControllers, manualCLAssignmentMenuItem);

			if (toneReader.loadTone(this, toneFile)) {
				return true;
			} else {

				TWUtils.showAlert(AlertType.ERROR, "Error", "Error loading tone!", true, mainStage);

				// Since a tone was not loaded (or at least, not correctly),
				toneFile = null;

				return false;
			}

		} else {

			TWUtils.showAlert(AlertType.ERROR, "Error", "That file doesn't exist!", true, mainStage);

			return false;
		}
	}

	private void resetStageTitle() {
		mainStage.setTitle(MainApp.APP_NAME);
	}
	private void updateStageTitle() {
		if (toneFile != null) {
			mainStage.setTitle(MainApp.APP_NAME + " - " + toneFile.getName());
		} else {
			resetStageTitle();
		}

	}

	/*
	 * File Menu Actions
	 */
	@FXML void handleNewTone() {
		if (checkSave() && createNewTone()) {
			clearChantLines();
			editMenu.setDisable(false);
			saveToneAsMenuItem.setDisable(false);

			// Reset settings pertaining to any previously-loaded tone
			poetText = "";
			composerText = "";
			currentKey = "C major";
			manualCLAssignmentMenuItem.setSelected(false);

			createChantLine(false);
			createChantLine(true);
			resetToneEditedStatus();
			handleSave(); // So that the tone is loadable (will be empty)
		}
	}
	void handleOpenTone(File selectedFile) {
		LoadingTone = MainApp.lilyPondAvailable(); // Don't block re-renders during loading if there's no lilypond
		if (checkSave() && loadTone(selectedFile)) {
			editMenu.setDisable(false);
			saveToneMenuItem.setDisable(false);
			saveToneAsMenuItem.setDisable(false);
			resetToneEditedStatus();

			saveToneMenuItem.setDisable(saveDisabled());

			askToOverwriteOutput = false;

			refreshAllChords();
		}

		LoadingTone = false;
	}
	@FXML private void handleOpenTone() {
		handleOpenTone(null);
	}
	@FXML void handleSave() {
		if (toneFile == null || saveDisabled()) return;

		ToneReaderWriter toneWriter = new ToneReaderWriter(chantLineControllers, manualCLAssignmentMenuItem, currentKey,
				poetText, composerText);
		if (!toneWriter.saveTone(toneFile)) {
			TWUtils.showAlert(AlertType.ERROR, "Error", "Saving error!", true, mainStage);
		} else { // Save successful
			resetToneEditedStatus();
		}

	}
	@FXML private void handleSaveAs() {
		if (createNewTone()) handleSave();
	}
	@FXML private void handleExit() {
		if (checkSave()) {
			Platform.exit();
		}
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
		ImageView keyIcon = new ImageView(getClass().getResource(keyIconPath).toExternalForm());
		keyIcon.setFitHeight(50);
		keyIcon.setFitWidth(50);
		dialog.setGraphic(keyIcon);
		dialog.initOwner(mainStage);
		Optional<String> result = dialog.showAndWait();

		result.ifPresent(key -> {
			toneEdited();
			setCurrentKey(key);
		});
	}
	@FXML private void handleEditHeaderInfo() {

		new Thread(() -> Platform.runLater(() -> {
			Dialog<Pair<String, String>> dialog = new Dialog<>();
			dialog.setTitle("Header Info");
			dialog.setHeaderText("Input header info for first page");
			ImageView composerIcon = new ImageView(getClass().getResource(composerIconPath).toExternalForm());
			composerIcon.setFitHeight(50);
			composerIcon.setFitWidth(50);
			dialog.setGraphic(composerIcon);
			dialog.initOwner(mainStage);

			dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

			GridPane grid = new GridPane();
			grid.setHgap(10);
			grid.setVgap(10);
			grid.setPadding(new Insets(20, 20, 10, 10));

			TextField poetField = new TextField(poetText);
			poetField.setPromptText("Tone #");
			TextField composerField = new TextField(composerText);
			composerField.setPromptText("Composer - System");
			composerField.setPrefWidth(200);

			grid.add(new Label("Left side:"), 0, 0);
			grid.add(poetField, 1, 0);
			grid.add(new Label("Right side:"), 2, 0);
			grid.add(composerField, 3, 0);

			dialog.getDialogPane().setContent(grid);

			dialog.setResultConverter(dialogButton -> {
				if (dialogButton == ButtonType.OK) {
					return new Pair<>(poetField.getText(), composerField.getText());
				}
				return null;
			});

			Optional<Pair<String, String>> result = dialog.showAndWait();

			result.ifPresent(poetComposer -> {
				if (poetComposer.getKey().matches("[0-9]")) poetText = "Tone " + poetComposer.getKey();
				else poetText = poetComposer.getKey();

				composerText = poetComposer.getValue();
			});
		})).start();

	}
	@FXML private void handleToggleManualCLAssignment() {
		toneEdited();
		syncCVLMapping();
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
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Please select the folder which contains the LilyPond executable");
		directoryChooser.setInitialDirectory(new File(MainApp.prefs.get(MainApp.PREFS_LILYPOND_LOCATION, MainApp.getPlatformSpecificRootDir())));
		File savingDirectory = directoryChooser.showDialog(mainStage);
		if (savingDirectory == null) return;

		String previousLocation = MainApp.prefs.get(MainApp.PREFS_LILYPOND_LOCATION, null);
		MainApp.prefs.put(MainApp.PREFS_LILYPOND_LOCATION, savingDirectory.getAbsolutePath());
		if (new File(savingDirectory.getAbsolutePath() + File.separator + MainApp.getPlatformSpecificLPExecutable()).exists()) {
			TWUtils.showAlert(AlertType.INFORMATION, "Restart",
					String.format("This change will take effect the next time you restart %s.", MainApp.APP_NAME), true, mainStage);
		} else {
			if (previousLocation == null) {
				MainApp.prefs.remove(MainApp.PREFS_LILYPOND_LOCATION);
			} else {
				MainApp.prefs.put(MainApp.PREFS_LILYPOND_LOCATION, previousLocation);
			}

			TWUtils.showAlert(AlertType.ERROR, "Error",
					"That directory does not contain a valid LilyPond executable.", true, mainStage);

		}

	}
	@FXML private void handleResetLilyPondDir() {
		MainApp.prefs.remove(MainApp.PREFS_LILYPOND_LOCATION);
		TWUtils.showAlert(AlertType.INFORMATION, "Restart",
				String.format("This change will take effect the next time you restart %s.", MainApp.APP_NAME), true,
				mainStage);
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
		FXMLLoaderIO.loadFXMLLayoutAsync("AboutScene.fxml", loader -> {
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
	}
	@FXML private void handleUpdateCheck() {
		AutoUpdater.AutoUpdate(mainStage, false);
	}

	private void refreshChordKeySignatures(String key) {
		for (ChantLineViewController chantLineController : chantLineControllers) {
	    	chantLineController.setKeySignature(key);
	    }
	}
	private void refreshAllChords() {
		if (!MainApp.lilyPondAvailable()) return;

		TWUtils.cleanUpTempFiles();

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
		chantLineBox.getChildren().remove(chantLineViewController.getMainPane());
		chantLineControllers.remove(chantLineViewController);
		recalcCLNames();
	}
	void removeVerseLine(VerseLineViewController verseLineViewController) {
		verseLineBox.getChildren().remove(verseLineViewController.getRootPane());
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
				chantLine.toggleFirstRepeated();
			}
		}
	}

	@FXML private void handleExport() {

		if (askToOverwriteOutput) {
			Optional<ButtonType> result = TWUtils.showAlert(AlertType.CONFIRMATION, "Overwrite",
					"Do you want to overwrite the previous output? (Choose cancel to create a new file)", true,
					mainStage);
			if (result.isPresent() && result.get() == ButtonType.CANCEL) {
				if (!getNewRenderFilename()) return;
			} else if (!deletePreviousRender()) {
				TWUtils.showAlert(AlertType.ERROR, "Error",
						"An error occurred while overwriting the previous files, attempting to output anyway...",
						true, mainStage);
			}
		} else {
			if (getNewRenderFilename()) {
				askToOverwriteOutput = true;
			} else {
				return;
			}
		}

		try {
			if (!LilyPondWriter.writeToLilypond(currentSavingDirectory, currentRenderFileName, verseLineControllers, currentKey,
					largeTitleCheckBox.isSelected(), titleTextField.getText(), subtitleTextField.getText(), poetText, composerText,
					verseTopChoice.getValue(), verseTopField.getText(), verseBottomChoice.getValue(), verseBottomField.getText(), paperSize)) {
				TWUtils.showAlert(AlertType.ERROR, "Error", "An error occurred while saving!",
						true, mainStage);
			}
		} catch (IOException e) {
			e.printStackTrace();
			TWUtils.showAlert(AlertType.ERROR, "Error", "There was an IO error while saving!",
					true, mainStage);
		}

	}

	private boolean builtInToneLoaded() {
		return toneFile.getAbsolutePath().contains(builtInDir.getAbsolutePath());
	}

	private boolean saveDisabled() {
		return builtInToneLoaded() && !MainApp.developerMode;
	}

	private boolean getNewRenderFilename() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialFileName(titleTextField.getText());
		fileChooser.setInitialDirectory(currentSavingDirectory);
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF file (*.pdf)", "*.pdf"));
		fileChooser.setTitle("Export As");
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
				syllableStage.initOwner(mainStage);
				syllableStage.show();

				controller.focusFilterField();
			});
		});

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

	void toneEdited() {
		if (!askToSaveTone && isToneSavable()) {
			askToSaveTone = true;
			mainStage.setTitle("*" + mainStage.getTitle());
		}
	}

	private void resetToneEditedStatus() {
		askToSaveTone = false;
		updateStageTitle();
	}

}
