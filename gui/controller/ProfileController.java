package com.palveo.gui.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import com.palveo.config.AppConfig;
import com.palveo.gui.manager.SessionManager;
import com.palveo.gui.util.AlertFactory;
import com.palveo.gui.util.FXMLLoaderWrapper;
import com.palveo.model.Comment;
import com.palveo.model.Event;
import com.palveo.model.User;
import com.palveo.service.CommentService;
import com.palveo.service.EventService;
import com.palveo.service.ParticipantService;
import com.palveo.service.RatingService;
import com.palveo.service.TagService;
import com.palveo.service.UserService;
import com.palveo.service.exception.CommentOperationException;
import com.palveo.service.exception.RatingOperationException;
import com.palveo.service.exception.TagOperationException;
import com.palveo.service.exception.UserNotFoundException;
import com.palveo.service.exception.UserOperationException;
import com.palveo.service.impl.CommentServiceImpl;
import com.palveo.service.impl.EventServiceImpl;
import com.palveo.service.impl.ParticipantServiceImpl;
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
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
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
import javafx.stage.FileChooser;

public class ProfileController {

    @FXML
    private ImageView profileImageView;
    @FXML
    private Label usernameLabel;
    @FXML
    private Label fullNameLabel;
    @FXML
    private Label bioLabel;
    @FXML
    private Button editProfileButton;
    @FXML
    private VBox gatheringsContainer;
    @FXML
    private Label noGatheringsLabel;
    @FXML
    private VBox bioViewBox;
    @FXML
    private TextFlow tagsTextFlow;
    @FXML
    private Label averageRatingLabel;
    @FXML
    private Button changeAvatarButton;

    @FXML
    private VBox commentsContainer;
    @FXML
    private Label noCommentsLabel;

    private SessionManager sessionManager;
    private UserService userService;
    private EventService eventService;
    private ParticipantService participantService;
    private TagService tagService;
    private RatingService ratingService;
    private CommentService commentService;
    private User currentUser;
    private MainPanelController mainPanelController;

    public ProfileController() {
        this.sessionManager = SessionManager.getInstance();
        this.userService = new UserServiceImpl();
        this.eventService = new EventServiceImpl();
        this.participantService = new ParticipantServiceImpl();
        this.tagService = new TagServiceImpl();
        this.ratingService = new RatingServiceImpl();
        this.commentService = new CommentServiceImpl();
    }

    public void setMainPanelController(MainPanelController mainPanelController) {
        this.mainPanelController = mainPanelController;
    }

    @FXML
    private void initialize() {
        refreshViewData();
    }

    public void refreshViewData() {
        refreshCurrentUser();
        if (currentUser == null) {
            AlertFactory.showErrorAlert("Error", "Not Logged In",
                    "Cannot load profile. Please log in again.");
            if (mainPanelController != null) {
                mainPanelController.loadView("login.fxml");
                mainPanelController.setCurrentViewFxmlPath("login.fxml");
            }
            return;
        }
        populateProfileData();
        loadUserTagsAndRating();
        loadGatherings();
        loadProfileComments();
    }


    private void refreshCurrentUser() {
        if (sessionManager.getCurrentUser() != null) {
            Optional<User> updatedUserOpt =
                    userService.getUserById(sessionManager.getCurrentUser().getId());
            if (updatedUserOpt.isPresent()) {
                currentUser = updatedUserOpt.get();
                sessionManager.setCurrentUser(currentUser);
            } else {
                currentUser = sessionManager.getCurrentUser();
            }
        } else {
            currentUser = null;
        }
    }

