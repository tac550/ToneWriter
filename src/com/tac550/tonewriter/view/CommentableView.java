package com.tac550.tonewriter.view;

import javafx.scene.image.Image;

import java.util.Objects;

public interface CommentableView {
	
	Image bubbleImage = new Image(Objects.requireNonNull(ChordViewController.class.getResourceAsStream("/media/bubble.png")));
	Image hoveredBubbleImage = new Image(Objects.requireNonNull(ChordViewController.class.getResourceAsStream("/media/bubble-hovered.png")));
	Image activeBubbleImage = new Image(Objects.requireNonNull(ChordViewController.class.getResourceAsStream("/media/bubble-active.png")));
	
	String getEncodedComment();
	void setComment(String comment);

}
