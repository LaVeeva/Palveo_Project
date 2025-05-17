package com.palveo.gui.controller;

import java.io.IOException;
import com.palveo.MainApp;
import com.palveo.gui.util.AlertFactory;
import com.palveo.service.AuthService;
import com.palveo.service.exception.RegistrationException;
import com.palveo.service.impl.AuthServiceImpl;
import com.palveo.util.ValidationUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;

public class ResetPasswordController {

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmNewPasswordField;

    @FXML
    private Button resetPasswordButton; 
    @FXML
    private Button cancelButton; 
    private String username;
    private AuthService authService;

    public ResetPasswordController() {
        this.authService = new AuthServiceImpl();
    }

    public void initData(String username) {
        this.username = username;
    }

    @FXML
    private void initialize() {
            }

    @FXML
    private void handleResetPasswordButtonAction(ActionEvent event) {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmNewPasswordField.getText();

        if (ValidationUtils.isNullOrEmpty(newPassword)
                || ValidationUtils.isNullOrEmpty(confirmPassword)) {
            AlertFactory.showErrorAlert("Input Error", "Passwords Required",
                    "Please enter and confirm your new password.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            AlertFactory.showErrorAlert("Password Mismatch", "Passwords Do Not Match",
                    "The new password and confirmation password do not match.");
            confirmNewPasswordField.clear();             return;
        }

        if (!ValidationUtils.isPasswordStrongEnough(newPassword)) {
            AlertFactory.showErrorAlert("Weak Password", "Password Not Strong Enough",
                    "Your new password is not strong enough. It must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit, and a special character.");
            return;
        }

        try {
            if (authService.resetPassword(username, newPassword)) {
                AlertFactory.showInformationAlert("Success!", "Password Reset Successful",
                        "Your password has been successfully reset. You can now login with your new password.");
                navigateToLoginScreen();
            } else {
                                                                AlertFactory.showErrorAlert("Password Reset Failed", "An Unknown Error Occurred",
                        "Could not reset your password due to an unexpected error. Please try again.");
            }
        } catch (RegistrationException e) {
            AlertFactory.showErrorAlert("Password Reset Failed", "Error during reset",
                    e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelButtonAction(ActionEvent event) {
        navigateToLoginScreen();
    }

    private void navigateToLoginScreen() {
        try {
            MainApp.changeScene("login.fxml", "Palveo - Login", 350, 600);
        } catch (IOException e) {
            AlertFactory.showErrorAlert("Navigation Error", "Could not return to login screen.",
                    e.getMessage());
            e.printStackTrace();
        }
    }
}
