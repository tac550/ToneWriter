package com.tac550.tonewriter.view;

import javafx.scene.image.Image;

public interface CommentableView { // TODO: This needs work
	
	Image bubbleImage = new Image(ChantChordController.class.getResourceAsStream("/media/bubble.png"));
	Image hoveredBubbleImage = new Image(ChantChordController.class.getResourceAsStream("/media/bubble-hovered.png"));
	Image activeBubbleImage = new Image(ChantChordController.class.getResourceAsStream("/media/bubble-active.png"));
	
	String getEncodedComment();
	void setComment(String comment);
	boolean hasComment();
	void applyCommentGraphic(Image image);
	
}
