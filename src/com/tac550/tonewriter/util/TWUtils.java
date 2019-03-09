package com.tac550.tonewriter.util;

import java.awt.Toolkit;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.scene.paint.Color;

public class TWUtils {
	
	// Color
	
	// For converting Color objects to strings used to style UI elements.
    public static String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }
    
    // Display
    public static Float getRetinaScaleFactor() {
		Object obj = Toolkit.getDefaultToolkit().getDesktopProperty("apple.awt.contentScaleFactor");
		if (obj != null) {
			if (obj instanceof Float) return (Float) obj;
		}
		
		return null;
	}
	public static boolean hasRetinaDisplay() {
		Float fRetinaFactor = getRetinaScaleFactor();
		if (fRetinaFactor != null) {
			if (fRetinaFactor > 0) {
				int nScale = fRetinaFactor.intValue();
				return (nScale == 2); // 1 indicates a regular mac display, 2 is for retina
			}
		}
		
		return false;
	}
	public static float getUIScaleFactor() {
		float fResolutionFactor = ((float) Toolkit.getDefaultToolkit().getScreenResolution() / 96f);
		if (hasRetinaDisplay()) {
			fResolutionFactor = fResolutionFactor * getRetinaScaleFactor().floatValue();
		}
		
		return fResolutionFactor;
	}
    
	// Strings
	public static int countOccurrences(String string, String single_character) {
		return string.length() - string.replace(single_character, "").length();
	}
	
	// I/O
	public static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		
		return new String(encoded, encoding);
	}
	
}