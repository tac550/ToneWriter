package com.tac550.tonewriter.util;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Automates some Desktop boilerplate to avoid application hangs on Linux
 */
public class DesktopInterface {

    private static boolean supported = false;
    private static boolean checkedSupported = false;

    private static boolean checkSupported() {
        if (!checkedSupported) {
            supported = Desktop.isDesktopSupported();
            if (!supported)
                TWUtils.showError("Failed to show folder or website because desktop interactions are not supported on this system.", true);
            checkedSupported = true;
        }

        return supported;
    }

    public static void openFile(File file) {
        if (file.exists()) {
            if (checkSupported())
                new Thread(() -> {
                    try {
                        Desktop.getDesktop().open(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
        } else {
            TWUtils.showError("File does not exist.", true);
        }
    }
    public static void highlightFile(File file) {
        if (file.exists()) {
            if (checkSupported())
                new Thread(() -> {
                    try { // The below call is seemingly not implemented on Windows 10 as of OpenJDK 17
                        Desktop.getDesktop().browseFileDirectory(file);
                    } catch (UnsupportedOperationException e) {
                        openFile(file.getParentFile());
                    }
                }).start();
        } else {
            openFile(file.getParentFile());
        }
    }
    public static void browseURI(String uri) {
        if (checkSupported())
            new Thread(() -> {
                try {
                    Desktop.getDesktop().browse(new URI(uri));
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }
            }).start();
    }
}
