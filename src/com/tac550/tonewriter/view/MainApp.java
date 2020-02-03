package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.AutoUpdater;
import com.tac550.tonewriter.io.LilyPondWriter;
import com.tac550.tonewriter.io.MidiInterface;
import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class MainApp extends Application {

	public static final String APP_NAME = "ToneWriter";
	public static final String APP_VERSION = "0.5";
	public static final Image APP_ICON = new Image(MainApp.class.getResourceAsStream("/media/AppIcon.png"));
	public static final String OS_NAME = System.getProperty("os.name").toLowerCase();

	static final boolean developerMode = "true".equalsIgnoreCase(System.getProperty("developerMode"));

	// Splash screen stuff
	private static final Effect frostEffect =
			new GaussianBlur(40);
	private static final Stage splashStage = new Stage();
	private static final ImageView splashBackground = new ImageView();
	private static final StackPane splashPane = new StackPane();

	// Preferences object and key strings.
	public static Preferences prefs;
	static final String PREFS_LILYPOND_LOCATION = "LilyPond-Bin-Location";
	public static final String PREFS_THOU_THY = "Thou-Thy-Enabled";
	public static final String PREFS_SAVE_LILYPOND_FILE = "Save-LP-File";
	static final String PREFS_PAPER_SIZE = "Paper-Size";
	static final String PREFS_DARK_MODE = "Dark-Mode-Enabled";
	static final String PREFS_HOVER_HIGHLIGHT = "Hover-Highlight-Enabled";

	// The colors that each chord group will take. The maximum number of chord groups is determined by the length of this array.
	static final Color[] CHORD_COLORS = new Color[]{Color.GREEN, Color.CORNFLOWERBLUE, Color.DARKORANGE,
			Color.DEEPSKYBLUE, Color.BURLYWOOD, Color.AQUA, Color.GOLD};
	static final Color END_CHORD_COLOR = Color.DARKMAGENTA;

	// How tall to make note buttons in the verse view.
	static final int NOTE_BUTTON_HEIGHT = 15;

	private static boolean darkModeEnabled = false;

	// LilyPond connection stuff.
	private static boolean lilyPondAvailable = false;
	private static File lilyPondDirectory;

	// Fields for main stage and controller.
	private Stage mainStage;
	private MainSceneController mainController;

	public static void main(String[] args) {

		System.out.println("Developer mode: " + (developerMode ? "enabled" : "disabled"));

		// OS-specific fixes
		if (OS_NAME.startsWith("mac")) {
			System.setProperty("prism.lcdtext", "false"); // This fixes some nasty text rendering issues on macOS 10.15
		}

		launch(args);
	}

	@Override
	public void start(Stage main_stage) {

		showSplash();

		// Set up preferences system
		prefs = Preferences.userNodeForPackage(this.getClass());

		// Initialize dark mode state
		darkModeEnabled = prefs.getBoolean(MainApp.PREFS_DARK_MODE, getSystemDarkMode());

		// Print saved LilyPond directory if any, otherwise print null.
		System.out.println("Saved LilyPond directory: " + prefs.get(PREFS_LILYPOND_LOCATION, null));

		// See if first-time setup is needed if using built-in LilyPond
		if (prefs.get(PREFS_LILYPOND_LOCATION, null) == null) {
			platformSpecificInitialization(); // First-time setup processes
		}

		// Run auto update check
		AutoUpdater.AutoUpdate(true);

		// Check for LilyPond installation - from prefs first
		lilyPondDirectory = new File(prefs.get(PREFS_LILYPOND_LOCATION, getPlatformSpecificDefaultLPDir()));
		if (new File(lilyPondDirectory.getAbsolutePath() + getPlatformSpecificLPExecutable()).exists()) {
			lilyPondAvailable = true;
			System.out.println("LilyPond Found!");
			MidiInterface.setUpMidiSystem(); // Set up the midi playback system only if lilypond is present.

		} else {
			System.out.println("LilyPond Missing!");
		}

		if (lilyPondAvailable()) {
			try {
				runLilyPondStartup(() -> {
					splashStage.close();
					loadMainStage(main_stage);
				});
			} catch (IOException e) {
				TWUtils.showAlert(AlertType.ERROR, "Error", "Failed to run LilyPond!", false);
			}
		} else {
			splashStage.close();
			loadMainStage(main_stage);
		}
	}

	@Override
	public void stop() {
		MidiInterface.closeMidiSystem();
	}

	private void loadMainStage(Stage main_stage) {
		mainStage = main_stage;
		mainStage.getIcons().add(APP_ICON);
		loadMainLayout();

		// Ensure that the process exits when the main window is closed
		mainStage.setOnCloseRequest(ev -> {
			if (!mainController.checkSave()) {
				ev.consume();
			}
		});

		// Show the stage (required for the next operation to work)
		this.mainStage.show();

		// Makes sure the stage can't be made too small.
		// The stage opens showing the scene at its pref size. This makes that initial size the minimum.
		mainStage.setMinWidth(mainStage.getWidth());
		mainStage.setMinHeight(mainStage.getHeight());

		// Start the application maximized.
		// This also mitigates an issue with UI widgets disappearing when hovered (assuming the user keeps it maximized).
		// TODO: This issue needs a more comprehensive fix.
		// https://stackoverflow.com/questions/38308591/javafx-ui-elements-hover-style-not-rendering-correctly-after-resizing-applicatio
		mainStage.setMaximized(true);

		// Launching with tone loading TODO: Implement Mac support for this
		List<String> params = getParameters().getRaw();
		if (params.size() > 0) {
			File openFile = new File(params.get(0));
			if (openFile.isFile()) mainController.handleOpenTone(new File(params.get(0)));
		}
	}

	private void showSplash() {
		splashPane.setAlignment(Pos.CENTER);
		splashPane.getChildren().setAll(createSplashContent());
		splashPane.setStyle("-fx-background-color: null");

		Scene scene = new Scene(
				splashPane,
				300, 200,
				Color.TRANSPARENT
		);

		splashStage.initStyle(StageStyle.TRANSPARENT);
		splashStage.setScene(scene);
		splashStage.getIcons().add(APP_ICON);
		splashStage.show();

		Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
		splashStage.setX((primScreenBounds.getWidth() - splashStage.getWidth()) / 2);
		splashStage.setY((primScreenBounds.getHeight() - splashStage.getHeight()) / 2);

		splashBackground.setImage(APP_ICON);
		splashBackground.setEffect(frostEffect);
	}

	private Node[] createSplashContent() {
		VBox box = new VBox();
		box.setAlignment(Pos.CENTER);

		Text name = new Text(APP_NAME);
		name.setFont(new Font(100));

		Text loading = new Text("Loading...");
		name.setFont(new Font(25));

		box.getChildren().addAll(name, loading);

		return new Node[]{splashBackground, box};
	}

	private void runLilyPondStartup(Runnable final_actions) throws IOException {
		// Create the temporary file to hold the lilypond markup
		File lilypondFile = File.createTempFile(MainApp.APP_NAME + "--", "-STARTUP.ly");
		File outputFile = new File(lilypondFile.getAbsolutePath().replace(".ly", ".pdf"));
		lilypondFile.deleteOnExit();
		outputFile.deleteOnExit();

		try {
			LilyPondWriter.exportResource("renderTemplate.ly", lilypondFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}

		LilyPondWriter.executePlatformSpecificLPRender(lilypondFile, false, () -> {
			if (!(lilypondFile.delete() && outputFile.delete())) {
				System.out.println("Warning: Could not delete temporary file(s)");
			}
			Platform.runLater(final_actions);
		});
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
			return (prefs.get(PREFS_LILYPOND_LOCATION, null) == null) ? "/lilypond"
					: "/LilyPond.app/Contents/Resources/bin/lilypond";
		} if (OS_NAME.startsWith("lin")) {
			return "/lilypond";
		} else return null;
	}

	// Returns the directory where built-in LilyPond is installed.
	private static String getPlatformSpecificDefaultLPDir() {
		if (OS_NAME.startsWith("win")) {
			return System.getenv("ProgramFiles(X86)") + "\\LilyPond\\usr\\bin";
		} if (OS_NAME.startsWith("mac")) {
			return "/opt/local/bin";
		} if (OS_NAME.startsWith("lin")) {
			return "/usr/bin";
		} else return null;
	}

	// Returns the extension for midi files produced by LilyPond on the current platform.
	public static String getPlatformSpecificMidiExtension() {
		if (OS_NAME.startsWith("win")) {
			return ".mid";
		} if (OS_NAME.startsWith("mac")) {
			return ".midi";
		} if (OS_NAME.startsWith("lin")) {
			return ".midi";
		} else return null;
	}

	public static String getPlatformSpecificRootDir() {
		if (OS_NAME.startsWith("win")) {
			System.out.println(System.getenv("SystemDrive"));
			return System.getenv("SystemDrive") + "\\";
		} if (OS_NAME.startsWith("mac")) {
			return "/";
		} if (OS_NAME.startsWith("lin")) {
			return "/";
		} else return null;
	}

	public static boolean darkModeEnabled() {
		return darkModeEnabled;
	}
	static void setDarkMode(boolean dark_mode) {
		darkModeEnabled = dark_mode;
	}

	private static boolean getSystemDarkMode() {
		if (OS_NAME.startsWith("win")) {

			// TODO: Not sure how to determine light/dark theme on Windows

			return false;
		} if (OS_NAME.startsWith("mac")) {
			try {
				// checking for exit status only.
				final Process process = Runtime.getRuntime().exec(new String[] {"defaults", "read", "-g", "AppleInterfaceStyle"});
				process.waitFor(100, TimeUnit.MILLISECONDS);
				return process.exitValue() == 0;
			} catch (IOException | InterruptedException | IllegalThreadStateException ex) {
				// IllegalThreadStateException thrown by process.exitValue() if process didn't terminate
				return false;
			}
		} else return false;
	}

	private static void platformSpecificInitialization() {
		if (OS_NAME.startsWith("win")) {
			if (!new File(getPlatformSpecificDefaultLPDir() + getPlatformSpecificLPExecutable()).exists()) {

				TWUtils.showAlert(AlertType.INFORMATION, "First Time Setup", String.format("Welcome to %s!" +
						" Please click through the following LilyPond installer to complete setup.", MainApp.APP_NAME), true);

				try {
					Process process = Runtime.getRuntime().exec(String.format("cmd /c lilypond\\%s",
							Objects.requireNonNull(new File("lilypond\\").listFiles(
									file -> !file.isHidden() && !file.getName().startsWith(".")))[0].getName()));
					process.waitFor();
					if (process.exitValue() != 0) {
						Platform.exit();
					}
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		} if (OS_NAME.startsWith("mac")) {
			
			if (new File("/opt/local/share/lilypond").exists()) {
				// Not sure why I have to do the following line. If I use the relative path Java thinks it doesn't exist
				File localLPVerDir = new File(new File("lilypond/opt/local/share/lilypond").getAbsolutePath());
				String LPVersion = Objects.requireNonNull(localLPVerDir.listFiles(file -> !file.isHidden()))[0].getName();
				for (File file : Objects.requireNonNull(new File("/opt/local/share/lilypond").listFiles())) {
					if (file.getName().equals(LPVersion)) return;
				}
			}

			TWUtils.showAlert(AlertType.INFORMATION, "First Time Setup",
					String.format("Welcome to %s! Please enter your password when prompted to complete setup.", MainApp.APP_NAME), true);

			try {
				String[] command = {
						"osascript",
						"-e",
						String.format("do shell script \"cp -Rf %s /opt/\" with administrator privileges",
								new File("lilypond/opt/local").getAbsolutePath()) };
				Process process = Runtime.getRuntime().exec(command);
				process.waitFor();
				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(process.getErrorStream()));
				String line;
				while ((line = bufferedReader.readLine()) != null)
					System.out.println(line);

				System.out.println("SETUP EXIT CODE: " + process.exitValue());
				if (process.exitValue() != 0) {
					Platform.exit();
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}

		} if (OS_NAME.startsWith("lin")) {
			if (!new File(getPlatformSpecificDefaultLPDir() + getPlatformSpecificLPExecutable()).exists()) {

				TWUtils.showAlert(AlertType.INFORMATION, "First Time Setup", String.format("Welcome to %s! Please either install \"lilypond\" from your " +
						"distribution's repositories or locate your copy from the Options menu.", MainApp.APP_NAME), true);

			}
		}
	}

}
