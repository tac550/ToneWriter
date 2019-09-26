package com.tac550.tonewriter.io;

import com.tac550.tonewriter.view.MainApp;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;

import java.io.IOException;

public class FXMLLoaderIO {

	public static Task<FXMLLoader> loadFXMLLayout(String filename, AdditionalActions additionalActions) {
		Task<FXMLLoader> loaderTask = new Task<>() {

			@Override
			protected FXMLLoader call() throws IOException {

				// Load layout from fxml file
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(MainApp.class.getResource(filename));
				loader.load();

				additionalActions.run(loader);

				return loader;
			}
		};

		//noinspection ThrowableNotThrown
		loaderTask.setOnFailed(e -> loaderTask.getException().printStackTrace());

		return loaderTask;

	}

	public interface AdditionalActions {

		void run(FXMLLoader loader);

	}

}
