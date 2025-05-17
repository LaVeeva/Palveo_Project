package com.palveo.gui.controller;

import java.io.IOException;
import com.palveo.MainApp;
import com.palveo.gui.util.AlertFactory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

public class WelcomeController {

    @FXML
    private Label loginLabel;

    @FXML
    private Label signUpLabel;

    @FXML
    private void initialize() {
        addHoverEffect(loginLabel);
        addHoverEffect(signUpLabel);
    }

    private void addHoverEffect(Label label) {
        label.setOnMouseEntered(_ -> {
            label.setUnderline(true);
        });
        label.setOnMouseExited(_ -> {
            label.setUnderline(false);
        });
    }

    @FXML
    private void handleLoginLabelClick(MouseEvent event) {
        try {
            MainApp.changeScene("login.fxml", "Palveo - Login", 350, 600);
        } catch (IOException e) {
            System.err.println("WelcomeController: login.fxml yüklenemedi: " + e.getMessage());
            e.printStackTrace();
            AlertFactory.showErrorAlert("Navigasyon Hatası", "Giriş ekranı yüklenemedi.",
                    e.getMessage());
        }
    }

    @FXML
    private void handleSignUpLabelClick(MouseEvent event) {
        try {
            MainApp.changeScene("signup.fxml", "Palveo - Sign Up", 350, 600);
        } catch (IOException e) {
            System.err.println("WelcomeController: signup.fxml yüklenemedi: " + e.getMessage());
            e.printStackTrace();
            AlertFactory.showErrorAlert("Navigasyon Hatası", "Kayıt ekranı yüklenemedi.",
                    e.getMessage());
        }
    }
}
