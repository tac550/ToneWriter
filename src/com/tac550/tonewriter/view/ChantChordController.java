package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.FXMLLoaderIO;
import com.tac550.tonewriter.io.LilyPondInterface;
import com.tac550.tonewriter.io.MidiInterface;
import com.tac550.tonewriter.model.MainChord;
import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public abstract class ChantChordController implements CommentableView {
	
	protected ChantLineViewController chantLineController;

	private File midiFile;
	
	private String keySignature = "C major";
	protected Color chordColor;
	
	private String commentString = "";
	private Image commentButtonState;

	private TextField lastFocusedField;
	private String lastFocusedContents;
	
	@FXML private AnchorPane mainPane;
	
	@FXML protected Text numText;
	@FXML private ImageView chordView;
	@FXML private TextField SField, AField, TField, BField;
	@FXML protected Button preButton, posButton;

	@FXML private Button copyButton;
	@FXML private Button pasteButton;
	@FXML private Button playButton;
	@FXML private Button commentButton;
	@FXML ImageView moveHandleImage;

	@FXML protected void initialize() {
		// Fields
		for (TextField field : new TextField[] {SField, AField, TField, BField}) {
			// Listening for user-generated edits to the part fields.
			field.textProperty().addListener((ov, oldVal, newVal) -> chantLineController.edited());
			field.focusedProperty().addListener((ov, oldVal, newVal) -> {
				if (!newVal) { // Re-render when focus switched away only if the contents of this field changed
					if (lastFocusedField == field && !field.getText().equals(lastFocusedContents)) {
						refreshChordPreview();
					}
				} else { // When focus switched to the field
					lastFocusedField = field;
					lastFocusedContents = field.getText();

					field.selectAll();
				}
			});
		}

		// Buttons
		ImageView copyIcon = new ImageView(getClass().getResource("/media/copy.png").toExternalForm());
		copyIcon.setFitHeight(20);
		copyIcon.setFitWidth(20);
		copyButton.setGraphic(copyIcon);

		ImageView pasteIcon = new ImageView(getClass().getResource("/media/paste.png").toExternalForm());
		pasteIcon.setFitHeight(20);
		pasteIcon.setFitWidth(20);
		pasteButton.setGraphic(pasteIcon);

		copyButton.setText("");
		pasteButton.setText("");
		playButton.setText("\u25B6");

		// Comment button behavior
		applyCommentGraphic(bubbleImage); // Initial state - No comments
		commentButton.setOnMouseEntered((me) -> applyCommentGraphic(hoveredBubbleImage));
		commentButton.setOnMouseExited((me) -> applyCommentGraphic(commentButtonState));

		// Consume mouse click events so that move handle will not pan the scroll pane.
		moveHandleImage.addEventFilter(MouseEvent.MOUSE_DRAGGED, Event::consume);

	}
	
	void setChantLineController(ChantLineViewController parent) {
		chantLineController = parent;
		
		if (!MainApp.lilyPondAvailable()) return;
		
		refreshChordPreview();
	}
	public void setColor(Color color) {
		chordColor = color;
		setMainElementsColor(color);
	}
	Color getColor() {
		return chordColor;
	}
	void setKeySignature(String new_key) {
		keySignature = new_key;

		refreshChordPreview();
	}
	protected void disableButtons() {
		preButton.setDisable(true);
		posButton.setDisable(true);
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
	public String getEncodedComment() {
		return commentString.replaceAll("\n", "/n");
	}
	public boolean hasComment() {
		return !commentString.isEmpty();
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
		if (commentString.equals(comment)) return;

		chantLineController.edited();
		commentString = comment.replaceAll("/n", "\n");
		if (!comment.isEmpty()) {
			applyCommentGraphic(activeBubbleImage);
		} else {
			applyCommentGraphic(bubbleImage);
		}
	}

	AnchorPane getMainPane() {
		return mainPane;
	}

	@FXML void refreshChordPreview() {

		if (MainSceneController.LoadingTone) return; // Avoid unnecessary refreshes while loading a tone

		if (!MainApp.lilyPondAvailable()) {
			playButton.setDisable(true);
			chordView.setImage(new Image(getClass().getResource(MainApp.isDarkModeEnabled() ?
					"/media/NoLilyPondMessage-Dark.png" : "/media/NoLilyPondMessage.png").toExternalForm()));
			return;
		}

		try {
			LilyPondInterface.renderChord(getFields(), keySignature, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setMediaFilesDirectly(File[] files) {
		chordView.setImage(new Image(files[0].toURI().toString()));
		midiFile = files[1];
	}

	@FXML private void addPrepChord() throws IOException {
		if (this instanceof MainChord thisMainChord) {
			thisMainChord.addPrepChord("");
		}
	}
	@FXML private void addPostChord() throws IOException {
		if (this instanceof MainChord thisMainChord) {
			thisMainChord.addPostChord("");
		}
	}

	@FXML public void remove() {
		chantLineController.edited();
		delete();
	}
	public abstract void delete(); // Deletes this chord and its associated preps and posts.

	@FXML private void copy() {
		MainSceneController.copiedChord = getFields();
	}
	@FXML private void paste() {
		chantLineController.edited();
		setFields(MainSceneController.copiedChord);
	}
	@FXML public void playMidi() {
		MidiInterface.playMidi(midiFile, playButton);
	}
	@FXML private void editComment() {

		FXMLLoaderIO.loadFXMLLayoutAsync("commentView.fxml", loader -> {
			BorderPane rootLayout = loader.getRoot();
			CommentViewController controller = loader.getController();

			controller.setCommentText(commentString);
			controller.setTargetText(String.format(Locale.US, "Chord: %s (%s)", getName(), getFields()));

			Platform.runLater(() -> {
				Stage commentStage = new Stage();
				commentStage.setTitle("Comment");
				commentStage.getIcons().add(MainApp.APP_ICON);
				commentStage.setScene(new Scene(rootLayout));
				commentStage.initModality(Modality.APPLICATION_MODAL);
				commentStage.setResizable(false);
				commentStage.show();
				controller.setParentView(this);
			});
		});

	}
	
	public void applyCommentGraphic(Image image) {
		ImageView imageView = new ImageView(image);
		imageView.setFitHeight(15);
		imageView.setFitWidth(15);
		
		commentButton.setGraphic(imageView);
		commentButton.setText("");
		
		// Set the applied image as the default one (the one returned to when the mouse exits the comment button)
		// if it is not the hovered image.
		if (image != hoveredBubbleImage) {
			commentButtonState = image;
		}
	}

	private void setMainElementsColor(Color color) {
		for (Node node : new Node[] {preButton, posButton, SField, AField, TField, BField}) {
			setElementColor(node, color);
		}
	}
	private void setElementColor(Node node, Color color) {
		node.setStyle(String.format(Locale.US, "-fx-base: %s", TWUtils.toRGBCode(color)));
	}

	void setHighlighted(boolean highlighted) {
		// Set or reset highlight colors
		setMainElementsColor(highlighted ? new Color(0.922, 0.286, 0.035, 0) : chordColor);
		// Set scroll position
		if (highlighted) chantLineController.scrollChordIntoView(mainPane);
	}

	void indicateInsertionLeft() {
		setElementColor(preButton, Color.RED);
	}
	void indicateInsertionRight() {
		setElementColor(posButton, Color.RED);
	}
	void clearInsertionIndication() {
		setMainElementsColor(chordColor);
	}

	public abstract MainChord getAssociatedMainChord();
}
