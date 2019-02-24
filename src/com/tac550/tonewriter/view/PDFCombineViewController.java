package com.tac550.tonewriter.view;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class PDFCombineViewController {

	private File defaultDirectory;
	
	@FXML Text dragInstructions;
	@FXML private VBox fileBox;
	@FXML private TextField fileNameBox;
	
	private static final String TAB_DRAG_KEY = "pdfPane";
	private ObjectProperty<GridPane> draggingPane = new SimpleObjectProperty<GridPane>();
	
	@FXML private void initialize() {
		dragInstructions.setVisible(false);
	}
	
	@FXML private void handleAddFolder() {
		DirectoryChooser dirChooser = new DirectoryChooser();
		dirChooser.setInitialDirectory(defaultDirectory);
		dirChooser.setTitle("Choose Folder");
		
		File pdfFolder = dirChooser.showDialog(fileBox.getScene().getWindow());
		
		if (pdfFolder != null) {
			List<File> allFiles = Arrays.asList(pdfFolder.listFiles());
			for (File file : allFiles) {
				if (file.getName().endsWith(".pdf")) {
					addItem(file);
				}
			}
			
			// Update default directory.
			defaultDirectory = pdfFolder;
		}
	}
	
	@FXML private void handleAddFiles() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(defaultDirectory);
		fileChooser.setTitle("Choose PDF File(s)");
		FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf");
		fileChooser.getExtensionFilters().add(extFilter);
		
		List<File> pdfFiles = fileChooser.showOpenMultipleDialog(fileBox.getScene().getWindow());
		
		if (pdfFiles != null) {
			for (File pdfFile : pdfFiles) {
				addItem(pdfFile);
			}
			
			// Update default directory.
			if (pdfFiles.size() > 0) {
				defaultDirectory = pdfFiles.get(0).getParentFile();
			}
			
		}
	}
	
	@FXML private void handleCancel() {
		closeStage();
	}
	
	@FXML private void handleCombine() {
		
		if (fileBox.getChildren().isEmpty()) {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("Warning");
			alert.setHeaderText("No PDF files selected!");
			alert.showAndWait();
			return;
		} if (fileNameBox.getText().isEmpty()) {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("Warning");
			alert.setHeaderText("Please enter a filename!");
			alert.showAndWait();
			return;
		}
		
		DirectoryChooser DirChooser = new DirectoryChooser();
		DirChooser.setInitialDirectory(defaultDirectory);
		DirChooser.setTitle("Choose Where to Save Combined PDF");
		
		File outFolder = DirChooser.showDialog(fileBox.getScene().getWindow());
		
		String destFileName = outFolder.getAbsolutePath() + File.separator + fileNameBox.getText().replace(".pdf", "") + ".pdf";
		
		PDFMergerUtility pdfMerger = new PDFMergerUtility();
		pdfMerger.setDestinationFileName(destFileName);
		
		for (Node node : fileBox.getChildren()) {
			GridPane pane = (GridPane) node;
			
			File pdfFile = new File(((Label)pane.getChildren().get(0)).getText());
			
			try {
				pdfMerger.addSource(pdfFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Error");
				alert.setHeaderText("Error reading file \"" + ((Label)pane.getChildren().get(0)).getText() + "\"");
				alert.showAndWait();
				return;
			}
		}
		
		try {
			pdfMerger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
		} catch (IOException e) {
			e.printStackTrace();
			e.printStackTrace();
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Error combining files!");
			alert.showAndWait();
		}
		
		try {
			Desktop.getDesktop().open(new File(destFileName));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		closeStage();
	}
	
	private void addItem(File file) {
		dragInstructions.setVisible(true);
		
		GridPane itemPane = new GridPane();
		Label itemText = new Label(file.getAbsolutePath());
		itemText.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
		Button removeButton = new Button("X");
		removeButton.setStyle("-fx-text-fill: #ff0000; -fx-font-weight: bold");
		
		// Remove button behavior
		removeButton.setOnAction((ae) -> {
			fileBox.getChildren().remove(itemPane);
		});
		
		// Dragging behavior
		itemPane.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                final Dragboard dragboard = event.getDragboard();
                if (dragboard.hasString()
                        && TAB_DRAG_KEY.equals(dragboard.getString())
                        && draggingPane.get() != null) {
                    event.acceptTransferModes(TransferMode.MOVE);
                    event.consume();
                }
            }
        });
		itemPane.setOnDragDropped(new EventHandler<DragEvent>() {
            public void handle(final DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    VBox parent = (VBox) itemPane.getParent();
                    Object source = event.getGestureSource();
                    int sourceIndex = parent.getChildren().indexOf(source);
                    int targetIndex = parent.getChildren().indexOf(itemPane);
                    List<Node> nodes = new ArrayList<Node>(parent.getChildren());
                    if (sourceIndex < targetIndex) {
                        Collections.rotate(
                                nodes.subList(sourceIndex, targetIndex + 1), -1);
                    } else {
                        Collections.rotate(
                                nodes.subList(targetIndex, sourceIndex + 1), 1);
                    }
                    parent.getChildren().clear();
                    parent.getChildren().addAll(nodes);
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            }
        });
		itemPane.setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                Dragboard dragboard = itemPane.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent clipboardContent = new ClipboardContent();
                clipboardContent.putString(TAB_DRAG_KEY);
                dragboard.setContent(clipboardContent);
                draggingPane.set(itemPane);
                event.consume();
            }
        });
		
		ColumnConstraints column1 = new ColumnConstraints(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
		column1.setHgrow(Priority.ALWAYS);
		itemPane.addColumn(0, itemText);
		itemPane.addColumn(1, removeButton);
		itemPane.getColumnConstraints().add(column1);
		
		fileBox.getChildren().add(itemPane);
	}
	
	private void closeStage() {
		Stage stage = (Stage) fileBox.getScene().getWindow();
		stage.close();
	}
	
	public void setDefaultDirectory(File directory) {
		defaultDirectory = directory;
	}
	
}
