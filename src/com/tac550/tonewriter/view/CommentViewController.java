package com.tac550.tonewriter.view;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class CommentViewController {

	CommentableView parentController;
	
	@FXML Text targetText;
	@FXML TextArea commentTextArea;
	
	public void setParentView(CommentableView view) {
		parentController = view;
	}
	
	public void setTargetText(String text) {
		targetText.setText(text);
	}
	public void setCommentText(String comment) {
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
