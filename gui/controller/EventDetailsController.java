package com.palveo.gui.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import com.palveo.MainApp;
import com.palveo.config.AppConfig;
import com.palveo.gui.manager.SessionManager;
import com.palveo.gui.util.AlertFactory;
import com.palveo.gui.util.FXMLLoaderWrapper;
import com.palveo.model.Comment;
import com.palveo.model.Event;
import com.palveo.model.Friendship;
import com.palveo.model.Participant;
import com.palveo.model.Tag;
import com.palveo.model.User;
import com.palveo.service.CommentService;
import com.palveo.service.EventService;
import com.palveo.service.FriendshipService;
import com.palveo.service.ParticipantService;
import com.palveo.service.TagService;
import com.palveo.service.UserService;
import com.palveo.service.exception.EventNotFoundException;
import com.palveo.service.exception.FriendshipOperationException;
import com.palveo.service.exception.TagOperationException;
import com.palveo.service.exception.UserNotFoundException;
import com.palveo.service.impl.CommentServiceImpl;
import com.palveo.service.impl.EventServiceImpl;
import com.palveo.service.impl.FriendshipServiceImpl;
import com.palveo.service.impl.ParticipantServiceImpl;
import com.palveo.service.impl.TagServiceImpl;
import com.palveo.service.impl.UserServiceImpl;
import com.palveo.util.ValidationUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class EventDetailsController {

    @FXML private Button backButton;
    @FXML private Label eventTitleHeaderLabel;
    @FXML private ScrollPane detailsScrollPane;
    @FXML private VBox eventDetailsContainer;
    @FXML private ImageView eventImageView;
    @FXML private Label eventTitleText;
    @FXML private Label categoryText;
    @FXML private HBox privacyInfoBox;
    @FXML private ImageView privacyIconView;
    @FXML private Label privacyText;
    @FXML private Label hostedByLabel;
    @FXML private Label dateTimeText;
    @FXML private Label locationText;
    @FXML private TextFlow descriptionTextFlow;
    @FXML private Text descriptionText;
    @FXML private HBox participantsLinkBox;
    @FXML private Label participantsCountLabel;
    @FXML private VBox actionFooter;
    @FXML private Label eventStatusBadge;
    @FXML private HBox actionButtonsContainer;

    @FXML private FlowPane eventTagsFlowPane;
    @FXML private Button manageEventTagsButton;
    @FXML private Label noEventTagsLabel;

    @FXML private ScrollPane eventCommentsScrollPane;
    @FXML private VBox eventCommentsVBox;
    @FXML private Label noEventCommentsLabel;
    @FXML private VBox eventCommentInputBox;
    @FXML private TextArea newEventCommentArea;
    @FXML private Button postEventCommentButton;

    private Event currentEvent;
    private User currentUser;
    private MainPanelController mainPanelController;
    private ParticipantService participantService;
    private UserService userService;
    private EventService eventService;
    private FriendshipService friendshipService;
    private TagService tagService;
    private CommentService commentService;
    private SessionManager sessionManager;

    private List<User> eventParticipantsCache;
    private Participant currentUserRsvpCache;
    private String previousViewFxml;
    private User hostUserCache;

    public EventDetailsController() {
        this.participantService = new ParticipantServiceImpl();
        this.userService = new UserServiceImpl();
        this.eventService = new EventServiceImpl();
        this.friendshipService = new FriendshipServiceImpl();
        this.tagService = new TagServiceImpl();
        this.commentService = new CommentServiceImpl();
        this.sessionManager = SessionManager.getInstance();
    }

    public Event getCurrentEvent() {
        return currentEvent;
    }

    public void setMainPanelController(MainPanelController mainPanelController) {
        this.mainPanelController = mainPanelController;
    }

    public void setPreviousViewFxml(String fxmlPath) {
        this.previousViewFxml = fxmlPath;
    }

    public void refreshViewData() {
        if (this.currentEvent != null) {
            loadEventData(this.currentEvent);
        }
    }

    @FXML
    private void initialize() {
        this.currentUser = sessionManager.getCurrentUser();
        participantsLinkBox.setOnMouseClicked(_ -> navigateToParticipantsList());

        if (eventImageView != null) {
            eventImageView.setFitHeight(200);
            try {
                eventImageView.setImage(new Image(
                        getClass().getResourceAsStream(AppConfig.DEFAULT_EVENT_IMAGE_PATH)));
            } catch (Exception e) {
                System.err.println("Error loading default event image: " + e.getMessage());
            }
        }
        if (manageEventTagsButton != null) {
            manageEventTagsButton.setVisible(false);
            manageEventTagsButton.setManaged(false);
        }
        if (noEventTagsLabel != null) {
            noEventTagsLabel.setVisible(false);
            noEventTagsLabel.setManaged(false);
        }
        if (noEventCommentsLabel != null) {
            noEventCommentsLabel.setVisible(false);
            noEventCommentsLabel.setManaged(false);
        }
        if(eventCommentInputBox != null){
            eventCommentInputBox.setVisible(false);
            eventCommentInputBox.setManaged(false);
        }
    }

    public void loadEventData(Event event) {
        this.currentEvent = event;
        if (event == null || currentUser == null) {
            AlertFactory.showErrorAlert("Error", "Data Missing",
                    "Cannot display event details or user not logged in.");
            Platform.runLater(() -> handleBackButtonAction(null));
            return;
        }
        eventTitleHeaderLabel.setText(currentEvent.getTitle());
        fetchAndPopulateEventDetails();
    }

    private void fetchAndPopulateEventDetails() {
        Task<Void> loadTask = new Task<>() {
            int participantCount = 0;
            boolean isFriendWithHost = false;

            @Override
            protected Void call() throws Exception {
                if (currentEvent == null) return null;

                Optional<User> hostOpt = userService.getUserById(currentEvent.getHostUserId());
                if (hostOpt.isPresent()) {
                    hostUserCache = hostOpt.get();
                    if (currentUser.getId() != hostUserCache.getId() && Event.PrivacySetting.PRIVATE.equals(currentEvent.getPrivacy())) {
                        try {
                            isFriendWithHost = friendshipService.getFriendshipStatus(currentUser, hostUserCache) == Friendship.FriendshipStatus.ACCEPTED;
                        } catch (UserNotFoundException | FriendshipOperationException e) {
                             System.err.println("Error checking friendship status in EventDetails: " + e.getMessage());
                        }
                    }
                }
                eventParticipantsCache = participantService.getConfirmedUsersForEvent(currentEvent.getId());
                participantCount = eventParticipantsCache.size();
                Optional<Participant> rsvpOpt = participantService.getUserRsvpForEvent(currentEvent.getId(), currentUser.getId());
                currentUserRsvpCache = rsvpOpt.orElse(null);
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    populateStaticDetails(hostUserCache);
                    updateDynamicElements(participantCount, currentUserRsvpCache, isFriendWithHost);
                    loadAndDisplayEventTags();
                    loadAndDisplayEventComments();
                    updateCommentInputAreaVisibility();
                });
            }

            @Override
            protected void failed() {
                Throwable ex = getException();
                if (ex != null) ex.printStackTrace();
                Platform.runLater(() -> {
                    AlertFactory.showErrorAlert("Load Error", "Failed to load all event data.", (ex != null ? ex.getMessage() : "Unknown error"));
                    populateStaticDetails(null);
                    updateDynamicElements(0, null, false);
                });
            }
        };
        new Thread(loadTask).start();
    }
    
    private void updateCommentInputAreaVisibility() {
        if (currentEvent == null || currentUser == null || eventCommentInputBox == null) return;

        boolean canComment = false;
        if (currentEvent.getPrivacy() == Event.PrivacySetting.PUBLIC) {
            canComment = true;
        } else { 
            boolean isHost = currentUser.getId() == currentEvent.getHostUserId();
            boolean isParticipant = false;
            if (currentUserRsvpCache != null) {
                isParticipant = (currentUserRsvpCache.getStatus() == Participant.RsvpStatus.JOINED || 
                                 currentUserRsvpCache.getStatus() == Participant.RsvpStatus.ATTENDED);
            }
            canComment = isHost || isParticipant;
        }
        
        eventCommentInputBox.setVisible(canComment);
        eventCommentInputBox.setManaged(canComment);
    }


    private void populateStaticDetails(User hostUser) {
        if (currentEvent == null) return;

        String imageToLoad = AppConfig.DEFAULT_EVENT_IMAGE_PATH;
        if (currentEvent.getEventImagePath() != null && !currentEvent.getEventImagePath().isBlank()) {
            imageToLoad = currentEvent.getEventImagePath();
        }
        try {
            if (!imageToLoad.startsWith("/")) imageToLoad = "/" + imageToLoad;
            Image eventImg = new Image(getClass().getResourceAsStream(imageToLoad), 350, 0, true, true);
            if (eventImg.isError()) {
                eventImageView.setImage(new Image(getClass().getResourceAsStream(AppConfig.DEFAULT_EVENT_IMAGE_PATH)));
            } else {
                eventImageView.setImage(eventImg);
            }
        } catch (Exception e) {
            eventImageView.setImage(new Image(getClass().getResourceAsStream(AppConfig.DEFAULT_EVENT_IMAGE_PATH)));
        }

        eventTitleText.setText(currentEvent.getTitle());
        categoryText.setText(currentEvent.getCategory() != null ? currentEvent.getCategory().toString() : "N/A");

        String privacyIconPath = null;
        String privacyDisplayText = "";
        if (Event.PrivacySetting.PUBLIC.equals(currentEvent.getPrivacy())) {
            privacyIconPath = "/images/public_icon.png";
            privacyDisplayText = Event.PrivacySetting.PUBLIC.toString();
        } else if (Event.PrivacySetting.PRIVATE.equals(currentEvent.getPrivacy())) {
            privacyIconPath = "/images/private_icon.png";
            privacyDisplayText = Event.PrivacySetting.PRIVATE.toString();
        }
        if (privacyIconPath != null) {
            try {
                Image img = new Image(getClass().getResourceAsStream(privacyIconPath));
                if (img != null && !img.isError()) privacyIconView.setImage(img);
                else privacyIconView.setImage(null);
            } catch (Exception e) { privacyIconView.setImage(null); }
        } else {
            privacyIconView.setImage(null);
        }
        privacyText.setText(privacyDisplayText);

        if (hostUser != null) {
            hostedByLabel.setText("Hosted by: " + hostUser.getUsername());
            hostedByLabel.setCursor(Cursor.HAND);
            hostedByLabel.setUnderline(true);
            hostedByLabel.setOnMouseClicked(_ -> viewUserProfile(hostUser.getId()));
        } else {
            hostedByLabel.setText("Hosted by: Unknown");
            hostedByLabel.setOnMouseClicked(null);
            hostedByLabel.setCursor(Cursor.DEFAULT);
            hostedByLabel.setUnderline(false);
        }

        dateTimeText.setText(currentEvent.getEventDateTime().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' HH:mm")));
        locationText.setText(currentEvent.getLocationString() != null ? currentEvent.getLocationString() : "Not specified.");
        descriptionText.setText(currentEvent.getDescription() != null && !currentEvent.getDescription().isEmpty() ? currentEvent.getDescription() : "No description provided.");
    }

    private void updateDynamicElements(int participantCount, Participant currentUserRsvp, boolean isFriendWithHost) {
        if (currentEvent == null || currentUser == null) return;
        participantsCountLabel.setText("(" + participantCount + ")");
        boolean isPast = currentEvent.getEventDateTime().isBefore(LocalDateTime.now());
        boolean isHost = currentEvent.getHostUserId() == currentUser.getId();
        String statusText = "";
        String statusStyle = "-fx-font-family: 'System Bold'; -fx-font-size: 12px; -fx-padding: 6 10; -fx-background-radius: 15px; ";

        if (isHost) {
            statusText = "YOU ARE THE HOST";
            statusStyle += "-fx-background-color: #FFF9C4; -fx-text-fill: #F9A825;";
        } else if (isPast) {
            if (currentUserRsvp != null && currentUserRsvp.getStatus() == Participant.RsvpStatus.ATTENDED) {
                statusText = "YOU ATTENDED THIS EVENT";
                statusStyle += "-fx-background-color: #C8E6C9; -fx-text-fill: #2E7D32;";
            } else if (currentUserRsvp != null && currentUserRsvp.getStatus() == Participant.RsvpStatus.JOINED) {
                statusText = "YOU MISSED THIS EVENT";
                statusStyle += "-fx-background-color: #FFCDD2; -fx-text-fill: #C62828;";
            } else {
                statusText = "EVENT HAS PASSED";
                statusStyle += "-fx-background-color: #E0E0E0; -fx-text-fill: #616161;";
            }
        } else {
            if (currentUserRsvp != null) {
                switch (currentUserRsvp.getStatus()) {
                    case JOINED: statusText = "YOU'VE JOINED THIS EVENT"; statusStyle += "-fx-background-color: #B3E5FC; -fx-text-fill: #0277BD;"; break;
                    case DECLINED: statusText = "YOU DECLINED THIS EVENT"; statusStyle += "-fx-background-color: #FFCCBC; -fx-text-fill: #E64A19;"; break;
                    default: statusText = "EVENT STATUS: " + currentUserRsvp.getStatus(); statusStyle += "-fx-background-color: #E0E0E0; -fx-text-fill: #616161;";
                }
            } else {
                if (Event.PrivacySetting.PRIVATE.equals(currentEvent.getPrivacy())) {
                    if (isFriendWithHost) { statusText = "PRIVATE EVENT (YOU CAN JOIN)"; statusStyle += "-fx-background-color: #C5E1A5; -fx-text-fill: #33691E;";}
                    else { statusText = "PRIVATE EVENT"; statusStyle += "-fx-background-color: #CFD8DC; -fx-text-fill: #37474F;"; }
                } else {
                    statusText = "YOU CAN JOIN THIS EVENT"; statusStyle += "-fx-background-color: #C5E1A5; -fx-text-fill: #33691E;";
                }
            }
        }
        eventStatusBadge.setText(statusText);
        eventStatusBadge.setStyle(statusStyle);
        setupActionButtons(isHost, isPast, currentUserRsvp, isFriendWithHost);
    }
    
    private void loadAndDisplayEventTags() {
        if (currentEvent == null || eventTagsFlowPane == null || tagService == null) return;

        eventTagsFlowPane.getChildren().clear();
        noEventTagsLabel.setVisible(false);
        noEventTagsLabel.setManaged(false);

        manageEventTagsButton.setVisible(true); 
        manageEventTagsButton.setManaged(true);

        try {
            List<Tag> tags = tagService.getTagsForEvent(currentEvent.getId());
            if (tags.isEmpty()) {
                noEventTagsLabel.setVisible(true);
                noEventTagsLabel.setManaged(true);
            } else {
                for (Tag tag : tags) {
                    Label tagLabel = new Label(tag.getTagName());
                    tagLabel.setStyle("-fx-background-color: #CFD8DC; -fx-text-fill: #37474F; -fx-padding: 3 8; -fx-background-radius: 12px; -fx-font-size: 11px;");
                    eventTagsFlowPane.getChildren().add(tagLabel);
                }
            }
        } catch (EventNotFoundException | TagOperationException e) {
            System.err.println("Error loading event tags: " + e.getMessage());
            noEventTagsLabel.setText("Error loading tags.");
            noEventTagsLabel.setVisible(true);
            noEventTagsLabel.setManaged(true);
        }
    }

    private void loadAndDisplayEventComments() {
        if (currentEvent == null || eventCommentsVBox == null || commentService == null) return;

        eventCommentsVBox.getChildren().clear();
        noEventCommentsLabel.setVisible(false);
        noEventCommentsLabel.setManaged(false);
        
        Label loadingCommentsLabel = new Label("Loading comments...");
        loadingCommentsLabel.setStyle("-fx-text-fill: #757575; -fx-font-style: italic; -fx-font-size: 13px;");
        eventCommentsVBox.getChildren().add(loadingCommentsLabel);


        Task<List<Comment>> loadCommentsTask = new Task<>() {
            @Override
            protected List<Comment> call() throws Exception {
                return commentService.getCommentsForEvent(currentEvent.getId());
            }
        };

        loadCommentsTask.setOnSucceeded(_ -> {
            List<Comment> comments = loadCommentsTask.getValue();
            Platform.runLater(() -> {
                eventCommentsVBox.getChildren().remove(loadingCommentsLabel);
                if (comments == null || comments.isEmpty()) {
                    noEventCommentsLabel.setText("No comments yet. Be the first to comment!");
                    noEventCommentsLabel.setVisible(true);
                    noEventCommentsLabel.setManaged(true);
                } else {
                    comments.forEach(comment -> eventCommentsVBox.getChildren().add(createEventCommentNode(comment)));
                }
            });
        });
        loadCommentsTask.setOnFailed(workerStateEvent -> {
            Throwable ex = workerStateEvent.getSource().getException();
            Platform.runLater(() -> {
                 eventCommentsVBox.getChildren().remove(loadingCommentsLabel);
                 noEventCommentsLabel.setText("Failed to load comments." + (ex != null ? " ("+ex.getMessage().substring(0, Math.min(ex.getMessage().length(), 40))+"...)" : ""));
                 noEventCommentsLabel.setVisible(true);
                 noEventCommentsLabel.setManaged(true);
                if (ex != null) ex.printStackTrace();
            });
        });
        new Thread(loadCommentsTask).start();
    }

    private Node createEventCommentNode(Comment comment) {
        VBox commentNode = new VBox(5);
        commentNode.setPadding(new Insets(8));
        commentNode.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 1px 0;");
        commentNode.setPrefWidth(Double.MAX_VALUE);

        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label authorLabel = new Label(comment.getAuthorUsername() != null ? comment.getAuthorUsername() : "Unknown User");
        authorLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        authorLabel.setTextFill(Color.web("#007bff"));
        authorLabel.setCursor(Cursor.HAND);
        authorLabel.setOnMouseClicked(_ -> viewUserProfile(comment.getAuthorUserId()));

        String formattedDate = comment.getCreatedAt() != null ? comment.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm")) : "Date unknown";
        if(comment.isEdited()) formattedDate += " (edited)";
        Label dateLabel = new Label(formattedDate);
        dateLabel.setFont(Font.font("System", 11));
        dateLabel.setTextFill(Color.web("#757575"));
        headerBox.getChildren().addAll(authorLabel, dateLabel);

        Text contentText = new Text(comment.getContent());
        contentText.setWrappingWidth(320); 
        contentText.setStyle("-fx-fill: #333333; -fx-font-size: 13px;");
        TextFlow contentFlow = new TextFlow(contentText);

        if (currentUser != null && (comment.getAuthorUserId() == currentUser.getId() || currentEvent.getHostUserId() == currentUser.getId())) {
            Button deleteButton = new Button("Delete");
            deleteButton.setFont(Font.font("System", FontWeight.NORMAL, 10));
            deleteButton.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-padding: 2 6; -fx-background-radius: 4px; -fx-cursor: hand;");
            deleteButton.setOnAction(_ -> handleDeleteEventCommentAction(comment, commentNode));
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            headerBox.getChildren().addAll(spacer, deleteButton);
        }
        commentNode.getChildren().addAll(headerBox, contentFlow);
        return commentNode;
    }

    private void setupActionButtons(boolean isHost, boolean isPast, Participant currentUserRsvp, boolean isFriendWithHost) {
        actionButtonsContainer.getChildren().clear();
        actionButtonsContainer.setManaged(false);
        actionButtonsContainer.setVisible(false);

        if (isHost && !isPast) {
            Button editButton = createActionButton("Edit Event", "#17a2b8", this::handleEditEventAction);
            Button cancelButton = createActionButton("Cancel Event", "#dc3545", this::handleCancelEventAction);
            actionButtonsContainer.getChildren().addAll(editButton, cancelButton);
        } else if (isHost && isPast) {
            Label manageLabel = new Label("Manage attendance via Participants list.");
            manageLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #757575;");
            actionButtonsContainer.getChildren().add(manageLabel);
        } else if (!isPast) {
            if (currentUserRsvp != null) {
                if (currentUserRsvp.getStatus() == Participant.RsvpStatus.JOINED) {
                    Button leaveButton = createActionButton("Leave Event", "#ffc107", this::handleLeaveEventAction);
                    actionButtonsContainer.getChildren().add(leaveButton);
                } else if (currentUserRsvp.getStatus() == Participant.RsvpStatus.DECLINED) {
                     boolean canRejoin = Event.PrivacySetting.PUBLIC.equals(currentEvent.getPrivacy()) || (Event.PrivacySetting.PRIVATE.equals(currentEvent.getPrivacy()) && isFriendWithHost);
                     if (canRejoin) {
                        Button rejoinButton = createActionButton("Re-Join", "#007bff", _ -> handleRsvpAction(Participant.RsvpStatus.JOINED));
                        actionButtonsContainer.getChildren().add(rejoinButton);
                     } else {
                        Label declinedMsg = new Label("You have declined this event.");
                        declinedMsg.setStyle("-fx-text-fill: #757575; -fx-font-style: italic;");
                        actionButtonsContainer.getChildren().add(declinedMsg);
                     }
                }
            } else {
                if (Event.PrivacySetting.PUBLIC.equals(currentEvent.getPrivacy()) || (Event.PrivacySetting.PRIVATE.equals(currentEvent.getPrivacy()) && isFriendWithHost)) {
                    Button joinButton = createActionButton("Join Event", "#007bff", _ -> handleRsvpAction(Participant.RsvpStatus.JOINED));
                    actionButtonsContainer.getChildren().add(joinButton);
                } else if (Event.PrivacySetting.PRIVATE.equals(currentEvent.getPrivacy()) && !isFriendWithHost) {
                     Label privateMsg = new Label("This is a private event and you are not friends with the host.");
                     privateMsg.setStyle("-fx-text-fill: #757575; -fx-font-style: italic; -fx-wrap-text: true; -fx-max-width: 280;");
                     privateMsg.setAlignment(Pos.CENTER);
                     actionButtonsContainer.getChildren().add(privateMsg);
                     HBox.setHgrow(privateMsg, Priority.ALWAYS);
                }
            }
        }

        if (!actionButtonsContainer.getChildren().isEmpty()) {
            actionButtonsContainer.setManaged(true);
            actionButtonsContainer.setVisible(true);
        }
    }

    private Button createActionButton(String text, String color, javafx.event.EventHandler<ActionEvent> handler) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8 16;");
        button.setOnAction(handler);
        HBox.setHgrow(button, Priority.ALWAYS);
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    @FXML
    private void handleManageEventTagsAction(ActionEvent event) {
         if (currentEvent == null || currentUser == null || mainPanelController == null){
            AlertFactory.showErrorAlert("Error", "Cannot manage tags.", "Ensure event data is loaded and you are logged in.");
            return;
         }
        try {
            FXMLLoaderWrapper.FXMLLoaderResult<ManageEventTagsDialogController> loaderResult =
                    FXMLLoaderWrapper.loadFXMLWithController("manage_event_tags_dialog.fxml");
            ManageEventTagsDialogController dialogController = loaderResult.controller;
            dialogController.initData(currentEvent, this);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Manage Tags for: " + currentEvent.getTitle());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            
            Stage ownerStage = MainApp.getPrimaryStage();
            if(ownerStage != null) {
                 dialogStage.initOwner(ownerStage);
            }

            Scene scene = new Scene(loaderResult.parent);
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.showAndWait();
        } catch (IOException e) {
            AlertFactory.showErrorAlert("Navigation Error", "Could not load tag management dialog.", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePostEventCommentAction(ActionEvent event) {
        String content = newEventCommentArea.getText().trim();
        if (ValidationUtils.isNullOrEmpty(content)) {
            AlertFactory.showWarningAlert("Input Error", "Comment cannot be empty.", null);
            return;
        }
        if(content.length() > 2000) {
            AlertFactory.showWarningAlert("Input Error", "Comment is too long.", "Maximum 2000 characters allowed.");
            return;
        }

        if (currentEvent == null || currentUser == null || commentService == null) {
            AlertFactory.showErrorAlert("Error", "Cannot post comment.", "System not ready or user not logged in.");
            return;
        }
        
        postEventCommentButton.setDisable(true);
        postEventCommentButton.setText("Posting...");

        Task<Comment> postTask = new Task<>() {
            @Override
            protected Comment call() throws Exception {
                return commentService.postCommentToEvent(currentEvent.getId(), currentUser, content);
            }
        };
        postTask.setOnSucceeded(_ -> {
            Platform.runLater(() -> {
                newEventCommentArea.clear();
                loadAndDisplayEventComments();
                postEventCommentButton.setDisable(false);
                postEventCommentButton.setText("Post Comment");
            });
        });
        postTask.setOnFailed(workerStateEvent -> {
             Throwable ex = workerStateEvent.getSource().getException();
             Platform.runLater(() -> {
                AlertFactory.showErrorAlert("Comment Error", "Could not post comment.", (ex != null ? ex.getMessage() : "Unknown error."));
                if(ex != null) ex.printStackTrace();
                postEventCommentButton.setDisable(false);
                postEventCommentButton.setText("Post Comment");
            });
        });
        new Thread(postTask).start();
    }

    private void handleDeleteEventCommentAction(Comment commentToDelete, Node commentNodeUI) {
         if (currentEvent == null || currentUser == null || commentService == null) {
            AlertFactory.showErrorAlert("Error", "Cannot delete comment.", "System not ready or user not logged in.");
            return;
        }
        AlertFactory.showConfirmationAlert("Delete Comment", "Are you sure?", "Do you want to delete this comment permanently?")
            .filter(response -> response == ButtonType.OK)
            .ifPresent(_ -> {
                Task<Void> deleteTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        commentService.deleteComment(commentToDelete.getCommentId(), currentUser);
                        return null;
                    }
                };
                deleteTask.setOnSucceeded(_ -> Platform.runLater(this::loadAndDisplayEventComments));
                deleteTask.setOnFailed(workerStateEvent -> {
                    Throwable ex = workerStateEvent.getSource().getException();
                    Platform.runLater(() -> {
                        AlertFactory.showErrorAlert("Delete Error", "Could not delete comment.", (ex != null ? ex.getMessage() : "Unknown error."));
                        if(ex != null) ex.printStackTrace();
                    });
                });
                new Thread(deleteTask).start();
            });
    }
    
    private void handleRsvpAction(Participant.RsvpStatus newStatus) {
        String originalStatusText = eventStatusBadge.getText();
        String originalStatusStyle = eventStatusBadge.getStyle();
        actionButtonsContainer.setDisable(true);

        Task<Participant> rsvpTask = new Task<>() {
            @Override
            protected Participant call() throws Exception {
                if (newStatus == Participant.RsvpStatus.JOINED) {
                    return participantService.joinEvent(currentEvent.getId(), currentUser);
                } else if (newStatus == Participant.RsvpStatus.DECLINED) {
                    return participantService.updateRsvpStatus(currentEvent.getId(), currentUser, Participant.RsvpStatus.DECLINED);
                }
                throw new IllegalStateException("Unhandled RSVP action: " + newStatus);
            }
        };
        rsvpTask.setOnSucceeded(_ -> {
            currentUserRsvpCache = rsvpTask.getValue();
            String actionText = (newStatus == Participant.RsvpStatus.JOINED) ? "joined" : "RSVP updated for";
            AlertFactory.showInformationAlert("Success", "RSVP Updated", "You have successfully " + actionText + " the event: " + currentEvent.getTitle());
            refreshViewData(); 
            actionButtonsContainer.setDisable(false);
        });
        rsvpTask.setOnFailed(_ -> {
            AlertFactory.showErrorAlert("Error", "RSVP Update Failed", (rsvpTask.getException()!=null? rsvpTask.getException().getMessage() : "Unknown error"));
            if (rsvpTask.getException() != null) rsvpTask.getException().printStackTrace();
            eventStatusBadge.setText(originalStatusText);
            eventStatusBadge.setStyle(originalStatusStyle);
            actionButtonsContainer.setDisable(false);
            refreshViewData();
        });
        new Thread(rsvpTask).start();
    }

    private void handleLeaveEventAction(ActionEvent eventAction) {
        if (mainPanelController == null) return;
        AlertFactory.showConfirmationAlert("Leave Event", "Are you sure?", "Do you really want to leave the event '" + currentEvent.getTitle() + "'?")
            .filter(response -> response == ButtonType.OK)
            .ifPresent(_ -> {
                actionButtonsContainer.setDisable(true);
                Task<Void> leaveTask = new Task<>() {
                    @Override protected Void call() throws Exception { participantService.leaveEvent(currentEvent.getId(), currentUser); return null; }
                };
                leaveTask.setOnSucceeded(_ -> {
                    AlertFactory.showInformationAlert("Success", "Left Event", "You have left the event: " + currentEvent.getTitle());
                    refreshViewData();
                    actionButtonsContainer.setDisable(false);
                });
                leaveTask.setOnFailed(_ -> {
                    AlertFactory.showErrorAlert("Error", "Failed to Leave Event", (leaveTask.getException()!=null ? leaveTask.getException().getMessage() : "Unknown error") );
                    if (leaveTask.getException() != null) leaveTask.getException().printStackTrace();
                    actionButtonsContainer.setDisable(false);
                     refreshViewData();
                });
                new Thread(leaveTask).start();
            });
    }

    private void handleEditEventAction(ActionEvent eventAction) {
        if (mainPanelController == null || currentEvent == null) return;
        try {
            FXMLLoaderWrapper.FXMLLoaderResult<EditGatheringController> loaderResult = FXMLLoaderWrapper.loadFXMLWithController("edit_gathering.fxml");
            EditGatheringController editController = loaderResult.controller;
            editController.setMainPanelController(mainPanelController);
            editController.loadEventToEdit(currentEvent);
            mainPanelController.setCurrentViewFxmlPath("edit_gathering.fxml");
            mainPanelController.setContent(loaderResult.parent);
        } catch (IOException e) {
            AlertFactory.showErrorAlert("Navigation Error", "Could not load edit event view.", e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleCancelEventAction(ActionEvent eventAction) {
        if (mainPanelController == null || currentEvent == null || currentUser == null) return;
        AlertFactory.showConfirmationAlert("Cancel Event", "Are you sure?", "Do you really want to cancel the event '" + currentEvent.getTitle() + "'? This action cannot be undone.")
            .filter(response -> response == ButtonType.OK)
            .ifPresent(_ -> {
                actionButtonsContainer.setDisable(true);
                Task<Void> cancelTask = new Task<>() {
                    @Override protected Void call() throws Exception { eventService.cancelEvent(currentEvent.getId(), currentUser); return null; }
                };
                cancelTask.setOnSucceeded(_ -> {
                    AlertFactory.showInformationAlert("Success", "Event Cancelled", "The event '" + currentEvent.getTitle() + "' has been cancelled.");
                    Platform.runLater(() -> {
                        String targetView = (previousViewFxml != null && mainPanelController.getCurrentViewFxmlPath().equals("event_details_panel.fxml"))
                                          ? previousViewFxml
                                          : "home_feed.fxml";
                        mainPanelController.loadView(targetView, controller -> {
                             if(controller instanceof HomeFeedController) ((HomeFeedController) controller).refreshViewData();
                             else if (controller instanceof MyGatheringsController) ((MyGatheringsController) controller).refreshViewData();
                        });
                    });
                });
                cancelTask.setOnFailed(_ -> {
                    AlertFactory.showErrorAlert("Error", "Failed to Cancel Event", (cancelTask.getException() != null ? cancelTask.getException().getMessage() : "Unknown error.") );
                    if (cancelTask.getException() != null) cancelTask.getException().printStackTrace();
                    actionButtonsContainer.setDisable(false);
                });
                new Thread(cancelTask).start();
            });
    }

    private void navigateToParticipantsList() {
        if (mainPanelController == null || currentEvent == null) return;
        try {
            FXMLLoaderWrapper.FXMLLoaderResult<EventParticipantsListController> loaderResult = FXMLLoaderWrapper.loadFXMLWithController("event_participants_list.fxml");
            EventParticipantsListController participantsController = loaderResult.controller;
            participantsController.setMainPanelController(mainPanelController);
            participantsController.setPreviousViewFxml("event_details_panel.fxml");
            participantsController.loadParticipants(currentEvent, eventParticipantsCache);
            mainPanelController.setCurrentViewFxmlPath("event_participants_list.fxml");
            mainPanelController.setContent(loaderResult.parent);
        } catch (IOException e) {
            AlertFactory.showErrorAlert("Navigation Error", "Could not load participants view.", e.getMessage());
        }
    }

    private void viewUserProfile(int userId) {
        if (mainPanelController == null) return;
        if (currentUser != null && userId == currentUser.getId()) {
            mainPanelController.loadView("profile.fxml");
            mainPanelController.setCurrentViewFxmlPath("profile.fxml");
        } else {
            try {
                FXMLLoaderWrapper.FXMLLoaderResult<OtherProfileController> loaderResult = FXMLLoaderWrapper.loadFXMLWithController("other_profile_panel.fxml");
                OtherProfileController otherProfileController = loaderResult.controller;
                otherProfileController.setMainPanelController(mainPanelController);
                otherProfileController.setPreviousViewFxml("event_details_panel.fxml"); 
                otherProfileController.loadUserProfile(userId);
                mainPanelController.setCurrentViewFxmlPath("other_profile_panel.fxml");
                mainPanelController.setContent(loaderResult.parent);
            } catch (IOException ioEx) {
                AlertFactory.showErrorAlert("Navigation Error", "Could not load user profile.", ioEx.getMessage());
            }
        }
    }

    @FXML
    private void handleBackButtonAction(ActionEvent event) {
        if (mainPanelController != null) {
            String viewToLoad = (previousViewFxml != null && !previousViewFxml.isEmpty()) ? previousViewFxml : "home_feed.fxml";
             mainPanelController.loadView(viewToLoad, controller -> {
                if (controller instanceof HomeFeedController && "home_feed.fxml".equals(viewToLoad)) {
                    ((HomeFeedController) controller).refreshViewData();
                } else if (controller instanceof MyGatheringsController && "my_gatherings_panel.fxml".equals(viewToLoad)) {
                     ((MyGatheringsController) controller).refreshViewData();
                } else if (controller instanceof ProfileController && "profile.fxml".equals(viewToLoad)) {
                     ((ProfileController) controller).refreshViewData();
                } else if (controller instanceof OtherProfileController && "other_profile_panel.fxml".equals(viewToLoad)) {
                    OtherProfileController opc = (OtherProfileController) controller;
                    if(opc.displayedUser != null) opc.refreshViewData();
                } else if (controller instanceof EventParticipantsListController && "event_participants_list.fxml".equals(viewToLoad)) {
                     ((EventParticipantsListController) controller).refreshViewData();
                }
            });
        } else {
            AlertFactory.showErrorAlert("Navigation Error", "Cannot go back.", "Main panel reference is missing.");
        }
    }
}