    private void populateProfileData() {
        if (currentUser == null)
            return;
        usernameLabel.setText(currentUser.getUsername());

        String fName = currentUser.getFirstName() == null ? "" : currentUser.getFirstName();
        String lName = currentUser.getLastName() == null ? "" : currentUser.getLastName();
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

        bioLabel.setText(currentUser.getBio() != null && !currentUser.getBio().isEmpty()
                ? currentUser.getBio()
                : "No bio set yet. Click 'Edit Profile' to add one!");
        
        updateAvatarDisplay(currentUser.getProfileImagePath());
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
                     System.err.println("ProfileController: Error loading classpath avatar " + avatarFileNameOrClassPath + ": " + e.getMessage());
                }
            } else { 
                File avatarFile = new File(AppConfig.getUserAvatarsDir(), avatarFileNameOrClassPath);
                if (avatarFile.exists() && avatarFile.isFile()) {
                    try {
                        imageToDisplay = new Image(avatarFile.toURI().toString());
                    } catch (Exception e){
                         System.err.println("ProfileController: Error loading file system avatar " + avatarFile.getAbsolutePath() + ": " + e.getMessage());
                    }
                }
            }
        }

        if (imageToDisplay == null || imageToDisplay.isError()) {
            try (InputStream defaultStream = getClass().getResourceAsStream(AppConfig.DEFAULT_AVATAR_PATH)) {
                 if(defaultStream != null) imageToDisplay = new Image(defaultStream);
            } catch (Exception e) {
                 System.err.println("ProfileController: Error loading DEFAULT avatar: " + e.getMessage());
            }
        }
        profileImageView.setImage(imageToDisplay);
    }


    private void loadUserTagsAndRating() {
        if (currentUser == null)
            return;
        tagsTextFlow.getChildren().clear();
        try {
            List<com.palveo.model.Tag> userTags = tagService.getTagsForUser(currentUser.getId());
            if (userTags.isEmpty()) {
                Text noTagsText = new Text("No tags yet.");
                noTagsText.setFill(Color.GRAY);
                noTagsText.setFont(Font.font("System", 12));
                tagsTextFlow.getChildren().add(noTagsText);
            } else {
                for (com.palveo.model.Tag tag : userTags) {
                    Label tagLabel = new Label(tag.getTagName());
                    tagLabel.setStyle(
                            "-fx-background-color: #ECEFF1; -fx-text-fill: #37474F; -fx-padding: 3 8; -fx-background-radius: 12; -fx-font-size: 11px;");
                    tagsTextFlow.getChildren().addAll(tagLabel, new Text(" "));
                }
            }
        } catch (UserNotFoundException | TagOperationException e) {
            Text errorText = new Text("Could not load tags.");
            errorText.setFill(Color.RED);
            tagsTextFlow.getChildren().add(errorText);
        }

        averageRatingLabel.setGraphic(null);
        averageRatingLabel.setText("");
        try {
            double avgRating = ratingService.calculateAverageRatingForUser(currentUser.getId());
            HBox ratingDisplayBox = new HBox(3);
            ratingDisplayBox.setAlignment(Pos.CENTER);

            Image starFilledImg = null, starHalfImg = null, starEmptyImg = null;
            try {
                starFilledImg =
                        new Image(getClass().getResourceAsStream("/images/star_filled.png"));
            } catch (Exception e) {
                System.err.println("Error loading star_filled.png");
            }
            try {
                starHalfImg = new Image(getClass().getResourceAsStream("/images/star_half.png"));
            } catch (Exception e) {
                System.err.println("Error loading star_half.png");
            }
            try {
                starEmptyImg = new Image(getClass().getResourceAsStream("/images/star_empty.png"));
            } catch (Exception e) {
                System.err.println("Error loading star_empty.png");
            }

            for (int i = 1; i <= 5; i++) {
                ImageView star = new ImageView();
                star.setFitHeight(16);
                star.setFitWidth(16);
                Image selectedStarImage = starEmptyImg;
                if (avgRating >= i) {
                    if (starFilledImg != null && !starFilledImg.isError())
                        selectedStarImage = starFilledImg;
                } else if (avgRating >= i - 0.75 && avgRating > i - 1) {
                    if (starHalfImg != null && !starHalfImg.isError())
                        selectedStarImage = starHalfImg;
                    else if (starFilledImg != null && !starFilledImg.isError())
                        selectedStarImage = starFilledImg;
                }
                if (selectedStarImage == null || selectedStarImage.isError()) {
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

    private void loadGatherings() {
        if (currentUser == null)
            return;
        gatheringsContainer.getChildren().clear();
        noGatheringsLabel.setVisible(false);
        noGatheringsLabel.setManaged(false);
        Task<List<Event>> loadEventsTask = new Task<>() {
            @Override
            protected List<Event> call() throws Exception {
                return eventService.getEventsHostedBy(currentUser.getId());
            }
        };

        loadEventsTask.setOnSucceeded(_ -> {
            List<Event> hostedEvents = loadEventsTask.getValue();
            Platform.runLater(() -> {
                if (hostedEvents != null) {
                    Collections.sort(hostedEvents,
                            Comparator.comparing(Event::getEventDateTime).reversed());
                    if (hostedEvents.isEmpty()) {
                        noGatheringsLabel.setText("You haven't created any gatherings yet.");
                        noGatheringsLabel.setVisible(true);
                        noGatheringsLabel.setManaged(true);
                    } else {
                        noGatheringsLabel.setVisible(false);
                        noGatheringsLabel.setManaged(false);
                        for (Event event : hostedEvents) {
                            gatheringsContainer.getChildren().add(createGatheringNode(event));
                        }
                    }
                } else {
                    noGatheringsLabel
                            .setText("Could not load your created gatherings (null list).");
                    noGatheringsLabel.setVisible(true);
                    noGatheringsLabel.setManaged(true);
                }
            });
        });

        loadEventsTask.setOnFailed(_ -> {
            Platform.runLater(() -> {
                noGatheringsLabel.setText("Error loading your created gatherings.");
                noGatheringsLabel.setVisible(true);
                noGatheringsLabel.setManaged(true);
                if(loadEventsTask.getException() != null) {
                    loadEventsTask.getException().printStackTrace();
                }
            });
        });
        new Thread(loadEventsTask).start();
    }

    private Node createGatheringNode(Event event) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle(
                "-fx-background-color: #FFFFFF; -fx-border-color: #EEEEEE; -fx-border-width: 1px; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0.1, 0, 1);");
        card.setMinWidth(330);
        card.setMaxWidth(330);

        Label titleLabel = new Label(event.getTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        titleLabel.setTextFill(Color.web("#333333"));
        titleLabel.setWrapText(true);

        HBox dateAndCategoryBox = new HBox(10);
        dateAndCategoryBox.setAlignment(Pos.CENTER_LEFT);

        Label dateLabel = new Label(event.getEventDateTime()
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm")));
        dateLabel.setFont(Font.font("System", 12));
        dateLabel.setTextFill(Color.web("#555555"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label categoryBadge = new Label(event.getCategory() != null ? event.getCategory().toString() : "N/A");
        categoryBadge.setFont(Font.font("System", FontWeight.SEMI_BOLD, 10));
        categoryBadge.setStyle(
                "-fx-background-color: #E3F2FD; -fx-text-fill: #1E88E5; -fx-padding: 3 7; -fx-background-radius: 10;");
        dateAndCategoryBox.getChildren().addAll(dateLabel, spacer, categoryBadge);

        HBox participantsAndActionBox = new HBox(10);
        participantsAndActionBox.setAlignment(Pos.CENTER_LEFT);
        Label participantsInfo = new Label("Loading participants...");
        participantsInfo.setFont(Font.font("System", 12));
        participantsInfo.setTextFill(Color.web("#757575"));

        Task<Integer> countTask = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                return participantService.getConfirmedUsersForEvent(event.getId()).size();
            }
        };
        countTask.setOnSucceeded(_ -> Platform
                .runLater(() -> participantsInfo.setText(countTask.getValue() + " Participants")));
        countTask.setOnFailed(
                _ -> Platform.runLater(() -> participantsInfo.setText("Participants: Error")));
        new Thread(countTask).start();

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Button manageButton = new Button("Details / Manage");
        manageButton.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
        manageButton.setStyle(
                "-fx-background-color: #007bff; -fx-text-fill: white; -fx-background-radius: 6px; -fx-padding: 5 10;");
        manageButton.setOnAction(_ -> showEventDetails(event));

        participantsAndActionBox.getChildren().addAll(participantsInfo, spacer2, manageButton);

        card.getChildren().addAll(titleLabel, dateAndCategoryBox, participantsAndActionBox);
        card.setCursor(Cursor.DEFAULT);
        return card;
    }


    private void loadProfileComments() {
        if (currentUser == null)
            return;

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
                try {
                    return commentService.getCommentsForUserProfile(currentUser.getId());
                } catch (UserNotFoundException e) {
                    System.err.println("User not found while fetching comments for profile: "
                            + currentUser.getId());
                    return Collections.emptyList();
                } catch (CommentOperationException e) {
                    System.err.println("Error fetching comments for profile: " + e.getMessage());
                    throw e;
                }
            }
        };

        loadCommentsTask.setOnSucceeded(_ -> {
            List<Comment> comments = loadCommentsTask.getValue();
            Platform.runLater(() -> {
                commentsContainer.getChildren().remove(loadingCommentsLabel);
                if (comments == null || comments.isEmpty()) {
                    noCommentsLabel.setText("No comments on this profile yet.");
                    noCommentsLabel.setVisible(true);
                    noCommentsLabel.setManaged(true);
                } else {
                    comments.sort(Comparator.comparing(Comment::getCreatedAt).reversed());
                    comments.forEach(comment -> {
                        commentsContainer.getChildren().add(createCommentNode(comment));
                    });
                }
            });
        });

        loadCommentsTask.setOnFailed(_ -> {
            Throwable ex = loadCommentsTask.getException();
            Platform.runLater(() -> {
                commentsContainer.getChildren().remove(loadingCommentsLabel);
                String errorMsg = "Failed to load comments.";
                if (ex != null && ex.getMessage() != null) {
                    errorMsg = "Failed to load comments: "
                            + ex.getMessage().substring(0, Math.min(ex.getMessage().length(), 60))
                            + "...";
                }
                noCommentsLabel.setText(errorMsg);
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
                    otherProfileController.setPreviousViewFxml("profile.fxml");
                    otherProfileController.loadUserProfile(comment.getAuthorUserId());
                    mainPanelController.setCurrentViewFxmlPath("other_profile_panel.fxml");
                    mainPanelController.setContent(loaderResult.parent);
                } catch (IOException ioEx) {
                    AlertFactory.showErrorAlert("Navigation Error", "Could not load user profile.",
                            ioEx.getMessage());
                }
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
                || (comment.getTargetProfileUserId() != null && comment.getTargetProfileUserId() == currentUser.getId())) {
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

    private void showEventDetails(Event event) {
        if (mainPanelController == null || event == null)
            return;
        try {
            FXMLLoaderWrapper.FXMLLoaderResult<EventDetailsController> loaderResult =
                    FXMLLoaderWrapper.loadFXMLWithController("event_details_panel.fxml");
            EventDetailsController detailsController = loaderResult.controller;
            detailsController.setMainPanelController(mainPanelController);
            detailsController.setPreviousViewFxml("profile.fxml");
            detailsController.loadEventData(event);
            mainPanelController.setCurrentViewFxmlPath("event_details_panel.fxml");
            mainPanelController.setContent(loaderResult.parent);
        } catch (IOException e) {
            AlertFactory.showErrorAlert("Navigation Error", "Could not load event details view.",
                    e.getMessage());
        }
    }

    @FXML
    private void handleChangeAvatarAction(ActionEvent actionEvent) {
        if (currentUser == null)
            return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", ".png", ".jpg", ".jpeg", ".gif"));
        File selectedFile = fileChooser.showOpenDialog(changeAvatarButton.getScene().getWindow());

        if (selectedFile != null) {
            changeAvatarButton.setDisable(true);
            changeAvatarButton.setText("...");
            Task<String> uploadTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    String fileExtension = "";
                    String fileNameOriginal = selectedFile.getName();
                    int dotIndex = fileNameOriginal.lastIndexOf('.');
                    if (dotIndex > 0 && dotIndex < fileNameOriginal.length() - 1) {
                        fileExtension = fileNameOriginal.substring(dotIndex);
                    }
                    String newImageFileName = "user_" + currentUser.getId() + "_avatar_"
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
                String newAvatarFileName = uploadTask.getValue();
                User updates = new User();
                updates.setId(currentUser.getId());
                updates.setProfileImagePath(newAvatarFileName);
                updates.setFirstName(currentUser.getFirstName()); 
                updates.setLastName(currentUser.getLastName());   
                updates.setCity(currentUser.getCity());       
                
                try {
                    userService.updateUserProfile(updates, currentUser);
                    AlertFactory.showInformationAlert("Success", "Avatar Updated",
                            "Your profile picture has been updated.");
                    refreshViewData();
                } catch (UserNotFoundException | UserOperationException e) {
                    AlertFactory.showErrorAlert("Update Failed",
                            "Could not update avatar in database.", e.getMessage());
                }
                changeAvatarButton.setDisable(false);
                changeAvatarButton.setText("");
            });

            uploadTask.setOnFailed(_ -> {
                AlertFactory.showErrorAlert("Upload Failed", "Could not upload image.",
                        uploadTask.getException() != null ? uploadTask.getException().getMessage() : "Unknown error");
                if(uploadTask.getException() != null) uploadTask.getException().printStackTrace();
                changeAvatarButton.setDisable(false);
                changeAvatarButton.setText("");
            });
            new Thread(uploadTask).start();
        }
    }

    @FXML
    private void handleEditProfileButtonAction(ActionEvent actionEvent) {
        if (mainPanelController == null || currentUser == null)
            return;
        try {
            FXMLLoaderWrapper.FXMLLoaderResult<SettingsPanelController> loaderResult =
                    FXMLLoaderWrapper.loadFXMLWithController("settings_panel.fxml");
            SettingsPanelController settingsController = loaderResult.controller;
            settingsController.setMainPanelController(mainPanelController);
            settingsController.loadUserSettings(currentUser);

            mainPanelController.setCurrentViewFxmlPath("settings_panel.fxml");
            mainPanelController.setContent(loaderResult.parent);
        } catch (IOException e) {
            AlertFactory.showErrorAlert("Navigation Error", "Could not load settings panel.",
                    e.getMessage());
        }
    }
}