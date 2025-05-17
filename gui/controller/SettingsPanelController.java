package com.palveo.gui.controller;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import com.palveo.config.AppConfig;
import com.palveo.gui.manager.SessionManager;
import com.palveo.gui.util.AlertFactory;
import com.palveo.model.User;
import com.palveo.service.UserService;
import com.palveo.service.exception.UserNotFoundException;
import com.palveo.service.exception.UserOperationException;
import com.palveo.service.impl.UserServiceImpl;
import com.palveo.util.PasswordUtils;
import com.palveo.util.ValidationUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

public class SettingsPanelController {

    @FXML
    private Button backToProfileButton;
    @FXML
    private ImageView avatarImageView;
    @FXML
    private Button changeAvatarButton;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextArea bioTextArea;
    @FXML
    private TextField cityField;
    @FXML
    private TextField districtField;
    @FXML
    private Button saveProfileInfoButton;
    @FXML
    private Label usernameDisplayLabel;
    @FXML
    private Label emailDisplayLabel;
    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmNewPasswordField;
    @FXML
    private Button changePasswordButton;
    @FXML
    private TextArea securityQuestionArea;
    @FXML
    private PasswordField securityAnswerField;
    @FXML
    private Button updateSecurityQAButton;

    private MainPanelController mainPanelController;
    private SessionManager sessionManager;
    private UserService userService;
    private User currentUserToEdit;
    private String pendingAvatarFileTargetName;

    public SettingsPanelController() {
        this.sessionManager = SessionManager.getInstance();
        this.userService = new UserServiceImpl();
    }

    public void setMainPanelController(MainPanelController mainPanelController) {
        this.mainPanelController = mainPanelController;
    }

    public void loadUserSettings(User user) {
        this.currentUserToEdit = user;
        if (currentUserToEdit == null) {
            AlertFactory.showErrorAlert("Error", "User Data Missing",
                    "Cannot load settings for an unknown user.");
            Platform.runLater(() -> {
                if (mainPanelController != null) {
                    mainPanelController.loadView("profile.fxml");
                    mainPanelController.setCurrentViewFxmlPath("profile.fxml");
                }
            });
            return;
        }
        this.pendingAvatarFileTargetName = currentUserToEdit.getProfileImagePath();
        populateForm();
    }

    private void populateForm() {
        if (currentUserToEdit == null)
            return;

        updateAvatarDisplay(this.pendingAvatarFileTargetName);

        firstNameField.setText(
                currentUserToEdit.getFirstName() != null ? currentUserToEdit.getFirstName() : "");
        lastNameField.setText(
                currentUserToEdit.getLastName() != null ? currentUserToEdit.getLastName() : "");
        bioTextArea.setText(currentUserToEdit.getBio() != null ? currentUserToEdit.getBio() : "");
        cityField.setText(currentUserToEdit.getCity() != null ? currentUserToEdit.getCity() : "");
        districtField.setText(
                currentUserToEdit.getDistrict() != null ? currentUserToEdit.getDistrict() : "");

        usernameDisplayLabel.setText(currentUserToEdit.getUsername());
        emailDisplayLabel.setText(currentUserToEdit.getEmail());
        securityQuestionArea.setText(currentUserToEdit.getSecurityQuestion() != null
                ? currentUserToEdit.getSecurityQuestion()
                : "");
    }

