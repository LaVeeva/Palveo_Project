package com.palveo.gui.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
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

public class EditGatheringController {

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
    private Button updateButton;
    @FXML
    private Button cancelButton;

    private EventService eventService;
    private SessionManager sessionManager;
    private MainPanelController mainPanelController;
    private Event eventToEdit;

    public EditGatheringController() {
        this.eventService = new EventServiceImpl();
        this.sessionManager = SessionManager.getInstance();
    }

    public void setMainPanelController(MainPanelController mainPanelController) {
        this.mainPanelController = mainPanelController;
    }

    public void loadEventToEdit(Event event) {
        this.eventToEdit = event;
        populateFormWithEventData();
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
    }

    private void populateFormWithEventData() {
        if (eventToEdit == null) {
            AlertFactory.showErrorAlert("Error", "No Event Data",
                    "Cannot load event data for editing.");
            handleCancelButtonAction(null);
            return;
        }
        titleField.setText(eventToEdit.getTitle());
        descriptionArea
                .setText(eventToEdit.getDescription() != null ? eventToEdit.getDescription() : "");
        categoryComboBox.setValue(eventToEdit.getCategory());
        privacyComboBox.setValue(eventToEdit.getPrivacy());

        if (eventToEdit.getEventDateTime() != null) {
            datePicker.setValue(eventToEdit.getEventDateTime().toLocalDate());
            timeField.setText(eventToEdit.getEventDateTime().toLocalTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        locationField.setText(
                eventToEdit.getLocationString() != null ? eventToEdit.getLocationString() : "");
    }

    @FXML
    private void handleUpdateButtonAction(ActionEvent event) {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            AlertFactory.showErrorAlert("Authentication Error", "Not Logged In",
                    "You must be logged in to update an event.");
            return;
        }
        if (eventToEdit == null) {
            AlertFactory.showErrorAlert("Error", "No Event to Update",
                    "Cannot update event: data missing.");
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

        Event updatedEventDetails = new Event();
        updatedEventDetails.setTitle(title);
        updatedEventDetails.setDescription(description);
        updatedEventDetails.setCategory(category);
        updatedEventDetails.setPrivacy(privacy);
        updatedEventDetails.setEventDateTime(eventDateTime);
        updatedEventDetails.setLocationString(location);

        try {
            eventService.updateEvent(eventToEdit.getId(), updatedEventDetails, currentUser);
            AlertFactory.showInformationAlert("Success", "Gathering Updated",
                    "Event '" + title + "' has been successfully updated!");

            if (mainPanelController != null) {
                Optional<Event> reloadedEventOpt = eventService.getEventById(eventToEdit.getId());
                if (reloadedEventOpt.isPresent()) {
                    mainPanelController.loadView("event_details_panel.fxml", controller -> {
                        if (controller instanceof EventDetailsController) {
                            ((EventDetailsController) controller)
                                    .loadEventData(reloadedEventOpt.get());
                        }
                    });
                } else {
                    mainPanelController.loadView("home_feed.fxml");
                }
            }
        } catch (EventOperationException e) {
            AlertFactory.showErrorAlert("Update Failed", "Could not update event", e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            AlertFactory.showErrorAlert("System Error", "An unexpected error occurred",
                    e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelButtonAction(ActionEvent event) {
        if (mainPanelController != null && eventToEdit != null) {
            mainPanelController.loadView("event_details_panel.fxml", controller -> {
                if (controller instanceof EventDetailsController) {
                    ((EventDetailsController) controller).loadEventData(eventToEdit);
                }
            });
        } else if (mainPanelController != null) {
            mainPanelController.loadView("home_feed.fxml");
        }
    }
}