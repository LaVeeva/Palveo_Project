package com.palveo;

import java.io.IOException;
import java.net.URL;
import com.palveo.db.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Welcome to Palveo Source Code!
 * 
 * Please follow the guidance from Readme.md to run properly.
 * 
 * @author Ahmet Alp Çamlıbel
 * @author Batuhan Küçük
 * @author Batuhan Yıldırım
 * @author Eren Sarıgül
 * @author Ömür Meriç Arıcı
 * 
 */

public class MainApp extends Application {

    private static Stage primaryStage;

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        URL fxmlLocation = getClass().getResource("/fxml/welcome.fxml");
        if (fxmlLocation == null) {
            System.err.println("FXML file not found: /fxml/welcome.fxml");
            System.err.println("Make sure the 'resources' folder is in the source paths and the FXML file exists.");
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 335, 560);
        primaryStage.setTitle("Palveo");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    public static void changeScene(String fxmlFile, String title, int width, int height) throws IOException {
        if (primaryStage == null) {
            System.err.println("The main scene could not be initialized.");
            return;
        }
        URL fxmlLocation = MainApp.class.getResource("/fxml/" + fxmlFile);
        if (fxmlLocation == null) {
            System.err.println("FXML file not found: /fxml/" + fxmlFile);
            return;
        }
        Parent pane = FXMLLoader.load(fxmlLocation);
        primaryStage.getScene().setRoot(pane);
        primaryStage.setTitle(title);
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
    }

    @Override
    public void stop() throws Exception {
        System.out.println("MainApp: stop() is called. DB Connection shutting down.");
        DatabaseConnection.closeConnection();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