    private void updateAvatarDisplay(String avatarFileNameOrClassPath) {
        Image imageToDisplay = null;
        if (avatarFileNameOrClassPath != null && !avatarFileNameOrClassPath.isBlank()) {
            if (avatarFileNameOrClassPath.startsWith("/")) { 
                try (InputStream classpathStream = getClass().getResourceAsStream(avatarFileNameOrClassPath)) {
                    if (classpathStream != null) {
                        imageToDisplay = new Image(classpathStream);
                    }
                } catch (Exception e) {
                     System.err.println("SettingsPanel: Error loading classpath avatar " + avatarFileNameOrClassPath + ": " + e.getMessage());
                }
            } else { 
                File avatarFile = new File(AppConfig.getUserAvatarsDir(), avatarFileNameOrClassPath);
                if (avatarFile.exists() && avatarFile.isFile()) {
                    try {
                        imageToDisplay = new Image(avatarFile.toURI().toString());
                    } catch (Exception e){
                        System.err.println("SettingsPanel: Error loading file system avatar " + avatarFile.getAbsolutePath() + ": " + e.getMessage());
                    }
                }
            }
        }

        if (imageToDisplay == null || imageToDisplay.isError()) {
            try (InputStream defaultStream = getClass().getResourceAsStream(AppConfig.DEFAULT_AVATAR_PATH)) {
                 if(defaultStream != null) imageToDisplay = new Image(defaultStream);
            } catch (Exception e) {
                 System.err.println("SettingsPanel: Error loading DEFAULT avatar: " + e.getMessage());
            }
        }
        avatarImageView.setImage(imageToDisplay);
    }

