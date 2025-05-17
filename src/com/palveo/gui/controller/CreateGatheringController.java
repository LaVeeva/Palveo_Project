package com.palveo.gui.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import com.palveo.gui.manager.SessionManager;
import com.palveo.gui.util.AlertFactory;
import com.palveo.model.Event;
import com.palveo.model.User;
import com.palveo.service.EventService;
import com.palveo.service.exception.EventOperationException;
import com.palveo.service.impl.EventServiceImpl;
import com.palveo.util.ValidationUtils;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

public class CreateGatheringController {

    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private ComboBox<Event.EventCategory> categoryComboBox;
    @FXML
    private ComboBox<Event.PrivacySetting> privacyComboBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private TextField timeField;
    @FXML
    private TextField locationField;
    @FXML
    private Button submitButton;
    @FXML
    private Button cancelButton;

    private EventService eventService;
    private SessionManager sessionManager;
    private MainPanelController mainPanelController;

    public CreateGatheringController() {
        this.eventService = new EventServiceImpl();
        this.sessionManager = SessionManager.getInstance();
    }

    public void setMainPanelController(MainPanelController mainPanelController) {
        this.mainPanelController = mainPanelController;
    }

    @FXML
    private void initialize() {
        categoryComboBox.setItems(FXCollections.observableArrayList(Event.EventCategory.values()));
        categoryComboBox.setConverter(new StringConverter<Event.EventCategory>() {
            @Override
            public String toString(Event.EventCategory object) {
                return object == null ? "" : object.toString();
            }
            @Override
            public Event.EventCategory fromString(String string) {
                return Event.EventCategory.fromString(string);
            }
        });

        privacyComboBox.setItems(FXCollections.observableArrayList(Event.PrivacySetting.values()));
         privacyComboBox.setConverter(new StringConverter<Event.PrivacySetting>() {
            @Override
            public String toString(Event.PrivacySetting object) {
                return object == null ? "" : object.toString();
            }
            @Override
            public Event.PrivacySetting fromString(String string) {
                return Event.PrivacySetting.fromString(string);
            }
        });
        privacyComboBox.setValue(Event.PrivacySetting.PUBLIC);
        datePicker.setValue(LocalDate.now().plusDays(1));

        titleField.setPromptText("Enter event title");
        descriptionArea.setPromptText("Enter event description (optional)");
        categoryComboBox.setPromptText("Select type...");
        privacyComboBox.setPromptText("Select privacy...");
        timeField.setPromptText("e.g., 14:30");
        locationField.setPromptText("Enter location details");
        cancelButton.setText("Cancel");
        submitButton.setText("Create");
    }

    @FXML
    private void handleSubmitButtonAction(ActionEvent event) {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            AlertFactory.showErrorAlert("Authentication Error", "Not Logged In",
                    "You must be logged in to create an event.");
            return;
        }

        String title = titleField.getText();
        String description = descriptionArea.getText();
        Event.EventCategory category = categoryComboBox.getValue();
        Event.PrivacySetting privacy = privacyComboBox.getValue();
        LocalDate date = datePicker.getValue();
        String timeString = timeField.getText();
        String location = locationField.getText();

        if (ValidationUtils.isNullOrEmpty(title) || category == null
                || date == null || ValidationUtils.isNullOrEmpty(timeString)
                || ValidationUtils.isNullOrEmpty(location)
                || privacy == null) {
            AlertFactory.showErrorAlert("Input Error", "Missing Information",
                    "Please fill in all required fields (Title, Type, Privacy, Date, Time, Location).");
            return;
        }

        LocalTime time;
        try {
            time = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("[H:mm][HH:mm]"));
        } catch (DateTimeParseException e) {
            AlertFactory.showErrorAlert("Input Error", "Invalid Time Format",
                    "Please enter time in HH:MM format (e.g., 09:30 or 14:00).");
            return;
        }

        LocalDateTime eventDateTime = LocalDateTime.of(date, time);

        if (eventDateTime.isBefore(LocalDateTime.now())) {
            AlertFactory.showErrorAlert("Input Error", "Invalid Date/Time",
                    "Event date and time must be in the future.");
            return;
        }

        Event newEvent = new Event();
        newEvent.setTitle(title);
        newEvent.setDescription(description);
        newEvent.setCategory(category);
        newEvent.setPrivacy(privacy);
        newEvent.setEventDateTime(eventDateTime);
        newEvent.setLocationString(location);

        try {
            Event createdEvent = eventService.createEvent(newEvent, currentUser);
            AlertFactory.showInformationAlert("Success", "Gathering Created",
                    "Your event '" + createdEvent.getTitle() + "' has been successfully created!");
            goBackToHomeFeed();

        } catch (EventOperationException e) {
            AlertFactory.showErrorAlert("Creation Failed", "Could not create event",
                    e.getMessage());
        } catch (Exception e) {
            AlertFactory.showErrorAlert("System Error", "An unexpected error occurred",
                    e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelButtonAction(ActionEvent event) {
        clearForm();
        goBackToHomeFeed();
    }

    private void goBackToHomeFeed() {
        if (mainPanelController != null) {
            mainPanelController.loadView("home_feed.fxml");
        } else {
            System.err.println(
                    "CreateGatheringController: MainPanelController reference is null. Cannot navigate back.");
        }
    }

    private void clearForm() {
        titleField.clear();
        descriptionArea.clear();
        categoryComboBox.getSelectionModel().clearSelection();
        privacyComboBox.setValue(Event.PrivacySetting.PUBLIC);
        datePicker.setValue(LocalDate.now().plusDays(1));
        timeField.clear();
        locationField.clear();
    }
}