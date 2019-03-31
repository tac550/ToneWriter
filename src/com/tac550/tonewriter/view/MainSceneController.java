package com.tac550.tonewriter.view;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.tac550.tonewriter.io.LilyPondWriter;
import com.tac550.tonewriter.io.Syllables;
import com.tac550.tonewriter.io.ToneReaderWriter;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/*
 * TEST VERSES:
 * 

Lord, I call upon You, hear me!
Hear me O Lord!
Lord, I call upon You, hear me!
Receive the voice of my prayer
when I call upon You.
Hear me O Lord!

Let my prayer arise
in Your sight as incense.
And let the lifting up of my hands
be an evening sacrifice.
Hear me O Lord!

 */

public class MainSceneController {

	private Stage thisStage;
	
	private File toneDirectory;
	
	private String keyChoice = "C major";
	private String composerText = "";

	@FXML private MenuItem newToneMenuItem;
	@FXML private MenuItem openToneMenuItem;
	@FXML private MenuItem saveToneMenuItem;
	@FXML private MenuItem saveToneAsMenuItem;
	@FXML private MenuItem exitMenuItem;
	
	@FXML private MenuItem addCLMenuItem;
	@FXML private MenuItem setKeyMenuItem;
	@FXML private MenuItem setComposerMenuItem;
	
	@FXML private Menu editMenu;
	@FXML private CheckMenuItem playMidiMenuItem;
	@FXML private CheckMenuItem dontSaveLPMenuItem;
	@FXML private MenuItem setLilyPondLocationItem;
	@FXML private MenuItem resetLilyPondLocationItem;
	
	@FXML private MenuItem combinePDFsMenuItem;
	
	@FXML private MenuItem aboutMenuItem;
	
	@FXML private ChoiceBox<String> verseTopChoice;
	@FXML private TextField verseTopField;
	@FXML private Button verseTopButton;
	@FXML private TextField titleTextField;
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
		setComposerMenuItem.setGraphic(composerIcon);
		
		ImageView pdfIcon = new ImageView(getClass().getResource("/media/file-pdf.png").toExternalForm());
		pdfIcon.setFitHeight(iconSize);
		pdfIcon.setFitWidth(iconSize);
		combinePDFsMenuItem.setGraphic(pdfIcon);
		
		// Modify LilyPond location editing menu items on Mac
		if (MainApp.OS_NAME.startsWith("mac")) {
			setLilyPondLocationItem.setText("Locate LilyPond.app");
			resetLilyPondLocationItem.setText("Reset LilyPond.app Location (use /Applications)");
		}
		
		// If Lilypond is not present, disable the option to play midi as chords are assigned.
		if (!MainApp.lilyPondAvailable()) {
			playMidiMenuItem.setSelected(false);
			playMidiMenuItem.setDisable(true);
		}

		// Behavior for "don't save LilyPond file" option
		dontSaveLPMenuItem.selectedProperty().addListener((ov, oldVal, newVal) ->
				MainApp.prefs.putBoolean(MainApp.PREFS_DONT_SAVE_LILYPOND_FILE, newVal));
		// Set initial state for "don't save LilyPond file" option
		dontSaveLPMenuItem.setSelected(MainApp.prefs.getBoolean(MainApp.PREFS_DONT_SAVE_LILYPOND_FILE, false));

