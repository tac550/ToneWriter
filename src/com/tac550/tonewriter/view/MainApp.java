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
import javafx.scene.control.ButtonBar;
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
import org.apache.commons.io.FilenameUtils;

import javax.swing.filechooser.FileSystemView;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MainApp extends Application {

	public static final String APP_NAME = "ToneWriter";
	public static final String APP_VERSION = "1.1.0";
	public static final Image APP_ICON = new Image(Objects.requireNonNull(MainApp.class.getResourceAsStream("/media/AppIcon.png")));
	public static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ROOT);

	static final boolean developerMode = "true".equalsIgnoreCase(System.getProperty("developerMode"));
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
	static final String PREFS_PAPER_SIZE = "Paper-Size";
	static final String PREFS_DARK_MODE = "Dark-Mode-Enabled";
	static final String PREFS_HOVER_HIGHLIGHT = "Hover-Highlight-Enabled";
	static final String PREFS_CHECK_UPDATE_APPSTARTUP = "Check-Update-Appstart";
	static final String PREFS_AUTO_OPEN_EXPORT = "Auto-Open-Completed-Export";

	// UI Stuff
	// The colors that each chord group will take. The maximum number of chord groups is determined by the length of this array.
	static final Color[] CHORD_COLORS = new Color[] {Color.FORESTGREEN, Color.CORNFLOWERBLUE, Color.DARKORANGE,
			Color.DEEPSKYBLUE, Color.BURLYWOOD, Color.AQUA, Color.GOLD};
	static final Color END_CHORD_COLOR = Color.DARKMAGENTA;

	private static boolean darkModeEnabled = false;

	// LilyPond stuff.
	private static final File bundledLPDir = new File(new File("lilypond" + File.separator).getAbsolutePath());
	// Not sure why I have to do the above line. If I use the relative path Java thinks it doesn't exist

	private static String requiredLPVersion;
	private static boolean lilyPondAvailable = false;
	private static File lilyPondDirectory;

	// Fields for main stage, controller, and main tab pane.
	private static Stage mainStage;
	private static TopSceneController topSceneController;

	public static void main(String[] args) {

		System.out.println("Developer mode: " + (developerMode ? "enabled" : "disabled"));

		if (noOtherAppInstanceRunning())
			TWUtils.cleanUpTempFiles();

		establishFileLock();

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
		darkModeEnabled = prefs.getBoolean(PREFS_DARK_MODE, getSystemDarkMode());

		// Initialize LilyPond

		refreshLilyPondLocation();
		// If the Windows LilyPond installation is no good, do initialization
		if (!lilyPondAvailable() && OS_NAME.startsWith("win")) {
			promptWinLilyPondInstall();

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

	/*
	 * This fixes the application not closing correctly if the user played midi.
	 */
	@Override
	public void stop() {
		if (noOtherAppInstanceRunning())
			TWUtils.cleanUpTempFiles();

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
		if (prefs.getBoolean(PREFS_CHECK_UPDATE_APPSTARTUP, true)) AutoUpdater.updateCheck(mainStage, true);

		// Makes sure the stage can't be made too small.
		// The stage opens showing the scene at its pref size. This makes that initial size the minimum.
		mainStage.setMinWidth(mainStage.getWidth());
		mainStage.setMinHeight(mainStage.getHeight());

		// Start the application maximized.
		// This also mitigates an issue with UI widgets disappearing when hovered (assuming the user keeps it maximized).
		// TODO: This issue needs a more comprehensive fix.
		// https://stackoverflow.com/questions/38308591/javafx-ui-elements-hover-style-not-rendering-correctly-after-resizing-applicatio
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
			TWUtils.exportFSResource("chordTemplate.ly", lilypondFile);
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
			loader.setLocation(MainApp.class.getResource("TopScene.fxml"));
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
			if (params.size() > 0) {
				File openFile = new File(params.get(0));
				if (openFile.isFile()) fileToOpen = openFile;
			}
			topSceneController.performSetup(mainStage, fileToOpen);

			Scene mainScene = new Scene(rootPane);
			mainStage.setScene(mainScene);

			mainScene.getAccelerators().put(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN),
					() -> topSceneController.closeSelectedTab());

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
		if (OS_NAME.startsWith("mac"))
			return "/LilyPond.app/Contents/Resources/bin/lilypond";
		if (OS_NAME.startsWith("lin"))
			return "/lilypond";
		else return null;
	}

	// Returns the directory where built-in LilyPond is installed.
	private static String getPlatformSpecificDefaultLPDir() {
		if (OS_NAME.startsWith("win"))
			return System.getenv("ProgramFiles(X86)") + "\\LilyPond\\usr\\bin";
		if (OS_NAME.startsWith("mac"))
			return "lilypond";
		if (OS_NAME.startsWith("lin"))
			return "/usr/bin";
		else return null;
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
				Process process = Runtime.getRuntime().exec(
						"reg query HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize /v AppsUseLightTheme");

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
				final Process process = Runtime.getRuntime().exec(new String[] {"defaults", "read", "-g", "AppleInterfaceStyle"});
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

	private static void promptWinLilyPondInstall() {

		// First, prompt the user asking how to proceed
		// (either to locate a compatible LilyPond installation, continue anyway, or install the bundled one).

		ButtonType locateInstall = new ButtonType("Locate LilyPond Installation");
		ButtonType updateLilyPond = new ButtonType(isLilyPondInstalled() ? "Update" : "Install", ButtonBar.ButtonData.OK_DONE);

		Optional<ButtonType> result = TWUtils.showAlert(AlertType.INFORMATION, "First Time Setup",
				String.format("Welcome to %s! LilyPond must be %s in order to continue " +
								(OS_NAME.startsWith("lin") ? "(likely available in your distro's repositories)." :
										"(Will install to default location)."), APP_NAME,
						isLilyPondInstalled() ? "updated to version " + getRequiredLPVersion() : "installed"), true, null,
				OS_NAME.startsWith("lin") ? new ButtonType[] {ButtonType.CANCEL, locateInstall} :
						new ButtonType[] {updateLilyPond, ButtonType.CANCEL, locateInstall}, locateInstall);

		if (result.isPresent()) {
			if (result.get() == ButtonType.CANCEL) return; // continue without change
			else if (result.get() == locateInstall) { // Set directory and continue
				setLilyPondDir(splashStage, true);
				if (!isLilyPondVersionCompatible()) { // Try again if version incompatible
					promptWinLilyPondInstall();
				}
				return;
			}
		} else return; // No result, probably pressed close button; continue without change

		// Reset to default directory for after installation
		resetLilyPondDir(true);

		try {
			// Uninstall old version if present
			String uninstallerLocation = new File(Objects.requireNonNull(getPlatformSpecificDefaultLPDir()))
					.getParentFile().getParentFile().getAbsolutePath() + "\\uninstall.exe";

			if (new File(uninstallerLocation).exists()) {
				Optional<ButtonType> uninsResult = TWUtils.showAlert(AlertType.CONFIRMATION, "Uninstall",
						"Previous LilyPond installation will be removed.", true);
				if (uninsResult.isPresent() && uninsResult.get() == ButtonType.OK) {
					Process uninsProc = Runtime.getRuntime().exec(String.format("cmd /c \"%s\"", uninstallerLocation));
					uninsProc.waitFor();

					AtomicBoolean done = new AtomicBoolean(false);
					int loops = 0;
					while (!done.get() && loops < 3) {
						Thread.sleep(1000);
						String line;
						Process p = Runtime.getRuntime().exec
								(System.getenv("windir") + "\\system32\\" + "tasklist.exe");
						try (BufferedReader input =
								new BufferedReader(new InputStreamReader(p.getInputStream()))) {
							while ((line = input.readLine()) != null) {
								if (line.startsWith("Au_.exe ")) {
									loops = 0;
									break;
								}
							}
							loops++;
						}
					}

				} else return;
			}

			// Install bundled version
			Process process = Runtime.getRuntime().exec(String.format("cmd /c \"lilypond\\%s\"",
					Objects.requireNonNull(bundledLPDir.listFiles(
							file -> !file.isHidden() && !file.getName().startsWith(".")))[0].getName()));
			process.waitFor();
			if (process.exitValue() != 0) {
				TWUtils.showAlert(AlertType.ERROR, "Error", "LilyPond installation failed!", true);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	static void setLilyPondDir(Stage owner, boolean startup) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Select the folder containing the LilyPond executable");
		directoryChooser.setInitialDirectory(new File(prefs.get(PREFS_LILYPOND_LOCATION, getPlatformSpecificRootDir())));
		File savingDirectory = directoryChooser.showDialog(owner);
		if (savingDirectory == null) return;

		String previousLocation = prefs.get(PREFS_LILYPOND_LOCATION, null);
		prefs.put(PREFS_LILYPOND_LOCATION, savingDirectory.getAbsolutePath());
		if (new File(savingDirectory.getAbsolutePath() + File.separator + getPlatformSpecificLPExecutable()).exists()) {
			refreshLilyPondLocation();
			if (!isLilyPondVersionCompatible())
				TWUtils.showAlert(AlertType.ERROR, "Error", "LilyPond version must be " +
						getRequiredLPVersion() + " or above. This one is " + getInstalledLPVersion(), true);
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

	static void resetLilyPondDir(boolean startup) {
		MainApp.prefs.remove(MainApp.PREFS_LILYPOND_LOCATION);
		refreshLilyPondLocation();
		if (!startup) topSceneController.refreshAllChordPreviews();
	}

	private static void refreshLilyPondLocation() {
		System.out.println("Saved LilyPond directory: " + prefs.get(PREFS_LILYPOND_LOCATION, null));

		lilyPondDirectory = new File(prefs.get(PREFS_LILYPOND_LOCATION, getPlatformSpecificDefaultLPDir()));

		lilyPondAvailable = isLilyPondInstalled() && isLilyPondVersionCompatible();
	}

	private static boolean isLilyPondInstalled() {
		// First check that LilyPond is available
		return new File(getLilyPondPath() + getPlatformSpecificLPExecutable()).exists();
	}

	private static boolean isLilyPondVersionCompatible() {
		return TWUtils.versionCompare(getInstalledLPVersion(), getRequiredLPVersion()) != 2;
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
		// Cache the required version so we don't keep checking the file
		if (requiredLPVersion != null) {
			return requiredLPVersion;
		}

		try {
			String versionLine = new BufferedReader(new InputStreamReader(
					Objects.requireNonNull(LilyPondInterface.class.getResourceAsStream("exportTemplate.ly")))).readLine();

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
		FXMLLoaderIO.loadFXMLLayoutAsync("projectRecoveryView.fxml", loader -> {
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

	private static void establishFileLock() {
		String pid = String.valueOf(ProcessHandle.current().pid());

		try {
			Files.write(TWUtils.createTWTempFile("", "FileLock").toPath(), List.of(pid));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private static boolean noOtherAppInstanceRunning() {
		boolean instanceRunning = false;

		Set<Long> livePIDs = ProcessHandle.allProcesses()
				.filter(ProcessHandle::isAlive)
				.map(ProcessHandle::pid)
				.collect(Collectors.toSet());

		try {
			Set<Path> fileLocks = Files.list(Path.of(System.getProperty("java.io.tmpdir")))
					.filter(Files::isRegularFile)
					.filter(path -> FilenameUtils.removeExtension(path.getFileName().toString()).endsWith("-FileLock"))
					.collect(Collectors.toSet());

			for (Path path : fileLocks) {
				long pid = Long.parseLong(Files.readString(path).strip());
				if (!livePIDs.contains(pid))
					Files.delete(path);
				else if (pid != ProcessHandle.current().pid())
					instanceRunning = true;
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return !instanceRunning;
	}

}
