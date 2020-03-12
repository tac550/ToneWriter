package com.tac550.tonewriter.view;

import com.tac550.tonewriter.io.AutoUpdater;
import com.tac550.tonewriter.io.LilyPondWriter;
import com.tac550.tonewriter.io.MidiInterface;
import com.tac550.tonewriter.util.TWUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

public class MainApp extends Application {

	public static final String APP_NAME = "ToneWriter";
	public static final String APP_VERSION = "0.6";
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
	static final String PREFS_CHECK_UPDATE_APPSTARTUP = "Check-Update-Appstart";

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

	// Fields for main stage, controller, and main tab pane.
	private static Stage mainStage;
	private static final HashMap<Tab, MainSceneController> tabControllerMap = new HashMap<>();
	private static TabPane tabPane;

	private static final ArrayList<MainSceneController> resetStatusIfCloseCanceled = new ArrayList<>();

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
		mainStage.setTitle(APP_NAME);
		mainStage.getIcons().add(APP_ICON);
		loadMainLayout();

		// Ensure that the process exits when the main window is closed
		mainStage.setOnCloseRequest(ev -> {
			for (Tab tab : tabPane.getTabs()) {
				MainSceneController controller = tabControllerMap.get(tab);
				if (!controller.isToneEdited()) continue;

				if (!controller.checkSave()) {

					for (MainSceneController controllerToReset : resetStatusIfCloseCanceled) {
						controllerToReset.toneEdited();
					}

					ev.consume();
					break;
				} else { // "Save" or "Don't Save" selected. Avoids repeat prompts.
					refreshToneInstances(controller.getToneFile(), controller, true);
				}
			}
		});

		// Show the stage (required for the next operation to work)
		mainStage.show();

		// Run auto update check TODO: Move this call further down?
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

