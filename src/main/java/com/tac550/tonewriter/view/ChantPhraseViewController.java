package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.FXMLLoaderIO;
import com.tac550.tonewriter.model.*;
import com.tac550.tonewriter.util.TWUtils;
import com.tac550.tonewriter.view.chord.*;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ChantPhraseViewController implements CommentableView {

	private MainSceneController mainController;
	
	private boolean firstRepeated = false;
	private boolean selfTriggered = false;
	private int numAlternates = 0;
	private boolean hasPrime = false;
	
	private String commentString = "";
	private Image commentButtonState;
	
	@FXML private GridPane mainPane;
	@FXML private ScrollPane chordScrollPane;

	@FXML private Button upButton;
	@FXML private Button downButton;
	@FXML private Button firstRepeatedButton;
	@FXML private ChoiceBox<String> nameChoice;
	@FXML private Button commentButton;
	@FXML private Button playButton;
	
	@FXML private HBox chordBox;
	private final List<ChordViewController> chordViewControllers = new ArrayList<>();
	
	private boolean makePrimeLater = false;
	private boolean makeAlternateLater = false;

	// Fields related to drag reordering of chords
	private static final String CHORD_DRAG_KEY = "ToneWriter chord: ";
	private final ObjectProperty<AnchorPane> draggingChord = new SimpleObjectProperty<>();
	private final ObjectProperty<ChordViewController> draggingController = new SimpleObjectProperty<>();

	// Fields for automatic drag-scrolling
	AnimationTimer autoScroller;
	private double prevTime = -1;
	private boolean mouseReversed = false;
	private final double scrollThreshold = 0.1;
	private final double scrollSpeed = 10;
	private double cursorLocation;
	private double previousMouseX = 0;
	private double mouseX;

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
		
		// Indicate the user caused the recalculation to avoid a stack overflow
		nameChoice.setOnMouseClicked((ov) -> selfTriggered = true);
		
		// First repeated line status should not be available for "prime" lines
		nameChoice.getSelectionModel().selectedIndexProperty().addListener((ov, oldVal, newVal) -> {
			if (newVal.equals(0)) {
				if (!nameChoice.getItems().getFirst().endsWith("A")) {
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
	
	void setName(char new_letter, boolean previous_is_prime, int next_alternate) {
		int previousSelection = nameChoice.getSelectionModel().getSelectedIndex();
		if (new_letter == 'A') {
			nameChoice.setItems(FXCollections.observableArrayList(
					"Phrase " + new_letter));
			
			setFirstRepeatedAvailable(false);
			upButton.setDisable(true);
		} else if (previous_is_prime) {
			nameChoice.setItems(FXCollections.observableArrayList(
					"Phrase " + new_letter));
		} else {
			nameChoice.setItems(FXCollections.observableArrayList(
				    "Phrase " + new_letter, "Phrase " + --new_letter + "'",
					new_letter + " alternate " + next_alternate));
			
			setFirstRepeatedAvailable(true);
			upButton.setDisable(false);
		}
		downButton.setDisable(false);

		decideSelection(previousSelection);
	}
	void makeFinal() {
		nameChoice.setItems(FXCollections.observableArrayList(
			    "Final Phrase")
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
	public boolean isFirstRepeated() {
		return firstRepeated;
	}
	boolean isPrime() {
		return nameChoice.getValue().contains("'");
	}
	boolean isAlternate() {
		return nameChoice.getValue().contains("alternate");
	}
	public String getName() {
		return nameChoice.getValue();
	}
	public List<ChordViewController> getChords() {
		return chordViewControllers;
	}

	private void setFirstRepeatedAvailable(boolean available) {
		resetFRState();
		firstRepeatedButton.setDisable(!available);
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
		
		if (previousSelection == -1 || nameChoice.getItems().getFirst().equals("A") || previousSelection > nameChoice.getItems().size() - 1) {
			nameChoice.getSelectionModel().select(0);
		} else {
			nameChoice.getSelectionModel().select(previousSelection);
		}
		
	}
	private int countRecitingChords() {
		int count = 0;
		for (ChordViewController chord : chordViewControllers) {
			if (chord instanceof RecitingChordView) {
				count++;
			}
		}
		return count;
	}
	private int countEndChords() {
		int result = 0;
		for (ChordViewController chord : chordViewControllers) {
			if (chord.getColor() == MainApp.END_CHORD_COLOR) {
				result++;
			}
		}
		return result;
	}
	
	GridPane getMainPane() {
		return mainPane;
	}

	private void addChord(int position, ChordViewController other_controller) throws IOException {
		// Load layout from fxml file
		FXMLLoader loader = new FXMLLoader();
		loader.setControllerFactory(aClass -> other_controller);
		loader.setLocation(MainApp.class.getResource("/fxml/ChordView.fxml"));

		AnchorPane chordPane = loader.load();
		ChordViewController controller = loader.getController();

		chordViewControllers.add(position, controller);
		chordBox.getChildren().add(position, chordPane);

		// Drag-reordering behavior
		controller.moveHandleImage.setOnDragDetected(event -> {
			Dragboard dragboard = chordPane.startDragAndDrop(TransferMode.MOVE);
			ClipboardContent clipboardContent = new ClipboardContent();
			clipboardContent.putString(CHORD_DRAG_KEY + controller.getFields() + " ");
			dragboard.setContent(clipboardContent);

			// Dragging image creation
			Screen screen;
			try {
				screen = Screen.getScreensForRectangle(event.getX(), event.getY(), 0, 0).getFirst();
			} catch (IndexOutOfBoundsException e) {
				screen = Screen.getScreens().getFirst();
			}
			double scaleX = screen.getOutputScaleX();
			double scaleY = screen.getOutputScaleY();

			final Bounds bounds = chordPane.getLayoutBounds();
			final WritableImage writableImage = new WritableImage(
					(int) Math.round(bounds.getWidth() * scaleX),
					(int) Math.round(bounds.getHeight() * scaleY));
			final SnapshotParameters parameters = new SnapshotParameters();
			parameters.setTransform(Transform.scale(scaleX, scaleY));
			parameters.setFill(TWUtils.getUIBaseColor());
			WritableImage snapshot = chordPane.snapshot(parameters, writableImage);
			dragboard.setDragView(snapshot, chordPane.getWidth() / 2 * scaleX, chordPane.getHeight() / 2 * scaleY);

			draggingChord.set(chordPane);
			draggingController.set(controller);

			// Begin automatic scrolling task
			if (autoScroller == null) {

				autoScroller = new AnimationTimer() {
					@Override
					public void handle(long now) {
						if (draggingChord.get() == null || draggingController.get() == null) {
							prevTime = now;
							return;
						}

						if (prevTime == -1) prevTime = now;

						double timeDelta = (now - prevTime) / 1000000000; // In seconds
						double previousVectorAbs = Math.abs(previousMouseX);
						double mouseVectorAbs = Math.abs(mouseX);

						// Only proceed if cursor is within scrolling threshold
						if (cursorLocation < scrollThreshold || cursorLocation > 1 - scrollThreshold) {
							if (mouseVectorAbs > previousVectorAbs) {
								chordScrollPane.setHvalue(chordScrollPane.getHvalue() + ((scrollSpeed * mouseX * timeDelta)
										* (chordScrollPane.getWidth() / (2 * chordBox.getWidth()))));
								if (mouseReversed) mouseReversed = false;
							} else if (mouseVectorAbs == previousVectorAbs && !mouseReversed) {
								chordScrollPane.setHvalue(chordScrollPane.getHvalue() + ((scrollSpeed * mouseX * timeDelta)
										* (chordScrollPane.getWidth() / (2 * chordBox.getWidth()))));
							} else if (mouseVectorAbs < previousVectorAbs) {
								mouseReversed = true;
							}
						}
						previousMouseX = mouseX;
						prevTime = now;
					}
				};
				autoScroller.start();

			}

			event.consume();
		});
		chordPane.setOnDragOver(event -> {
			if (draggingChord.get() == null || draggingController.get() == null) return;

			// Update auto scrolling values
			cursorLocation = getCursorPositionFraction(chordPane, event.getX());
			mouseX = cursorLocation - 0.5;

			final Dragboard dragboard = event.getDragboard();
			if (dragboard.hasString()
					&& dragboard.getString().startsWith(CHORD_DRAG_KEY)) {

				int sourceIndex = chordBox.getChildren().indexOf(draggingChord.get());
				int hoveredIndex = chordBox.getChildren().indexOf(chordPane);
				ChordViewController hoveredChord = chordViewControllers.get(hoveredIndex);
				ChordViewController otherChord = null;
				if (sourceIndex < hoveredIndex && hoveredIndex < chordViewControllers.size() - 1) {
					otherChord = chordViewControllers.get(hoveredIndex + 1);
				} else if (sourceIndex > hoveredIndex && hoveredIndex > 0) {
					otherChord = chordViewControllers.get(hoveredIndex - 1);
				} else if (sourceIndex == hoveredIndex) { // Don't accept moves with equal source and target
					event.consume();
					return;
				}

				// Move validation
				if (draggingController.get() instanceof RecitingChordView) {
					// Disallow any move that would place it (and its preps/posts) in the middle of any other main chord's preps/posts.
					if (otherChord != null) {
						if (otherChord.getAssociatedMainChord() == hoveredChord.getAssociatedMainChord()
								|| otherChord.getAssociatedMainChord() == draggingController.get()
								|| hoveredChord.getAssociatedMainChord() == draggingController.get()) {
							event.consume();
							return;
						}
					} else {
						if (hoveredChord instanceof EndChordView) {
							event.consume();
							return;
						}
					}
				} else if (draggingController.get() instanceof EndChordView) {
					// Disallow all moves.
					event.consume();
					return;
				} else if (draggingController.get() instanceof SubChordView) {
					// Disallow any move outside its own group of preps or posts.
					if (!(hoveredChord instanceof SubChordView)
							|| hoveredChord.getClass() != draggingController.get().getClass()
							|| hoveredChord.getAssociatedMainChord() != draggingController.get().getAssociatedMainChord()) {
						event.consume();
						return;
					}
				}

				// Success
				event.acceptTransferModes(TransferMode.MOVE);

				if (sourceIndex < hoveredIndex) hoveredChord.indicateInsertionRight();
				else hoveredChord.indicateInsertionLeft();

				event.consume();
			}
		});
		chordPane.setOnDragExited(event -> {
			if (draggingChord.get() == null || draggingController.get() == null) return;

			ChordViewController exitedChord = chordViewControllers.get(chordBox.getChildren().indexOf(chordPane));
			exitedChord.clearInsertionIndication();
		});
		chordPane.setOnDragDropped(event -> {
			Dragboard db = event.getDragboard();
			boolean success = false;
			if (db.hasString()) {
				Node source = (Node) event.getGestureSource();
				int sourceIndex = chordBox.getChildren().indexOf(source);
				int targetIndex = chordBox.getChildren().indexOf(chordPane);
				List<Node> nodes = new ArrayList<>(chordBox.getChildren());
				List<ChordViewController> controllers = new ArrayList<>(chordViewControllers);
				if (sourceIndex != targetIndex) {

					int numPreps = draggingController.get() instanceof RecitingChordView rDraggingChord ?
							rDraggingChord.getPreps().size() : 0;
					int numPosts = draggingController.get() instanceof RecitingChordView rDraggingChord ?
							rDraggingChord.getPosts().size() : 0;

					ChordViewController targetController = chordViewControllers.get(targetIndex);

					int distance = 1 + numPreps + numPosts;

					if (sourceIndex < targetIndex) {
						int fromIndex = sourceIndex - numPreps;
						int toIndex = targetIndex + 1;
						Collections.rotate(
								nodes.subList(fromIndex, toIndex), -distance);
						Collections.rotate(
								controllers.subList(fromIndex, toIndex), -distance);
					} else {
						int toIndex = sourceIndex + numPosts + 1;
						Collections.rotate(
								nodes.subList(targetIndex, toIndex), distance);
						Collections.rotate(
								controllers.subList(targetIndex, toIndex), distance);
					}

					chordBox.getChildren().clear();
					chordBox.getChildren().addAll(nodes);
					chordViewControllers.clear();
					chordViewControllers.addAll(controllers);

					if (draggingController.get() instanceof SubChordView draggingSubChord
							&& targetController instanceof SubChordView targetSubChord)
						draggingController.get().getAssociatedMainChord().rotatePrepsOrPosts(
								draggingSubChord, targetSubChord);

					recalcCHNames();
					edited();
					success = true;
				}
			}

			event.setDropCompleted(success);
			event.consume();
		});
		chordPane.setOnDragDone(event -> {
			draggingChord.set(null);
			draggingController.set(null);
		});

		controller.setChantPhraseController(this);
		controller.setKeySignature(mainController.getKeySignature());

	}
	public RecitingChordView addRecitingChord() throws IOException {
		if (countRecitingChords() >= MainApp.CHORD_COLORS.length) { // Cap number of main (reciting) chords to available colors
			return null;
		}
		// Add to end of lists but before any ending chords
		int position = chordViewControllers.size() - countEndChords();

		RecitingChordView controller = new RecitingChordView();

		addChord(position, controller);

		recalcCHNames();

		return controller;
	}
	public PrepChordView addPrepChord(MainChordView caller_chord, Color chord_color) throws IOException {

		int before_reciting_chord = chordViewControllers.indexOf(caller_chord);

		PrepChordView controller = new PrepChordView();

		addChord(before_reciting_chord, controller);

		controller.setColor(chord_color);
		recalcCHNames();
		
		return controller;
	}
	public PostChordView addPostChord(MainChordView caller_chord, Color chord_color) throws IOException {

		int after_reciting_chord = chordViewControllers.indexOf(caller_chord) + 1;

		PostChordView controller = new PostChordView();

		addChord(after_reciting_chord, controller);

		controller.setColor(chord_color);
		recalcCHNames();
		
		return controller;
	}
	public EndChordView addEndChord() throws IOException {

		int last_position = chordViewControllers.size();

		EndChordView controller = new EndChordView();

		addChord(last_position, controller);

		controller.setColor(MainApp.END_CHORD_COLOR);
		recalcCHNames();
		
		return controller;
	}
	public void removeChord(ChordViewController chord) {
		chordBox.getChildren().remove(chord.getMainPane());
		chordViewControllers.remove(chord);
		recalcCHNames();
	}
	private void removeEndingChords() {
		List<ChordViewController> chordsToDelete = new ArrayList<>();
		for (ChordViewController chord : chordViewControllers) {
			if (chord instanceof EndChordView) {
				chordsToDelete.add(chord);
			}
		}
		for (ChordViewController chord : chordsToDelete) {
			chord.delete();
		}
	}

	void scrollChordIntoView(AnchorPane chordPane) {

		mainController.scrollCLineIntoView(mainPane);

		double viewportWidth = chordScrollPane.getViewportBounds().getWidth();
		double scrollPaneWidth = chordScrollPane.getContent().getBoundsInLocal().getWidth();
		double maxX = chordPane.getBoundsInParent().getMaxX();

		if (maxX < (viewportWidth / 2)) {
			chordScrollPane.setHvalue(0);
		} else if ((maxX >= (viewportWidth / 2)) & (maxX <= (scrollPaneWidth - viewportWidth / 2))) {
			chordScrollPane.setHvalue((maxX - (viewportWidth / 2)) / (scrollPaneWidth - viewportWidth));
		} else if (maxX >= (scrollPaneWidth - (viewportWidth / 2))) {
			chordScrollPane.setHvalue(1);
		}
	}

	private double getCursorPositionFraction(Node caller, double mouse_position) {
		return ((caller.getLocalToSceneTransform().getTx() + mouse_position
				- chordScrollPane.getLocalToSceneTransform().getTx())
				/ chordScrollPane.getWidth());
	}
	
	private void recalcCHNames() {
		int currentNumber = 0;
		for (ChordViewController chantChord : chordViewControllers) {
			if (chantChord instanceof RecitingChordView rChord) {
                rChord.setNumber(currentNumber + 1);
                rChord.setColor(MainApp.CHORD_COLORS[currentNumber]);
				currentNumber++;
			}
		}

		if (!mainController.isLoadingTone()) {
			mainController.syncCVLMapping();
		}
	}
	
	void setKeySignature(String new_key) {
		for (ChordViewController chord : chordViewControllers) {
			chord.setKeySignature(new_key);
		}
	}

	void refreshAllChordPreviews() {
		for (ChordViewController chord : chordViewControllers) {
			chord.refreshChordPreview();
		}
	}

	@FXML public void endButtonAction() {
		edited();
		removeEndingChords();
		try {
			addEndChord();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@FXML public void delete() {
		mainController.removeChantPhrase(this);
	}
	@FXML public void moveUp() {
		mainController.chantPhraseUp(this);
	}
	@FXML public void moveDown() {
		mainController.chantPhraseDown(this);
	}
	@FXML public void toggleFirstRepeated() {
		edited();
		if (!firstRepeated) {
			mainController.clearFirstRepeated();
			firstRepeated = true;
			firstRepeatedButton.setStyle("-fx-base: #90ff89");
		} else resetFRState();
		mainController.syncCVLMapping();
	}
	public void makePrime() {
		if (!nameChoice.getItems().isEmpty()) {
			nameChoice.getSelectionModel().select(1); // Works if both selections have already been loaded	
		} else {
			makePrimeLater = true;
		}
	}
	void setHasPrime(boolean has_prime) {
		hasPrime = has_prime;
	}
	boolean getHasPrime() {
		return hasPrime;
	}
	public void makeAlternate() {
		if (!nameChoice.getItems().isEmpty()) {
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
		edited();
		addRecitingChord();
	}
	@FXML private void editComment() {

		FXMLLoaderIO.loadFXMLLayoutAsync("/fxml/CommentView.fxml", loader -> {
			BorderPane rootLayout = loader.getRoot();
			CommentViewController controller = loader.getController();

			controller.setCommentText(commentString);
			controller.setTargetText(String.format(Locale.US, "Phrase: %s", getName()));

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
	@FXML private void handlePlay() {
		new Thread(() -> {

			for (ChordViewController controller : chordViewControllers) {
				controller.playMidi();

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	public String getEncodedComment() {
		return TWUtils.encodeNewLines(commentString);
	}
	public void setComment(String comment) {
		if (commentString.equals(comment)) return;

		mainController.toneEdited();
		commentString = TWUtils.decodeNewLines(comment);
		if (!comment.isEmpty()) {
			applyCommentGraphic(activeBubbleImage);
		} else {
			applyCommentGraphic(bubbleImage);
		}
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

	public void edited() {
		mainController.toneEdited();
	}

	boolean isLoadingTone() {
		return mainController.isLoadingTone();
	}

	public ChantPhrase generatePhraseModel() {
		List<Chord> chords = new ArrayList<>();
		for (ChordViewController chordController : chordViewControllers) {
			if (chordController instanceof MainChordView mc) {
				Chord currentMain = mc.generateChordModel();
				chords.add(currentMain);
				for (PrepChordView prep : mc.getPreps()) {
					Chord prepModel = prep.generateChordModel();
					chords.add(prepModel);
					currentMain.addPrep(prepModel);
				}
				for (PostChordView post : mc.getPosts()) {
					Chord postModel = post.generateChordModel();
					chords.add(postModel);
					currentMain.addPost(postModel);
				}
			}
		}
		return new ChantPhrase.ChantPhraseBuilder().name(TWUtils.shortenPhraseName(getName())).comment(getEncodedComment()).chords(chords).buildChantPhrase();
	}
}
