package com.palveo.gui.controller;

import java.io.IOException;
import com.palveo.MainApp;
import com.palveo.db.DatabaseConnection;
import com.palveo.gui.util.AlertFactory;
import com.palveo.model.User;
import com.palveo.service.AuthService;
import com.palveo.service.exception.RegistrationException;
import com.palveo.service.impl.AuthServiceImpl;
import com.palveo.util.ValidationUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.TextFlow;

public class SignUpController {

    @FXML
    private TextField usernameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField cityField;
    @FXML
    private TextField districtField;
    @FXML
    private CheckBox eulaCheckBox;
    @FXML
    private CheckBox ageCheckBox;
    @FXML
    private Button signUpButton;
    @FXML
    private Hyperlink loginLink;
    @FXML
    private Hyperlink eulaLink;
    @FXML
    private TextField securityQuestionField;
    @FXML
    private PasswordField securityAnswerField;

    @FXML
    private TextFlow eulaTextFlow;

    private AuthService authService;

    public SignUpController() {
        this.authService = new AuthServiceImpl();
    }

    @FXML
    private void initialize() {
        if (!DatabaseConnection.isConnected()) {
            AlertFactory.showErrorAlert("Database Connection Error", "Failed to Connect",
                    "Unable to connect to the database. Some features may be unavailable.");
        }

        Platform.runLater(() -> {
            if (eulaTextFlow != null && eulaCheckBox != null) {
                eulaTextFlow.prefWidthProperty().bind(eulaCheckBox.widthProperty().subtract(15));
            }
        });
    }

    @FXML
    private void handleSignUpButtonAction(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String city = cityField.getText().trim();
        String district = districtField.getText().trim();
        String securityQuestion = securityQuestionField.getText().trim();
        String securityAnswer = securityAnswerField.getText();
        boolean eulaAccepted = eulaCheckBox.isSelected();
        boolean ageVerified = ageCheckBox.isSelected();

        if (ValidationUtils.isNullOrEmpty(username) || ValidationUtils.isNullOrEmpty(email)
                || ValidationUtils.isNullOrEmpty(password)
                || ValidationUtils.isNullOrEmpty(confirmPassword)
                || ValidationUtils.isNullOrEmpty(firstName)
                || ValidationUtils.isNullOrEmpty(lastName) || ValidationUtils.isNullOrEmpty(city)
                || ValidationUtils.isNullOrEmpty(securityQuestion)
                || ValidationUtils.isNullOrEmpty(securityAnswer)) {
            AlertFactory.showErrorAlert("Registration Error", "Missing Information",
                    "All required fields must be filled.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            AlertFactory.showErrorAlert("Registration Error", "Password Mismatch",
                    "Passwords do not match.");
            passwordField.clear();
            confirmPasswordField.clear();
            passwordField.requestFocus();
            return;
        }

        if (!eulaAccepted) {
            AlertFactory.showErrorAlert("Registration Error", "EULA Not Accepted",
                    "You must accept the EULA to register.");
            return;
        }
        if (!ageVerified) {
            AlertFactory.showErrorAlert("Registration Error", "Age Not Verified",
                    "You must confirm you are 18 years or older.");
            return;
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setCity(city);
        newUser.setDistrict(district.isEmpty() ? null : district);
        newUser.setEulaAccepted(eulaAccepted);
        newUser.setAgeVerified(ageVerified);

        try {
            authService.registerUser(newUser, password, securityQuestion, securityAnswer);
            AlertFactory.showInformationAlert("Registration Successful", "Account Created!",
                    "Welcome, " + newUser.getUsername()
                            + "! Your account has been successfully created. Please login.");
            handleLoginLinkAction(null);
        } catch (RegistrationException e) {
            AlertFactory.showErrorAlert("Registration Failed", "Could not create account",
                    e.getMessage());
        } catch (Exception e) {
            AlertFactory.showErrorAlert("System Error", "Unexpected Error",
                    "An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLoginLinkAction(ActionEvent event) {
        try {
            MainApp.changeScene("login.fxml", "Palveo - Login", 350, 600);
        } catch (IOException e) {
            AlertFactory.showErrorAlert("Navigation Error", "Error Loading Login Screen",
                    e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEulaLinkAction(ActionEvent event) {
        String eulaText = "End User License Agreement for Palveo:\n\n"
                + "1. You agree to use this application responsibly.\n"
                + "2. You are responsible for your own safety and interactions.\n"
                + "3. We collect username, email, and activity data as described in our Privacy Policy.\n"
                + "4. (More terms here...)\n\n"
                + "This is a placeholder EULA for CS102 Palveo project.";
        AlertFactory.showInformationAlert("Palveo EULA", "End User License Agreement", eulaText);
    }
}
