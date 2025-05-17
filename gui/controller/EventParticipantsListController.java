package com.palveo.gui.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.palveo.config.AppConfig;
import com.palveo.gui.manager.SessionManager;
import com.palveo.gui.util.AlertFactory;
import com.palveo.gui.util.FXMLLoaderWrapper;
import com.palveo.model.Event;
import com.palveo.model.Friendship;
import com.palveo.model.Participant;
import com.palveo.model.User;
import com.palveo.service.FriendshipService;
import com.palveo.service.ParticipantService;
import com.palveo.service.impl.FriendshipServiceImpl;
import com.palveo.service.impl.ParticipantServiceImpl;
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
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class EventParticipantsListController {

    @FXML
    private Button backButton;
    @FXML
    private Label headerTitleLabel;
    @FXML
    private HBox searchFilterBox;
    @FXML
    private TextField searchParticipantField;
    @FXML
    private ScrollPane participantsScrollPane;
    @FXML
    private VBox participantsVBox;
    @FXML
    private Label statusMessageLabel;

    private MainPanelController mainPanelController;
    private SessionManager sessionManager;
    private ParticipantService participantService;
    private FriendshipService friendshipService;

    private Event currentEvent;
    private User currentUser;
    private List<User> allParticipantsForEventCache;
    private List<Friendship> currentUserFriendshipsCache;
    private boolean isCurrentUserHost;
    private String previousViewFxml;

    public EventParticipantsListController() {
        this.sessionManager = SessionManager.getInstance();
        new UserServiceImpl();
        this.participantService = new ParticipantServiceImpl();
        this.friendshipService = new FriendshipServiceImpl();
    }

    public void setMainPanelController(MainPanelController mainPanelController) {
        this.mainPanelController = mainPanelController;
    }

    public void setPreviousViewFxml(String fxmlPath) {
        this.previousViewFxml = fxmlPath;
    }

    @FXML
    private void initialize() {
        currentUser = sessionManager.getCurrentUser();
        searchParticipantField.textProperty()
                .addListener((_, _, newVal) -> filterAndDisplayParticipants(newVal));
        if (currentUser == null) {
            statusMessageLabel.setText("Error: Not logged in.");
            statusMessageLabel.setVisible(true);
            statusMessageLabel.setManaged(true);
            if (searchFilterBox != null)
                searchFilterBox.setDisable(true);
        }
    }
    
    public void refreshViewData(){
         if(this.currentEvent != null && this.allParticipantsForEventCache != null){
             loadParticipants(this.currentEvent, this.allParticipantsForEventCache);
         } else if(this.currentEvent != null){
             loadParticipants(this.currentEvent, Collections.emptyList()); 
         }
    }

    public void loadParticipants(Event event, List<User> preloadedParticipants) {
        this.currentEvent = event;
        if (currentUser == null || currentEvent == null) {
            statusMessageLabel.setText("Error: Cannot load participants data.");
            statusMessageLabel.setVisible(true);
            statusMessageLabel.setManaged(true);
            return;
        }
        this.isCurrentUserHost = currentUser.getId() == currentEvent.getHostUserId();
        headerTitleLabel.setText(currentEvent.getTitle() + " - Participants");

        statusMessageLabel.setText("Loading participants details...");
        statusMessageLabel.setVisible(true);
        statusMessageLabel.setManaged(true);
        participantsVBox.getChildren().clear();

        Task<List<Friendship>> loadFriendshipsTask = new Task<>() {
            @Override
            protected List<Friendship> call() throws Exception {
                if (currentUser == null)
                    return Collections.emptyList();
                return friendshipService.listFriends(currentUser).stream()
                        .map(friend -> new Friendship(currentUser.getId(), friend.getId(),
                                Friendship.FriendshipStatus.ACCEPTED, currentUser.getId()))
                        .collect(Collectors.toList());
            }

            @Override
            protected void succeeded() {
                currentUserFriendshipsCache = getValue();
                if (preloadedParticipants != null && !preloadedParticipants.isEmpty()) {
                    allParticipantsForEventCache = new ArrayList<>(preloadedParticipants);
                    sortAndFilterParticipants("");
                } else {
                    fetchAllParticipantsData();
                }
            }

            @Override
            protected void failed() {
                currentUserFriendshipsCache = Collections.emptyList();
                if (preloadedParticipants != null && !preloadedParticipants.isEmpty()) {
                    allParticipantsForEventCache = new ArrayList<>(preloadedParticipants);
                    sortAndFilterParticipants("");
                } else {
                    fetchAllParticipantsData();
                }
                 if(getException() != null) getException().printStackTrace();
                statusMessageLabel
                        .setText("Failed to load supporting data. Displaying basic list.");
            }
        };
        new Thread(loadFriendshipsTask).start();
    }

    private void fetchAllParticipantsData() {
        Task<List<User>> fetchTask = new Task<>() {
            @Override
            protected List<User> call() throws Exception {
                if (currentEvent == null)
                    return Collections.emptyList();
                return participantService.getConfirmedUsersForEvent(currentEvent.getId());
            }

            @Override
            protected void succeeded() {
                allParticipantsForEventCache = getValue();
                sortAndFilterParticipants("");
            }

            @Override
            protected void failed() {
                statusMessageLabel.setText("Failed to load participants list.");
                statusMessageLabel.setVisible(true);
                statusMessageLabel.setManaged(true);
                if (!participantsVBox.getChildren().contains(statusMessageLabel))
                    participantsVBox.getChildren().add(statusMessageLabel);
                if(getException() != null) getException().printStackTrace();
            }
        };
        new Thread(fetchTask).start();
    }

    private void sortAndFilterParticipants(String searchTerm) {
        if (allParticipantsForEventCache != null) {
            Collections.sort(allParticipantsForEventCache,
                    Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));
        }
        filterAndDisplayParticipants(searchTerm);
    }

    private void filterAndDisplayParticipants(String searchTerm) {
        participantsVBox.getChildren().clear();
        List<User> usersToDisplay;

        if (allParticipantsForEventCache == null) {
            statusMessageLabel.setText("Participant list is not available.");
            statusMessageLabel.setVisible(true);
            statusMessageLabel.setManaged(true);
            if (!participantsVBox.getChildren().contains(statusMessageLabel))
                participantsVBox.getChildren().add(statusMessageLabel);
            return;
        }

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            usersToDisplay = allParticipantsForEventCache;
        } else {
            String lowerCaseSearchTerm = searchTerm.trim().toLowerCase();
            usersToDisplay = allParticipantsForEventCache.stream().filter(user -> user.getUsername()
                    .toLowerCase().contains(lowerCaseSearchTerm)
                    || (user.getFirstName() != null
                            && user.getFirstName().toLowerCase().contains(lowerCaseSearchTerm))
                    || (user.getLastName() != null
                            && user.getLastName().toLowerCase().contains(lowerCaseSearchTerm)))
                    .collect(Collectors.toList());
        }

        if (usersToDisplay.isEmpty()) {
            statusMessageLabel.setText("No participants found"
                    + (!searchTerm.trim().isEmpty() ? " matching your search." : "."));
            statusMessageLabel.setVisible(true);
            statusMessageLabel.setManaged(true);
            if (!participantsVBox.getChildren().contains(statusMessageLabel))
                participantsVBox.getChildren().add(statusMessageLabel);
        } else {
            statusMessageLabel.setVisible(false);
            statusMessageLabel.setManaged(false);
            for (User participant : usersToDisplay) {
                participantsVBox.getChildren().add(createParticipantNode(participant));
            }
        }
    }

    private boolean isFriend(int userId) {
        if (currentUserFriendshipsCache == null || currentUser == null)
            return false;
        return currentUserFriendshipsCache.stream()
                .anyMatch(f -> f.getOtherUserId(currentUser.getId()) == userId
                        && f.getStatus() == Friendship.FriendshipStatus.ACCEPTED);
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
                     System.err.println("EventParticipantList: Error loading classpath avatar " + avatarFileNameOrClassPath + ": " + e.getMessage());
                }
            } else { // File system resource (user-uploaded avatars)
                File avatarFile = new File(AppConfig.getUserAvatarsDir(), avatarFileNameOrClassPath);
                if (avatarFile.exists() && avatarFile.isFile()) {
                     try {
                        imageToDisplay = new Image(avatarFile.toURI().toString());
                    } catch (Exception e){
                         System.err.println("EventParticipantList: Error loading file system avatar " + avatarFile.getAbsolutePath() + ": " + e.getMessage());
                    }
                }
            }
        }

        if (imageToDisplay == null || imageToDisplay.isError()) {
            try (InputStream defaultStream = getClass().getResourceAsStream(AppConfig.DEFAULT_AVATAR_PATH)) {
                 if(defaultStream != null) imageToDisplay = new Image(defaultStream);
            } catch (Exception e) {
                 System.err.println("EventParticipantList: Error loading DEFAULT avatar: " + e.getMessage());
            }
        }
        imageView.setImage(imageToDisplay);
    }


    private Node createParticipantNode(User participant) {
        HBox participantNode = new HBox(10);
        participantNode.setAlignment(Pos.CENTER_LEFT);
        participantNode.setPadding(new Insets(8));
        participantNode.setStyle(
                "-fx-background-color: #FFFFFF; -fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");

        ImageView avatarView = new ImageView();
        avatarView.setFitHeight(40);
        avatarView.setFitWidth(40);
        Circle clip = new Circle(20, 20, 20);
        avatarView.setClip(clip);
        
        loadAvatarForImageView(avatarView, participant.getProfileImagePath());

        VBox nameBox = new VBox(-2);
        Label usernameLabel = new Label(participant.getUsername());
        usernameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        usernameLabel.setTextFill(Color.web("#333333"));

        String fullName = (participant.getFirstName() != null ? participant.getFirstName() : "")
                + (participant.getFirstName() != null && participant.getLastName() != null
                        && !participant.getFirstName().isBlank()
                        && !participant.getLastName().isBlank() ? " " : "")
                + (participant.getLastName() != null ? participant.getLastName() : "");
        Label fullNameLabel = new Label(fullName.trim());
        fullNameLabel.setFont(Font.font("System", 12));
        fullNameLabel.setTextFill(Color.web("#666666"));
        nameBox.getChildren().addAll(usernameLabel, fullNameLabel);

        HBox userInfoAndFriendStatusBox = new HBox(5);
        userInfoAndFriendStatusBox.setAlignment(Pos.CENTER_LEFT);
        userInfoAndFriendStatusBox.getChildren().add(nameBox);
        HBox.setHgrow(userInfoAndFriendStatusBox, Priority.ALWAYS);

        if (currentUser != null && participant.getId() != currentUser.getId()
                && isFriend(participant.getId())) {
            ImageView friendIcon = new ImageView();
            try {
                Image img = new Image(getClass().getResourceAsStream("/images/friends_icon.png"));
                if (img != null && !img.isError())
                    friendIcon.setImage(img);
            } catch (Exception e) {
                friendIcon.setImage(null);
            }
            friendIcon.setFitHeight(16);
            friendIcon.setFitWidth(16);
            friendIcon.setOpacity(0.7);
            userInfoAndFriendStatusBox.getChildren().add(friendIcon);
            HBox.setMargin(friendIcon, new Insets(0, 0, 0, 5));
        }

        userInfoAndFriendStatusBox.setCursor(Cursor.HAND);
        userInfoAndFriendStatusBox.setOnMouseClicked(_ -> viewUserProfile(participant.getId()));

        HBox actionButtons = new HBox(5);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);

        if (isCurrentUserHost && currentUser != null
                && participant.getId() != currentUser.getId()) {
            Optional<Participant> rsvpOpt = participantService
                    .getUserRsvpForEvent(currentEvent.getId(), participant.getId());
            Participant.RsvpStatus status = rsvpOpt.map(Participant::getStatus).orElse(null);

            if (currentEvent.getEventDateTime().isBefore(LocalDateTime.now())) {
                Button attendedButton = new Button(
                        status == Participant.RsvpStatus.ATTENDED ? "Attended" : "Mark Attended");
                styleAttendanceButton(attendedButton, status == Participant.RsvpStatus.ATTENDED,
                        true);
                if (status == Participant.RsvpStatus.ATTENDED)
                    attendedButton.setDisable(true);
                attendedButton.setOnAction(_ -> handleMarkAttendance(participant, Participant.RsvpStatus.ATTENDED));


                Button notAttendedButton = new Button("Mark Not Attended");
                if (status == Participant.RsvpStatus.DECLINED)
                    notAttendedButton.setText("Declined");
                else if (status != null && status != Participant.RsvpStatus.ATTENDED
                        && status != Participant.RsvpStatus.JOINED
                        && status != Participant.RsvpStatus.INVITED)
                    notAttendedButton.setText("Not Attended");

                styleAttendanceButton(notAttendedButton,
                        status == Participant.RsvpStatus.DECLINED
                                || (status != Participant.RsvpStatus.ATTENDED && status != null
                                        && status != Participant.RsvpStatus.JOINED
                                        && status != Participant.RsvpStatus.INVITED),
                        false);
                 if (status == Participant.RsvpStatus.DECLINED ||
                     (status != null && status != Participant.RsvpStatus.ATTENDED && status != Participant.RsvpStatus.JOINED && status != Participant.RsvpStatus.INVITED) ) {
                    notAttendedButton.setDisable(true);
                }
                notAttendedButton.setOnAction(_ -> handleMarkAttendance(participant, Participant.RsvpStatus.DECLINED));
                actionButtons.getChildren().addAll(attendedButton, notAttendedButton);

            } else {
                Button removeButton = new Button("Remove");
                removeButton.setFont(Font.font("System", FontWeight.NORMAL, 11));
                removeButton.setStyle(
                        "-fx-background-color: #FFEBEE; -fx-text-fill: #D32F2F; -fx-background-radius: 15px; -fx-padding: 4 10; -fx-cursor: hand;");
                removeButton
                        .setOnAction(_ -> handleRemoveParticipant(participant, participantNode));
                actionButtons.getChildren().add(removeButton);
            }
        }
        participantNode.getChildren().addAll(avatarView, userInfoAndFriendStatusBox, actionButtons);
        return participantNode;
    }

    private void styleAttendanceButton(Button button, boolean isActive, boolean isAttendedType) {
        button.setFont(Font.font("System", FontWeight.NORMAL, 10));
        button.setMinWidth(100);
        button.setCursor(Cursor.HAND);
        if (isActive) {
            button.setStyle(isAttendedType
                    ? "-fx-background-color: #C8E6C9; -fx-text-fill: #2E7D32; -fx-background-radius: 15px; -fx-padding: 4 10;"
                    : "-fx-background-color: #FFCDD2; -fx-text-fill: #C62828; -fx-background-radius: 15px; -fx-padding: 4 10;");
        } else {
            button.setStyle(isAttendedType
                    ? "-fx-background-color: #E8F5E9; -fx-text-fill: #388E3C; -fx-background-radius: 15px; -fx-padding: 4 10;"
                    : "-fx-background-color: #FFEBEE; -fx-text-fill: #D32F2F; -fx-background-radius: 15px; -fx-padding: 4 10;");
        }
    }

    private void handleMarkAttendance(User participant, Participant.RsvpStatus newStatusForUser) {
        Task<Participant> markTask = new Task<>() {
            @Override
            protected Participant call() throws Exception {
                 if (newStatusForUser == Participant.RsvpStatus.ATTENDED) {
                     return participantService.markAttendance(currentEvent.getId(), participant.getId(), currentUser);
                 } else {
                     return participantService.updateRsvpStatus(currentEvent.getId(), participant, newStatusForUser);
                 }
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    Participant updatedRsvp = getValue();
                    String statusText = (updatedRsvp != null && updatedRsvp.getStatus() == newStatusForUser)
                            ? newStatusForUser.toString()
                            : "Status Unchanged";
                    AlertFactory.showInformationAlert("Success", "Attendance Updated",
                            participant.getUsername() + " status set to " + statusText + " for this event.");
                    refreshViewData(); 
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    AlertFactory.showErrorAlert("Error", "Update Failed",
                            getException() != null ? getException().getMessage() : "Unknown error marking attendance.");
                    if (getException() != null) getException().printStackTrace();
                    refreshViewData();
                });
            }
        };
        new Thread(markTask).start();
    }


    private void handleRemoveParticipant(User participant, Node participantUINode) {
        if (currentUser == null || currentEvent == null) return;

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Remove Participant");
        confirmationAlert.setHeaderText("Confirm Removal");
        confirmationAlert
                .setContentText("Are you sure you want to remove " + participant.getUsername()
                        + " from this event? Their RSVP will be set to DECLINED.");
        confirmationAlert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        Optional<ButtonType> result = confirmationAlert.showAndWait();

        result.filter(responseButtonType -> responseButtonType == ButtonType.YES)
                .ifPresent(_ -> {
                    Node yesButtonNode = confirmationAlert.getDialogPane().lookupButton(ButtonType.YES);
                    if (yesButtonNode != null) {
                        yesButtonNode.setDisable(true);
                    }
                    Task<Void> removeTask = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            participantService.updateRsvpStatus(currentEvent.getId(), participant,
                                    Participant.RsvpStatus.DECLINED);
                            return null;
                        }
                        @Override
                        protected void succeeded() {
                            Platform.runLater(() -> {
                                AlertFactory.showInformationAlert("Success", "Participant Removed",
                                        participant.getUsername() + " has been removed (RSVP set to DECLINED).");
                                refreshViewData(); 
                            });
                        }
                        @Override
                        protected void failed() {
                            Platform.runLater(() -> {
                                AlertFactory.showErrorAlert("Error", "Removal Failed",
                                       (getException() != null ? getException().getMessage() : "Unknown error.") );
                                if (getException() != null) getException().printStackTrace();
                                if (yesButtonNode != null) {
                                    yesButtonNode.setDisable(false);
                                }
                            });
                        }
                    };
                    new Thread(removeTask).start();
                });
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
                OtherProfileController otherProfileController = loaderResult.controller;
                otherProfileController.setMainPanelController(mainPanelController);
                otherProfileController.setPreviousViewFxml("event_participants_list.fxml");
                otherProfileController.loadUserProfile(userId);
                mainPanelController.setCurrentViewFxmlPath("other_profile_panel.fxml");
                mainPanelController.setContent(loaderResult.parent);
            } catch (IOException ioEx) {
                AlertFactory.showErrorAlert("Navigation Error", "Could not load user profile.",
                        ioEx.getMessage());
            }
        }
    }

    @FXML
    private void handleBackButtonAction(ActionEvent event) {
        if (mainPanelController != null) {
            String targetView = "home_feed.fxml";
            if (previousViewFxml != null && !previousViewFxml.isEmpty()) {
                targetView = previousViewFxml;
            } else if (currentEvent != null) {
                targetView = "event_details_panel.fxml";
            }

            if (targetView.equals("event_details_panel.fxml") && currentEvent != null) {
                 mainPanelController.loadView(targetView, controller -> {
                     if(controller instanceof EventDetailsController){
                         ((EventDetailsController) controller).loadEventData(currentEvent);
                         ((EventDetailsController) controller).refreshViewData();
                     }
                 });
            } else {
                mainPanelController.loadView(targetView);
            }
        }
    }
}