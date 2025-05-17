package com.palveo.gui.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import com.palveo.config.AppConfig;
import com.palveo.dao.FriendshipDao;
import com.palveo.dao.impl.FriendshipDaoImpl;
import com.palveo.gui.manager.SessionManager;
import com.palveo.gui.util.AlertFactory;
import com.palveo.gui.util.FXMLLoaderWrapper;
import com.palveo.model.Comment;
import com.palveo.model.Event;
import com.palveo.model.Friendship;
import com.palveo.model.User;
import com.palveo.service.CommentService;
import com.palveo.service.EventService;
import com.palveo.service.FriendshipService;
import com.palveo.service.RatingService;
import com.palveo.service.TagService;
import com.palveo.service.UserService;
import com.palveo.service.exception.FriendshipOperationException;
import com.palveo.service.exception.RatingOperationException;
import com.palveo.service.exception.TagOperationException;
import com.palveo.service.exception.UserNotFoundException;
import com.palveo.service.impl.CommentServiceImpl;
import com.palveo.service.impl.EventServiceImpl;
import com.palveo.service.impl.FriendshipServiceImpl;
import com.palveo.service.impl.RatingServiceImpl;
import com.palveo.service.impl.TagServiceImpl;
import com.palveo.service.impl.UserServiceImpl;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class OtherProfileController {

    @FXML
    private Label profileHeaderLabel;
    @FXML
    private Button backButton;
    @FXML
    private ImageView profileImageView;
    @FXML
    private Label usernameLabel;
    @FXML
    private Label fullNameLabel;
    @FXML
    private TextFlow tagsTextFlow;
    @FXML
    private Label averageRatingLabel;
    @FXML
    private Label bioLabel;
    @FXML
    private Button rateButton;
    @FXML
    private Button tagButton;
    @FXML
    private Button friendButton;
    @FXML
    private VBox hostedGatheringsContainer;
    @FXML
    private Label noHostedGatheringsLabel;
    @FXML
    private HBox actionButtonsBox;

    @FXML    private VBox commentsContainer;
    @FXML    private Label noCommentsLabel;
    @FXML    private Button addCommentButton;

    User displayedUser;
    private User currentUser;
    private MainPanelController mainPanelController;
    private SessionManager sessionManager;
    private UserService userService;
    private EventService eventService;
    private TagService tagService;
    private RatingService ratingService;
    private FriendshipService friendshipService;
    private FriendshipDao friendshipDao;
    private CommentService commentService;
    private String previousViewFxml;

    public OtherProfileController() {
        this.sessionManager = SessionManager.getInstance();
        this.userService = new UserServiceImpl();
        this.eventService = new EventServiceImpl();
        this.tagService = new TagServiceImpl();
        this.ratingService = new RatingServiceImpl();
        this.friendshipService = new FriendshipServiceImpl();
        this.friendshipDao = new FriendshipDaoImpl();
        this.commentService = new CommentServiceImpl();
    }

    public void setMainPanelController(MainPanelController mainPanelController) {
        this.mainPanelController = mainPanelController;
    }

    public void setPreviousViewFxml(String fxmlPath) {
        this.previousViewFxml = fxmlPath;
    }

    @FXML
    private void initialize() {
        this.currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            Platform.runLater(() -> {
                if (mainPanelController != null) {
                    mainPanelController.loadView("login.fxml");
                    mainPanelController.setCurrentViewFxmlPath("login.fxml");
                }
            });
        }
        if (addCommentButton != null) {
            addCommentButton.setDisable(true);
        }
    }
    
    public void refreshViewData() {
        if (this.displayedUser != null) {
            loadUserProfile(this.displayedUser.getId());
        }
    }

    public void loadUserProfile(int userIdToView) {
        if (currentUser == null) {
            Platform.runLater(() -> {
                if (mainPanelController != null) {
                    mainPanelController.loadView("login.fxml");
                    mainPanelController.setCurrentViewFxmlPath("login.fxml");
                }
            });
            return;
        }

        if (userIdToView == currentUser.getId()) {
            Platform.runLater(() -> {
                if (mainPanelController != null) {
                    mainPanelController.loadView("profile.fxml");
                    mainPanelController.setCurrentViewFxmlPath("profile.fxml");
                }
            });
            return;
        }

        Task<Optional<User>> loadUserTask = new Task<>() {
            @Override
            protected Optional<User> call() throws Exception {
                return userService.getUserById(userIdToView);
            }
        };

        loadUserTask.setOnSucceeded(_ -> {
            Optional<User> userOpt = loadUserTask.getValue();
            if (userOpt.isPresent()) {
                this.displayedUser = userOpt.get();
                Platform.runLater(() -> {
                    populateProfileDetails();
                    if (addCommentButton != null)
                        addCommentButton.setDisable(false);
                });
            } else {
                Platform.runLater(() -> {
                    AlertFactory.showErrorAlert("Error", "User Not Found",
                            "The requested user profile could not be loaded.");
                    if (mainPanelController != null) {
                        mainPanelController.loadView(
                                previousViewFxml != null ? previousViewFxml : "home_feed.fxml");
                        mainPanelController.setCurrentViewFxmlPath(
                                previousViewFxml != null ? previousViewFxml : "home_feed.fxml");
                    }
                });
            }
        });
        loadUserTask.setOnFailed(_ -> {
            Throwable ex = loadUserTask.getException();
            Platform.runLater(() -> {
                AlertFactory.showErrorAlert("Error", "Loading Profile Failed",
                        "Could not load user profile: " + (ex != null ? ex.getMessage() : "Unknown error"));
                if (mainPanelController != null) {
                    mainPanelController.loadView(
                            previousViewFxml != null ? previousViewFxml : "home_feed.fxml");
                     mainPanelController.setCurrentViewFxmlPath(
                                previousViewFxml != null ? previousViewFxml : "home_feed.fxml");
                }
                if(ex != null) ex.printStackTrace();
            });
        });
        new Thread(loadUserTask).start();
    }

    private void populateProfileDetails() {
        if (displayedUser == null)
            return;

        profileHeaderLabel.setText(displayedUser.getUsername() + "'s Profile");
        usernameLabel.setText(displayedUser.getUsername());

        String fName = displayedUser.getFirstName() == null ? "" : displayedUser.getFirstName();
        String lName = displayedUser.getLastName() == null ? "" : displayedUser.getLastName();
        String currentFullName = (fName + " " + lName).trim();

        if (!currentFullName.isEmpty()) {
            fullNameLabel.setText(currentFullName);
            fullNameLabel.setVisible(true);
            fullNameLabel.setManaged(true);
        } else {
            fullNameLabel.setText("");
            fullNameLabel.setVisible(false);
            fullNameLabel.setManaged(false);
        }

        bioLabel.setText(displayedUser.getBio() != null && !displayedUser.getBio().isEmpty()
                ? displayedUser.getBio()
                : "No bio set yet.");

        loadAvatarForImageView(profileImageView, displayedUser.getProfileImagePath());

        loadUserTagsAndRating();
        loadHostedGatherings();
        loadProfileComments();
        updateFriendButtonState();
    }
    
    private void loadAvatarForImageView(ImageView imageView, String avatarFileNameOrClassPath) {
        Image imageToDisplay = null;
        if (avatarFileNameOrClassPath != null && !avatarFileNameOrClassPath.isBlank()) {
            if (avatarFileNameOrClassPath.startsWith("/")) {
                try (InputStream classpathStream = getClass().getResourceAsStream(avatarFileNameOrClassPath)) {
                    if (classpathStream != null) {
                        imageToDisplay = new Image(classpathStream);
                    }
                } catch (Exception e) {
                     System.err.println("OtherProfileController: Error loading classpath avatar " + avatarFileNameOrClassPath + ": " + e.getMessage());
                }
            } else { 
                File avatarFile = new File(AppConfig.getUserAvatarsDir(), avatarFileNameOrClassPath);
                if (avatarFile.exists() && avatarFile.isFile()) {
                    try {
                        imageToDisplay = new Image(avatarFile.toURI().toString());
                    } catch (Exception e){
                         System.err.println("OtherProfileController: Error loading file system avatar " + avatarFile.getAbsolutePath() + ": " + e.getMessage());
                    }
                }
            }
        }

        if (imageToDisplay == null || imageToDisplay.isError()) {
            try (InputStream defaultStream = getClass().getResourceAsStream(AppConfig.DEFAULT_AVATAR_PATH)) {
                 if(defaultStream != null) imageToDisplay = new Image(defaultStream);
            } catch (Exception e) {
                 System.err.println("OtherProfileController: Error loading DEFAULT avatar: " + e.getMessage());
            }
        }
        imageView.setImage(imageToDisplay);
    }


    @SuppressWarnings("unused")
    private void setFallbackAvatar() {
        try (InputStream fallbackStream =
                getClass().getResourceAsStream(AppConfig.DEFAULT_AVATAR_PATH)) {
            if (fallbackStream != null)
                profileImageView.setImage(new Image(fallbackStream));
            else
                profileImageView.setImage(null);
        } catch (Exception ex) {
            profileImageView.setImage(null);
        }
    }

    private void loadProfileComments() {
        if (displayedUser == null || commentsContainer == null) {
            return;
        }

        commentsContainer.getChildren().clear();
        noCommentsLabel.setVisible(false);
        noCommentsLabel.setManaged(false);

        Label loadingCommentsLabel = new Label("Loading comments...");
        loadingCommentsLabel
                .setStyle("-fx-text-fill: #757575; -fx-font-style: italic; -fx-font-size: 13px;");
        commentsContainer.getChildren().add(loadingCommentsLabel);

        Task<List<Comment>> loadCommentsTask = new Task<>() {
            @Override
            protected List<Comment> call() throws Exception {
                return commentService.getCommentsForUserProfile(displayedUser.getId());
            }
        };

        loadCommentsTask.setOnSucceeded(_ -> {
            List<Comment> comments = loadCommentsTask.getValue();
            Platform.runLater(() -> {
                if (commentsContainer.getChildren().contains(loadingCommentsLabel)) {
                    commentsContainer.getChildren().remove(loadingCommentsLabel);
                }
                if (comments == null || comments.isEmpty()) {
                    noCommentsLabel.setText("No comments on this profile yet.");
                    noCommentsLabel.setVisible(true);
                    noCommentsLabel.setManaged(true);
                } else {
                    comments.sort(Comparator.comparing(Comment::getCreatedAt).reversed());
                    comments.forEach(comment -> commentsContainer.getChildren()
                            .add(createCommentNode(comment)));
                }
            });
        });

        loadCommentsTask.setOnFailed(_ -> {
            Throwable ex = loadCommentsTask.getException();
            Platform.runLater(() -> {
                if (commentsContainer.getChildren().contains(loadingCommentsLabel)) {
                    commentsContainer.getChildren().remove(loadingCommentsLabel);
                }
                noCommentsLabel
                        .setText(
                                "Failed to load comments." + (ex != null
                                        ? " (" + ex.getMessage().substring(0,
                                                Math.min(ex.getMessage().length(), 50)) + "...)"
                                        : ""));
                noCommentsLabel.setVisible(true);
                noCommentsLabel.setManaged(true);
                if (ex != null)
                    ex.printStackTrace();
            });
        });
        new Thread(loadCommentsTask).start();
    }

    private Node createCommentNode(Comment comment) {
        VBox commentNode = new VBox(5);
        commentNode.setPadding(new Insets(10));
        commentNode.setStyle(
                "-fx-background-color: #f9f9f9; -fx-border-color: #e9e9e9; -fx-border-width: 0 0 1 0;");
        commentNode.setPrefWidth(Double.MAX_VALUE);

        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label authorLabel = new Label(
                comment.getAuthorUsername() != null ? comment.getAuthorUsername() : "Unknown User");
        authorLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        authorLabel.setTextFill(Color.web("#007bff"));
        authorLabel.setCursor(Cursor.HAND);
        authorLabel.setOnMouseClicked(_ -> {
            if (mainPanelController != null && comment.getAuthorUserId() != currentUser.getId()) {
                try {
                    FXMLLoaderWrapper.FXMLLoaderResult<OtherProfileController> loaderResult =
                            FXMLLoaderWrapper.loadFXMLWithController("other_profile_panel.fxml");
                    OtherProfileController otherProfileController = loaderResult.controller;
                    otherProfileController.setMainPanelController(mainPanelController);
                    otherProfileController.setPreviousViewFxml("other_profile_panel.fxml");
                    otherProfileController.loadUserProfile(comment.getAuthorUserId());
                    mainPanelController.setCurrentViewFxmlPath("other_profile_panel.fxml");
                    mainPanelController.setContent(loaderResult.parent);
                } catch (IOException ioEx) {
                    AlertFactory.showErrorAlert("Navigation Error", "Could not load user profile.",
                            ioEx.getMessage());
                }
            } else if (mainPanelController != null && comment.getAuthorUserId() == currentUser.getId()) {
                 mainPanelController.loadView("profile.fxml");
                 mainPanelController.setCurrentViewFxmlPath("profile.fxml");
            }
        });

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm");
        String formattedDate =
                comment.getCreatedAt() != null ? comment.getCreatedAt().format(formatter)
                        : "Date unknown";
        if (comment.isEdited()) {
            formattedDate += " (edited)";
        }
        Label dateLabel = new Label(formattedDate);
        dateLabel.setFont(Font.font("System", 11));
        dateLabel.setTextFill(Color.web("#757575"));

        headerBox.getChildren().addAll(authorLabel, dateLabel);

        Text contentTextNode = new Text(comment.getContent());
        contentTextNode.setWrappingWidth(340);
        contentTextNode.setStyle("-fx-fill: #333333; -fx-font-size: 13px;");

        TextFlow contentFlow = new TextFlow(contentTextNode);
        contentFlow.setPadding(new Insets(3, 0, 0, 0));

        if (comment.getAuthorUserId() == currentUser.getId()
                || displayedUser.getId() == currentUser.getId()) {
            Button deleteButton = new Button("Delete");
            deleteButton.setFont(Font.font("System", FontWeight.NORMAL, 10));
            deleteButton.setStyle(
                    "-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-padding: 2 6; -fx-background-radius: 4px; -fx-cursor: hand;");
            deleteButton.setOnAction(_ -> handleDeleteCommentAction(comment, commentNode));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            headerBox.getChildren().addAll(spacer, deleteButton);
        }

        commentNode.getChildren().addAll(headerBox, contentFlow);
        return commentNode;
    }

    private void handleDeleteCommentAction(Comment comment, Node commentNodeUI) {
        AlertFactory
                .showConfirmationAlert("Delete Comment", "Are you sure?",
                        "Do you want to delete this comment permanently?")
                .filter(response -> response == ButtonType.OK).ifPresent(_ -> {
                    Task<Void> deleteTask = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            commentService.deleteComment(comment.getCommentId(), currentUser);
                            return null;
                        }
                    };
                    deleteTask.setOnSucceeded(_ -> Platform.runLater(this::loadProfileComments));
                    deleteTask.setOnFailed(_ -> {
                        Platform.runLater(() -> AlertFactory.showErrorAlert("Error",
                                "Failed to delete comment",
                                deleteTask.getException().getMessage()));
                    });
                    new Thread(deleteTask).start();
                });
    }

    @FXML
    private void handleAddCommentAction(ActionEvent event) {
        if (displayedUser == null || currentUser == null) {
            AlertFactory.showErrorAlert("Error", "Cannot add comment.", "User data not loaded.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Comment");
        dialog.setHeaderText("Write a comment for " + displayedUser.getUsername() + "'s profile:");
        dialog.setContentText("Comment:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(commentContent -> {
            String trimmedComment = commentContent.trim();
            if (trimmedComment.isEmpty()) {
                AlertFactory.showWarningAlert("Input Error", "Comment cannot be empty.", null);
                return;
            }
            if (trimmedComment.length() > 2000) {
                    AlertFactory.showWarningAlert("Input Error", "Comment is too long.", "Maximum 2000 characters allowed.");
                return;
            }

            Task<Void> postCommentTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    commentService.postCommentToUserProfile(displayedUser.getId(), currentUser,
                            trimmedComment);
                    return null;
                }
            };
            postCommentTask.setOnSucceeded(_ -> Platform.runLater(() -> {
                AlertFactory.showInformationAlert("Comment Posted", "Success",
                        "Your comment has been posted.");
                loadProfileComments();
            }));
            postCommentTask.setOnFailed(f -> Platform.runLater(() -> {
                AlertFactory.showErrorAlert("Comment Error", "Could not post comment.",
                        postCommentTask.getException().getMessage());
                if (f.getSource().getException() != null)
                    f.getSource().getException().printStackTrace();
            }));
            new Thread(postCommentTask).start();
        });
    }

    private void updateFriendButtonState() {
        if (currentUser == null || displayedUser == null
                || currentUser.getId() == displayedUser.getId()) {
            if (friendButton != null && actionButtonsBox.getChildren().contains(friendButton)) {
                actionButtonsBox.getChildren().remove(friendButton);
            }
            return;
        }
        if (friendButton != null && !actionButtonsBox.getChildren().contains(friendButton)) {
            int targetIndex = actionButtonsBox.getChildren().size();
            if (actionButtonsBox.getChildren().contains(tagButton)) {
                targetIndex = actionButtonsBox.getChildren().indexOf(tagButton) + 1;
            } else if (actionButtonsBox.getChildren().contains(rateButton)) {
                targetIndex = actionButtonsBox.getChildren().indexOf(rateButton) + 1;
            } else {
                targetIndex = 0;
            }
            actionButtonsBox.getChildren().add(targetIndex, friendButton);
        }
        if (friendButton == null)
            return;

        friendButton.setText("Loading...");
        friendButton.setDisable(true);
        String baseButtonStyle =
                "-fx-font-size: 12px; -fx-padding: 6 10; -fx-background-radius: 6px; ";
        friendButton
                .setStyle(baseButtonStyle + "-fx-background-color: #AEB6BF; -fx-text-fill: white;");

        Task<Friendship.FriendshipStatus> statusTask = new Task<>() {
            @Override
            protected Friendship.FriendshipStatus call() throws Exception {
                if (currentUser == null || displayedUser == null)
                    return null;
                return friendshipService.getFriendshipStatus(currentUser, displayedUser);
            }
        };

        statusTask.setOnSucceeded(_ -> {
            Friendship.FriendshipStatus status = statusTask.getValue();
            Platform.runLater(() -> {
                friendButton.setDisable(false);
                if (status == null) {
                    friendButton.setText("Add Friend");
                    friendButton.setStyle(baseButtonStyle
                            + "-fx-background-color: #28a745; -fx-text-fill: white;");
                } else {
                    switch (status) {
                        case PENDING:
                            Task<Optional<Friendship>> fsDetailsTask = new Task<>() {
                                @Override
                                protected Optional<Friendship> call() throws Exception {
                                    return friendshipDao.findFriendship(currentUser.getId(),
                                            displayedUser.getId());
                                }
                            };
                            fsDetailsTask.setOnSucceeded(_ -> {
                                Optional<Friendship> fsOpt = fsDetailsTask.getValue();
                                Platform.runLater(() -> {
                                    if (fsOpt.isPresent() && fsOpt.get()
                                            .getActionUserId() == currentUser.getId()) {
                                        friendButton.setText("Request Sent");
                                        friendButton.setDisable(true);
                                        friendButton.setStyle(baseButtonStyle
                                                + "-fx-background-color: #ffc107; -fx-text-fill: black;");
                                    } else if (fsOpt.isPresent() && fsOpt.get()
                                            .getActionUserId() == displayedUser.getId()) {
                                        friendButton.setText("Respond to Request");
                                        friendButton.setStyle(baseButtonStyle
                                                + "-fx-background-color: #007bff; -fx-text-fill: white;");
                                    } else {
                                        friendButton.setText("Pending");
                                        friendButton.setDisable(true);
                                        friendButton.setStyle(baseButtonStyle
                                                + "-fx-background-color: #ffc107; -fx-text-fill: black;");
                                    }
                                });
                            });
                            fsDetailsTask.setOnFailed(_-> friendButton.setText("Error"));
                            new Thread(fsDetailsTask).start();
                            break;
                        case ACCEPTED:
                            friendButton.setText("Friends");
                            friendButton.setStyle(baseButtonStyle
                                    + "-fx-background-color: #6c757d; -fx-text-fill: white;");
                            break;
                        case DECLINED:
                        case BLOCKED:
                            if(status == Friendship.FriendshipStatus.DECLINED){
                                friendButton.setText("Add Friend");
                                friendButton.setStyle(baseButtonStyle + "-fx-background-color: #28a745; -fx-text-fill: white;");
                                break;
                            }
                            Task<Optional<Friendship>> blockedFsDetailsTask = new Task<>() {
                                @Override
                                protected Optional<Friendship> call() throws Exception {
                                    return friendshipDao.findFriendship(currentUser.getId(),
                                            displayedUser.getId());
                                }
                            };
                            blockedFsDetailsTask.setOnSucceeded(_-> {
                                Optional<Friendship> fsOpt = blockedFsDetailsTask.getValue();
                                Platform.runLater(() -> {
                                    if (fsOpt.isPresent() && fsOpt.get().getStatus() == Friendship.FriendshipStatus.BLOCKED && fsOpt.get()
                                            .getActionUserId() == currentUser.getId()) {
                                        friendButton.setText("Unblock");
                                        friendButton.setStyle(baseButtonStyle
                                                + "-fx-background-color: #17a2b8; -fx-text-fill: white;");
                                    } else {
                                        friendButton.setText("Blocked");
                                        friendButton.setDisable(true);
                                        friendButton.setStyle(baseButtonStyle
                                                + "-fx-background-color: #dc3545; -fx-text-fill: white;");
                                    }
                                });
                            });
                            blockedFsDetailsTask.setOnFailed(_-> friendButton.setText("Error"));
                            new Thread(blockedFsDetailsTask).start();
                            break;
                    }
                }
            });
        });
        statusTask.setOnFailed(_ -> Platform.runLater(() -> {
            friendButton.setText("Status N/A");
            friendButton.setDisable(false);
            friendButton.setStyle(
                    baseButtonStyle + "-fx-background-color: #AEB6BF; -fx-text-fill: white;");
            if(statusTask.getException() != null) statusTask.getException().printStackTrace();
        }));
        new Thread(statusTask).start();
    }

    @FXML
    private void handleBackButtonAction(ActionEvent event) {
        if (mainPanelController != null) {
            String viewToLoad =
                    (previousViewFxml != null && !previousViewFxml.isEmpty()) ? previousViewFxml
                            : "home_feed.fxml";
            
            mainPanelController.loadView(viewToLoad, controller -> {
                if (controller instanceof EventDetailsController && "event_details_panel.fxml".equals(viewToLoad) ) {
                     EventDetailsController edc = (EventDetailsController) controller;
                     if (edc.getCurrentEvent() != null) edc.refreshViewData();
                } else if (controller instanceof ProfileController && "profile.fxml".equals(viewToLoad)) {
                    ((ProfileController) controller).refreshViewData();
                } else if (controller instanceof HomeFeedController && "home_feed.fxml".equals(viewToLoad)){
                     ((HomeFeedController) controller).refreshViewData();
                } else if (controller instanceof FriendsPanelController && "friends_panel.fxml".equals(viewToLoad)) {
                    ((FriendsPanelController) controller).refreshViewData();
                }
            });

        }
    }


    @FXML
    private void handleRateUserAction(ActionEvent event) {
        if (displayedUser == null || currentUser == null
                || currentUser.getId() == displayedUser.getId()) {
            AlertFactory.showWarningAlert("Action Not Allowed", "You cannot rate yourself.", null);
            return;
        }

        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Rate User: " + displayedUser.getUsername());
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Node okButtonNode = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButtonNode.setDisable(true);
        VBox dialogContent = new VBox(15);
        dialogContent.setPadding(new Insets(20));
        dialogContent.setPrefWidth(350);

        Label scoreHeader = new Label("How would you rate " + displayedUser.getUsername() + "?");
        scoreHeader.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox starBox = new HBox(5);
        starBox.setAlignment(Pos.CENTER);
        ImageView[] stars = new ImageView[5];
        final int[] currentRatingHolder = {0};
        Image imgStarEmpty = null, imgStarFilled = null;
        try (InputStream emptyStream = getClass().getResourceAsStream("/images/star_empty.png")) {
            if (emptyStream != null)
                imgStarEmpty = new Image(emptyStream);
        } catch (Exception e) {}
        try (InputStream filledStream = getClass().getResourceAsStream("/images/star_filled.png")) {
            if (filledStream != null)
                imgStarFilled = new Image(filledStream);
        } catch (Exception e) {}
        final Image finalImgStarEmpty = imgStarEmpty;
        final Image finalImgStarFilled = imgStarFilled;

        for (int i = 0; i < 5; i++) {
            stars[i] = new ImageView();
            stars[i].setFitHeight(28);
            stars[i].setFitWidth(28);
            if (finalImgStarEmpty != null && !finalImgStarEmpty.isError())
                stars[i].setImage(finalImgStarEmpty);
            else
                stars[i].setImage(null);
            stars[i].setCursor(Cursor.HAND);
            final int ratingValue = i + 1;

            stars[i].setOnMouseEntered(_ -> {
                for (int j = 0; j < 5; j++) {
                    Image targetImg = (j < ratingValue && finalImgStarFilled != null
                            && !finalImgStarFilled.isError()) ? finalImgStarFilled
                                    : finalImgStarEmpty;
                    if (targetImg != null && !targetImg.isError())
                        stars[j].setImage(targetImg);
                }
            });
            stars[i].setOnMouseExited(_ -> {
                for (int j = 0; j < 5; j++) {
                    Image targetImg = (j < currentRatingHolder[0] && finalImgStarFilled != null
                            && !finalImgStarFilled.isError()) ? finalImgStarFilled
                                    : finalImgStarEmpty;
                    if (targetImg != null && !targetImg.isError())
                        stars[j].setImage(targetImg);
                }
            });
            stars[i].setOnMouseClicked(_ -> {
                currentRatingHolder[0] = ratingValue;
                for (int j = 0; j < 5; j++) {
                    Image targetImg = (j < currentRatingHolder[0] && finalImgStarFilled != null
                            && !finalImgStarFilled.isError()) ? finalImgStarFilled
                                    : finalImgStarEmpty;
                    if (targetImg != null && !targetImg.isError())
                        stars[j].setImage(targetImg);
                }
                okButtonNode.setDisable(false);
            });
            starBox.getChildren().add(stars[i]);
        }

        Label commentHeader = new Label("Optional Comment:");
        commentHeader.setFont(Font.font("System", FontWeight.NORMAL, 13));
        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Max 1000 characters...");
        commentArea.setWrapText(true);
        commentArea.setPrefRowCount(4);
        commentArea.setStyle(
                "-fx-font-size: 13px; -fx-control-inner-background: #FFFFFF; -fx-border-color: #E0E0E0; -fx-border-width: 1px; -fx-border-radius: 4px; -fx-prompt-text-fill: #AAAAAA;");

        dialogContent.getChildren().addAll(scoreHeader, starBox, commentHeader, commentArea);
        dialog.getDialogPane().setContent(dialogContent);

        Platform.runLater(starBox::requestFocus);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return currentRatingHolder[0];
            }
            return 0;
        });

        Optional<Integer> result = dialog.showAndWait();

        result.filter(score -> score > 0).ifPresent(score -> {
            String comment = commentArea.getText();
            Task<Void> rateTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ratingService.submitRatingForUser(displayedUser.getId(), currentUser, score,
                            comment);
                    return null;
                }
            };
            rateTask.setOnSucceeded(_ -> Platform.runLater(() -> {
                AlertFactory.showInformationAlert("Rating Submitted", "Success",
                        "Your rating for " + displayedUser.getUsername() + " has been submitted.");
                loadUserTagsAndRating();
                loadProfileComments(); 
            }));
            rateTask.setOnFailed(_ -> Platform.runLater(() -> {
                AlertFactory.showErrorAlert("Rating Error", "Could not submit rating.",
                       (rateTask.getException()!=null ? rateTask.getException().getMessage(): "Unknown error."));
                 if(rateTask.getException() != null) rateTask.getException().printStackTrace();
            }));
            new Thread(rateTask).start();
        });
    }

    @FXML
    private void handleTagUserAction(ActionEvent event) {
        if (displayedUser == null || currentUser == null
                || currentUser.getId() == displayedUser.getId()) {
            AlertFactory.showWarningAlert("Action Not Allowed", "You cannot tag yourself.", null);
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tag User");
        dialog.setHeaderText("Enter a tag for " + displayedUser.getUsername()
                + " (e.g., helpful, funny).\nOne tag at a time.");
        dialog.setContentText("Tag name:");
        dialog.getEditor().setStyle(
                "-fx-pref-height: 38px; -fx-font-size: 13px; -fx-border-color: #E0E0E0; -fx-border-radius:4px; -fx-background-color: #FFFFFF;");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(tagName -> {
            String trimmedTagName = tagName.trim();
            if (trimmedTagName.isEmpty()) {
                AlertFactory.showWarningAlert("Input Error", "Tag name cannot be empty.", null);
                return;
            }
            if (trimmedTagName.length() > 50) {
                AlertFactory.showWarningAlert("Input Error", "Tag name too long.",
                        "Maximum 50 characters allowed for a tag.");
                return;
            }

            Task<Void> tagTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    tagService.applyTagToUser(displayedUser.getId(), trimmedTagName, currentUser);
                    return null;
                }
            };
            tagTask.setOnSucceeded(_ -> Platform.runLater(() -> {
                AlertFactory.showInformationAlert("Tag Applied", "Success",
                        "Tag '" + trimmedTagName + "' applied to " + displayedUser.getUsername());
                loadUserTagsAndRating();
            }));
            tagTask.setOnFailed(f -> Platform.runLater(() -> {
                AlertFactory.showErrorAlert("Tagging Error",
                        "Could not apply tag: " + (f.getSource().getException() != null ? f.getSource().getException().getMessage() : "Unknown error") , null);
                if (f.getSource().getException() != null)
                    f.getSource().getException().printStackTrace();
            }));
            new Thread(tagTask).start();
        });
    }

    @FXML
    private void handleFriendAction(ActionEvent event) {
        if (displayedUser == null || currentUser == null
                || currentUser.getId() == displayedUser.getId())
            return;

        final String currentButtonText = friendButton.getText();
        String originalStyle = friendButton.getStyle();
        friendButton.setDisable(true);
        friendButton.setText("Processing...");
        Task<String> friendActionTask = new Task<>() {
            String successMessage = "";

            @Override
            protected String call() throws Exception {
                if (currentUser == null || displayedUser == null)
                    throw new FriendshipOperationException("User session invalid.");
                if ("Add Friend".equals(currentButtonText)) {
                    friendshipService.sendFriendRequest(currentUser, displayedUser);
                    successMessage = "Friend request sent to " + displayedUser.getUsername();
                } else if ("Respond to Request".equals(currentButtonText)) {
                    friendshipService.acceptFriendRequest(currentUser, displayedUser); 
                    successMessage = "You are now friends with " + displayedUser.getUsername();
                } else if ("Unblock".equals(currentButtonText)) {
                    friendshipService.unblockUser(currentUser, displayedUser);
                    successMessage = displayedUser.getUsername() + " has been unblocked.";
                } else {
                    throw new UnsupportedOperationException(
                            "Unhandled friend button action: " + currentButtonText);
                }
                return successMessage;
            }
        };

        if ("Friends".equals(currentButtonText)) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Unfriend User");
            confirmation.setHeaderText("Unfriend " + displayedUser.getUsername() + "?");
            confirmation.setContentText("Are you sure you want to remove "
                    + displayedUser.getUsername() + " from your friends list?");
            confirmation.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

            Optional<ButtonType> response = confirmation.showAndWait();
            if (response.isPresent() && response.get() == ButtonType.YES) {
                Task<Void> unfriendTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        if (currentUser == null || displayedUser == null)
                            throw new FriendshipOperationException(
                                    "User session invalid for unfriend.");
                        friendshipService.removeFriend(currentUser, displayedUser);
                        return null;
                    }
                    @Override protected void succeeded() { Platform.runLater(OtherProfileController.this::updateFriendButtonState);}
                    @Override protected void failed() { Platform.runLater(OtherProfileController.this::updateFriendButtonState);}
                };
                new Thread(unfriendTask).start();
            } else {
                friendButton.setDisable(false);
                friendButton.setText(currentButtonText);
                friendButton.setStyle(originalStyle);
            }
            return;
        }

        friendActionTask.setOnSucceeded(_ -> Platform.runLater(() -> {
            String msg = friendActionTask.getValue();
            if (msg != null && !msg.isEmpty()) {
                AlertFactory.showInformationAlert("Friendship Update", "Success", msg);
            }
            updateFriendButtonState();
        }));

        friendActionTask.setOnFailed(_ -> Platform.runLater(() -> {
            Throwable ex = friendActionTask.getException();
            if (!(ex instanceof UnsupportedOperationException)) {
                 if (ex != null) AlertFactory.showErrorAlert("Friendship Error", "Operation failed.", ex.getMessage());
                 else AlertFactory.showErrorAlert("Friendship Error", "Operation failed.", "Unknown error.");
            }
            if(ex != null) ex.printStackTrace();
            updateFriendButtonState();
        }));
        new Thread(friendActionTask).start();
    }

    private void loadUserTagsAndRating() {
        if (displayedUser == null)
            return;
        tagsTextFlow.getChildren().clear();
        try {
            List<com.palveo.model.Tag> userTags = tagService.getTagsForUser(displayedUser.getId());
            if (userTags.isEmpty()) {
                Text noTagsText = new Text("No tags yet.");
                noTagsText.setFill(Color.GRAY);
                noTagsText.setFont(Font.font("System", 12));
                tagsTextFlow.getChildren().add(noTagsText);
            } else {
                userTags.forEach(tag -> {
                    Label tagLabel = new Label(tag.getTagName());
                    tagLabel.setStyle( "-fx-background-color: #ECEFF1; -fx-text-fill: #37474F; -fx-padding: 3 8; -fx-background-radius: 12; -fx-font-size: 11px;");
                    tagsTextFlow.getChildren().addAll(tagLabel, new Text(" "));
                });
            }
        } catch (UserNotFoundException | TagOperationException e) {
            Text errorText = new Text("Could not load tags.");
            errorText.setFill(Color.RED);
            tagsTextFlow.getChildren().add(errorText);
        }

        averageRatingLabel.setGraphic(null);
        averageRatingLabel.setText("");
        try {
            double avgRating = ratingService.calculateAverageRatingForUser(displayedUser.getId());
            HBox ratingDisplayBox = new HBox(3);
            ratingDisplayBox.setAlignment(Pos.CENTER);

            Image starFilledImg = null, starHalfImg = null, starEmptyImg = null;
            try (InputStream filledStream =
                    getClass().getResourceAsStream("/images/star_filled.png")) {
                if (filledStream != null)
                    starFilledImg = new Image(filledStream);
            } catch (Exception e) {}
            try (InputStream halfStream = getClass().getResourceAsStream("/images/star_half.png")) {
                if (halfStream != null)
                    starHalfImg = new Image(halfStream);
            } catch (Exception e) {}
            try (InputStream emptyStream =
                    getClass().getResourceAsStream("/images/star_empty.png")) {
                if (emptyStream != null)
                    starEmptyImg = new Image(emptyStream);
            } catch (Exception e) {}

            for (int i = 1; i <= 5; i++) {
                ImageView star = new ImageView();
                star.setFitHeight(16);
                star.setFitWidth(16);
                Image selectedStarImage =
                        (starEmptyImg != null && !starEmptyImg.isError()) ? starEmptyImg : null;

                if (avgRating >= i) {
                    if (starFilledImg != null && !starFilledImg.isError())
                        selectedStarImage = starFilledImg;
                } else if (avgRating >= i - 0.75 && avgRating > i - 1) {
                    if (starHalfImg != null && !starHalfImg.isError())
                        selectedStarImage = starHalfImg;
                    else if (starFilledImg != null && !starFilledImg.isError())
                        selectedStarImage = starFilledImg;
                }

                if (selectedStarImage == null) {
                    Label starFallback = new Label("*");
                    starFallback.setFont(Font.font("System", FontWeight.BOLD, 16));
                    starFallback.setTextFill(avgRating >= i ? Color.GOLD : Color.LIGHTGRAY);
                    ratingDisplayBox.getChildren().add(starFallback);
                } else {
                    star.setImage(selectedStarImage);
                    ratingDisplayBox.getChildren().add(star);
                }
            }
            if (avgRating > 0) {
                Label ratingValueText = new Label(String.format(" (%.1f)", avgRating));
                ratingValueText.setStyle("-fx-font-size: 12px; -fx-text-fill: #444;");
                ratingDisplayBox.getChildren().add(ratingValueText);
            } else {
                Label noRatingText = new Label(" Not rated yet");
                noRatingText.setStyle(
                        "-fx-font-size: 12px; -fx-text-fill: #757575; -fx-font-style: italic;");
                ratingDisplayBox.getChildren().add(noRatingText);
            }
            averageRatingLabel.setGraphic(ratingDisplayBox);

        } catch (UserNotFoundException | RatingOperationException e) {
            averageRatingLabel.setGraphic(null);
            averageRatingLabel.setText("Rating N/A");
        }
    }

    private void loadHostedGatherings() {
        if (displayedUser == null)
            return;
        hostedGatheringsContainer.getChildren().clear();
        noHostedGatheringsLabel.setVisible(false);
        noHostedGatheringsLabel.setManaged(false);

        Task<List<Event>> loadHostedTask = new Task<>() {
            @Override
            protected List<Event> call() throws Exception {
                return eventService.getEventsHostedBy(displayedUser.getId());
            }
        };
        loadHostedTask.setOnSucceeded(_ -> {
            List<Event> events = loadHostedTask.getValue();
            Platform.runLater(() -> {
                hostedGatheringsContainer.getChildren().clear();
                boolean hasVisibleEvents = false;
                if (events != null && !events.isEmpty()) {
                    for (Event event : events) {
                        boolean isFriendWithCurrentUser = false;
                        if (currentUser != null && displayedUser != null
                                && currentUser.getId() != displayedUser.getId()) {
                            try {
                                isFriendWithCurrentUser = friendshipService.getFriendshipStatus(
                                        currentUser,
                                        displayedUser) == Friendship.FriendshipStatus.ACCEPTED;
                            } catch (UserNotFoundException
                                    | FriendshipOperationException exHandler) {
                            }
                        }

                        boolean showEvent = event.getPrivacy() == Event.PrivacySetting.PUBLIC;
                        if (event.getPrivacy() == Event.PrivacySetting.PRIVATE
                                && (isFriendWithCurrentUser
                                        || (currentUser != null && displayedUser.getId() == currentUser.getId()))) {
                             showEvent = true;
                        }

                        if (showEvent) {
                            hostedGatheringsContainer.getChildren()
                                    .add(createHostedGatheringNode(event));
                            hasVisibleEvents = true;
                        }
                    }
                }

                if (!hasVisibleEvents) {
                    if (events == null || events.isEmpty()) {
                        noHostedGatheringsLabel.setText(displayedUser.getUsername()
                                + " hasn't created any gatherings yet.");
                    } else {
                        noHostedGatheringsLabel.setText(displayedUser.getUsername()
                                + " has no public or friend-visible private gatherings.");
                    }
                    noHostedGatheringsLabel.setVisible(true);
                    noHostedGatheringsLabel.setManaged(true);
                } else {
                    noHostedGatheringsLabel.setVisible(false);
                    noHostedGatheringsLabel.setManaged(false);
                }
            });
        });
        loadHostedTask.setOnFailed(_ -> {
            Platform.runLater(() -> {
                noHostedGatheringsLabel.setText("Failed to load hosted gatherings.");
                noHostedGatheringsLabel.setVisible(true);
                noHostedGatheringsLabel.setManaged(true);
                if (loadHostedTask.getException() != null)
                    loadHostedTask.getException().printStackTrace();
            });
        });
        new Thread(loadHostedTask).start();
    }

    private Node createHostedGatheringNode(Event event) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle(
                "-fx-background-color: #FFFFFF; -fx-border-color: #EEEEEE; -fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        card.setMinWidth(330);
        card.setMaxWidth(330);

        Label titleLabel = new Label(event.getTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#333333"));
        titleLabel.setWrapText(true);

        Label dateLabel = new Label(event.getEventDateTime()
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm")));
        dateLabel.setFont(Font.font("System", 12));
        dateLabel.setTextFill(Color.web("#555555"));

        Label categoryLabel = new Label(event.getCategory() != null ? event.getCategory().toString() : "N/A");
        categoryLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 10));
        categoryLabel.setStyle(
                "-fx-background-color: #E3F2FD; -fx-text-fill: #1E88E5; -fx-padding: 2 6; -fx-background-radius: 8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox infoLine = new HBox(dateLabel, spacer, categoryLabel);
        infoLine.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(titleLabel, infoLine);
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(_ -> {
            if (mainPanelController != null) {
                try {
                    FXMLLoaderWrapper.FXMLLoaderResult<EventDetailsController> loaderResult =
                            FXMLLoaderWrapper.loadFXMLWithController("event_details_panel.fxml");
                    EventDetailsController detailsController = loaderResult.controller;
                    detailsController.setMainPanelController(mainPanelController);
                    detailsController.setPreviousViewFxml("other_profile_panel.fxml");
                    detailsController.loadEventData(event);
                    mainPanelController.setCurrentViewFxmlPath("event_details_panel.fxml");
                    mainPanelController.setContent(loaderResult.parent);
                } catch (IOException ioException) {
                    AlertFactory.showErrorAlert("Navigation Error", "Could not load event details.",
                            ioException.getMessage());
                }
            }
        });
        return card;
    }
}