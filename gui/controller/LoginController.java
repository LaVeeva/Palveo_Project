package com.palveo.gui.controller;

import java.io.IOException;
import java.util.Optional;
import com.palveo.MainApp;
import com.palveo.db.DatabaseConnection;
import com.palveo.gui.manager.SessionManager;
import com.palveo.gui.util.AlertFactory;
import com.palveo.model.User;
import com.palveo.service.AuthService;
import com.palveo.service.impl.AuthServiceImpl;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Hyperlink signUpLink;

    @FXML
    private Hyperlink forgotPasswordLink;

    private AuthService authService;
    private SessionManager sessionManager;

    public LoginController() {
        this.authService = new AuthServiceImpl();
        this.sessionManager = SessionManager.getInstance();
    }

    @FXML
    private void initialize() {
        if (!DatabaseConnection.isConnected()) {
            AlertFactory.showErrorAlert("Database Connection Error", "Failed to Connect",
                    "Unable to connect to the database. Login may not be possible.");
        }
    }

    @FXML
    private void handleLoginButtonAction(ActionEvent event) {
        String usernameOrEmail = usernameField.getText().trim();
        String password = passwordField.getText();

        if (usernameOrEmail.isEmpty() || password.isEmpty()) {
            AlertFactory.showErrorAlert("Login Error", "Input Required",
                    "Username/Email and password cannot be empty.");
            return;
        }

        Optional<User> loggedInUserOpt = authService.loginUser(usernameOrEmail, password);

        if (loggedInUserOpt.isPresent()) {
            sessionManager.setCurrentUser(loggedInUserOpt.get());
            AlertFactory.showInformationAlert("Login Successful",
                    "Welcome back, " + loggedInUserOpt.get().getUsername() + "!",
                    "You are now logged in.");

            try {
                MainApp.changeScene("main_panel.fxml", "Palveo - Main", 800, 600);
            } catch (IOException e) {
                System.err.println(
                        "LoginController: Failed to load main panel FXML: " + e.getMessage());
                e.printStackTrace();
                AlertFactory.showErrorAlert("Navigation Error",
                        "Could not load the main application screen.", "Please contact support.");
            }

        } else {
            AlertFactory.showErrorAlert("Login Failed", "Invalid Credentials",
                    "The username/email or password you entered is incorrect. Please try again.");
            passwordField.clear();
        }
    }

    @FXML
    private void handleSignUpLinkAction(ActionEvent event) {
        try {
            MainApp.changeScene("signup.fxml", "Palveo - Sign Up", 350, 600);
        } catch (IOException e) {
            System.err.println("LoginController: Failed to load signup FXML: " + e.getMessage());
            e.printStackTrace();
            AlertFactory.showErrorAlert("Navigation Error", "Could not load the sign-up screen.",
                    "Please try again later.");
        }
    }

    @FXML
    private void handleForgotPasswordLinkAction(ActionEvent event) {
        try {
            MainApp.changeScene("forgot_password_username.fxml", "Palveo - Forgot Password", 350,
                    400);
        } catch (IOException e) {
            System.err.println("LoginController: Failed to load forgot_password_username.fxml: "
                    + e.getMessage());
            e.printStackTrace();
            AlertFactory.showErrorAlert("Navigation Error",
                    "Could not load the forgot password screen.", "Please try again later.");
        }
    }
}
