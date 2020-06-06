package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.QuickVerseIO;
import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class QuickVerseController {

	@FXML private BorderPane mainPane;

	@FXML private Button youThouSwitch;
	
	@FXML private TextField filterInput;
	@FXML private TextField resultField;

	private TextField targetField;
	
	private ListView<String> verseList;
	
	private final ObservableList<String> verses = FXCollections.observableArrayList();
	
	@FXML private void initialize() {

		// Set text for youThouSwitch button.
		if (MainApp.prefs.getBoolean(MainApp.PREFS_THOU_THY, false)) {
			youThouSwitch.setText("Switch to You/Your");
		}
		
		// Filter field setup
	    FilteredList<String> filteredData = new FilteredList<>(verses, s -> true);
	    filterInput.textProperty().addListener(obs -> {
	        String filter = filterInput.getText();
	        if (filter == null || filter.isEmpty()) {
	            filteredData.setPredicate(s -> true);
	        } else {
	            filteredData.setPredicate(s -> s.toLowerCase().contains(filter.toLowerCase()));
	        }

	        // Automatically place top filter result in result field
	        if (filteredData.size() > 0) {
	        	resultField.setText(filteredData.get(0));
	        }

	    });
	    
	    // Verse list setup
	    verseList = new ListView<>(filteredData);
	    verseList.getSelectionModel().selectedItemProperty().addListener((ov, oldVal, newVal) ->
			    resultField.setText(newVal));
	    verseList.setOnMouseClicked((me) -> {
	    	if (me.getButton().equals(MouseButton.PRIMARY) && me.getClickCount() == 2) {
	    		handleOK();
	    	}
	    });
	    
	    // Verse list cell factory (for deletion context menus)
	    verseList.setCellFactory(listV -> {

            ListCell<String> cell = new ListCell<>();

            ContextMenu contextMenu = new ContextMenu();

            MenuItem deleteItem = new MenuItem();
            deleteItem.textProperty().bind(Bindings.format("Remove \"%s\"", cell.itemProperty()));
            deleteItem.setOnAction(event -> {
            	try {
                	// Only remove the verse if it was a custom one (QuickVerseIO.removeCustomVerse returns true).
					if (QuickVerseIO.removeCustomVerse(cell.itemProperty().get())) {
						verses.remove(cell.itemProperty().get());	
					} else {
						TWUtils.showAlert(AlertType.INFORMATION, "Deletion",
								"Can't delete this verse because it's built-in.", true, getStage());
					}
				} catch (IOException e) {
					e.printStackTrace();
					TWUtils.showAlert(AlertType.ERROR, "Error", "IO Error while deleting verse!",
							true, getStage());
				}
            });
            contextMenu.getItems().add(deleteItem);

            cell.textProperty().bind(cell.itemProperty());

            cell.emptyProperty().addListener((obs, wasEmpty, isNowEmpty) -> {
                if (isNowEmpty) {
                    cell.setContextMenu(null);
                } else {
                    cell.setContextMenu(contextMenu);
                }
            });
            return cell;
        });
	    
	    // Add elements to the UI
	    mainPane.setCenter(verseList);
	    
	    // Set up keyboard events
	    EventHandler<KeyEvent> keyHandler = ke -> {
			if (ke.getCode() == KeyCode.ESCAPE) {
				handleCancel();
			}

			if (ke.getCode() == KeyCode.ENTER) {
				handleOK();
			}

			if (ke.getCode() == KeyCode.UP && verseList.getSelectionModel().isSelected(0)) {
				filterInput.requestFocus();
			}
		};
	    
		// Set escape, enter, and up key behavior for scene and list view
		mainPane.setOnKeyPressed(keyHandler);
		verseList.setOnKeyPressed(keyHandler);
		// Set down arrow key behavior for filter field (focuses list view)
		filterInput.setOnKeyPressed((ke) -> {
			if (ke.getCode() == KeyCode.DOWN) {
				verseList.requestFocus();
				verseList.getSelectionModel().select(0);
				verseList.scrollTo(0);
			}
		});

		refreshVerses();
	}
	void setTargetField(TextField target) {
		targetField = target;
	}
	void focusFilterField() {
		filterInput.requestFocus();
	}
	
	@FXML private void handleSwitchYouThou() {
		if (MainApp.prefs.getBoolean(MainApp.PREFS_THOU_THY, false)) {
			youThouSwitch.setText("Switch to Thou/Thy");
			MainApp.prefs.putBoolean(MainApp.PREFS_THOU_THY, false);
		} else {
			youThouSwitch.setText("Switch to You/Your");
			MainApp.prefs.putBoolean(MainApp.PREFS_THOU_THY, true);
		}

		filterInput.requestFocus();
		
		refreshVerses();
	}
	
	@FXML private void handleAddToList() {
		try {
			if (!verses.contains(resultField.getText())) {
				QuickVerseIO.addCustomVerse(resultField.getText());

				verses.add(resultField.getText());
			} else {
				TWUtils.showAlert(AlertType.ERROR, "Add failed", "That verse is already in the list!",
						true, getStage());
			}
		} catch (IOException e) {
			e.printStackTrace();
			TWUtils.showAlert(AlertType.ERROR, "Error", "IO Error saving verse!", true, getStage());
		}
	}
	
	@FXML private void handleOK() {
		if (!resultField.getText().isEmpty()) {
			targetField.setText(resultField.getText());
			Platform.runLater(() -> {
				targetField.requestFocus();
				targetField.positionCaret(targetField.getText().length());
			});
		}

		closeStage();
	}
	
	@FXML private void handleCancel() {
		resultField.clear();
		closeStage();
	}
	
	private void refreshVerses() {
		verses.clear();
		
		// Built-in verses
		try {
			for (String item : QuickVerseIO.getBuiltinVerses()) {
				if (!(item.startsWith("-") || item.startsWith("^"))) {
					verses.add(item);
				}
			}
		} catch (IOException e) {
			verses.add("ERROR READING INTERNAL FILE");
			e.printStackTrace();
		}
		
		// Custom verses
		try {
			verses.addAll(QuickVerseIO.getCustomVerses());
		} catch (IOException e) {
			verses.add("ERROR READING EXTERNAL FILE");
			e.printStackTrace();
		}
		
	}
	
	private void closeStage() {
		getStage().close();
	}

	private Stage getStage() {
		return (Stage) mainPane.getScene().getWindow();
	}
	
}
