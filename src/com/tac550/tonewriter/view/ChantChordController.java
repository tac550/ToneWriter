package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.LilyPondWriter;
import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class ChantChordController implements CommentableView {
	
	private ChantLineViewController chantLineController;

	private File midiFile;
	
	private String keySignature = "C major";
	private Color chordColor;
	
	private String commentString = "";
	private Image commentButtonState;
	
	@FXML private AnchorPane rootLayout;
	
	@FXML private Text numText;
	@FXML private ImageView chordView;
	@FXML private TextField SField, AField, TField, BField;
	@FXML private Button preButton, posButton;

	@FXML private Button copyButton;
	@FXML private Button pasteButton;
	@FXML private Button playButton;
	@FXML private Button commentButton;
	
	private ArrayList<ChantChordController> prepsAndPosts = new ArrayList<>();
	private ChantChordController associatedRecitingChord; // Only populated if this is a prep or post chord
	
	@FXML private void initialize() {
		// Fields
		for (TextField field : new TextField[] {SField, AField, TField, BField}) {
			field.focusedProperty().addListener((ov, old_val, new_val) -> {
				if (!new_val) { // Re-render when focus switched away from field
					playButton.setDisable(true);

					refreshChordPreview();

					playButton.setDisable(false);
				} else { // Select all when focus switched to the field
					field.selectAll();
				}
			});
		}

		// Buttons
		copyButton.setText("\u2398");
		pasteButton.setText("\u2399");
		playButton.setText("\u25B6");
		// Comment button behavior
		applyCommentGraphic(bubbleImage); // Initial state - No comments
		commentButton.setOnMouseEntered((me) -> applyCommentGraphic(hoveredBubbleImage));
		commentButton.setOnMouseExited((me) -> applyCommentGraphic(commentButtonState));
		
	}
	
	void setChantLineController(ChantLineViewController parent) {
		chantLineController = parent;
		
		if (!MainApp.lilyPondAvailable()) return;
		
		refreshChordPreview();
	}
	void setNumber(int number) {
		numText.setText(String.valueOf(number));
	}
	void setColor(Color color) {
		chordColor = color;

		setElementColor(chordColor);
		
		for (ChantChordController chord : prepsAndPosts) {
			chord.setColor(chordColor);
		}
	}
	Color getColor() {
		return chordColor;
	}
	void setKeySignature(String new_key) {
		keySignature = new_key;

		refreshChordPreview();
	}
	void makePrep() {
		numText.setText("Prep");
		disableButtons();
	}
	void makePost() {
		numText.setText("Post");
		disableButtons();
	}
	void makeFinalChord() {
		numText.setText("End");
		preButton.setDisable(false);
		posButton.setDisable(true);
	}
	private void disableButtons() {
		preButton.setDisable(true);
		posButton.setDisable(true);
	}
	public int getType() {
		if (numText.getText().equals("Text")) return 0; // Default state; chord is not set up.
		if (numText.getText().equals("Prep")) return -1; // Chord is a prep chord.
		if (numText.getText().equals("Post")) return -2; // Chord is a post chord.
		if (numText.getText().equals("End")) return -3; // Chord is an end chord.
		else return 1; // Chord is a normal reciting tone.
	}
	public String getName() {
		return numText.getText();
	}
	public String getFields() {
		return String.format(Locale.US,
				"%s-%s-%s-%s",
				SField.getText().isEmpty() ? " " : SField.getText(),
				AField.getText().isEmpty() ? " " : AField.getText(),
				TField.getText().isEmpty() ? " " : TField.getText(),
				BField.getText().isEmpty() ? " " : BField.getText());
	}
	public String getComment() {
		return commentString.replaceAll("\n", "/n");
	}
	public boolean hasComment() {
		return !commentString.isEmpty();
	}
	public ArrayList<ChantChordController> getPrepsAndPosts() {
		return prepsAndPosts;
	}
	private void setAssociatedRecitingChord(ChantChordController chord) {
		associatedRecitingChord = chord;
	}
	public void setFields(String data) {
		if (data == null || !data.contains("-")) {
			return;
		}
		String[] values = data.replace("-", " - ").split("-");
		SField.setText(values[0].trim());
		AField.setText(values[1].trim());
		TField.setText(values[2].trim());
		BField.setText(values[3].trim());

		refreshChordPreview();
	}
	public void setComment(String comment) {
		commentString = comment.replaceAll("/n", "\n");
		if (!comment.isEmpty()) {
			applyCommentGraphic(activeBubbleImage);
		} else {
			applyCommentGraphic(bubbleImage);
		}
	}

	AnchorPane getRootLayout() {
		return rootLayout;
	}

	@FXML private File refreshChordPreview() {

		if (MainSceneController.LoadingTone) return null; // Avoid unnecessary refreshes while loading a tone

		if (!MainApp.lilyPondAvailable()) {
			playButton.setDisable(true);
			chordView.setImage(new Image(getClass().getResource(MainApp.darkModeEnabled() ?
					"/media/NoLilyPondMessage-Dark.png" : "/media/NoLilyPondMessage.png").toExternalForm()));
			return null;
		}

		File lilypondFile = LilyPondWriter.createTempLYChordFile(chantLineController.getMainController().getToneFile().getName());

		try {
			File[] files = LilyPondWriter.renderChord(lilypondFile, getFields(), keySignature, chordView);
			midiFile = files[1];
			return files[0];
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	void setChordInfoDirectly(File[] files) {
		chordView.setImage(new Image(files[0].toURI().toString()));
		midiFile = files[1];
	}
	
	@FXML public void addPrepChord() throws IOException {
		addPrepChord("");
	}
	public ChantChordController addPrepChord(String values) throws IOException {
		ChantChordController prepChordController = chantLineController.addPrepChord(this, chordColor);
		prepsAndPosts.add(prepChordController);
		prepChordController.setAssociatedRecitingChord(this);
		prepChordController.setFields(values);
		
		return prepChordController;
	}
	@FXML public void addPostChord() throws IOException {
		if (getType() == -3) {
			ChantChordController endChord = chantLineController.addEndChord();
			prepsAndPosts.add(endChord);
			endChord.setAssociatedRecitingChord(this);
		} else {
			addPostChord("");
		}
	}
	public ChantChordController addPostChord(String values) throws IOException {
		ChantChordController postChordController = chantLineController.addPostChord(this, chordColor);
		prepsAndPosts.add(postChordController);
		postChordController.setAssociatedRecitingChord(this);
		postChordController.setFields(values);
		
		return postChordController;
	}
	@FXML public void deleteAll() { // Deletes this chord and its associated preps and posts.
		for (ChantChordController chord : prepsAndPosts) {
			chord.deleteAll();
		}
		if (associatedRecitingChord != null) {
			associatedRecitingChord.getPrepsAndPosts().remove(this);
		}
		chantLineController.removeChord(this);
	}
	@FXML private void copy() {
		MainSceneController.copiedChord = getFields();
	}
	@FXML private void paste() {
		setFields(MainSceneController.copiedChord);
	}
	@FXML public void playMidi() {
		Task<Void> midiTask = new Task<>() {
			@Override
			protected Void call() throws Exception {
				// From file
				Sequence sequence = MidiSystem.getSequence(midiFile);

				// Create a sequencer for the sequence
				Sequencer sequencer = MidiSystem.getSequencer();
				sequencer.open();
				sequencer.setSequence(sequence);

				// Start playing
				sequencer.start();

				// Thread to close midi after it's had long enough to finish playing.
				// This fixes the application not closing correctly if the user played midi.
				Thread stopThread = new Thread(() -> {
					playButton.setStyle("-fx-base: #fffa61");
					try {
						Thread.sleep(1000);
						playButton.setStyle("");
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					sequencer.close();
				});
				stopThread.start();

				return null;
			}
		};

		Thread midiThread = new Thread(midiTask);
		midiThread.start();
	}
	@FXML private void editComment() {
		Platform.runLater(() -> {
			try {
				// Load layout from fxml file
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(MainApp.class.getResource("commentView.fxml"));
				BorderPane rootLayout = loader.load();
				CommentViewController controller = loader.getController();
				controller.setCommentText(commentString);
				controller.setTargetText(String.format(Locale.US, "Chord: %s (%s)", getName(), getFields()));
				
				Stage commentStage = new Stage();
				commentStage.setTitle("Comment");
				commentStage.getIcons().add(MainApp.APP_ICON);
				commentStage.setScene(new Scene(rootLayout));
				commentStage.initModality(Modality.APPLICATION_MODAL);
				commentStage.setResizable(false);
				commentStage.show();
				controller.setParentView(this);
				
			} catch (IOException e) {
				e.printStackTrace();
			}	
		});
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

	private void setElementColor(Color color) {
		for (Node node : new Node[] {preButton, posButton, SField, AField, TField, BField}) {
			node.setStyle(String.format(Locale.US, "-fx-base: %s", TWUtils.toRGBCode(color)));
		}
	}

	void setHighlighted(boolean value) {
		setElementColor(value ? new Color(0.922, 0.286, 0.035, 0) : chordColor);
	}

}
