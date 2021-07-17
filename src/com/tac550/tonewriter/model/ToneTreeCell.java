package com.tac550.tonewriter.model;

import javafx.scene.control.TreeCell;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class ToneTreeCell extends TreeCell<File> {

    @Override public void updateItem(File item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            setText(getString());
            setGraphic(getTreeItem().getGraphic());
        }
    }

    private String getString() {
        return getItem() == null ? "" : FilenameUtils.removeExtension(getItem().getName());
    }
}
