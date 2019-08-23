package com.tac550.tonewriter.view;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class CommentViewController {

	private CommentableView parentController;
	
	@FXML Text targetText;
	@FXML TextArea commentTextArea;
	
	void setParentView(CommentableView view) {
		parentController = view;

		commentTextArea.getScene().setOnKeyPressed((ke) -> {
			if (ke.getCode() == KeyCode.ESCAPE) {
				handleCancel();
			}
		});
	}
	
	void setTargetText(String text) {
		targetText.setText(text);
	}
	void setCommentText(String comment) {
		commentTextArea.setText(comment);
	}
	
	@FXML private void initialize() {

	}
	
	@FXML private void handleOK() {
		parentController.setComment(commentTextArea.getText());
		
		closeStage();
	}
	
	@FXML private void handleCancel() {
		closeStage();
	}
	
	private void closeStage() {
		Stage stage = (Stage) commentTextArea.getScene().getWindow();
		stage.close();
	}
	
}