		// Set up behavior for reader verse text completion buttons and fields
		verseTopButton.setOnAction((ae) -> {
			String result = showVerseBox();
			if (!result.isEmpty()) {
				verseTopField.setText(result);	
			}
		});
		verseBottomButton.setOnAction((ae) -> {
			String result = showVerseBox();
			if (!result.isEmpty()) {
				verseBottomField.setText(result);	
			}
		});
		
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
		thisStage = stage;
	}
	File getToneDirectory() {
		return toneDirectory;
	}
	String getCurrentKey() {
		return keyChoice;
	}
	public void setCurrentKey(String key) {
		keyChoice = key;
		refreshChordKeySignatures(keyChoice);
	}
	public void setComposerText(String text) {
		composerText = text;
	}

	private void createVerseLine(String line) {
		try {
			// Load layout from fxml file
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("verseLineView.fxml"));
			StackPane verseLineLayout = loader.load();
			VerseLineViewController controller = loader.getController();
			controller.setParentController(this);
			
			controller.setVerseLine(line);
			
			verseLineControllers.add(controller);
			verseLineBox.getChildren().add(verseLineLayout);
			
		} catch (IOException e) {
			e.printStackTrace();
		}			
	}
	public ChantLineViewController createChantLine(boolean manual) {
		ChantLineViewController controller;

		try {
			// Load layout from fxml file
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("chantLineView.fxml"));
			GridPane chantLineLayout = loader.load();
			controller = loader.getController();
			controller.setMainController(this);
			
			chantLineControllers.add(controller);
			chantLineBox.getChildren().add(chantLineLayout);

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		if (manual) {
			recalcCLNames();
		}

		return controller;
	}

	public void recalcCLNames() {
		boolean previousWasPrime = false;
		int alternateCount = 1;
		char currentLetter = 65;
		ChantLineViewController prevMainLine = null;
		mainChantLines.clear();
		
		for (ChantLineViewController chantLine : chantLineControllers) {
			if (!chantLineControllers.get(chantLineControllers.size()-1).equals(chantLine)) { // If not the last
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
		syncCVLMapping();
	}
	
	public void syncCVLMapping() {
		if (toneDirectory == null) return; // No tone is loaded; don't do anything
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
			alert.setHeaderText("Are you sure you want to set this verse text? (changes and chord assignmets in the current text will be lost)");
			alert.initOwner(thisStage);
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get() == ButtonType.CANCEL) return;
			else askToOverwrite = false;
		}
		
		clearVerseLines();
		
		if (verseArea.getText().isEmpty()) return;
		
		verseSet = true;
		
		// Sends off the contents of the verse field (trimmed, and with any multi-spaces reduced to one) to be broken into syllables.
		String[] lines = Syllables.getSyllabificationLines(verseArea.getText());
		
		for (String line : lines) {
			createVerseLine(line);
		}
		
		syncCVLMapping();
	}
		
	
	boolean checkSave() {
		if (toneDirectory == null) {
			return true;
		} if (toneDirectory.getAbsolutePath().startsWith(builtInDir.getAbsolutePath()) && !MainApp.developerMode) {
			return true;
		}
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Save Confirmation");
		alert.setHeaderText("Do you want to save tone \"" + toneDirectory.getName().replaceAll("-", " ") + "\"?");
		alert.initOwner(thisStage);
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
		resetStageTitle();
		DirectoryChooser directoryChooser = new DirectoryChooser();
		// The second condition is there to make sure the user can't create a new tone in the built-in tones directory.
		if (toneDirectory != null && !toneDirectory.getAbsolutePath().contains(System.getProperty("user.dir") + File.separator + "Built-in Tones")) {
			directoryChooser.setInitialDirectory(new File(toneDirectory.getAbsolutePath().substring(0, toneDirectory.getAbsolutePath().lastIndexOf(File.separator))));
		} else {
			directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		}
		File workingDirectory = directoryChooser.showDialog(thisStage);
		if (workingDirectory == null) return false;
		
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("New Tone");
		dialog.setHeaderText("New Tone Name");
		dialog.initOwner(thisStage);
		Optional<String> result = dialog.showAndWait();
		result.ifPresent(name -> toneDirectory = new File(workingDirectory.getAbsolutePath() + "/"
				+ name.replace(" ", "-")));
		if (!toneDirectory.exists()) {
			return toneDirectory.mkdirs();

		} else {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("That tone already exists!");
			alert.initOwner(thisStage);
			alert.showAndWait();
			return false;
		}
	}
	private boolean loadTone() {
		resetStageTitle();
		DirectoryChooser directoryChooser = new DirectoryChooser();
		if (toneDirectory != null) {
			directoryChooser.setInitialDirectory(new File(toneDirectory.getAbsolutePath().substring(0, toneDirectory.getAbsolutePath().lastIndexOf(File.separator))));
		} else {
			if (builtInDir.exists()) {
				directoryChooser.setInitialDirectory(builtInDir);	
			} else {
				directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
			}
		}
		File selectedDirectory = directoryChooser.showDialog(thisStage);

		ToneReaderWriter toneReader = new ToneReaderWriter(chantLineControllers);
		if (selectedDirectory != null) {
			if (selectedDirectory.exists()) {
				toneDirectory = selectedDirectory;

				if (toneReader.loadTone(this, toneDirectory)) {
					return true;
				} else {
					Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("");
					alert.setHeaderText("Error loading tone!");
					alert.initOwner(thisStage);
					alert.showAndWait();
					
					// Since a tone was not loaded (or at least, not correctly),
					toneDirectory = null;
					
					return false;
				}

			} else {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("That folder doesn't exist!");
				alert.initOwner(thisStage);
				alert.showAndWait();
				return false;
			}
			
		} else return false;
		
	}
	
	private void resetStageTitle() {
		thisStage.setTitle(MainApp.APP_NAME);
	}
	private void updateStageTitle() {
		thisStage.setTitle(MainApp.APP_NAME + " - " + toneDirectory.getName().replaceAll("-", " "));
	}
	
	/*
	 * File Menu Actions
	 */
	@FXML private void handleNewTone() {
		if (checkSave() && createNewTone()) {
			clearChantLines();
			editMenu.setDisable(false);
			saveToneMenuItem.setDisable(false);
			saveToneAsMenuItem.setDisable(false);
			
			createChantLine(false);
			createChantLine(true);
			updateStageTitle();
			handleSave(); // So that the tone is loadable (will be empty)
		}
	}
	@FXML private void handleOpenTone() {
		LoadingTone = true;
		if (checkSave() && loadTone()) {
			editMenu.setDisable(false);
			saveToneMenuItem.setDisable(false);
			saveToneAsMenuItem.setDisable(false);
			updateStageTitle();
			
			saveToneMenuItem.setDisable(toneDirectory.getAbsolutePath().startsWith(builtInDir.getAbsolutePath())
					&& !MainApp.developerMode);
			
		}
		LoadingTone = false;
		
		refreshAllChords();
	}
	@FXML private void handleSave() {
		ToneReaderWriter toneWriter = new ToneReaderWriter(chantLineControllers, keyChoice, composerText);
		if (!toneWriter.saveTone(toneDirectory)) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Saving error!");
			alert.initOwner(thisStage);
			alert.showAndWait();
		} else {
			saveToneMenuItem.setDisable(false);
		}
		
	}
	@FXML private void handleSaveAs() {
		if (!createNewTone()) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Error creating new tone!");
			alert.initOwner(thisStage);
			alert.showAndWait();
		} else { // Success
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
		choices.add("F♯ major");
		choices.add("C♯ major");
		choices.add("F major");
		choices.add("B♭ major");
		choices.add("E♭ major");
		choices.add("A♭ major");
		choices.add("D♭ major");
		choices.add("G♭ major");
		choices.add("C♭ major");
		
		choices.add("A minor");
		choices.add("E minor");
		choices.add("B minor");
		choices.add("F♯ minor");
		choices.add("C♯ minor");
		choices.add("G♯ minor");
		choices.add("D♯ minor");
		choices.add("A♯ minor");
		choices.add("D minor");
		choices.add("G minor");
		choices.add("C minor");
		choices.add("F minor");
		choices.add("B♭ minor");
		choices.add("E♭ minor");
		choices.add("A♭ minor");
		

		ChoiceDialog<String> dialog = new ChoiceDialog<>(keyChoice, choices);
		dialog.setTitle("Key Choice");
		dialog.setHeaderText("Choose a key");
		dialog.initOwner(thisStage);
		Optional<String> result = dialog.showAndWait();

		result.ifPresent(letter -> setCurrentKey(result.get()));
	}
	
	@FXML private void handleSetComposerText() {

		TextInputDialog dialog = new TextInputDialog(composerText); // Initial text is existing composer text, if any.
		dialog.setTitle("Composer Text");
		dialog.setHeaderText("Set composer text (usually tone number and name of the system)");
		dialog.initOwner(thisStage);
		Optional<String> result = dialog.showAndWait();

		result.ifPresent(letter -> composerText = result.get());
	}
	
	/*
	 * Tools Menu Actions
	 */
	@FXML private void handleCombinePDFs() {
		try {
			// Load layout from fxml file
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("pdfCombineView.fxml"));
			BorderPane rootLayout = loader.load();
			PDFCombineViewController controller = loader.getController();
			controller.setDefaultDirectory(currentSavingDirectory);
			
			Stage pdfStage = new Stage();
			pdfStage.setTitle("Combine PDFs");
			pdfStage.getIcons().add(new Image(getClass().getResourceAsStream("/media/AppIcon.png")));
			pdfStage.setScene(new Scene(rootLayout));
			pdfStage.setResizable(false);
			pdfStage.show();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Options Menu Actions
	 */
	@FXML private void handleSetLilyPondDir() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Please select the folder which contains lilypond.exe");
		directoryChooser.setInitialDirectory(new File(MainApp.prefs.get(MainApp.PREFS_LILYPOND_LOCATION, System.getProperty("user.home"))));
		File savingDirectory = directoryChooser.showDialog(thisStage);
		if (savingDirectory == null) return;
		
		if (new File(savingDirectory.getAbsolutePath() + File.separator + MainApp.getPlatformSpecificLPExecutable()).exists()) {
			MainApp.prefs.put(MainApp.PREFS_LILYPOND_LOCATION, savingDirectory.getAbsolutePath());
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Restart");
			alert.setHeaderText(String.format(Locale.US, "This change will take effect the next time you restart %s.", MainApp.APP_NAME));
			alert.initOwner(thisStage);
			alert.showAndWait();
		} else {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("That directory does not contain a valid LilyPond executable.");
			alert.initOwner(thisStage);
			alert.showAndWait();
		}
		
	}
	@FXML private void handleResetLilyPondDir() {
		MainApp.prefs.remove(MainApp.PREFS_LILYPOND_LOCATION);
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Restart");
		alert.setHeaderText(String.format(Locale.US, "This change will take effect the next time you restart %s.", MainApp.APP_NAME));
		alert.initOwner(thisStage);
		alert.showAndWait();
	}

	/*
	 * Help Menu Actions
	 */
	@FXML private void handleAbout() {
		Platform.runLater(() -> {
			try {
				// Load layout from fxml file
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(MainApp.class.getResource("AboutScene.fxml"));
				BorderPane aboutLayout = loader.load();
				
				Stage aboutStage = new Stage();
				aboutStage.setScene(new Scene(aboutLayout));
				
				aboutStage.setTitle("About " + MainApp.APP_NAME);
				aboutStage.setResizable(false);
				aboutStage.initOwner(thisStage);
				aboutStage.initModality(Modality.APPLICATION_MODAL);
				aboutStage.getIcons().add(new Image(getClass().getResourceAsStream("/media/AppIcon.png")));
				aboutStage.show();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	private void refreshChordKeySignatures(String key) {
		for (ChantLineViewController chantLineController : chantLineControllers) {
	    	chantLineController.setKeySignature(key);
	    }
	}
	private void refreshAllChords() {
		for (ChantLineViewController chantLineController : chantLineControllers) {
	    	try {
				chantLineController.refreshAllChords();
			} catch (IOException e) {
				if (e.getMessage().contains("Cannot run program")) {
					Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Error");
					alert.setHeaderText(String.format(Locale.US, "Error running \"%s\"!", MainApp.getPlatformSpecificLPExecutable()));
					alert.initOwner(thisStage);
					alert.showAndWait();	
				}
				e.printStackTrace();
			}
	    }
	}
	
	void removeChantLine(ChantLineViewController chantLineViewController) {
		chantLineBox.getChildren().remove(chantLineViewController.getRootLayout());
		chantLineControllers.remove(chantLineViewController);
		recalcCLNames();
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
			alert.setHeaderText("Do you want to overwrite the previous render? (Choose cancel to save as new)");
			alert.initOwner(thisStage);
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get() == ButtonType.CANCEL) {
				if (!getNewRenderFilename()) return;
			} else if (!deletePreviousRender()) {
				Alert alert2 = new Alert(AlertType.ERROR);
				alert2.setTitle("Error");
				alert2.setHeaderText("An error occurred while overwriting the previous files, attempting to render anyway...");
				alert2.initOwner(thisStage);
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
			if (!LilyPondWriter.writeToLilypond(currentSavingDirectory, currentRenderFileName, verseLineControllers, keyChoice, 
					titleTextField.getText(), composerText, verseTopChoice.getValue(), verseTopField.getText(), verseBottomChoice.getValue(), verseBottomField.getText())) {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("An error occurred while saving!");
				alert.initOwner(thisStage);
				alert.showAndWait();
			}
		} catch (IOException e) {
			e.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("There was an IO error while saving!");
			alert.initOwner(thisStage);
			alert.showAndWait();
		}
		
	}
	
	private boolean getNewRenderFilename() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setInitialDirectory(currentSavingDirectory);
		File savingDirectory = directoryChooser.showDialog(thisStage);
		if (savingDirectory == null) return false;
		else currentSavingDirectory = savingDirectory;
		
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Lilypond Output");
		dialog.setHeaderText("Name the .ly file");
		dialog.initOwner(thisStage);
		Optional<String> result = dialog.showAndWait();
		
		currentRenderFileName = result.orElse(MainApp.APP_NAME + "-OUTPUT-" + new Timestamp(System.currentTimeMillis()).toString());
		
		return true;
	}
	
	private boolean deletePreviousRender() {
		File lyFile = new File(currentSavingDirectory + File.separator + currentRenderFileName + ".ly");
		File pdfFile = new File(currentSavingDirectory + File.separator + currentRenderFileName + ".pdf");

		if (lyFile.exists()) {
            return pdfFile.delete() && lyFile.delete();
        } else {
		    return pdfFile.delete();
        }

	}
	
	private String showVerseBox() {
		try {
			// Load layout from fxml file
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("quickVerseView.fxml"));
			BorderPane rootLayout = loader.load();
			QuickVerseController controller = loader.getController();
			
			Stage syllableStage = new Stage();
			syllableStage.setTitle("Verse Finder");
			syllableStage.getIcons().add(new Image(getClass().getResourceAsStream("/media/AppIcon.png")));
			syllableStage.setScene(new Scene(rootLayout));
			syllableStage.initModality(Modality.APPLICATION_MODAL); 
			syllableStage.setResizable(false);
			syllableStage.initOwner(thisStage);
			syllableStage.showAndWait();
			
			return controller.getSelectedVerse();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	boolean playMidiAsAssigned() {
		return playMidiMenuItem.isSelected();
	}

}
