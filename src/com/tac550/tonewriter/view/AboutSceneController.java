package com.tac550.tonewriter.view;

import com.tac550.tonewriter.util.TWUtils;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;

public class AboutSceneController {

	@FXML private Text appNameText;
	@FXML private TextArea licenseTextArea;
	@FXML private Text appVersionText;
	
	@FXML private ImageView appIconView;
	
	@FXML private void initialize() {
		appNameText.setText(MainApp.APP_NAME);
		
		try {
			licenseTextArea.setText(TWUtils.readFile("licenses/third-party-licenses.txt", Charset.defaultCharset()));
		} catch (IOException e) {
			licenseTextArea.setText("Error reading file \"licenses/third-party-licenses.txt\"");
			e.printStackTrace();
		}
		
		appVersionText.setText(appVersionText.getText().replaceAll("%VERSION%", MainApp.APP_VERSION));
		
		appIconView.setImage(MainApp.APP_ICON);
		
	}
	
	@FXML private void handleClose() {
		Stage stage = (Stage) appNameText.getScene().getWindow();
		stage.close();
	}
	
	@FXML private void handleGitHubLink() {
		try {
			if (MainApp.OS_NAME.startsWith("lin")) {
				Runtime.getRuntime().exec("xdg-open https://github.com/tac550/ToneWriter/");
			} else {
				Desktop.getDesktop().browse(new URL("https://github.com/tac550/ToneWriter/").toURI());
			}
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
}