    @FXML
    private void handleChangeAvatarAction(ActionEvent event) {
        if (currentUserToEdit == null)
            return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select New Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", ".png", ".jpg", ".jpeg", ".gif"));
        File selectedFile = fileChooser.showOpenDialog(changeAvatarButton.getScene().getWindow());

        if (selectedFile != null) {
            changeAvatarButton.setDisable(true);
            changeAvatarButton.setText("Processing...");

            Task<String> uploadTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    String fileExtension = "";
                    String fileNameOriginal = selectedFile.getName();
                    int dotIndex = fileNameOriginal.lastIndexOf('.');
                    if (dotIndex > 0 && dotIndex < fileNameOriginal.length() - 1) {
                        fileExtension = fileNameOriginal.substring(dotIndex).toLowerCase();
                    }
                    String newImageFileName = "user_" + currentUserToEdit.getId() + "_avatar_"
                            + System.currentTimeMillis() + fileExtension;
                    
                    Path targetDirectoryPath = Paths.get(AppConfig.getUserAvatarsDir());
                    if (!Files.exists(targetDirectoryPath)) {
                        Files.createDirectories(targetDirectoryPath);
                    }
                    Path targetFilePath = targetDirectoryPath.resolve(newImageFileName);
                    Files.copy(selectedFile.toPath(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);
                    return newImageFileName; 
                }
            };

            uploadTask.setOnSucceeded(_ -> {
                this.pendingAvatarFileTargetName = uploadTask.getValue();
                updateAvatarDisplay(this.pendingAvatarFileTargetName);
                Platform.runLater(() -> {
                    changeAvatarButton.setDisable(false);
                    changeAvatarButton.setText("Change Avatar");
                    AlertFactory.showInformationAlert("Info", "Avatar Preview Changed",
                            "New avatar selected. Click 'Save Profile Changes' to apply permanently.");
                });
            });

            uploadTask.setOnFailed(_ -> {
                AlertFactory.showErrorAlert("Upload Failed", "Could not process selected image.",
                       uploadTask.getException() != null ? uploadTask.getException().getMessage() : "Unknown error.");
                if(uploadTask.getException() != null) uploadTask.getException().printStackTrace();
                Platform.runLater(() -> {
                    changeAvatarButton.setDisable(false);
                    changeAvatarButton.setText("Change Avatar");
                });
            });
            new Thread(uploadTask).start();
        }
    }

    @FXML
    private void handleSaveProfileInfoAction(ActionEvent event) {
        if (currentUserToEdit == null)
            return;

        User updates = new User();
        updates.setId(currentUserToEdit.getId());
        updates.setFirstName(firstNameField.getText().trim());
        updates.setLastName(lastNameField.getText().trim());
        updates.setBio(bioTextArea.getText().trim());
        updates.setCity(cityField.getText().trim());
        updates.setDistrict(districtField.getText().trim());
        updates.setProfileImagePath(this.pendingAvatarFileTargetName);
        
        updates.setSecurityQuestion(currentUserToEdit.getSecurityQuestion());
        updates.setSecurityAnswerHash(currentUserToEdit.getSecurityAnswerHash());
        updates.setSecurityAnswerSalt(currentUserToEdit.getSecurityAnswerSalt());

        try {
            if (ValidationUtils.isNullOrEmpty(updates.getFirstName())
                    || !ValidationUtils.isValidNameFormat(updates.getFirstName())) {
                throw new UserOperationException("Valid first name is required.");
            }
            if (ValidationUtils.isNullOrEmpty(updates.getLastName())
                    || !ValidationUtils.isValidNameFormat(updates.getLastName())) {
                throw new UserOperationException("Valid last name is required.");
            }
            if (ValidationUtils.isNullOrEmpty(updates.getCity())) {
                throw new UserOperationException("City is required.");
            }
            if (updates.getBio() != null && updates.getBio().length() > 5000) {
                throw new UserOperationException("Bio is too long (max 5000 characters).");
            }

            User updatedUser = userService.updateUserProfile(updates, currentUserToEdit);
            sessionManager.setCurrentUser(updatedUser);
            this.currentUserToEdit = updatedUser; 
            this.pendingAvatarFileTargetName = updatedUser.getProfileImagePath();

            AlertFactory.showInformationAlert("Success", "Profile Updated",
                    "Your profile information has been saved.");
            
            if (mainPanelController != null) {
                 mainPanelController.loadView("profile.fxml", controller -> {
                    if (controller instanceof ProfileController) {
                        ((ProfileController) controller).refreshViewData();
                    }
                });
            }
        } catch (UserNotFoundException | UserOperationException e) {
            AlertFactory.showErrorAlert("Save Failed", "Could not save profile changes.",
                    e.getMessage());
        }
    }

    @FXML
    private void handleChangePasswordAction(ActionEvent event) {
        if (currentUserToEdit == null)
            return;

        String currentPass = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmNewPass = confirmNewPasswordField.getText();

        if (ValidationUtils.isNullOrEmpty(currentPass)) {
            AlertFactory.showErrorAlert("Input Error", "Current Password Required",
                    "Please enter your current password to change it.");
            currentPasswordField.requestFocus();
            return;
        }
        if (ValidationUtils.isNullOrEmpty(newPass)
                || ValidationUtils.isNullOrEmpty(confirmNewPass)) {
            AlertFactory.showErrorAlert("Input Error", "New Password Fields Required",
                    "Please enter and confirm your new password.");
            return;
        }
        if (!newPass.equals(confirmNewPass)) {
            AlertFactory.showErrorAlert("Password Mismatch", "New passwords do not match.",
                    "Please ensure the new password and confirmation match.");
            confirmNewPasswordField.clear();
            return;
        }
        if (!ValidationUtils.isPasswordStrongEnough(newPass)) {
            AlertFactory.showErrorAlert("Weak Password", "New password is not strong enough.",
                    "It must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit, and a special character.");
             return;
        }

        changePasswordButton.setDisable(true);
        changePasswordButton.setText("Changing...");
        Task<Boolean> changePassTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return userService.changeUserPassword(currentUserToEdit, currentPass, newPass);
            }
        };

        changePassTask.setOnSucceeded(_ -> Platform.runLater(() -> {
            if (changePassTask.getValue()) {
                AlertFactory.showInformationAlert("Success", "Password Changed",
                        "Your password has been successfully updated.");
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmNewPasswordField.clear();
            } else {
                AlertFactory.showErrorAlert("Failed", "Password Change Failed",
                        "Could not change password. Ensure current password is correct.");
            }
            changePasswordButton.setDisable(false);
            changePasswordButton.setText("Change Password");
        }));
        changePassTask.setOnFailed(_ -> Platform.runLater(() -> {
            AlertFactory.showErrorAlert("Error", "Password Change Error",
                     (changePassTask.getException()!=null ? changePassTask.getException().getMessage() : "Unknown error."));
             if(changePassTask.getException()!=null) changePassTask.getException().printStackTrace();
            changePasswordButton.setDisable(false);
            changePasswordButton.setText("Change Password");
        }));
        new Thread(changePassTask).start();
    }

    @FXML
    private void handleUpdateSecurityQAAction(ActionEvent event) {
        if (currentUserToEdit == null)
            return;

        String currentPass = currentPasswordField.getText();
        String newQuestion = securityQuestionArea.getText().trim();
        String newRawAnswer = securityAnswerField.getText();

        if (ValidationUtils.isNullOrEmpty(currentPass)) {
            AlertFactory.showErrorAlert("Input Error", "Current Password Required",
                    "Please enter your current password to update security question/answer.");
            currentPasswordField.requestFocus();
            return;
        }
        if (ValidationUtils.isNullOrEmpty(newQuestion)
                || ValidationUtils.isNullOrEmpty(newRawAnswer)) {
            AlertFactory.showErrorAlert("Input Error", "New Question and Answer Required",
                    "New security question and answer cannot be empty.");
            return;
        }
        if (newQuestion.length() > 255 || newRawAnswer.length() > 255) {
            AlertFactory.showErrorAlert("Input Error", "Text Too Long",
                    "Security question or answer is too long (max 255 characters).");
            return;
        }
        if (newQuestion.equals(currentUserToEdit.getSecurityQuestion()) && ValidationUtils.isNullOrEmpty(newRawAnswer)) {
             AlertFactory.showWarningAlert("No Change Detected", "No Change", "If updating only the question, please also provide the answer for that question.");
            return;
        }

        updateSecurityQAButton.setDisable(true);
        updateSecurityQAButton.setText("Updating...");

        Task<Void> updateQATask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                User userForAuth = userService.getUserById(currentUserToEdit.getId()).orElseThrow(
                        () -> new UserNotFoundException("User not found for Q&A update."));
                if (!PasswordUtils.verifyPassword(currentPass, userForAuth.getPasswordHash(),
                        userForAuth.getSalt())) {
                    throw new UserOperationException("Incorrect current password provided.");
                }

                User updates = new User();
                updates.setId(currentUserToEdit.getId());
                updates.setSecurityQuestion(newQuestion);

                String newSecurityAnswerSalt = PasswordUtils.generateSalt();
                String newHashedSecurityAnswer = PasswordUtils.hashPassword(newRawAnswer, newSecurityAnswerSalt);
                updates.setSecurityAnswerHash(newHashedSecurityAnswer);
                updates.setSecurityAnswerSalt(newSecurityAnswerSalt);
                
                updates.setFirstName(currentUserToEdit.getFirstName());
                updates.setLastName(currentUserToEdit.getLastName());
                updates.setCity(currentUserToEdit.getCity());
                updates.setProfileImagePath(currentUserToEdit.getProfileImagePath());

                userService.updateUserProfile(updates, currentUserToEdit);
                return null;
            }
        };

        updateQATask.setOnSucceeded(_ -> Platform.runLater(() -> {
            AlertFactory.showInformationAlert("Success", "Security Q&A Updated",
                    "Your security question and answer have been updated.");
            
            User updatedSessionUser = userService.getUserById(currentUserToEdit.getId()).orElse(currentUserToEdit);
            sessionManager.setCurrentUser(updatedSessionUser);
            this.currentUserToEdit = updatedSessionUser;

            securityAnswerField.clear();
            currentPasswordField.clear();
            updateSecurityQAButton.setDisable(false);
            updateSecurityQAButton.setText("Update Security Q&A");
            populateForm();
        }));

        updateQATask.setOnFailed(_ -> Platform.runLater(() -> {
            AlertFactory.showErrorAlert("Update Failed", "Could not update security Q&A.",
                    (updateQATask.getException() != null ? updateQATask.getException().getMessage() : "Unknown error."));
            if(updateQATask.getException() != null) updateQATask.getException().printStackTrace();
            updateSecurityQAButton.setDisable(false);
            updateSecurityQAButton.setText("Update Security Q&A");
        }));
        new Thread(updateQATask).start();
    }

    @FXML
    private void handleBackToProfileButtonAction(ActionEvent event) {
        if (mainPanelController != null) {
            mainPanelController.loadView("profile.fxml", controller -> {
                 if (controller instanceof ProfileController) {
                    ((ProfileController) controller).refreshViewData();
                }
            });
            mainPanelController.setCurrentViewFxmlPath("profile.fxml");
        }
    }
}