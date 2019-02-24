package com.tac550.tonewriter.view;

import javafx.scene.image.Image;

public interface CommentableView {
	
	static final Image bubbleImage = new Image(ChantChordController.class.getResourceAsStream("/media/bubble.png"));
	static final Image hoveredBubbleImage = new Image(ChantChordController.class.getResourceAsStream("/media/bubble-hovered.png"));
	static final Image activeBubbleImage = new Image(ChantChordController.class.getResourceAsStream("/media/bubble-active.png"));
	Image commentButtonState = bubbleImage;
	
	String getComment();
	void setComment(String comment);
	boolean hasComment();
	void applyCommentGraphic(Image image);
	
}
