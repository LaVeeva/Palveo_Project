package com.palveo.gui.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.palveo.gui.manager.SessionManager;
import com.palveo.gui.util.AlertFactory;
import com.palveo.gui.util.FXMLLoaderWrapper;
import com.palveo.model.Event;
import com.palveo.model.Participant;
import com.palveo.model.User;
import com.palveo.service.EventService;
import com.palveo.service.ParticipantService;
import com.palveo.service.UserService;
import com.palveo.service.exception.EventOperationException;
import com.palveo.service.exception.UserNotFoundException;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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

public class HomeFeedController {

    @FXML
    private ScrollPane feedScrollPane;
    @FXML
    private VBox feedContentBox;
    @FXML
    private Label loadingLabel;
    @FXML
    private TextField searchGatheringsField;

    private MainPanelController mainPanelController;
    private EventService eventService;
    private UserService userService;
    private ParticipantService participantService;
    private SessionManager sessionManager;
    private User currentUser;
    private List<Event> allLoadedEventsCache;

    public HomeFeedController() {
        this.eventService = new EventServiceImpl();
        this.userService = new UserServiceImpl();
        this.participantService = new ParticipantServiceImpl();
        this.sessionManager = SessionManager.getInstance();
    }

    public void setMainPanelController(MainPanelController mainPanelController) {
        this.mainPanelController = mainPanelController;
    }
    
    public void refreshViewData() {
        loadFeedEvents();
    }

    @FXML
    private void initialize() {
        this.currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            loadingLabel.setText("Error: User not logged in.");
            AlertFactory.showErrorAlert("Error", "Not Logged In", "Cannot display feed.");
            searchGatheringsField.setDisable(true);
            return;
        }
        refreshViewData();

