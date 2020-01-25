package com.tac550.tonewriter.io;

import com.tac550.tonewriter.view.MainApp;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.util.function.Consumer;

public class FXMLLoaderIO {

	public static Task<FXMLLoader> loadFXMLLayout(String filename, Consumer<FXMLLoader> additionalActions) {
		Task<FXMLLoader> loaderTask = new Task<>() {

			@Override
			protected FXMLLoader call() throws IOException {

				// Load layout from fxml file
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(MainApp.class.getResource(filename));
				loader.load();

				additionalActions.accept(loader);

				return loader;
			}
		};

		//noinspection ThrowableNotThrown
		loaderTask.setOnFailed(e -> loaderTask.getException().printStackTrace());

		Thread loaderThread = new Thread(loaderTask);
		loaderThread.start();

		return loaderTask;

	}

}
