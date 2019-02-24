package com.tac550.tonewriter.view;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import com.tac550.tonewriter.io.LilyPondWriter;
import com.tac550.tonewriter.util.TBUtils;

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

public class ChantChordController implements CommentableView {
	
	private ChantLineViewController chantLineController;

	private File lilypondFile;
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
	private ChantChordController associatedChord; // Only populated if this is a prep or post chord
	
	@FXML private void initialize() {
		// Fields
		for (TextField field : new TextField[] {SField, AField, TField, BField}) {
			field.focusedProperty().addListener((ov, old_val, new_val) -> {
				if (!new_val) { // Rerender when focus switched away from field
					playButton.setDisable(true);
					try {
						constructAndRenderChord();
					} catch (IOException e) {
						e.printStackTrace();
					}
					playButton.setDisable(false);
				} else { // Select all when focus switched to the field
					field.selectAll();
				}
			});
		}

		// Buttons
		copyButton.setText("⎘");
		pasteButton.setText("⎙");
		playButton.setText("▶");
		// Comment button behavior
		applyCommentGraphic(bubbleImage); // Initial state - No comments
		commentButton.setOnMouseEntered((me) -> {
			applyCommentGraphic(hoveredBubbleImage);
		});
		commentButton.setOnMouseExited((me) -> {
			applyCommentGraphic(commentButtonState);
		});
		
	}
	
	void setChantLineController(ChantLineViewController parent) throws IOException {
		chantLineController = parent;
		
		if (!MainApp.lilyPondAvailable()) return;
		
		// Create the temporary file to hold the lilypond markup
		lilypondFile = File.createTempFile(MainApp.APPNAME + "--"
				+ parent.getMainController().getToneDirectory().getName() + "-", "-chord.ly");
		lilypondFile.deleteOnExit();
		
		try {
			LilyPondWriter.ExportResource("chordTemplate.ly", lilypondFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			constructAndRenderChord();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	void setNumber(int number) {
		numText.setText(String.valueOf(number));
	}
	void setColor(Color color) {
		chordColor = color;
		for (Node node : new Node[] {preButton, posButton, SField, AField, TField, BField}) {
			node.setStyle(String.format(Locale.US, "-fx-base: %s", TBUtils.toRGBCode(color)));
		}
		
		for (ChantChordController chord : prepsAndPosts) {
			chord.setColor(color);
		}
	}
	public Color getColor() {
		return chordColor;
	}
	void setKeySignature(String new_key) {
		keySignature = new_key;
		try {
			constructAndRenderChord();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	void makePrep() {
		numText.setText("Prep");
		setButtonsDisabled(true);
	}
	void makePost() {
		numText.setText("Post");
		setButtonsDisabled(true);
	}
	public void makeFinalChord() {
		numText.setText("End");
		preButton.setDisable(false);
		posButton.setDisable(true);
	}
	private void setButtonsDisabled(boolean disabled) {
		preButton.setDisable(disabled);
		posButton.setDisable(disabled);
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
		return String.format("%s-%s-%s-%s", SField.getText(), AField.getText(), TField.getText(), BField.getText());
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
	private void setAssociatedChord(ChantChordController chord) {
		associatedChord = chord;
	}
	public ChantChordController getAssociatedChord() {
		return associatedChord;
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
		try {
			constructAndRenderChord();
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	@FXML public void constructAndRenderChord() throws IOException {
		if (MainSceneController.LoadingTone) return; // Avoid unnecessary refreshes while loading a tone
		
		if (!MainApp.lilyPondAvailable()) {
			playButton.setDisable(true);
			chordView.setImage(new Image(getClass().getResource("/media/NoLilyPondMessage.png").toExternalForm()));
			return;
		}
		
		List<String> lines = Files.readAllLines(lilypondFile.toPath(), StandardCharsets.UTF_8);

		// Key signature parsing
	
		lines.set(10, LilyPondWriter.keySignatureToLilyPond(keySignature));
		lines.set(18, LilyPondWriter.parseNoteRelative(SField.getText(), LilyPondWriter.ADJUSTMENT_SOPRANO));
		lines.set(24, LilyPondWriter.parseNoteRelative(AField.getText(), LilyPondWriter.ADJUSTMENT_ALTO));
		lines.set(30, LilyPondWriter.parseNoteRelative(TField.getText(), LilyPondWriter.ADJUSTMENT_TENOR));
		lines.set(36, LilyPondWriter.parseNoteRelative(BField.getText(), LilyPondWriter.ADJUSTMENT_BASS));
		Files.write(lilypondFile.toPath(), lines, StandardCharsets.UTF_8);

		File outputFile = new File(lilypondFile.getAbsolutePath().replace(".ly", ".png"));
		outputFile.deleteOnExit();
		midiFile = new File(lilypondFile.getAbsolutePath().replace(".ly", MainApp.getPlatformSpecificMidiExtension()));
		midiFile.deleteOnExit();
		// In case of a rendering failure that leaves .ps files in the temp location, delete those files.
		File psFile = new File(lilypondFile.getAbsolutePath().replace(".ly", ".ps"));
		psFile.deleteOnExit();

		
		LilyPondWriter.executePlatformSpecificLPRender(lilypondFile, true, () -> {
			chordView.setImage(new Image(outputFile.toURI().toString()));
		});
	}
	
	@FXML public void addPrepChord() throws IOException {
		addPrepChord("");
	}
	public ChantChordController addPrepChord(String values) throws IOException {
		ChantChordController prepChordController = chantLineController.addPrepChord(this, chordColor);
		prepsAndPosts.add(prepChordController);
		prepChordController.setAssociatedChord(this);
		prepChordController.setFields(values);
		
		return prepChordController;
	}
	@FXML public void addPostChord() throws IOException {
		if (getType() == -3) {
			ChantChordController endChord = chantLineController.addEndChord();
			prepsAndPosts.add(endChord);
			endChord.setAssociatedChord(this);
		} else {
			addPostChord("");
		}
	}
	public ChantChordController addPostChord(String values) throws IOException {
		ChantChordController postChordController = chantLineController.addPostChord(this, chordColor);
		prepsAndPosts.add(postChordController);
		postChordController.setAssociatedChord(this);
		postChordController.setFields(values);
		
		return postChordController;
	}
	@FXML public void deleteAll() { // Deletes this chord and its associated preps and posts.
		for (ChantChordController chord : prepsAndPosts) {
			chord.deleteAll();
		}
		if (associatedChord != null) {
			associatedChord.getPrepsAndPosts().remove(this);
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
		Task<Integer> midiTask = new Task<Integer>() {
			@Override
			protected Integer call() throws Exception {
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
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					sequencer.close();
				});
				stopThread.start();
				
				return null;
			}
		};
		
		midiTask.run();
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
				controller.setTargetText(String.format(Locale.US, "Chord: %s (%s)", getName(), getFields()));
				
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
