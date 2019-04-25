package com.tac550.tonewriter.view;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ChantLineViewController implements CommentableView {

	private MainSceneController mainController;
	
	private boolean firstRepeated = false;
	private boolean selfTriggered = false;
	private int numAlternates = 0;
	private boolean hasPrime = false;
	
	private String commentString = "";
	private Image commentButtonState;
	
	@FXML GridPane rootLayout;
	
	@FXML Button upButton;
	@FXML Button downButton;
	@FXML Button firstRepeatedButton;
	@FXML ChoiceBox<String> nameChoice;
	@FXML Button commentButton;
	@FXML Button playButton;
	
	@FXML private HBox chordBox;
	private ArrayList<ChantChordController> chantChordControllers = new ArrayList<>();
	
	@FXML Button endButton;
	
	private boolean makePrimeLater = false;
	private boolean makeAlternateLater = false;

	@FXML private void initialize() {
		initializeMenus();
		
		// Comment button behavior
		applyCommentGraphic(bubbleImage); // Initial state - No comments
		commentButton.setOnMouseEntered((me) -> applyCommentGraphic(hoveredBubbleImage));
		commentButton.setOnMouseExited((me) -> applyCommentGraphic(commentButtonState));
		
		// Play button appearance and behavior
		playButton.setText("\u25B6");
		if (!MainApp.lilyPondAvailable()) {
			playButton.setDisable(true);
		}
		
	}
	void setMainController(MainSceneController parent) {
		mainController = parent;
	}
	private void initializeMenus() {
		
		// NAME CHOICE
		
		// Indicate the the user caused the recalculation to avoid a stack overflow
		nameChoice.setOnMouseClicked((ov) -> selfTriggered = true);
		
		// First repeated line status should not be available for "prime" lines
		nameChoice.getSelectionModel().selectedIndexProperty().addListener((ov, old_val, new_val) -> {
			if (new_val.equals(0)) {
				if (!nameChoice.getItems().get(0).equals("A")) {
					setFirstRepeatedAvailable(true);
				}
			} else {
				setFirstRepeatedAvailable(false);
			}
			// Prevent an infinite loop (recalcCLNames() causes this listener to fire)
			if (selfTriggered) {
				selfTriggered = false;
				mainController.recalcCLNames();
			}
		});
		
	}
	
	void setName(char new_letter, boolean previous_is_prime, int alternate_count) {
		int previousSelection = nameChoice.getSelectionModel().getSelectedIndex();
		if (new_letter == 'A' || previous_is_prime) {
			nameChoice.setItems(FXCollections.observableArrayList(
				    String.valueOf(new_letter))
				);
			
			setFirstRepeatedAvailable(false);
			upButton.setDisable(true);
			downButton.setDisable(false);
		} else {
			nameChoice.setItems(FXCollections.observableArrayList(
				    String.valueOf(new_letter), --new_letter + "'",
					new_letter + " alternate " + alternate_count)
				);
			
			setFirstRepeatedAvailable(true);
			upButton.setDisable(false);
			downButton.setDisable(false);
		}
		
		decideSelection(previousSelection);
	}
	void makeCadence() {
		nameChoice.setItems(FXCollections.observableArrayList(
			    "Cadence")
			);
		nameChoice.getSelectionModel().select(0);
		
		setFirstRepeatedAvailable(false);
		upButton.setDisable(false);
		downButton.setDisable(true);
	}
	void resetFRState() {
		firstRepeated = false;
		firstRepeatedButton.setStyle("");
	}
	public boolean getFirstRepeated() {
		return firstRepeated;
	}
	boolean getIsPrime() {
		return nameChoice.getValue().contains("'");
	}
	boolean getIsAlternate() {
		return nameChoice.getValue().contains("alternate");
	}
	public String getName() {
		return nameChoice.getValue();
	}
	public ArrayList<ChantChordController> getChords() {
		return chantChordControllers;
	}

	private void setFirstRepeatedAvailable(boolean available) {
		if (available) {
			firstRepeatedButton.setDisable(false);
		} else {
			resetFRState();
			firstRepeatedButton.setDisable(true);
		}
	}
	private void decideSelection(int previousSelection) {
		
		if (makePrimeLater) {
			nameChoice.getSelectionModel().select(1);
			makePrimeLater = false;
			return;
		} else if (makeAlternateLater) {
			nameChoice.getSelectionModel().select(2);
			makeAlternateLater = false;
			return;
		}
		
		if (previousSelection == -1 || nameChoice.getItems().get(0).equals("A") || previousSelection > nameChoice.getItems().size() - 1) {
			nameChoice.getSelectionModel().select(0);
		} else {
			nameChoice.getSelectionModel().select(previousSelection);
		}
		
	}
	private int countMainChords() {
		int count = 0;
		for (ChantChordController chord : chantChordControllers) {
			if (chord.getType() > 0) {
				count++;
			}
		}
		return count;
	}
	private int countEndChords() {
		int result = 0;
		for (ChantChordController chord : chantChordControllers) {
			if (chord.getColor() == Color.RED) {
				result++;
			}
		}
		return result;
	}
	
	GridPane getRootLayout() {
		return rootLayout;
	}

	MainSceneController getMainController() {
		return mainController;
	}

	private FXMLLoader loadChord() {
		// Load layout from fxml file
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(MainApp.class.getResource("chantChordView.fxml"));

		return loader;
	}

	public ChantChordController addRecitingChord() throws IOException {
		if (countMainChords() >= MainApp.CHORDCOLORS.length) { // Cap number of main (reciting) chords to available colors
			return null;
		}
		FXMLLoader loader = loadChord();

		AnchorPane chordLayout = loader.load();
		ChantChordController controller = loader.getController();

		// Add to end of lists but before any ending chords
		int position = chantChordControllers.size() - countEndChords();
		chantChordControllers.add(position, controller);
		chordBox.getChildren().add(position, chordLayout);

		controller.setChantLineController(this);
		controller.setKeySignature(mainController.getCurrentKey());
		recalcCHNames();

		return controller;
	}
	ChantChordController addPrepChord(ChantChordController caller_chord, Color chord_color) throws IOException {
		FXMLLoader loader = loadChord();

		AnchorPane chordLayout = loader.load();
		ChantChordController controller = loader.getController();

		int before_reciting_chord = chantChordControllers.indexOf(caller_chord);
		
		chantChordControllers.add(before_reciting_chord, controller);
		chordBox.getChildren().add(before_reciting_chord, chordLayout);

		controller.setColor(chord_color);
		controller.setChantLineController(this);
		controller.makePrep();
		controller.setKeySignature(mainController.getCurrentKey());
		recalcCHNames();
		
		return controller;
	}
	ChantChordController addPostChord(ChantChordController caller_chord, Color chord_color) throws IOException {
		FXMLLoader loader = loadChord();

		AnchorPane chordLayout = loader.load();
		ChantChordController controller = loader.getController();
		
		int after_reciting_chord = chantChordControllers.indexOf(caller_chord) + 1;

		chantChordControllers.add(after_reciting_chord, controller);
		chordBox.getChildren().add(after_reciting_chord, chordLayout);

		controller.setColor(chord_color);
		controller.setChantLineController(this);
		controller.makePost();
		controller.setKeySignature(mainController.getCurrentKey());
		recalcCHNames();
		
		return controller;
	}
	public ChantChordController addEndChord() throws IOException {

		FXMLLoader loader = loadChord();

		AnchorPane chordLayout = loader.load();
		ChantChordController controller = loader.getController();

		// Add to end of lists
		chantChordControllers.add(controller);
		chordBox.getChildren().add(chordLayout);

		controller.setColor(Color.RED);
		controller.setChantLineController(this);
		controller.makeFinalChord();
		
		controller.setKeySignature(mainController.getCurrentKey());
		recalcCHNames();
		
		return controller;
	}
	void removeChord(ChantChordController chord) {
		chordBox.getChildren().remove(chord.getRootLayout());
		chantChordControllers.remove(chord);
		recalcCHNames();
	}
	@FXML private void removeEndingChords() {
		ArrayList<ChantChordController> chordsToDelete = new ArrayList<>();
		for (ChantChordController chord : chantChordControllers) {
			if (chord.getType() == -3) { // If it's an ending chord
				chordsToDelete.add(chord);
			}
		}
		for (ChantChordController chord : chordsToDelete) {
			chord.deleteAll();
		}
	}
	
	private void recalcCHNames() {
		int currentNumber = 0;
		for (ChantChordController chantChord : chantChordControllers) {
			if (!(chantChord.getType() < 0)) { // If not special
				chantChord.setNumber(currentNumber + 1);
				chantChord.setColor(MainApp.CHORDCOLORS[currentNumber]);
				currentNumber++;
			}
		}
	}
	
	void setKeySignature(String new_key) {
		for (ChantChordController chord : chantChordControllers) {
			chord.setKeySignature(new_key);
		}
	}
	
	void refreshAllChords() throws IOException {
		for (ChantChordController chord : chantChordControllers) {
			chord.constructAndRenderChord();
		}
	}

	// Make sure ending context menu can be opened by left-clicking the button
	@FXML public void setCadenceAction() {
		removeEndingChords();
		try {
			addEndChord();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@FXML public void delete() {
		mainController.removeChantLine(this);
	}
	@FXML public void moveUp() {
		mainController.chantLineUp(this);
	}
	@FXML public void moveDown() {
		mainController.chantLineDown(this);
	}
	@FXML public void makeFirstRepeated() {
		if (!firstRepeated) {
			mainController.clearFirstRepeated();
			firstRepeated = true;
			firstRepeatedButton.setStyle("-fx-base: #90ff89");
		} else resetFRState();
		mainController.syncCVLMapping();
	}
	public void makePrime() {
		if (nameChoice.getItems().size() > 0) {
			nameChoice.getSelectionModel().select(1); // Works if both selections have already been loaded	
		} else {
			makePrimeLater = true;
		}
	}
	void setHasPrime(boolean has_prime) {
		hasPrime = has_prime;
	}
	boolean hasPrime() {
		return hasPrime;
	}
	public void makeAlternate() {
		if (nameChoice.getItems().size() > 0) {
			nameChoice.getSelectionModel().select(2); // Works if both selections have already been loaded	
		} else {
			makeAlternateLater = true;
		}
	}
	void setNumAlts(int num_alts) {
		numAlternates = num_alts;
	}
	int getNumAlts() {
		return numAlternates;
	}
	@FXML public void addRecitingTone() throws IOException {
		addRecitingChord();
	}
	@FXML private void editComment() {
		Platform.runLater(() -> {
			try {
				// Load layout from fxml file
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(MainApp.class.getResource("commentView.fxml"));
				BorderPane rootLayout = loader.load();
				CommentViewController controller = loader.getController();
				controller.setParentView(this);
				controller.setCommentText(commentString);
				controller.setTargetText(String.format(Locale.US, "Line %s", getName()));
				
				Stage commentStage = new Stage();
				commentStage.setTitle("Comment");
				commentStage.getIcons().add(new Image(getClass().getResourceAsStream("/media/AppIcon.png")));
				commentStage.setScene(new Scene(rootLayout));
				commentStage.initModality(Modality.APPLICATION_MODAL); 
				commentStage.setResizable(false);
				commentStage.show();
				
			} catch (IOException e) {
				e.printStackTrace();
			}	
		});
	}
	@FXML private void handlePlay() {
		Task<Integer> midiTask = new Task<>() {
			@Override
			protected Integer call() throws Exception {

				for (ChantChordController controller : chantChordControllers) {
					controller.playMidi();

					Thread.sleep(1000);
				}

				return 0;
			}
		};
		
		Thread th = new Thread(midiTask);
		
		th.start();
	}
	
	public String getComment() {
		return commentString.replaceAll("\n", "/n");
	}
	public void setComment(String comment) {
		commentString = comment.replaceAll("/n", "\n");
		if (!comment.isEmpty()) {
			applyCommentGraphic(activeBubbleImage);
		} else {
			applyCommentGraphic(bubbleImage);
		}
	}
	public boolean hasComment() {
		return !commentString.isEmpty();
	}
	public void applyCommentGraphic(Image image) {
		ImageView imageView = new ImageView(image);
		imageView.setFitHeight(15);
		imageView.setFitWidth(15);
		
		commentButton.setGraphic(imageView);
		
		// Set the applied image as the default one (the one returned to when the mouse exits the comment button)
		// if it is not the hovered image.
		if (image != hoveredBubbleImage) {
			commentButtonState = image;
		}
	}
	
}
