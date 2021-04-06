package com.tac550.tonewriter.util;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Automates some Desktop boilerplate to avoid application hangs on Linux
 */
public class DesktopInterface {

    private static boolean supported = false;
    private static boolean checkedSupported = false;

    private static boolean checkSuppported() {
        if (!checkedSupported) {
            supported = Desktop.isDesktopSupported();
            if (!supported)
                TWUtils.showError("Desktop interactions not supported; the action cannot be completed.", true);
            checkedSupported = true;
        }

        return supported;
    }

    public static void openFile(File file) {
        if (checkSuppported())
                new Thread(() -> {
                    try {
                        Desktop.getDesktop().open(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
    }
    public static void browseURI(String uri) {
        if (checkSuppported())
            new Thread(() -> {
                try {
                    Desktop.getDesktop().browse(new URI(uri));
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }
            }).start();
    }
}
