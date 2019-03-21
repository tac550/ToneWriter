package com.tac550.tonewriter.view;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MainApp extends Application {

	public static final String APP_NAME = "ToneWriter";
	public static final String APP_VERSION = "0.3";
	public static final String OS_NAME = System.getProperty("os.name").toLowerCase();
	
	public static final boolean developerMode = "true".equalsIgnoreCase(System.getProperty("developerMode"));
	
	// Preferences object and key strings.
	public static Preferences prefs;
	public static final String PREFS_LILYPOND_LOCATION = "LilyPond-Bin-Location";
	public static final String PREFS_THOU_THY_ENABLED = "Thou-Thy-Enabled";
	
	// The colors that each chord group will take. The maximum number of chord groups is determined by the length of this array.
	public static final Color[] CHORDCOLORS = new Color[] {Color.DARKGREEN, Color.BROWN, Color.BLUEVIOLET, Color.DEEPSKYBLUE, Color.CADETBLUE, Color.BURLYWOOD, Color.GOLD};
	
	// How tall to make note buttons in the verse view.
	public static final int NOTEBUTTONHEIGHT = 15;

	// LilyPond connection stuff.
	private static boolean lilyPondAvailable = false;
	private static File lilyPondDirectory;
	
	// Fields for main stage and controller.
	private Stage mainStage;
	private MainSceneController mainController;

	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage main_stage) {
		// Set up preferences system
		prefs = Preferences.userNodeForPackage(this.getClass());

		// Print saved LilyPond directory if any, otherwise print null.
		System.out.println("Saved LilyPond directory: " + prefs.get(PREFS_LILYPOND_LOCATION, null));
		
		// Check for LilyPond installation - from prefs first
		lilyPondDirectory = new File(prefs.get(PREFS_LILYPOND_LOCATION, getPlatformSpecificDefaultLPDir()));
		if (new File(lilyPondDirectory.getAbsolutePath() + getPlatformSpecificLPExecutable()).exists()) {
			lilyPondAvailable = true;
			System.out.println("LilyPond Found!");
		} else {
			System.out.println("LilyPond Missing!");
		}
		
		mainStage = main_stage;
		mainStage.setTitle(APP_NAME);
		mainStage.getIcons().add(new Image(getClass().getResourceAsStream("/media/AppIcon.png")));
		loadMainLayout();
		
		// Ensure that the process exits when the main window is closed
		mainStage.setOnCloseRequest((ev) -> Platform.exit());
		
		// Start the application maximized.
		// This also mitigates an issue with UI widgets disappearing when hovered (assuming the user keeps it maximized).
		// TODO: That issue needs a more comprehensive fix.
		// https://stackoverflow.com/questions/38308591/javafx-ui-elements-hover-style-not-rendering-correctly-after-resizing-applicatio
		mainStage.setMaximized(true);
		
		this.mainStage.show();
		
	}
	
	@Override
	public void stop() {
		mainController.checkSave();
	}
	
	private void loadMainLayout() {
		try {
			// Load layout from fxml file
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("MainScene.fxml"));
			BorderPane rootLayout = loader.load();
			mainController = loader.getController();
			
			// Apply the layout as the new scene
			Scene scene = new Scene(rootLayout);
			mainStage.setScene(scene);
			
			// Attempts to make sure the scene can't be made too small. TODO: It's still possible to make it a bit too small.
			mainStage.setMinWidth(rootLayout.getPrefWidth());
			mainStage.setMinHeight(rootLayout.getPrefHeight());
			
			// Workaround for Mac bug where resizing is impossible after exiting fullscreen
			// https://stackoverflow.com/questions/47476328/how-to-make-main-javafx-window-still-resizable-coming-back-from-full-screen-mode
			mainStage.fullScreenProperty().addListener((v,o,n) -> {
		        if (!mainStage.isFullScreen()) {
		        	mainStage.setResizable(false);
		        	mainStage.setResizable(true);
		        }
		    });
			
			mainController.setStage(mainStage);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean lilyPondAvailable() {
		return lilyPondAvailable;
	}
	public static String getLilyPondPath() {
		return lilyPondDirectory.getAbsolutePath();
	}
	// Returns the path from the LilyPond directory to the executable itself.
	public static String getPlatformSpecificLPExecutable() {
		if (OS_NAME.startsWith("win")) {
			return "\\lilypond.exe";
		} if (OS_NAME.startsWith("mac")) {
			return "/LilyPond.app/Contents/Resources/bin/lilypond";
		} if (OS_NAME.startsWith("lin")) {
			return "/lilypond";
		} else return null;
	}
	// Returns the default directory where LilyPond is installed.
	private static String getPlatformSpecificDefaultLPDir() {
		if (OS_NAME.startsWith("win")) {
			return System.getenv("ProgramFiles(X86)") + "\\LilyPond\\usr\\bin";
		} if (OS_NAME.startsWith("mac")) {
			return "/Applications";
		} if (OS_NAME.startsWith("lin")) {
			return "/usr/bin";
		} else return null;
	}
	// Returns the extension for midi files produced by LilyPond on the current platform.
	static String getPlatformSpecificMidiExtension() {
		if (OS_NAME.startsWith("win")) {
			return ".mid";
		} if (OS_NAME.startsWith("mac")) {
			return ".midi";
		} if (OS_NAME.startsWith("lin")) {
			return ".midi";
		} else return null;
	}
	
}