		// Launching with tone loading TODO: Implement Mac support for this
		List<String> params = getParameters().getRaw();
		if (params.size() > 0) {
			File openFile = new File(params.get(0));
			if (openFile.isFile()) tabControllerMap.get(tabPane.getTabs().get(0)).handleOpenTone(new File(params.get(0)), true);
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

		splashBackground.setImage(APP_ICON);
		splashBackground.setEffect(frostEffect);

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
		File outputFile = new File(lilypondFile.getAbsolutePath().replace(".ly", ".pdf"));
		lilypondFile.deleteOnExit();
		outputFile.deleteOnExit();

		try {
			TWUtils.exportIOResource("renderTemplate.ly", lilypondFile);
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

		AnchorPane rootPane = new AnchorPane();
		tabPane = new TabPane();
		tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);
		tabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
			if (tabPane.getTabs().size() == 1) {
				tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
			} else {
				tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
			}
		});
		tabPane.getSelectionModel().selectedItemProperty().addListener(observable -> {
			MainSceneController controller = tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem());
			controller.updateStageTitle();
		});

		Button addTabButton = new Button();
		ImageView addImageView = new ImageView(new Image(getClass().getResource("/media/sign-add.png").toExternalForm(),
				16, 16, false, true));
		addTabButton.setGraphic(addImageView);
		addTabButton.setStyle("-fx-background-color: transparent");
		addTabButton.setOnAction(event -> addTab());
		addTabButton.setTooltip(new Tooltip(String.format(Locale.US,"Add item (%s)",
				OS_NAME.startsWith("mac") ? "\u2318T" : "Ctrl + T")));

		rootPane.getChildren().addAll(tabPane, addTabButton);
		AnchorPane.setTopAnchor(addTabButton, 3.0);
		AnchorPane.setRightAnchor(addTabButton, 15.0);
		AnchorPane.setTopAnchor(tabPane, 0d);
		AnchorPane.setRightAnchor(tabPane, 0d);
		AnchorPane.setLeftAnchor(tabPane, 0d);
		AnchorPane.setBottomAnchor(tabPane, 0d);

		Scene scene = new Scene(rootPane);
		// Allows for keyboard shortcuts to be directed to the correct (currently-selected) tab.
		// TODO: Unnecessary in JDK 14?
		scene.addEventFilter(KeyEvent.KEY_PRESSED, e ->
				tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()).handleShortcut(e));
		scene.getAccelerators().put(new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN),
				MainApp::addTab);
		scene.getAccelerators().put(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN),
				() -> closeTab(tabPane.getSelectionModel().getSelectedItem()));

		mainStage.setScene(scene);

		addTab();

		setDarkModeEnabled(darkModeEnabled);
	}

	private static void addTab() {
		try {
			// Load layout from fxml file
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("MainScene.fxml"));
			BorderPane mainLayout = loader.load();
			MainSceneController mainController = loader.getController();
			mainController.setStage(mainStage);

			Tab tab = new Tab();

			tab.textProperty().bind(mainController.getTitleTextProperty());
			mainController.setTitleText("Item " + (tabPane.getTabs().size() + 1));

			tabControllerMap.put(tab, mainController);

			AnchorPane anchorPane = new AnchorPane();
			anchorPane.getChildren().add(mainLayout);
			AnchorPane.setTopAnchor(mainLayout, 0d);
			AnchorPane.setBottomAnchor(mainLayout, 0d);
			AnchorPane.setLeftAnchor(mainLayout, 0d);
			AnchorPane.setRightAnchor(mainLayout, 0d);

			tab.setContent(anchorPane);
			tabPane.getTabs().add(tab);

			tab.setOnCloseRequest(event -> {
				// This is necessary to avoid a bug where tabs may be left unable to respond to UI events.
				tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);
				// TODO: Need to fix menu consistency issues (dark mode, etc), and closing save check issues ("first change sticks" if Save selected)

				Optional<ButtonType> result = TWUtils.showAlert(AlertType.CONFIRMATION, "Deleting Item",
						"Are you sure you want to remove \"" + tab.getText() + "\" from your project?", true, mainStage);
				if (result.isPresent() && result.get() == ButtonType.CANCEL) {
					event.consume();
				} else {
					tabControllerMap.remove(tab);
				}

				// This is necessary to avoid a bug where tabs may be left unable to respond to UI events.
				Platform.runLater(() -> tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER));
			});

			tabPane.getSelectionModel().selectLast();
			tabPane.getSelectionModel().getSelectedItem().getContent().requestFocus();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void closeTab(Tab tab) {
		EventHandler<Event> handler = tab.getOnCloseRequest();
		if (handler != null) {
			Event event = new Event(null, null, null);
			handler.handle(event);
			if (event.isConsumed()) return;
		}

		tabPane.getTabs().remove(tab);
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

	public static boolean isDarkModeEnabled() {
		return darkModeEnabled;
	}
	public static void setDarkModeEnabled(boolean dark_mode) {
		darkModeEnabled = dark_mode;

		if (dark_mode) MainApp.setUserAgentStylesheet("/styles/modena-dark/modena-dark.css");
		else MainApp.setUserAgentStylesheet(Application.STYLESHEET_MODENA);

		LilyPondWriter.clearAllCachedChordPreviews();

		for (Tab tab : tabPane.getTabs()) {
			MainSceneController controller = tabControllerMap.get(tab);
			controller.refreshVerseTextStyle();
			controller.refreshAllChordPreviews();
		}

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

	// Notifies other tabs that a tone file was saved to synchronize changes or avoid repeating save requests
	protected static void refreshToneInstances(File toneFile, MainSceneController caller, boolean shutdown) {
		for (Tab tab : tabPane.getTabs()) {
			MainSceneController controller = tabControllerMap.get(tab);
			if (controller != caller && controller.getToneFile() != null && controller.getToneFile().equals(toneFile)) {
				if (shutdown) {
					if (controller.isToneEdited()) {
						controller.resetToneEditedStatus();
						resetStatusIfCloseCanceled.add(controller);
					}
				} else {
					controller.handleOpenTone(toneFile, true);
				}
			}
		}
	}

	public static boolean isActiveTab(MainSceneController controller) {
		return tabControllerMap.get(tabPane.getSelectionModel().getSelectedItem()) == controller;
	}

}
