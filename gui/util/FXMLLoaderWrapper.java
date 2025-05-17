package com.palveo.gui.util;

import java.io.IOException;
import java.net.URL;
import com.palveo.MainApp;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class FXMLLoaderWrapper {

    public static Parent loadFXML(String fxmlFile) throws IOException {
        URL fxmlUrl = MainApp.class.getResource("/fxml/" + fxmlFile);
        if (fxmlUrl == null) {
            throw new IOException("Cannot load FXML file: /fxml/" + fxmlFile
                    + ". Check path and ensure 'resources' is a source folder.");
        }
        return FXMLLoader.load(fxmlUrl);
    }

    public static <T> FXMLLoaderResult<T> loadFXMLWithController(String fxmlFile)
            throws IOException {
        URL fxmlUrl = MainApp.class.getResource("/fxml/" + fxmlFile);
        if (fxmlUrl == null) {
            throw new IOException("Cannot load FXML file: /fxml/" + fxmlFile);
        }
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent parent = loader.load();
        T controller = loader.getController();
        if (controller == null) {
            System.err.println("Warning: Controller for " + fxmlFile
                    + " is null. Ensure fx:controller is set correctly in the FXML.");
        }
        return new FXMLLoaderResult<>(parent, controller);
    }

    public static class FXMLLoaderResult<T> {
        public final Parent parent;
        public final T controller;

        public FXMLLoaderResult(Parent parent, T controller) {
            this.parent = parent;
            this.controller = controller;
        }
    }

    private FXMLLoaderWrapper() {}
}
