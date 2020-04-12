package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.FXMLLoaderIO;
import com.tac550.tonewriter.util.TWUtils;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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

public class ChantLineViewController implements CommentableView {

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
	private final ArrayList<ChantChordController> chantChordControllers = new ArrayList<>();
	
	private boolean makePrimeLater = false;
	private boolean makeAlternateLater = false;

	// Fields related to drag reordering of chords
	private static final String CHORD_DRAG_KEY = "ToneWriter chord: ";
	private final ObjectProperty<AnchorPane> draggingChord = new SimpleObjectProperty<>();
	private final ObjectProperty<ChantChordController> draggingController = new SimpleObjectProperty<>();

	// Fields for automatic drag-scrolling
	AnimationTimer autoScroller;
	private double prevTime = -1;
	private boolean mouseReversed = false;
	private final double scrollThreshold = 0.1;
	private final double scrollSpeed = 10;
	private double cursorLocation;
	private double previousMouseVector = 0;
	private double mouseVector;

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
	
	void setName(char new_letter, boolean previous_is_prime, int next_alternate) {
		int previousSelection = nameChoice.getSelectionModel().getSelectedIndex();
		if (new_letter == 'A' || previous_is_prime) {
			nameChoice.setItems(FXCollections.observableArrayList(
				    String.valueOf(new_letter))
				);
			
			setFirstRepeatedAvailable(false);
			upButton.setDisable(true);
		} else {
			nameChoice.setItems(FXCollections.observableArrayList(
				    String.valueOf(new_letter), --new_letter + "'",
					new_letter + " alternate " + next_alternate)
				);
			
			setFirstRepeatedAvailable(true);
			upButton.setDisable(false);
		}
		downButton.setDisable(false);

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
			if (chord.getColor() == MainApp.END_CHORD_COLOR) {
				result++;
			}
		}
		return result;
	}
	
	GridPane getMainPane() {
		return mainPane;
	}

	private ChantChordController addChord(int position) throws IOException {
		// Load layout from fxml file
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(MainApp.class.getResource("chantChordView.fxml"));

		AnchorPane chordPane = loader.load();
		ChantChordController controller = loader.getController();

		chantChordControllers.add(position, controller);
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
				screen = Screen.getScreensForRectangle(event.getX(), event.getY(), 0, 0).get(0);
			} catch (IndexOutOfBoundsException e) {
				screen = Screen.getScreens().get(0);
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
						double previousVectorAbs = Math.abs(previousMouseVector);
						double mouseVectorAbs = Math.abs(mouseVector);

						// Only proceed if cursor is within scrolling threshold
						if (cursorLocation < scrollThreshold || cursorLocation > 1 - scrollThreshold) {
							if (mouseVectorAbs > previousVectorAbs) {
								chordScrollPane.setHvalue(chordScrollPane.getHvalue() + ((scrollSpeed * mouseVector * timeDelta)
										* (chordScrollPane.getWidth() / (2 * chordBox.getWidth()))));
								if (mouseReversed) mouseReversed = false;
							} else if (mouseVectorAbs == previousVectorAbs && !mouseReversed) {
								chordScrollPane.setHvalue(chordScrollPane.getHvalue() + ((scrollSpeed * mouseVector * timeDelta)
										* (chordScrollPane.getWidth() / (2 * chordBox.getWidth()))));
							} else if (mouseVectorAbs < previousVectorAbs) {
								mouseReversed = true;
							}
						}
						previousMouseVector = mouseVector;
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
			mouseVector = cursorLocation - 0.5;

			final Dragboard dragboard = event.getDragboard();
			if (dragboard.hasString()
					&& dragboard.getString().startsWith(CHORD_DRAG_KEY)) {

				int sourceIndex = chordBox.getChildren().indexOf(draggingChord.get());
				int hoveredIndex = chordBox.getChildren().indexOf(chordPane);
				ChantChordController hoveredChord = chantChordControllers.get(hoveredIndex);
				ChantChordController otherChord = null;
				if (sourceIndex < hoveredIndex && hoveredIndex < chantChordControllers.size() - 1) {
					otherChord = chantChordControllers.get(hoveredIndex + 1);
				} else if (sourceIndex > hoveredIndex && hoveredIndex > 0) {
					otherChord = chantChordControllers.get(hoveredIndex - 1);
				} else if (sourceIndex == hoveredIndex) { // Don't accept moves with equal source and target
					event.consume();
					return;
				}

				// Move validation
				if (draggingController.get().getType() == 1) { // If chord being dragged is a reciting chord...
					// Disallow any move that would place it (and its preps/posts) in the middle of any other reciting chord's preps/posts.
					if (otherChord != null) {
						if (otherChord.getAssociatedRecitingChord() == hoveredChord.getAssociatedRecitingChord()
								|| otherChord.getAssociatedRecitingChord() == draggingController.get()
								|| hoveredChord.getAssociatedRecitingChord() == draggingController.get()) {
							event.consume();
							return;
						}
					} else {
						if (hoveredChord.getType() == -3) {
							event.consume();
							return;
						}
					}
				} else if (draggingController.get().getType() == -3) { // If chord being dragged is an End chord...
					// Disallow all moves.
					event.consume();
					return;
				} else { // If chord being dragged is any other type (prep or post)...
					// Disallow any move outside its own group of preps or posts.
					if (hoveredChord.getType() != draggingController.get().getType()
							|| hoveredChord.getAssociatedRecitingChord() != draggingController.get().getAssociatedRecitingChord()) {
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

			ChantChordController exitedChord = chantChordControllers.get(chordBox.getChildren().indexOf(chordPane));
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
				List<ChantChordController> controllers = new ArrayList<>(chantChordControllers);
				if (sourceIndex != targetIndex) {

					int distance = 1 + draggingController.get().numPreps() + draggingController.get().numPosts();

					if (sourceIndex < targetIndex) {
						int fromIndex = sourceIndex - draggingController.get().numPreps();
						int toIndex = targetIndex + 1;
						Collections.rotate(
								nodes.subList(fromIndex, toIndex), -distance);
						Collections.rotate(
								controllers.subList(fromIndex, toIndex), -distance);
					} else {
						int toIndex = sourceIndex + draggingController.get().numPosts() + 1;
						Collections.rotate(
								nodes.subList(targetIndex, toIndex), distance);
						Collections.rotate(
								controllers.subList(targetIndex, toIndex), distance);
					}

					chordBox.getChildren().clear();
					chordBox.getChildren().addAll(nodes);
					chantChordControllers.clear();
					chantChordControllers.addAll(controllers);

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

		controller.setChantLineController(this);
		controller.setKeySignature(mainController.getCurrentKey());

		return controller;
	}
	public ChantChordController addRecitingChord() throws IOException {
		if (countMainChords() >= MainApp.CHORD_COLORS.length) { // Cap number of main (reciting) chords to available colors
			return null;
		}
		// Add to end of lists but before any ending chords
		int position = chantChordControllers.size() - countEndChords();

		ChantChordController controller = addChord(position);

		recalcCHNames();

		return controller;
	}
	ChantChordController addPrepChord(ChantChordController caller_chord, Color chord_color) throws IOException {

		int before_reciting_chord = chantChordControllers.indexOf(caller_chord);

		ChantChordController controller = addChord(before_reciting_chord);

		controller.setColor(chord_color);
		controller.makePrep();
		recalcCHNames();
		
		return controller;
	}
	ChantChordController addPostChord(ChantChordController caller_chord, Color chord_color) throws IOException {

		int after_reciting_chord = chantChordControllers.indexOf(caller_chord) + 1;

		ChantChordController controller = addChord(after_reciting_chord);

		controller.setColor(chord_color);
		controller.makePost();
		recalcCHNames();
		
		return controller;
	}
	public ChantChordController addEndChord() throws IOException {

		int last_position = chantChordControllers.size();

		ChantChordController controller = addChord(last_position);

		controller.setColor(MainApp.END_CHORD_COLOR);
		controller.makeFinalChord();
		recalcCHNames();
		
		return controller;
	}
	void removeChord(ChantChordController chord) {
		chordBox.getChildren().remove(chord.getMainPane());
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
		for (ChantChordController chantChord : chantChordControllers) {
			if (!(chantChord.getType() < 0)) { // If not special
				chantChord.setNumber(currentNumber + 1);
				chantChord.setColor(MainApp.CHORD_COLORS[currentNumber]);
				currentNumber++;
			}
		}
	}
	
	void setKeySignature(String new_key) {
		for (ChantChordController chord : chantChordControllers) {
			chord.setKeySignature(new_key);
		}
	}

	void refreshAllChordPreviews() {
		for (ChantChordController chord : chantChordControllers) {
			chord.refreshChordPreview();
		}
	}

	@FXML public void setCadenceAction() {
		edited();
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
		if (nameChoice.getItems().size() > 0) {
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
		edited();
		addRecitingChord();
	}
	@FXML private void editComment() {

		FXMLLoaderIO.loadFXMLLayoutAsync("commentView.fxml", loader -> {
			BorderPane rootLayout = loader.getRoot();
			CommentViewController controller = loader.getController();

			controller.setCommentText(commentString);
			controller.setTargetText(String.format(Locale.US, "Line %s", getName()));

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
		Task<Void> midiTask = new Task<>() {
			@Override
			protected Void call() throws Exception {

				for (ChantChordController controller : chantChordControllers) {
					controller.playMidi();

					Thread.sleep(1000);
				}
				return null;
			}
		};
		
		Thread th = new Thread(midiTask);
		th.start();
	}
	
	public String getComment() {
		return commentString.replaceAll("\n", "/n");
	}
	public void setComment(String comment) {
		if (commentString.equals(comment)) return;

		mainController.toneEdited();
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

	@Override
	public boolean equals(Object obj) {

		if (obj == this) return true;
		if (!(obj instanceof ChantLineViewController)) return false;
		ChantLineViewController cc = (ChantLineViewController) obj;

		if (cc.getChords().size() != this.getChords().size()) return false;

		for (int i = 0; i < this.getChords().size(); i++) {
			if (!(cc.getChords().get(i).getFields().equals(this.getChords().get(i).getFields()) &&
					cc.getChords().get(i).getName().equals(this.getChords().get(i).getName()) &&
					cc.getChords().get(i).getColor().equals(this.getChords().get(i).getColor())))
				return false;
		}

		return true;

	}

	void edited() {
		mainController.toneEdited();
	}

}
