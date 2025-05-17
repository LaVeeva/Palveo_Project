package com.palveo.gui.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.palveo.config.AppConfig;
import com.palveo.gui.manager.SessionManager;
import com.palveo.gui.util.AlertFactory;
import com.palveo.gui.util.FXMLLoaderWrapper;
import com.palveo.model.User;
import com.palveo.service.FriendshipService;
import com.palveo.service.exception.FriendshipOperationException;
import com.palveo.service.impl.FriendshipServiceImpl;
import com.palveo.service.impl.UserServiceImpl;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class FriendsPanelController {

    @FXML
    private TextField searchFriendsField;
    @FXML
    private FlowPane friendsFlowPane;
    @FXML
    private ScrollPane friendsScrollPane;
    @FXML
    private Label statusLabel;
    @FXML
    private Label pendingRequestsHeader;
    @FXML
    private VBox pendingRequestsBox;

    private MainPanelController mainPanelController;
    private SessionManager sessionManager;
    private FriendshipService friendshipService;
    private User currentUser;
    private List<User> allFriendsCache;
    private List<User> pendingRequestersCache;

    public FriendsPanelController() {
        this.sessionManager = SessionManager.getInstance();
        this.friendshipService = new FriendshipServiceImpl();
        new UserServiceImpl();
    }

    public void setMainPanelController(MainPanelController mainPanelController) {
        this.mainPanelController = mainPanelController;
    }

    @FXML
    private void initialize() {
       refreshViewData();
       searchFriendsField.textProperty().addListener((_, _, newValue) -> {
            filterFriends(newValue);
        });
    }
    
    public void refreshViewData() {
         this.currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            showStatus("Error: Not logged in.", true);
            searchFriendsField.setDisable(true);
            return;
        }
        loadInitialData();
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setTextFill(isError ? Color.RED : Color.web("#757575"));
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void hideStatus() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    private void loadInitialData() {
        loadPendingRequests();
        loadFriends();
    }

    private void loadFriends() {
        showStatus("Loading friends list...", false);
        friendsFlowPane.getChildren().clear();

        Task<List<User>> loadFriendsTask = new Task<>() {
            @Override
            protected List<User> call() throws Exception {
                if (currentUser == null)
                    return Collections.emptyList();
                return friendshipService.listFriends(currentUser);
            }
        };

        loadFriendsTask.setOnSucceeded(_ -> {
            allFriendsCache = loadFriendsTask.getValue();
            if (allFriendsCache != null) {
                Collections.sort(allFriendsCache,
                        Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));
            }
            Platform.runLater(() -> {
                if (allFriendsCache == null || allFriendsCache.isEmpty()) {
                    if (pendingRequestersCache == null || pendingRequestersCache.isEmpty()) {
                        showStatus("You have no friends yet. Search for users to add!", false);
                    } else {
                        hideStatus();
                    }
                } else {
                    hideStatus();
                }
                filterFriends("");
            });
        });

        loadFriendsTask.setOnFailed(_ -> {
            Platform.runLater(() -> {
                showStatus("Failed to load friends list.", true);
                AlertFactory.showErrorAlert("Error", "Could not load friends",
                       (loadFriendsTask.getException() != null ? loadFriendsTask.getException().getMessage() : "Unknown error"));
                 if(loadFriendsTask.getException() != null) loadFriendsTask.getException().printStackTrace();
            });
        });
        new Thread(loadFriendsTask).start();
    }

    private void loadPendingRequests() {
        pendingRequestsBox.getChildren().clear();
        pendingRequestsHeader.setVisible(false);
        pendingRequestsHeader.setManaged(false);
        pendingRequestsBox.setVisible(false);
        pendingRequestsBox.setManaged(false);

        Task<List<User>> loadRequestsTask = new Task<>() {
            @Override
            protected List<User> call() throws Exception {
                if (currentUser == null)
                    return Collections.emptyList();
                return friendshipService.listPendingIncomingRequests(currentUser);
            }
        };

        loadRequestsTask.setOnSucceeded(_ -> {
            pendingRequestersCache = loadRequestsTask.getValue();
            if (pendingRequestersCache != null) {
                Collections.sort(pendingRequestersCache,
                        Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));
            }
            Platform.runLater(() -> {
                if (pendingRequestersCache != null && !pendingRequestersCache.isEmpty()) {
                    pendingRequestsHeader.setVisible(true);
                    pendingRequestsHeader.setManaged(true);
                    pendingRequestsBox.setVisible(true);
                    pendingRequestsBox.setManaged(true);
                    pendingRequestersCache.forEach(requester -> pendingRequestsBox.getChildren()
                            .add(createPendingRequestNode(requester)));
                }
            });
        });
        loadRequestsTask.setOnFailed(_ -> {
             if(loadRequestsTask.getException() != null) {
                System.err.println("Failed to load pending requests: " + loadRequestsTask.getException().getMessage());
                loadRequestsTask.getException().printStackTrace();
             } else {
                System.err.println("Failed to load pending requests: Unknown error.");
             }
        });
        new Thread(loadRequestsTask).start();
    }

    private Node createPendingRequestNode(User requester) {
        HBox requestNode = new HBox(10);
        requestNode.setAlignment(Pos.CENTER_LEFT);
        requestNode.setPadding(new Insets(8));
        requestNode.setStyle(
                "-fx-background-color: #FFFFFF; -fx-border-color: #FFD54F; -fx-border-width:1px; -fx-border-radius: 4px; -fx-background-radius: 4px;");

        ImageView avatarView = new ImageView();
        avatarView.setFitHeight(30);
        avatarView.setFitWidth(30);
        Circle clip = new Circle(15, 15, 15);
        avatarView.setClip(clip);
        
        loadAvatarForImageView(avatarView, requester.getProfileImagePath());


        Label nameLabel = new Label(requester.getUsername() + " wants to be your friend.");
        nameLabel.setFont(Font.font("System", FontWeight.NORMAL, 13));
        nameLabel.setTextFill(Color.web("#333333"));
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        HBox buttons = new HBox(5);
        Button acceptButton = new Button("Accept");
        acceptButton.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
        acceptButton.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 4px; -fx-padding: 4 8;");
        acceptButton.setOnAction(
                _ -> handleFriendRequestResponse(requester, true, requestNode, acceptButton, null));

        Button rejectButton = new Button("Reject");
        rejectButton.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
        rejectButton.setStyle(
                "-fx-background-color: #f44336; -fx-text-fill: white; -fx-background-radius: 4px; -fx-padding: 4 8;");
        rejectButton.setOnAction(_ -> handleFriendRequestResponse(requester, false, requestNode,
                null, rejectButton));
        buttons.getChildren().addAll(acceptButton, rejectButton);

        requestNode.getChildren().addAll(avatarView, nameLabel, buttons);
        return requestNode;
    }

    private void handleFriendRequestResponse(User requester, boolean accepted, Node requestNodeUI,
            Button acceptBtn, Button rejectBtn) {
        if (acceptBtn != null)
            acceptBtn.setDisable(true);
        if (rejectBtn != null)
            rejectBtn.setDisable(true);
        if (acceptBtn != null)
            acceptBtn.setText(accepted ? "Accepting..." : "Accept");
        if (rejectBtn != null)
            rejectBtn.setText(!accepted ? "Rejecting..." : "Reject");

        Task<Void> responseTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (currentUser == null || requester == null)
                    throw new FriendshipOperationException(
                            "User data missing for request response.");
                if (accepted) {
                    friendshipService.acceptFriendRequest(currentUser, requester);
                } else {
                    friendshipService.rejectFriendRequest(currentUser, requester);
                }
                return null;
            }
        };
        responseTask.setOnSucceeded(_ -> Platform.runLater(() -> {
            AlertFactory.showInformationAlert("Request Handled", "Success", "Friend request from "
                    + requester.getUsername() + (accepted ? " accepted." : " rejected."));
            refreshViewData();
        }));
        responseTask.setOnFailed(_ -> Platform.runLater(() -> {
            AlertFactory.showErrorAlert("Error", "Failed to handle request",
                    (responseTask.getException() != null ? responseTask.getException().getMessage() : "Unknown error") );
            if (acceptBtn != null) acceptBtn.setDisable(false);
            if (rejectBtn != null) rejectBtn.setDisable(false);
            if (acceptBtn != null) acceptBtn.setText("Accept");
            if (rejectBtn != null) rejectBtn.setText("Reject");
        }));
        new Thread(responseTask).start();
    }

    private void filterFriends(String searchTerm) {
        friendsFlowPane.getChildren().clear();
        if (allFriendsCache == null) {
            if (!statusLabel.isVisible())
                showStatus("No friends loaded.", true);
            return;
        }

        List<User> filteredFriends;
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            filteredFriends = allFriendsCache;
        } else {
            String lowerCaseFilter = searchTerm.trim().toLowerCase();
            filteredFriends = allFriendsCache.stream()
                    .filter(user -> user.getUsername().toLowerCase().contains(lowerCaseFilter)
                            || (user.getFirstName() != null
                                    && user.getFirstName().toLowerCase().contains(lowerCaseFilter))
                            || (user.getLastName() != null
                                    && user.getLastName().toLowerCase().contains(lowerCaseFilter)))
                    .collect(Collectors.toList());
        }

        if (filteredFriends.isEmpty()) {
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                showStatus("No friends found matching '" + searchTerm + "'.", false);
            } else if (allFriendsCache.isEmpty()
                    && (pendingRequestersCache == null || pendingRequestersCache.isEmpty())) {
                showStatus("You have no friends yet. Join gatherings to meet with people!", false);
            } else {
                hideStatus();
            }
        } else {
            hideStatus();
            filteredFriends
                    .forEach(friend -> friendsFlowPane.getChildren().add(createFriendCard(friend)));
        }
    }
    
    private void loadAvatarForImageView(ImageView imageView, String avatarFileNameOrClassPath) {
        Image imageToDisplay = null;
        if (avatarFileNameOrClassPath != null && !avatarFileNameOrClassPath.isBlank()) {
            if (avatarFileNameOrClassPath.startsWith("/")) { // Classpath resource (default avatars)
                try (InputStream classpathStream = getClass().getResourceAsStream(avatarFileNameOrClassPath)) {
                    if (classpathStream != null) {
                        imageToDisplay = new Image(classpathStream);
                    }
                } catch (Exception e) {
                     System.err.println("Error loading classpath avatar " + avatarFileNameOrClassPath + ": " + e.getMessage());
                }
            } else { // File system resource (user-uploaded avatars)
                File avatarFile = new File(AppConfig.getUserAvatarsDir(), avatarFileNameOrClassPath);
                if (avatarFile.exists() && avatarFile.isFile()) {
                    try {
                        imageToDisplay = new Image(avatarFile.toURI().toString());
                    } catch (Exception e){
                         System.err.println("Error loading file system avatar " + avatarFile.getAbsolutePath() + ": " + e.getMessage());
                    }
                }
            }
        }

        if (imageToDisplay == null || imageToDisplay.isError()) {
            try (InputStream defaultStream = getClass().getResourceAsStream(AppConfig.DEFAULT_AVATAR_PATH)) {
                 if(defaultStream != null) imageToDisplay = new Image(defaultStream);
            } catch (Exception e) {
                 System.err.println("Error loading DEFAULT avatar: " + e.getMessage());
            }
        }
        imageView.setImage(imageToDisplay);
    }


    private Node createFriendCard(User friend) {
        VBox card = new VBox(5);
        card.setPrefSize(100, 120);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPadding(new Insets(10));
        card.setStyle(
                "-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-width: 1px; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 1);");
        card.setCursor(Cursor.HAND);

        ImageView profileIcon = new ImageView();
        profileIcon.setFitHeight(60);
        profileIcon.setFitWidth(60);
        Circle clip = new Circle(30, 30, 30);
        profileIcon.setClip(clip);

        loadAvatarForImageView(profileIcon, friend.getProfileImagePath());

        Label usernameLabel = new Label(friend.getUsername());
        usernameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        usernameLabel.setTextFill(Color.web("#333333"));
        usernameLabel.setAlignment(Pos.CENTER);
        usernameLabel.setWrapText(true);

        card.getChildren().addAll(profileIcon, usernameLabel);
        card.setOnMouseClicked(_ -> viewUserProfile(friend.getId()));
        return card;
    }

    private void viewUserProfile(int userId) {
        if (mainPanelController == null || currentUser == null)
            return;
        if (userId == currentUser.getId()) {
            mainPanelController.loadView("profile.fxml");
            mainPanelController.setCurrentViewFxmlPath("profile.fxml");
        } else {
            try {
                FXMLLoaderWrapper.FXMLLoaderResult<OtherProfileController> loaderResult =
                        FXMLLoaderWrapper.loadFXMLWithController("other_profile_panel.fxml");
                OtherProfileController profileController = loaderResult.controller;
                profileController.setMainPanelController(mainPanelController);
                profileController.setPreviousViewFxml("friends_panel.fxml");
                profileController.loadUserProfile(userId);
                mainPanelController.setCurrentViewFxmlPath("other_profile_panel.fxml");
                mainPanelController.setContent(loaderResult.parent);
            } catch (IOException e) {
                AlertFactory.showErrorAlert("Navigation Error", "Could not load friend's profile.",
                        e.getMessage());
            }
        }
    }
}