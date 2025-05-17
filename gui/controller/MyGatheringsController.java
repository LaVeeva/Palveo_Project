package com.palveo.gui.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import com.palveo.gui.manager.SessionManager;
import com.palveo.gui.util.AlertFactory;
import com.palveo.gui.util.FXMLLoaderWrapper;
import com.palveo.model.Event;
import com.palveo.model.Participant;
import com.palveo.model.User;
import com.palveo.service.EventService;
import com.palveo.service.ParticipantService;
import com.palveo.service.UserService;
import com.palveo.service.impl.EventServiceImpl;
import com.palveo.service.impl.ParticipantServiceImpl;
import com.palveo.service.impl.UserServiceImpl;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class MyGatheringsController {

    @FXML
    private ScrollPane gatheringsScrollPane;
    @FXML
    private VBox gatheringsContentBox;
    @FXML
    private Label statusLabel;

    private MainPanelController mainPanelController;
    private EventService eventService;
    private ParticipantService participantService;
    private UserService userService;
    private SessionManager sessionManager;
    private User currentUser;

    public MyGatheringsController() {
        this.eventService = new EventServiceImpl();
        this.participantService = new ParticipantServiceImpl();
        this.userService = new UserServiceImpl();
        this.sessionManager = SessionManager.getInstance();
    }

    public void setMainPanelController(MainPanelController mainPanelController) {
        this.mainPanelController = mainPanelController;
    }

    @FXML
    private void initialize() {
       refreshViewData();
    }
    
    public void refreshViewData() {
        this.currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            statusLabel.setText("Error: User not logged in.");
            AlertFactory.showErrorAlert("Error", "Not Logged In",
                    "Cannot display your gatherings.");
            return;
        }
        loadUserAssociatedGatherings();
    }

    private void loadUserAssociatedGatherings() {
        statusLabel.setText("Loading your gatherings...");
        statusLabel.setVisible(true);
        gatheringsContentBox.getChildren().clear();
        if (!gatheringsContentBox.getChildren().contains(statusLabel)) {
            gatheringsContentBox.getChildren().add(statusLabel);
        }

        Task<List<EventCardDataWrapper>> loadTask = new Task<>() {
            @Override
            protected List<EventCardDataWrapper> call() throws Exception {
                List<Event> hostedEvents = eventService.getEventsHostedBy(currentUser.getId());
                List<Event> participatedEvents = participantService.getEventsUserIsAssociatedWith(currentUser.getId());

                List<EventCardDataWrapper> eventWrappers = new ArrayList<>();
                
                for(Event event : hostedEvents) {
                    Optional<Participant> rsvpOpt = participantService.getUserRsvpForEvent(event.getId(), currentUser.getId());
                     String hostUsername = userService.getUserById(event.getHostUserId())
                                             .map(User::getUsername).orElse("Unknown Host");
                    eventWrappers.add(new EventCardDataWrapper(event, hostUsername, rsvpOpt.orElse(null), true));
                }

                for(Event event: participatedEvents) {
                    if(hostedEvents.stream().anyMatch(h -> h.getId() == event.getId())) continue; 
                    
                    Optional<Participant> rsvpOpt = participantService.getUserRsvpForEvent(event.getId(), currentUser.getId());
                    if (rsvpOpt.isPresent()) {
                         String hostUsername = userService.getUserById(event.getHostUserId())
                                                 .map(User::getUsername).orElse("Unknown Host");
                        eventWrappers.add(new EventCardDataWrapper(event, hostUsername, rsvpOpt.get(), false));
                    }
                }
                
                Collections.sort(eventWrappers,
                        Comparator.comparing((EventCardDataWrapper wrapper) -> wrapper.event.getEventDateTime()).reversed()
                );
                return eventWrappers;
            }
        };

        loadTask.setOnSucceeded(_ -> {
            List<EventCardDataWrapper> wrappedEvents = loadTask.getValue();
            Platform.runLater(() -> displayGatherings(wrappedEvents));
        });

        loadTask.setOnFailed(_ -> {
            Throwable exception = loadTask.getException();
            Platform.runLater(() -> {
                statusLabel.setText("Failed to load your gatherings.");
                AlertFactory.showErrorAlert("Loading Error", "Could not load your gatherings",
                        exception != null ? exception.getMessage() : "Unknown error");
                if(exception != null) exception.printStackTrace();
            });
        });
        new Thread(loadTask).start();
    }


    private void displayGatherings(List<EventCardDataWrapper> wrappedEvents) {
        gatheringsContentBox.getChildren().clear();

        if (wrappedEvents == null || wrappedEvents.isEmpty()) {
            statusLabel.setText("You aren't associated with any gatherings yet. Host one or join some from the feed!");
            if (!gatheringsContentBox.getChildren().contains(statusLabel)) {
                gatheringsContentBox.getChildren().add(statusLabel);
            }
            statusLabel.setVisible(true);
            return;
        }
        statusLabel.setVisible(false);

        for (EventCardDataWrapper wrapper : wrappedEvents) {
            Node eventNode = createGatheringNode(wrapper.event, wrapper.hostUsername, wrapper.rsvp, wrapper.isHost);
            gatheringsContentBox.getChildren().add(eventNode);
        }
    }

    private static class EventCardDataWrapper {
        Event event;
        String hostUsername;
        Participant rsvp;
        boolean isHost;

        public EventCardDataWrapper(Event event, String hostUsername, Participant rsvp, boolean isHost) {
            this.event = event;
            this.hostUsername = hostUsername;
            this.rsvp = rsvp;
            this.isHost = isHost;
        }
    }

    private Node createGatheringNode(Event event, String hostUsername, Participant rsvp, boolean isCurrentUserHost) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle(
                "-fx-background-color: #FFFFFF; -fx-border-color: #EEEEEE; -fx-border-width: 1px; -fx-background-radius: 8px; -fx-border-radius: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0.1, 0, 2);");
        card.setPrefWidth(Double.MAX_VALUE);

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(event.getTitle());
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.web("#333333"));
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label categoryLabel = new Label(event.getCategory() != null ? event.getCategory().toString() : "N/A");
        categoryLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 10));
        categoryLabel.setStyle(
                "-fx-background-color: #E3F2FD; -fx-text-fill: #1E88E5; -fx-padding: 3 7; -fx-background-radius: 10;");

        topRow.getChildren().addAll(titleLabel, categoryLabel);

        VBox detailsBox = new VBox(3);
        Label hostInfoLabel;
        if(isCurrentUserHost){
            hostInfoLabel = new Label("You are hosting this event");
             hostInfoLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            hostInfoLabel.setTextFill(Color.web("#1E88E5"));
        } else {
            hostInfoLabel = new Label("Hosted by: " + hostUsername);
            hostInfoLabel.setFont(Font.font("System", 12));
            hostInfoLabel.setTextFill(Color.web("#757575"));
            hostInfoLabel.setCursor(Cursor.HAND);
            hostInfoLabel.setOnMouseClicked(_ -> {
                Optional<User> hostUserOpt = userService.getUserByUsername(hostUsername);
                if (hostUserOpt.isPresent() && mainPanelController != null) {
                    User hostUser = hostUserOpt.get();
                    if (hostUser.getId() == currentUser.getId()) {
                        mainPanelController.loadView("profile.fxml");
                    } else {
                        try {
                            FXMLLoaderWrapper.FXMLLoaderResult<OtherProfileController> loaderResult =
                                    FXMLLoaderWrapper.loadFXMLWithController("other_profile_panel.fxml");
                            OtherProfileController otherProfileController = loaderResult.controller;
                            otherProfileController.setMainPanelController(mainPanelController);
                            otherProfileController.loadUserProfile(hostUser.getId());
                            mainPanelController.setCurrentViewFxmlPath("other_profile_panel.fxml");
                            mainPanelController.setContent(loaderResult.parent);
                        } catch (IOException ioEx) {
                            ioEx.printStackTrace();
                        }
                    }
                }
            });
        }
        detailsBox.getChildren().add(hostInfoLabel);

        Label dateTimeLabel = new Label("On: " + event.getEventDateTime()
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm")));
        dateTimeLabel.setFont(Font.font("System", 12));
        dateTimeLabel.setTextFill(Color.web("#555555"));
        detailsBox.getChildren().add(dateTimeLabel);


        HBox bottomSection = new HBox();
        bottomSection.setAlignment(Pos.CENTER_LEFT);

        Label statusBadge = new Label();
        statusBadge.setFont(Font.font("System", FontWeight.BOLD, 10));
        statusBadge.setStyle("-fx-padding: 3 8; -fx-background-radius: 12px;");

        boolean isPastEvent = event.getEventDateTime().isBefore(LocalDateTime.now());
        Participant.RsvpStatus currentStatus = (rsvp != null) ? rsvp.getStatus() : null;
        
        if(isCurrentUserHost && currentStatus == null && !isPastEvent){
             statusBadge.setText("HOSTING");
             statusBadge.setStyle(statusBadge.getStyle() + "-fx-background-color: #FFF9C4; -fx-text-fill: #F9A825;");
        } else if (isCurrentUserHost && currentStatus == null && isPastEvent){
             statusBadge.setText("HOSTED (PAST)");
             statusBadge.setStyle(statusBadge.getStyle() + "-fx-background-color: #E0E0E0; -fx-text-fill: #616161;");
        } else if (isPastEvent) {
            if (currentStatus == Participant.RsvpStatus.ATTENDED) {
                statusBadge.setText("ATTENDED");
                statusBadge.setStyle(statusBadge.getStyle()
                        + "-fx-background-color: #C8E6C9; -fx-text-fill: #2E7D32;");
            } else if (currentStatus == Participant.RsvpStatus.JOINED
                    || currentStatus == Participant.RsvpStatus.INVITED) {
                statusBadge.setText("MISSED");
                statusBadge.setStyle(statusBadge.getStyle()
                        + "-fx-background-color: #FFCDD2; -fx-text-fill: #C62828;");
            } else if (currentStatus == Participant.RsvpStatus.DECLINED) {
                statusBadge.setText("DECLINED (Past)");
                statusBadge.setStyle(statusBadge.getStyle()
                        + "-fx-background-color: #FFCCBC; -fx-text-fill: #E64A19;");
            } else {
                statusBadge.setText("EVENT PASSED");
                statusBadge.setStyle(statusBadge.getStyle()
                        + "-fx-background-color: #E0E0E0; -fx-text-fill: #616161;");
            }
        } else { 
            if (currentStatus == Participant.RsvpStatus.JOINED) {
                statusBadge.setText("JOINED");
                statusBadge.setStyle(statusBadge.getStyle()
                        + "-fx-background-color: #B3E5FC; -fx-text-fill: #0277BD;");
            } else if (currentStatus == Participant.RsvpStatus.INVITED) {
                statusBadge.setText("INVITED");
                statusBadge.setStyle(statusBadge.getStyle()
                        + "-fx-background-color: #FFF9C4; -fx-text-fill: #F9A825;");
            } else if (currentStatus == Participant.RsvpStatus.DECLINED) {
                statusBadge.setText("DECLINED");
                statusBadge.setStyle(statusBadge.getStyle()
                        + "-fx-background-color: #FFCCBC; -fx-text-fill: #E64A19;");
            } else {
                 if(!isCurrentUserHost){
                    statusBadge.setText("NOT JOINED");
                    statusBadge.setStyle(statusBadge.getStyle() + "-fx-background-color: #E0E0E0; -fx-text-fill: #616161;");
                 } else {
                    statusBadge.setVisible(false);
                    statusBadge.setManaged(false);
                 }
            }
        }


        ImageView privacyIconView = new ImageView();
        privacyIconView.setFitHeight(14);
        privacyIconView.setFitWidth(14);
        Label privacyTextLabel = new Label();
        privacyTextLabel.setFont(Font.font("System", FontPosture.ITALIC, 11));
        privacyTextLabel.setTextFill(Color.web("#757575"));

        String privacyIconPath = null;
        String privacyDisplayText = "";

        if (Event.PrivacySetting.PUBLIC.equals(event.getPrivacy())) {
            privacyIconPath = "/images/public_icon.png";
            privacyDisplayText = Event.PrivacySetting.PUBLIC.toString();
        } else if (Event.PrivacySetting.PRIVATE.equals(event.getPrivacy())) {
            privacyIconPath = "/images/private_icon.png";
            privacyDisplayText = Event.PrivacySetting.PRIVATE.toString();
        }

        if (privacyIconPath != null) {
            try {
                Image privacyImg = new Image(getClass().getResourceAsStream(privacyIconPath));
                if (privacyImg != null && !privacyImg.isError()) {
                    privacyIconView.setImage(privacyImg);
                } else {
                    privacyIconView.setImage(null);
                }
            } catch (Exception e) {
                privacyIconView.setImage(null);
            }
        }
        privacyTextLabel.setText(privacyDisplayText);
        HBox privacyDisplayBox = new HBox(3, privacyIconView, privacyTextLabel);
        privacyDisplayBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bottomSection.getChildren().addAll(privacyDisplayBox, spacer);
        if (statusBadge.isVisible()) {
            bottomSection.getChildren().add(statusBadge);
        }

        card.getChildren().addAll(topRow, detailsBox, bottomSection);
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(e -> {
             Node target = (Node) e.getTarget();
            boolean isHostLabelClicked = false;
            while (target != null && target != card) {
                if (target.equals(hostInfoLabel)) {
                    isHostLabelClicked = true;
                    break;
                }
                target = target.getParent();
            }
            if (!isHostLabelClicked) {
                 showEventDetails(event);
            }
        });
        return card;
    }

    private void showEventDetails(Event event) {
        if (mainPanelController == null) {
            AlertFactory.showErrorAlert("Navigation Error", "Cannot switch view.",
                    "Main panel reference is missing.");
            return;
        }
        try {
            FXMLLoaderWrapper.FXMLLoaderResult<EventDetailsController> loaderResult =
                    FXMLLoaderWrapper.loadFXMLWithController("event_details_panel.fxml");
            EventDetailsController detailsController = loaderResult.controller;
            detailsController.setMainPanelController(mainPanelController);
            detailsController.setPreviousViewFxml("my_gatherings_panel.fxml");
            detailsController.loadEventData(event);
            mainPanelController.setContent(loaderResult.parent);
             mainPanelController.setCurrentViewFxmlPath("event_details_panel.fxml");
        } catch (IOException e) {
            AlertFactory.showErrorAlert("Navigation Error", "Could not load event details view.",
                    e.getMessage());
            e.printStackTrace();
        }
    }
}