package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.AutoUpdater;
import com.tac550.tonewriter.io.FXMLLoaderIO;
import com.tac550.tonewriter.io.LilyPondInterface;
import com.tac550.tonewriter.io.MidiInterface;
import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.*;

import javax.swing.filechooser.FileSystemView;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainApp extends Application {

	public static final String APP_NAME = "ToneWriter";
	public static final String APP_VERSION = "1.4.0";
	public static final Image APP_ICON = new Image(Objects.requireNonNull(MainApp.class.getResourceAsStream("/media/AppIcon.png")));
	public static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ROOT);

	static final boolean developerMode = "true".equalsIgnoreCase(System.getenv("developerMode"));
	public static final File BUILT_IN_TONE_DIR = new File(System.getProperty("user.dir") + File.separator + "Built-in Tones");

	// Splash screen stuff
	private static final Effect gaussianBlur =
			new GaussianBlur(40);
	private static Stage splashStage;
	private static final ImageView splashBackground = new ImageView();
	private static final StackPane splashPane = new StackPane();

	// Preferences object and key strings.
	public static Preferences prefs;
	static final String PREFS_LILYPOND_LOCATION = "LilyPond-Bin-Location";
	public static final String PREFS_THOU_THY = "Thou-Thy-Enabled";
	public static final String PREFS_SAVE_LILYPOND_FILE = "Save-LP-File";
	public static final String PREFS_SAVE_MIDI_FILE = "Save-MIDI-File";
	static final String PREFS_PAPER_SIZE = "Paper-Size";
	static final String PREFS_DARK_MODE = "Dark-Mode-Enabled";
	static final String PREFS_HOVER_HIGHLIGHT = "Hover-Highlight-Enabled";
	static final String PREFS_CHECK_UPDATE_STARTUP = "Check-Update-Appstart";
	static final String PREFS_AUTO_OPEN_EXPORT = "Auto-Open-Completed-Export";
	static final String PREFS_MAXIMIZED_LAST_EXIT = "Maximized-On-Last-Exit";

	// UI Stuff
	// The colors that each chord group will take. The maximum number of chord groups is determined by the length of this array.
	static final Color[] CHORD_COLORS = new Color[] {Color.FORESTGREEN, Color.CORNFLOWERBLUE, Color.DARKORANGE,
			Color.DEEPSKYBLUE, Color.BURLYWOOD, Color.AQUA, Color.GOLD};
	static final Color END_CHORD_COLOR = Color.DARKMAGENTA;

	private static boolean darkModeEnabled = false;

	private static String requiredLPVersion;
	private static boolean lilyPondAvailable = false;
	private static File lilyPondDirectory;

	private static boolean placementUnrestricted = false;

	// Fields for main stage, controller, and main tab pane.
	private static Stage mainStage;
	private static TopSceneController topSceneController;

	public static void main(String[] args) {
		System.out.println("Developer mode: " + (developerMode ? "enabled" : "disabled"));

		TWUtils.cleanUpAllTempFiles();

		TWUtils.establishFileLock();

		// OS-specific fixes
		if (OS_NAME.startsWith("mac"))
			System.setProperty("prism.lcdtext", "false"); // Fix text rendering issues on macOS 10.15

		launch(args);
	}

	@Override
	public void start(Stage main_stage) {
		showSplash();

		// Set up preferences system
		prefs = Preferences.userNodeForPackage(this.getClass());

		// Initialize dark mode state
		darkModeEnabled = prefs.getBoolean(PREFS_DARK_MODE, getSystemDarkMode());

		// Initialize LilyPond
		refreshLilyPondLocation();

		if (!OS_NAME.startsWith("lin") && !lilyPondAvailable()) {
			if (OS_NAME.startsWith("mac") && !lilyPondDirectory.getAbsolutePath().equals(getPlatformSpecificDefaultLPDir()))
				// If the macOS LilyPond default setup is no good, this is probably a permissions issue
				attemptFixLilyPondMacAccess();

			// Final availability determination after initialization attempt
			refreshLilyPondLocation();
		}

		if (lilyPondAvailable()) {
			MidiInterface.setUpMidiSystem();

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
		TWUtils.cleanUpAllTempFiles();

		// Fix the application not closing correctly if the user played midi.
		MidiInterface.closeMidiSystem();
	}

	private void loadMainStage(Stage main_stage) {
		mainStage = main_stage;
		mainStage.setTitle(APP_NAME);
		mainStage.getIcons().add(APP_ICON);
		loadMainLayout();

		// Ensure that the process exits when the main window is closed
		mainStage.setOnCloseRequest(ev -> topSceneController.requestClose(ev));

		// Show the stage (required for the next operation to work)
		mainStage.show();

		// Run auto update check
		if (prefs.getBoolean(PREFS_CHECK_UPDATE_STARTUP, true)) AutoUpdater.updateCheck(mainStage, true);

		// Makes sure the stage can't be made too small.
		// The stage opens showing the scene at its pref size. This makes that initial size the minimum.
		mainStage.setMinWidth(mainStage.getWidth());
		mainStage.setMinHeight(mainStage.getHeight());

		// Start the application maximized if it was maximized at last exit.
		if (prefs.getBoolean(PREFS_MAXIMIZED_LAST_EXIT, false))
			mainStage.setMaximized(true);

		attemptProjectRecovery();
	}

	private void showSplash() {
		splashStage = new Stage();

		splashPane.setAlignment(Pos.CENTER);
		splashPane.getChildren().setAll(createSplashContent());
		splashPane.setStyle("-fx-background-color: null");
		splashPane.setCursor(Cursor.WAIT);

		Scene scene = new Scene(
				splashPane,
				300, 200,
				Color.TRANSPARENT
		);

		splashStage.initStyle(StageStyle.TRANSPARENT);
		splashStage.setScene(scene);
		splashStage.getIcons().add(APP_ICON);

		splashBackground.setImage(APP_ICON);
		splashBackground.setEffect(gaussianBlur);

		splashStage.show();

		Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
		splashStage.setX((primScreenBounds.getWidth() - splashStage.getWidth()) / 2);
		splashStage.setY((primScreenBounds.getHeight() - splashStage.getHeight()) / 2);
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
		File lilypondFile = TWUtils.createTWTempFile("", "-STARTUP.ly");

		try {
			TWUtils.exportFSResource("/lilypond/chordTemplate.ly", lilypondFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

		LilyPondInterface.executeLilyPondRender(lilypondFile, true,
				() -> Platform.runLater(final_actions));
	}

	private void loadMainLayout() {
		try {
			// Load layout from fxml file
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("/fxml/TopScene.fxml"));
			BorderPane rootPane = loader.load();
			topSceneController = loader.getController();

			// Fix bug where alt+tabbing away from and back to the app leaves menu mnemonics activated.
			// https://bugs.openjdk.java.net/browse/JDK-8238731
			AtomicReference<Boolean> altLastPressed = new AtomicReference<>(false);
			AtomicReference<Boolean> mnemonicsActive = new AtomicReference<>(false);
			mainStage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
				if (event.getCode() == KeyCode.ALT) {
					altLastPressed.set(true);
					mnemonicsActive.set(!mnemonicsActive.get());
				} else altLastPressed.set(false);
			});
			mainStage.focusedProperty().addListener(current -> {
				if (mainStage.isFocused() && mnemonicsActive.get() && altLastPressed.get())
					rootPane.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ALT, false, false, true, false));
			});

			// Launching with tone/project loading TODO: Implement Mac support for this
			List<String> params = getParameters().getRaw();
			File fileToOpen = null;
			if (!params.isEmpty()) {
				File openFile = new File(params.getFirst());
				if (openFile.isFile()) fileToOpen = openFile;
			}
			topSceneController.performSetup(mainStage, fileToOpen);

			Scene mainScene = new Scene(rootPane);
			mainStage.setScene(mainScene);

			mainScene.getAccelerators().put(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN),
					() -> topSceneController.closeSelectedTab());

			// Listeners for prep/post chord placement restriction override
			mainScene.setOnKeyPressed(event -> {
				if (event.getCode() == KeyCode.SHIFT) {
					placementUnrestricted = true;
					topSceneController.getSelectedTabScene().refreshSyllableActivation();
				}
			});
			mainScene.setOnKeyReleased(event -> {
				if (event.getCode() == KeyCode.SHIFT) {
					placementUnrestricted = false;
					topSceneController.getSelectedTabScene().refreshSyllableActivation();
				}
			});

		} catch (IOException e) {
			e.printStackTrace();
			TWUtils.showAlert(AlertType.ERROR, "Error",
					"Error loading main UI layout. Exiting application.", true);
			Platform.exit();
		}

		initializeDarkMode(darkModeEnabled);
	}

	public static boolean lilyPondAvailable() {
		return lilyPondAvailable;
	}

	public static String getLilyPondPath() {
		return lilyPondDirectory.getAbsolutePath();
	}

	// Returns the path from the LilyPond directory to the executable itself.
	public static String getPlatformSpecificLPExecutable() {
		if (OS_NAME.startsWith("win"))
			return "\\lilypond.exe";
		else return "/lilypond";
	}

	// Returns the directory where built-in LilyPond is installed.
	private static String getPlatformSpecificDefaultLPDir() {
		if (OS_NAME.startsWith("win"))
			return "lilypond\\bin";
		else return "lilypond/bin";
	}

	// Returns the extension for midi files produced by LilyPond on the current platform.
	public static String getPlatformSpecificMidiExtension() {
		if (OS_NAME.startsWith("win"))
			return ".mid";
		else return ".midi";
	}

	private static String getPlatformSpecificRootDir() {
		if (OS_NAME.startsWith("win"))
			return System.getenv("SystemDrive") + "\\";
		else return "/";
	}

	public static String getPlatformSpecificAppDataDir() throws IOException {
		String subDirString = File.separator + APP_NAME;

		if (OS_NAME.startsWith("win")) {
			File appDataDir = new File(System.getenv("APPDATA"));
			if (!appDataDir.isAbsolute())
				return null;
			return appDataDir.getCanonicalPath() + subDirString;
		} if (OS_NAME.startsWith("mac")) {
			return System.getProperty("user.home") + "/Library/Preferences" + subDirString;
		} if (OS_NAME.startsWith("lin")) {
			return System.getProperty("user.home") + "/.config" + subDirString;
		} else return null;
	}

	public static boolean isDarkModeEnabled() {
		return darkModeEnabled;
	}
	public static void setDarkModeEnabled(boolean dark_mode) {
		darkModeEnabled = dark_mode;

		if (dark_mode) setUserAgentStylesheet("/styles/modena-dark/modena-dark.css");
		else setUserAgentStylesheet(Application.STYLESHEET_MODENA);

		topSceneController.propagateDarkModeSetting();

	}
	private static void initializeDarkMode(boolean dark_mode) {
		darkModeEnabled = dark_mode;

		if (dark_mode) setUserAgentStylesheet("/styles/modena-dark/modena-dark.css");
		else setUserAgentStylesheet(Application.STYLESHEET_MODENA);

		topSceneController.propagateDarkModeSetting();
	}

	private static boolean getSystemDarkMode() {
		if (OS_NAME.startsWith("win")) {
			try {
				Process process = new ProcessBuilder(
						"reg query HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
						"/v", "AppsUseLightTheme").start();

				AtomicReference<Character> r = new AtomicReference<>();
				Thread reader = new Thread(() -> {
					try {
						int c;
						while ((c = process.getInputStream().read()) != -1) {
							if (!Character.isWhitespace((char) c)) r.set((char) c);
						}
					} catch (IOException ignored) {}
				});

				reader.start();
				process.waitFor();
				reader.join();
				return Integer.parseInt(String.valueOf(r.get())) == 0;

			} catch (IOException | InterruptedException | NumberFormatException e) {
				return false;
			}
		} if (OS_NAME.startsWith("mac")) {
			try {
				// checking for exit status only.
				Process process = Runtime.getRuntime().exec(new String[] {"defaults", "read", "-g", "AppleInterfaceStyle"});
				process.waitFor(100, TimeUnit.MILLISECONDS);
				return process.exitValue() == 0;
			} catch (IOException | InterruptedException | IllegalThreadStateException ex) {
				// IllegalThreadStateException thrown by process.exitValue() if process didn't terminate
				return false;
			}
		} else return false;
	}

	static File getPlatformSpecificInitialChooserDir() {
		return new File(MainApp.developerMode ? System.getProperty("user.home") + File.separator + "Downloads"
				: MainApp.OS_NAME.startsWith("mac") ? System.getProperty("user.home") + File.separator + "Documents"
				: FileSystemView.getFileSystemView().getDefaultDirectory().getPath());
	}

	private static void attemptFixLilyPondMacAccess() {
		Optional<ButtonType> result = TWUtils.showAlert(AlertType.INFORMATION, "Password",
				"Please enter your password once more to complete installation.", true);
		if (result.isPresent() && result.get().equals(ButtonType.CANCEL))
			return;

		File scriptFile;
		try {
			scriptFile = TWUtils.createTWTempFile("fixLilyPondScript",
					"version" + MainApp.APP_VERSION + ".sh");
		} catch (IOException e) {
			e.printStackTrace();
			Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
					"I/O error occurred while generating temp files!", true));
			return;
		}

		try {
			TWUtils.exportFSResource("/updater/tryfix-LilyPond-macOS.sh", scriptFile);
		} catch (Exception e) {
			e.printStackTrace();
			Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
					"Error occurred while exporting script!", true));
			return;
		}

		try {
			String appLocation = new File(System.getProperty("user.dir")).getParentFile().getParentFile().getAbsolutePath();
			if (TWUtils.versionCompare(System.getProperty("os.version"), "10.15.0") < 2)
				appLocation = "/System/Volumes/Data" + appLocation;

			Runtime.getRuntime().exec(new String[] {"chmod", "+x", scriptFile.getAbsolutePath()}).waitFor();
			String[] cmdlist = new String[] {"osascript", "-e", String.format("do shell script \"%s\" with administrator privileges", String.join(" ",
					new String[] {scriptFile.getAbsolutePath(), appLocation}))};
			Runtime.getRuntime().exec(cmdlist).waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			Platform.runLater(() -> TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
					"Failed to run script!", true));
		}
	}

	static void setLilyPondDir(Stage owner, boolean startup) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Select the folder containing the LilyPond executable");
		directoryChooser.setInitialDirectory(new File(prefs.get(PREFS_LILYPOND_LOCATION, getPlatformSpecificRootDir())));
		File selectedDirectory = directoryChooser.showDialog(owner);
		if (selectedDirectory == null) return;

		String previousLocation = prefs.get(PREFS_LILYPOND_LOCATION, null);
		prefs.put(PREFS_LILYPOND_LOCATION, selectedDirectory.getAbsolutePath());
		if (new File(selectedDirectory.getAbsolutePath() + File.separator + getPlatformSpecificLPExecutable()).exists()) {
			refreshLilyPondLocation();
			checkLilyPondVersionCompatibility();
			if (!startup) topSceneController.refreshAllChordPreviews();
		} else {
			if (previousLocation == null) {
				prefs.remove(PREFS_LILYPOND_LOCATION);
			} else {
				prefs.put(PREFS_LILYPOND_LOCATION, previousLocation);
			}

			TWUtils.showAlert(Alert.AlertType.ERROR, "Error",
					"That folder does not contain a valid LilyPond executable.", true, owner);

			setLilyPondDir(owner, startup);
		}
	}

	static void resetLilyPondDir() {
		MainApp.prefs.remove(MainApp.PREFS_LILYPOND_LOCATION);
		refreshLilyPondLocation();
		topSceneController.refreshAllChordPreviews();
	}

	private static void refreshLilyPondLocation() {
		System.out.println("Saved LilyPond directory: " + prefs.get(PREFS_LILYPOND_LOCATION, null));

		lilyPondDirectory = new File(prefs.get(PREFS_LILYPOND_LOCATION, getPlatformSpecificDefaultLPDir()));

		lilyPondAvailable = isLilyPondInstalled() && checkLilyPondVersionCompatibility();
	}

	private static boolean isLilyPondInstalled() {
		return new File(getLilyPondPath() + getPlatformSpecificLPExecutable()).exists();
	}

	private static boolean checkLilyPondVersionCompatibility() {
		if (TWUtils.versionCompare(getInstalledLPVersion(), getRequiredLPVersion()) != 2) {
			return true;
		} else {
			TWUtils.showAlert(AlertType.ERROR, "Error", "LilyPond version must be " +
					getRequiredLPVersion() + " or above. This one is " + getInstalledLPVersion(), true);
			return false;
		}
	}

	private static String getInstalledLPVersion() {
		if (!isLilyPondInstalled()) return null;

		String installedVersion = null;

		try {
			ProcessBuilder prb = new ProcessBuilder(getLilyPondPath()
					+ MainApp.getPlatformSpecificLPExecutable(), "--version");
			Process pr = prb.start();

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String s;
			while ((s = stdInput.readLine()) != null) {
				Matcher matcher = Pattern.compile("\\d+(\\.\\d+)+").matcher(s);
				if (matcher.find()) {
					installedVersion = matcher.group();
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Installed LilyPond version is " + installedVersion);
		return installedVersion;
	}

	private static String getRequiredLPVersion() {
		// Return cached required version, if any.
		if (requiredLPVersion != null)
			return requiredLPVersion;

		try {
			String versionLine = new BufferedReader(new InputStreamReader(
					Objects.requireNonNull(LilyPondInterface.class.getResourceAsStream("/lilypond/exportTemplate.ly")))).readLine();

			Matcher matcher = Pattern.compile("\\d+(\\.\\d+)+").matcher(versionLine);

			if (matcher.find()) {
				String ver = matcher.group();
				System.out.println("Required LilyPond version is " + ver);
				requiredLPVersion = ver;
				return requiredLPVersion;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void attemptProjectRecovery() {
		FXMLLoaderIO.loadFXMLLayoutAsync("/fxml/ProjectRecoveryView.fxml", loader -> {
			BorderPane rootLayout = loader.getRoot();
			ProjectRecoveryViewController controller = loader.getController();

			controller.setTopController(topSceneController);

			Platform.runLater(() -> {
				Stage recoveryStage = new Stage();
				recoveryStage.setTitle("Project Recovery");
				recoveryStage.initModality(Modality.APPLICATION_MODAL);
				recoveryStage.getIcons().add(APP_ICON);
				recoveryStage.setScene(new Scene(rootLayout));
				if (controller.hasAutosavedProject()) {
					recoveryStage.show();
					recoveryStage.setMinWidth(recoveryStage.getWidth());
					recoveryStage.setMinHeight(recoveryStage.getHeight());
				}
			});
		});
	}

	public static boolean isChordPlacementUnrestricted() {
		return placementUnrestricted;
	}
}