        searchGatheringsField.textProperty().addListener((_, _, newValue) -> {
            filterAndDisplayEvents(newValue);
        });
    }

    private void loadFeedEvents() {
        loadingLabel.setText("Loading gatherings...");
        loadingLabel.setVisible(true);
        feedContentBox.getChildren().clear();
        if (!feedContentBox.getChildren().contains(loadingLabel)) {
            feedContentBox.getChildren().add(loadingLabel);
        }

        Task<List<Event>> loadEventsTask = new Task<>() {
            @Override
            protected List<Event> call() throws Exception {
                return eventService.getUpcomingEventsForFeed(currentUser);
            }
        };

        loadEventsTask.setOnSucceeded(_ -> {
            allLoadedEventsCache = loadEventsTask.getValue();
            Platform.runLater(() -> filterAndDisplayEvents(searchGatheringsField.getText()));
        });

        loadEventsTask.setOnFailed(_ -> {
            Throwable exception = loadEventsTask.getException();
            Platform.runLater(() -> {
                loadingLabel.setText("Failed to load gatherings.");
                String errorMessage = "Could not load events";
                if (exception instanceof UserNotFoundException) {
                    errorMessage = "User session error: " + exception.getMessage();
                } else if (exception instanceof EventOperationException) {
                    errorMessage = "Event operation error: " + exception.getMessage();
                } else if (exception != null) {
                    errorMessage +=
                            (exception.getMessage() != null ? ": " + exception.getMessage() : "");
                }
                AlertFactory.showErrorAlert("Loading Error", errorMessage,
                        (exception != null && exception.getCause() != null)
                                ? exception.getCause().getMessage()
                                : "Please try again.");
                if (exception != null)
                    exception.printStackTrace();
            });
        });
        new Thread(loadEventsTask).start();
    }

    private void filterAndDisplayEvents(String searchTerm) {
        feedContentBox.getChildren().clear();
        List<Event> eventsToDisplay;

        if (allLoadedEventsCache == null) {
            loadingLabel.setText("No events loaded yet. Please wait or try again.");
            if (!feedContentBox.getChildren().contains(loadingLabel)) {
                feedContentBox.getChildren().add(loadingLabel);
            }
            loadingLabel.setVisible(true);
            return;
        }

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            eventsToDisplay = allLoadedEventsCache;
        } else {
            String lowerCaseSearchTerm = searchTerm.trim().toLowerCase();
            eventsToDisplay = allLoadedEventsCache.stream()
                    .filter(event -> (event.getTitle() != null
                            && event.getTitle().toLowerCase().contains(lowerCaseSearchTerm))
                            || (event.getCategory() != null && event.getCategory().toString().toLowerCase()
                                    .contains(lowerCaseSearchTerm))
                            || (event.getDescription() != null && event.getDescription()
                                    .toLowerCase().contains(lowerCaseSearchTerm)))
                    .collect(Collectors.toList());
        }
        
        if (eventsToDisplay != null) {
            eventsToDisplay.sort(Comparator.comparing(Event::getEventDateTime));
        }
        displayEvents(eventsToDisplay);
    }

    private void displayEvents(List<Event> events) {
        feedContentBox.getChildren().clear();

        if (events == null || events.isEmpty()) {
            String message = (searchGatheringsField.getText() == null || searchGatheringsField.getText().trim().isEmpty())
                           ? "No upcoming gatherings found in your feed."
                           : "No gatherings found matching your search criteria.";
            loadingLabel.setText(message);
            if (!feedContentBox.getChildren().contains(loadingLabel)) {
                feedContentBox.getChildren().add(loadingLabel);
            }
            loadingLabel.setVisible(true);
            return;
        }
        loadingLabel.setVisible(false);
        if (feedContentBox.getChildren().contains(loadingLabel)) {
            feedContentBox.getChildren().remove(loadingLabel);
        }

        for (Event event : events) {
            Task<EventCardData> dataFetchTask = new Task<>() {
                @Override
                protected EventCardData call() throws Exception {
                    Optional<User> hostOpt = userService.getUserById(event.getHostUserId());
                    Optional<Participant> rsvpOpt = participantService
                            .getUserRsvpForEvent(event.getId(), currentUser.getId());
                    return new EventCardData(hostOpt.map(User::getUsername).orElse("Unknown Host"),
                            rsvpOpt.orElse(null));
                }
            };

            dataFetchTask.setOnSucceeded(_ -> {
                EventCardData cardData = dataFetchTask.getValue();
                Platform.runLater(() -> {
                    Node eventNode =
                            createEventNode(event, cardData.hostUsername, cardData.currentUserRsvp);
                    feedContentBox.getChildren().add(eventNode);
                });
            });

            dataFetchTask.setOnFailed(workerStateEvent -> {
                Throwable ex = workerStateEvent.getSource().getException();
                System.err.println("Failed to fetch data for event card: " + event.getTitle()
                        + " - " + (ex != null ? ex.getMessage() : "Unknown error"));
                if (ex != null)
                    ex.printStackTrace();
                Platform.runLater(() -> {
                    Node eventNode = createEventNode(event, "Error loading host", null);
                    feedContentBox.getChildren().add(eventNode);
                });
            });
            new Thread(dataFetchTask).start();
        }
    }

    private static class EventCardData {
        String hostUsername;
        Participant currentUserRsvp;

        public EventCardData(String hostUsername, Participant rsvp) {
            this.hostUsername = hostUsername;
            this.currentUserRsvp = rsvp;
        }
    }

    private Node createEventNode(Event event, String hostUsername, Participant currentUserRsvp) {
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
        Label hostLabel = new Label("Hosted by: " + hostUsername);
        hostLabel.setFont(Font.font("System", 12));
        hostLabel.setTextFill(Color.web("#757575"));
        hostLabel.setCursor(Cursor.HAND);
        hostLabel.setOnMouseClicked(_ -> {
            if (hostUsername.equals("Unknown Host") || hostUsername.equals("Error loading host"))
                return;
            Optional<User> hostUserOpt = userService.getUserByUsername(hostUsername);
            if (hostUserOpt.isPresent() && mainPanelController != null) {
                User hostUser = hostUserOpt.get();
                if (hostUser.getId() == currentUser.getId()) {
                    mainPanelController.loadView("profile.fxml");
                    mainPanelController.setCurrentViewFxmlPath("profile.fxml");
                } else {
                    try {
                        FXMLLoaderWrapper.FXMLLoaderResult<OtherProfileController> loaderResult =
                                FXMLLoaderWrapper
                                        .loadFXMLWithController("other_profile_panel.fxml");
                        OtherProfileController otherProfileController = loaderResult.controller;
                        otherProfileController.setMainPanelController(mainPanelController);
                        otherProfileController.setPreviousViewFxml("home_feed.fxml");
                        otherProfileController.loadUserProfile(hostUser.getId());
                        mainPanelController.setCurrentViewFxmlPath("other_profile_panel.fxml");
                        mainPanelController.setContent(loaderResult.parent);
                    } catch (IOException ioEx) {
                        ioEx.printStackTrace();
                        AlertFactory.showErrorAlert("Navigation Error", "Could not load profile",
                                ioEx.getMessage());
                    }
                }
            }
        });

        Label dateTimeLabel = new Label("On: " + event.getEventDateTime()
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm")));
        dateTimeLabel.setFont(Font.font("System", 12));
        dateTimeLabel.setTextFill(Color.web("#555555"));

        detailsBox.getChildren().addAll(hostLabel, dateTimeLabel);

        if (event.getLocationString() != null && !event.getLocationString().isEmpty()) {
            Label locationLabel = new Label("Location: " + event.getLocationString());
            locationLabel.setFont(Font.font("System", 11));
            locationLabel.setTextFill(Color.web("#757575"));
            locationLabel.setWrapText(true);
            detailsBox.getChildren().add(locationLabel);
        }

        HBox bottomSection = new HBox();
        bottomSection.setAlignment(Pos.CENTER_LEFT);
        bottomSection.setSpacing(5);

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
                    System.err.println("Could not load privacy icon: " + privacyIconPath);
                }
            } catch (Exception e) {
                privacyIconView.setImage(null);
                System.err.println(
                        "Error loading privacy icon " + privacyIconPath + ": " + e.getMessage());
            }
        }
        privacyTextLabel.setText(privacyDisplayText);

        HBox privacyDisplayBox = new HBox(3, privacyIconView, privacyTextLabel);
        privacyDisplayBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button actionButton = new Button();
        actionButton.setFont(Font.font("System", FontWeight.SEMI_BOLD, 11));
        String baseButtonStyle = "-fx-background-radius: 15px; -fx-padding: 5 15;";
        actionButton.setStyle(baseButtonStyle);

        boolean isPastEvent = event.getEventDateTime().isBefore(LocalDateTime.now());
        boolean isHost = event.getHostUserId() == currentUser.getId();

        if (isHost) {
            actionButton.setText("Manage");
            actionButton.setStyle(
                    baseButtonStyle + "-fx-background-color: #6c757d; -fx-text-fill: white;");
            actionButton.setOnAction(_ -> showEventDetails(event));
        } else if (isPastEvent) {
            actionButton.setText("View Details");
            actionButton.setStyle(
                    baseButtonStyle + "-fx-background-color: #6c757d; -fx-text-fill: white;");
            actionButton.setOnAction(_ -> showEventDetails(event));
        } else {
            if (currentUserRsvp != null
                    && (currentUserRsvp.getStatus() == Participant.RsvpStatus.JOINED
                            || currentUserRsvp.getStatus() == Participant.RsvpStatus.ATTENDED)) {
                actionButton.setText("Joined");
                actionButton.setStyle(
                        baseButtonStyle + "-fx-background-color: #28a745; -fx-text-fill: white;");
                actionButton.setOnAction(_ -> showEventDetails(event)); 
            } else if (currentUserRsvp != null
                    && currentUserRsvp.getStatus() == Participant.RsvpStatus.DECLINED) {
                actionButton.setText("Re-Join");
                actionButton.setStyle(
                        baseButtonStyle + "-fx-background-color: #007bff; -fx-text-fill: white;");
                actionButton.setOnAction(_ -> handleJoinAction(event, actionButton));
            } else {
                actionButton.setText("Join");
                actionButton.setStyle(
                        baseButtonStyle + "-fx-background-color: #007bff; -fx-text-fill: white;");
                actionButton.setOnAction(_ -> handleJoinAction(event, actionButton));
            }
        }

        bottomSection.getChildren().addAll(privacyDisplayBox, spacer, actionButton);
        card.getChildren().addAll(topRow, detailsBox, bottomSection);

        card.setOnMouseClicked(e -> {
            Node target = (Node) e.getTarget();
            boolean isInteractiveElementClicked = false;
            while (target != null && target != card) {
                if (target.equals(actionButton) || target.equals(hostLabel)) {
                    isInteractiveElementClicked = true;
                    break;
                }
                target = target.getParent();
            }
            if (!isInteractiveElementClicked) {
                showEventDetails(event);
            }
        });
        return card;
    }

    private void handleJoinAction(Event event, Button joinButton) {
        if (currentUser == null) {
            AlertFactory.showErrorAlert("Error", "Not Logged In",
                    "You must be logged in to join events.");
            return;
        }
        String originalText = joinButton.getText();
        String originalStyle = joinButton.getStyle();

        joinButton.setDisable(true);
        joinButton.setText("Joining...");

        Task<Participant> joinTask = new Task<>() {
            @Override
            protected Participant call() throws Exception {
                return participantService.joinEvent(event.getId(), currentUser);
            }
        };

        joinTask.setOnSucceeded(_ -> {
            Platform.runLater(() -> {
                AlertFactory.showInformationAlert("Success", "Event Joined",
                        "You have successfully joined '" + event.getTitle() + "'.");
                refreshViewData(); 
            });
        });

        joinTask.setOnFailed(_ -> {
            Throwable exception = joinTask.getException();
            Platform.runLater(() -> {
                AlertFactory.showErrorAlert("Join Failed",
                        "Could not join event: " + event.getTitle(),
                        (exception != null && exception.getMessage() != null)
                                ? exception.getMessage()
                                : "Please try again.");
                joinButton.setDisable(false);
                joinButton.setText(originalText);
                joinButton.setStyle(originalStyle);
                if (exception != null)
                    exception.printStackTrace();
            });
        });
        new Thread(joinTask).start();
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
            detailsController.setPreviousViewFxml("home_feed.fxml");
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