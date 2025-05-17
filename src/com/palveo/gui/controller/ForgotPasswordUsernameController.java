package com.palveo.gui.controller;

import java.io.IOException;
import java.util.Optional;
import com.palveo.MainApp;
import com.palveo.gui.util.AlertFactory;
import com.palveo.gui.util.FXMLLoaderWrapper;
import com.palveo.service.AuthService;
import com.palveo.service.impl.AuthServiceImpl;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ForgotPasswordUsernameController {

    @FXML
    private TextField usernameField;

    @FXML
    private Button nextButton; 
                               
    @FXML
    private Button backToLoginButton; 

    private AuthService authService;

    public ForgotPasswordUsernameController() {
        this.authService = new AuthServiceImpl();
    }

    @FXML
    private void initialize() {}

    @FXML
    private void handleNextButtonAction(ActionEvent event) {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            AlertFactory.showErrorAlert("Input Error", "Username Required",
                    "Please enter your username.");
            return;
        }

        Optional<String> securityQuestionOpt = authService.getSecurityQuestionForUser(username);

        if (securityQuestionOpt.isPresent() && securityQuestionOpt.get() != null
                && !securityQuestionOpt.get().isEmpty()) {
            try {
                
                FXMLLoaderWrapper.FXMLLoaderResult<ForgotPasswordQuestionController> loaderResult =
                        FXMLLoaderWrapper.loadFXMLWithController("forgot_password_question.fxml");

                Parent pane = loaderResult.parent;
                ForgotPasswordQuestionController questionController = loaderResult.controller;

                
                questionController.initData(username, securityQuestionOpt.get());

                
                Stage stage = MainApp.getPrimaryStage(); 
                if (stage != null) {
                    stage.getScene().setRoot(pane);
                    stage.setTitle("Palveo - Security Question");
                    
                    stage.setWidth(400);
                    stage.setHeight(450);
                } else {
                    AlertFactory.showErrorAlert("Application Error", "Cannot change screen.",
                            "Primary stage is not available.");
                }

            } catch (IOException e) {
                AlertFactory.showErrorAlert("Navigation Error",
                        "Could not load the security question screen.", e.getMessage());
                e.printStackTrace();
            }
        } else {
            AlertFactory.showErrorAlert("User Not Found",
                    "Invalid Username or No Security Question",
                    "The username was not found, or no security question is set for this account.");
        }
    }

    @FXML
    private void handleBackToLoginAction(ActionEvent event) {
        try {
            MainApp.changeScene("login.fxml", "Palveo - Login", 350, 600);
        } catch (IOException e) {
            AlertFactory.showErrorAlert("Navigation Error", "Could not return to login screen.",
                    e.getMessage());
            e.printStackTrace();
        }
    }
}
