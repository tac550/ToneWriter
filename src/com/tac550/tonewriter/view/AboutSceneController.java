package com.tac550.tonewriter.view;

import java.io.IOException;
import java.nio.charset.Charset;

import com.tac550.tonewriter.util.TWUtils;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

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
		
		appIconView.setImage(new Image(getClass().getResourceAsStream("/media/AppIcon.png"), 100, 100, true, true));
		
	}
	
	@FXML private void handleClose() {
		Stage stage = (Stage) appNameText.getScene().getWindow();
		stage.close();
	}
	
}
