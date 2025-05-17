package com.palveo.gui.controller;

import java.io.IOException;
import com.palveo.MainApp;
import com.palveo.gui.util.AlertFactory;
import com.palveo.gui.util.FXMLLoaderWrapper;
import com.palveo.service.AuthService;
import com.palveo.service.impl.AuthServiceImpl;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class ForgotPasswordQuestionController {

    @FXML
    private Label securityQuestionLabel;

    @FXML
    private PasswordField securityAnswerField;

    @FXML
    private Button verifyButton; 

    @FXML
    private Button backButton; 

    private String username; 
    private AuthService authService;

    public ForgotPasswordQuestionController() {
        this.authService = new AuthServiceImpl();
    }

    
    public void initData(String username, String securityQuestion) {
        this.username = username;
        this.securityQuestionLabel.setText(securityQuestion);
    }

    @FXML
    private void initialize() {
        
    }

    @FXML
    private void handleVerifyButtonAction(ActionEvent event) {
        String answer = securityAnswerField.getText(); 
        if (answer.isEmpty()) {
            AlertFactory.showErrorAlert("Input Error", "Answer Required",
                    "Please enter your answer to the security question.");
            return;
        }

        if (authService.verifySecurityAnswer(username, answer)) {
            
            try {
                FXMLLoaderWrapper.FXMLLoaderResult<ResetPasswordController> loaderResult =
                        FXMLLoaderWrapper.loadFXMLWithController("reset_password.fxml");

                Parent pane = loaderResult.parent;
                ResetPasswordController resetController = loaderResult.controller;

                resetController.initData(username); 

                Stage stage = MainApp.getPrimaryStage();
                if (stage != null) {
                    stage.getScene().setRoot(pane);
                    stage.setTitle("Palveo - Reset Password");
                    stage.setWidth(400);
                    stage.setHeight(450);
                } else {
                    AlertFactory.showErrorAlert("Application Error", "Cannot change screen.",
                            "Primary stage is not available.");
                }

            } catch (IOException e) {
                AlertFactory.showErrorAlert("Navigation Error",
                        "Could not load the password reset screen.", e.getMessage());
                e.printStackTrace();
            }
        } else {
            AlertFactory.showErrorAlert("Verification Failed", "Incorrect Answer",
                    "The answer to your security question is incorrect. Please try again.");
            securityAnswerField.clear(); 
        }
    }

    @FXML
    private void handleBackButtonAction(ActionEvent event) {
        
        try {
            
            
            Parent pane = FXMLLoaderWrapper.loadFXML("forgot_password_username.fxml");

            Stage stage = MainApp.getPrimaryStage();
            if (stage != null) {
                stage.getScene().setRoot(pane);
                stage.setTitle("Palveo - Forgot Password");
                stage.setWidth(350); 
                stage.setHeight(400);
            } else {
                AlertFactory.showErrorAlert("Application Error", "Cannot change screen.",
                        "Primary stage is not available.");
            }

        } catch (IOException e) {
            AlertFactory.showErrorAlert("Navigation Error",
                    "Could not return to the previous screen.", e.getMessage());
            e.printStackTrace();
        }
    }
}
