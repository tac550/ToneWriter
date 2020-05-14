package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.*;
import com.tac550.tonewriter.model.MenuState;
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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.robot.Robot;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

	private Stage parentStage;
	private TopSceneController topSceneController; // TODO: Trace references here

	private final MenuState menuState = new MenuState();

	@FXML private SplitPane mainSplitPane;

	@FXML private VBox bottomRightBox;
	@FXML private ChoiceBox<String> verseTopChoice;
	@FXML private TextField verseTopField;
	@FXML private Button verseTopButton;
	@FXML private CheckBox largeTitleCheckBox;
	@FXML private TextField titleTextField;
	@FXML private TextField subtitleTextField;
	@FXML private TextArea verseArea;
	@FXML private ChoiceBox<String> verseBottomChoice;
	@FXML private TextField verseBottomField;
	@FXML private Button verseBottomButton;
	@FXML private Button setVerseButton;
	@FXML private HBox setVerseProgressBox;

	private Robot robot;

	private File toneFile;

	private String currentKey = "C major";
	private String poetText = "";
	private String composerText = "";

	static boolean LoadingTone = false;
	static String copiedChord = "";

	private boolean verseSet = false;
	private boolean askToOverwriteOutput = false; // If false, save dialog always appears for final output.
	private boolean toneEdited = false;
	private final File builtInDir = new File(System.getProperty("user.dir") + File.separator + "Built-in Tones");
	private String currentRenderFileName = MainApp.APP_NAME + " Render";

	@FXML private ScrollPane toneScrollPane;
	@FXML private VBox chantLineBox;
	private final ArrayList<ChantLineViewController> chantLineControllers = new ArrayList<>();

	private final List<ChantLineViewController> mainChantLines = new ArrayList<>();

	@FXML private VBox verseLineBox;
	private final ArrayList<VerseLineViewController> verseLineControllers = new ArrayList<>();

	private boolean setVerseCancelled = false;

	@FXML private void initialize() {

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

		Platform.runLater(() -> robot = new Robot());

	}

	void setStageAndTopScene(Stage stage, TopSceneController top_scene) {
		parentStage = stage;
		topSceneController = top_scene;
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

	private Task<FXMLLoader> createVerseLine(String line) {

		return FXMLLoaderIO.loadFXMLLayoutAsync("verseLineView.fxml", loader -> {
			VerseLineViewController controller = loader.getController();
			controller.setParentControllers(this, topSceneController);

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
				chantLine.makeCadence();
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
		if (toneFile == null) return; // No tone is loaded; don't do anything

		// If manual mode is selected, allow user to choose all chant line assignments.
		if (manualCLAssignmentEnabled()) {

			for (VerseLineViewController verseLine : verseLineControllers) {
				// Default last chant line selection to Cadence line.
				if (verseLineControllers.indexOf(verseLine) == verseLineControllers.size() - 1) {
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

					// If there are no main chant lines, use the cadence line.
					if (mainChantLines.isEmpty()) {
						verseLineControllers.get(VLNum).setChantLines(new ChantLineViewController[] {chantLineControllers.get(chantLineControllers.size()-1)});
						continue;
					}
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

				String[] lines = Syllables.getSyllabificationLines(verseArea.getText(), parentStage);

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
		if (toneFile == null
				|| !toneEdited
				|| !isToneSavable()) {
			return true;
		}

		ButtonType saveButton = new ButtonType("Save");
		ButtonType dontSaveButton = new ButtonType("Don't Save");
		ButtonType cancelButton = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

		Optional<ButtonType> result = TWUtils.showAlert(AlertType.CONFIRMATION, "Save Confirmation",
				"Do you want to save tone \"" + toneFile.getName() + "\"?", true, parentStage,
				new ButtonType[] {saveButton, dontSaveButton, cancelButton}, cancelButton);

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
		// The second condition is there to make sure the chooser doesn't offer the built-in tones directory.
		if (toneFile != null && isToneSavable()) {
			fileChooser.setInitialDirectory(toneFile.getParentFile());
		} else {
			fileChooser.setInitialDirectory(new File(FileSystemView.getFileSystemView().getDefaultDirectory().getPath()));
		}
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("TONE file (*.tone)", "*.tone"));
		File saveFile = fileChooser.showSaveDialog(parentStage);
		if (saveFile == null) return false;

		if (ToneReaderWriter.createToneFile(saveFile)) {
			toneFile = saveFile;

			menuState.saveToneMenuItemDisabled = false;
			topSceneController.setMenuState(menuState);

			return true;
		} else {

			TWUtils.showAlert(AlertType.ERROR, "Error", "An error occurred while creating the tone!",
					true, parentStage);

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
			selectedFile = fileChooser.showOpenDialog(parentStage);
		}
		if (selectedFile == null) return false;

		if (selectedFile.exists()) {
			toneFile = selectedFile;

			ToneReaderWriter toneReader = new ToneReaderWriter(chantLineControllers, this);

			if (toneReader.loadTone(this, toneFile)) {
				return true;
			} else {
				TWUtils.showAlert(AlertType.ERROR, "Error", "Error loading tone!", true, parentStage);
				// Since a tone was not loaded (or at least, not correctly),
				toneFile = null;
				return false;
			}

		} else {
			TWUtils.showAlert(AlertType.ERROR, "Error", "That file doesn't exist!", true, parentStage);
			return false;
		}
	}

	void updateTopLevelInfo() {
		if (topSceneController.isActiveTab(this)) {
			// Update stage title to show loaded tone name and edit status
			parentStage.setTitle((toneEdited ? "*" : "") + MainApp.APP_NAME + (toneFile != null ? " - " + toneFile.getName() : ""));

			topSceneController.setMenuState(menuState);
		}
	}

	/*
	 * File Menu Actions
	 */
	void handleNewTone() {
		if (checkSave() && createNewTone()) {
			clearChantLines();
			menuState.editMenuDisabled = false;
			menuState.saveToneAsMenuItemDisabled = false;

			// Reset settings pertaining to any previously-loaded tone
			poetText = "";
			composerText = "";
			currentKey = "C major";
			menuState.manualCLAssignmentSelected = false;

			Task<FXMLLoader> loaderTask = createChantLine(true);
			loaderTask.setOnSucceeded(event -> handleSave()); // So that the tone is loadable
		}
	}
	void handleOpenTone(File selectedFile, boolean auto_load) {
		LoadingTone = MainApp.lilyPondAvailable(); // Don't block re-renders during loading if there's no lilypond
		if ((auto_load || checkSave()) && loadTone(selectedFile)) {
			menuState.editMenuDisabled = false;
			menuState.saveToneMenuItemDisabled = false;
			menuState.saveToneAsMenuItemDisabled = false;
			menuState.saveToneMenuItemDisabled = !isToneSavable();

			resetToneEditedStatus();

			askToOverwriteOutput = false;
		}

		LoadingTone = false;
		refreshAllChordPreviews();
	}
	void handleSave() {
		if (toneFile == null || !isToneSavable()) return;

		ToneReaderWriter toneWriter = new ToneReaderWriter(chantLineControllers, this, currentKey,
				poetText, composerText);
		if (!toneWriter.saveTone(toneFile)) {
			TWUtils.showAlert(AlertType.ERROR, "Error", "Saving error!", true, parentStage);
		} else { // Save successful
			resetToneEditedStatus();
			topSceneController.refreshToneInstances(toneFile, this);
		}

	}
	void handleSaveAs() {
		if (createNewTone()) handleSave();
	}
	void handleExport() {

		if (askToOverwriteOutput) {
			Optional<ButtonType> result = TWUtils.showAlert(AlertType.CONFIRMATION, "Overwrite",
					"Do you want to overwrite the previous output? (Choose cancel to create a new file)", true,
					parentStage);
			if (result.isPresent() && result.get() == ButtonType.CANCEL) {
				if (!getNewRenderFilename()) return;
			} else if (!deletePreviousRender()) {
				TWUtils.showAlert(AlertType.ERROR, "Error",
						"An error occurred while overwriting the previous files, attempting to output anyway...",
						true, parentStage);
			}
		} else {
			if (getNewRenderFilename()) {
				askToOverwriteOutput = true;
			} else {
				return;
			}
		}

		try {
			if (!LilyPondInterface.writeToLilypond(topSceneController.currentSavingDirectory, currentRenderFileName, verseLineControllers, currentKey,
					largeTitleCheckBox.isSelected(), titleTextField.getText(), subtitleTextField.getText(), poetText, composerText,
					verseTopChoice.getValue(), verseTopField.getText(), verseBottomChoice.getValue(), verseBottomField.getText(), topSceneController.paperSize)) {
				TWUtils.showAlert(AlertType.ERROR, "Error", "An error occurred while saving!",
						true, parentStage);
			}
		} catch (IOException e) {
			e.printStackTrace();
			TWUtils.showAlert(AlertType.ERROR, "Error", "There was an IO error while saving!",
					true, parentStage);
		}

	}

	/*
	 * Edit Menu Actions
	 */
	void handleSetKeySignature() {
		List<String> choices = new ArrayList<>(List.of("C major", "G major", "D major", "A major", "E major", "B major",
				"F\u266F major", "C\u266F major", "F major", "B\u266Dmajor", "E\u266Dmajor", "A\u266Dmajor", "D\u266Dmajor",
				"G\u266Dmajor", "C\u266Dmajor", "A minor", "E minor", "B minor", "F\u266F minor", "C\u266F minor", "G\u266F minor",
				"D\u266F minor", "A\u266F minor", "D minor", "G minor", "C minor", "F minor", "B\u266Dminor", "E\u266Dminor",
				"A\u266Dminor"));

		ChoiceDialog<String> dialog = new ChoiceDialog<>(currentKey, choices);
		dialog.setTitle("Key Choice");
		dialog.setHeaderText("Choose a key");
		ImageView keyIcon = new ImageView(getClass().getResource(TopSceneController.keyIconPath).toExternalForm());
		keyIcon.setFitHeight(50);
		keyIcon.setFitWidth(50);
		dialog.setGraphic(keyIcon);
		dialog.initOwner(parentStage);
		Optional<String> result = dialog.showAndWait();

		result.ifPresent(key -> {
			toneEdited();
			setCurrentKey(key);
		});
	}
	void handleEditHeaderInfo() {

		new Thread(() -> Platform.runLater(() -> {
			Dialog<Pair<String, String>> dialog = new Dialog<>();
			dialog.setTitle("Header Info");
			dialog.setHeaderText("Input header info for first page");
			ImageView composerIcon = new ImageView(getClass().getResource(TopSceneController.composerIconPath).toExternalForm());
			composerIcon.setFitHeight(50);
			composerIcon.setFitWidth(50);
			dialog.setGraphic(composerIcon);
			dialog.initOwner(parentStage);

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
				String tempPoetText = poetComposer.getKey();
				String tempComposerText = poetComposer.getValue();
				if (poetComposer.getKey().matches("[0-9]")) tempPoetText = "Tone " + tempPoetText;

				if (!(tempPoetText.equals(poetText) && tempComposerText.equals(composerText))) {
					toneEdited();

					poetText = tempPoetText;
					composerText = tempComposerText;
				}
			});
		})).start();

	}
	void handleToggleManualCLAssignment() {
		toneEdited();
		syncCVLMapping();
	}
	public boolean manualCLAssignmentEnabled() {
		return topSceneController.manualCLAssignmentEnabled();
	}
	public void setManualCLAssignmentSilently(boolean enable) { // Doesn't trigger tone edited status
		menuState.manualCLAssignmentSelected = enable;
		topSceneController.setMenuState(menuState);
	}

	private void refreshChordKeySignatures(String key) {
		for (ChantLineViewController chantLineController : chantLineControllers) {
	    	chantLineController.setKeySignature(key);
	    }
	}
	void refreshAllChordPreviews() {
		if (!MainApp.lilyPondAvailable()) return;

		for (ChantLineViewController chantLineController : chantLineControllers) {
			chantLineController.refreshAllChordPreviews();
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

	private boolean builtInToneLoaded() {
		return toneFile.getAbsolutePath().startsWith(builtInDir.getAbsolutePath());
	}

	private boolean getNewRenderFilename() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialFileName(titleTextField.getText());
		fileChooser.setInitialDirectory(topSceneController.currentSavingDirectory);
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF file (*.pdf)", "*.pdf"));
		fileChooser.setTitle("Export As");
		File PDFFile = fileChooser.showSaveDialog(parentStage);
		if (PDFFile == null) return false;
		else topSceneController.currentSavingDirectory = PDFFile.getParentFile();

		currentRenderFileName = FilenameUtils.removeExtension(PDFFile.getName());

		return true;
	}

	private boolean deletePreviousRender() {
		File lyFile = new File(topSceneController.currentSavingDirectory + File.separator + currentRenderFileName + ".ly");
		File pdfFile = new File(topSceneController.currentSavingDirectory + File.separator + currentRenderFileName + ".pdf");

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
		if (!toneEdited && isToneSavable()) {
			toneEdited = true;
			updateTopLevelInfo();
		}
	}
	void resetToneEditedStatus() {
		toneEdited = false;
		updateTopLevelInfo();
	}
	boolean isToneUnedited() {
		return !toneEdited;
	}

	File getToneFile() {
		return toneFile;
	}

	ObservableStringValue getTitleTextProperty() {
		return titleTextField.textProperty();
	}
	void setTitleText(String title) {
		titleTextField.setText(title);
	}

	void setDividerPosition(double position) {
		mainSplitPane.setDividerPosition(0, position);
	}

	double getDividerPosition() {
		return mainSplitPane.getDividerPositions()[0];
	}

}
