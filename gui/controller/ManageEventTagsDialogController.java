package com.palveo.gui.controller;

import java.util.List;
import com.palveo.gui.manager.SessionManager;
import com.palveo.gui.util.AlertFactory;
import com.palveo.model.Event;
import com.palveo.model.Tag;
import com.palveo.model.User;
import com.palveo.service.TagService;
import com.palveo.service.impl.TagServiceImpl;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ManageEventTagsDialogController {

    @FXML private VBox rootDialogPane;
    @FXML private Label dialogTitleLabel;
    @FXML private ScrollPane currentTagsScrollPane;
    @FXML private FlowPane currentEventTagsFlowPane;
    @FXML private Label noCurrentTagsLabel;
    @FXML private TextField newTagTextField;
    @FXML private Button addTagButton;
    @FXML private Button doneButton;

    private Event currentEvent;
    private EventDetailsController parentController;
    private TagService tagService;
    private SessionManager sessionManager;
    private User currentUser;

    public ManageEventTagsDialogController() {
        this.tagService = new TagServiceImpl();
        this.sessionManager = SessionManager.getInstance();
    }

    public void initData(Event event, EventDetailsController parentController) {
        this.currentEvent = event;
        this.parentController = parentController;
        this.currentUser = sessionManager.getCurrentUser();

        if (currentEvent == null || currentUser == null) {
            AlertFactory.showErrorAlert("Error", "Initialization Failed", "Event or user data is missing for tag management.");
            closeDialog();
            return;
        }
        dialogTitleLabel.setText("Manage Tags for: " + currentEvent.getTitle());
        loadCurrentTags();
    }

    @FXML
    private void initialize() {
        noCurrentTagsLabel.setVisible(false);
        noCurrentTagsLabel.setManaged(false);
    }

    private void loadCurrentTags() {
        if (currentEvent == null) return;

        currentEventTagsFlowPane.getChildren().clear();
        noCurrentTagsLabel.setVisible(false);
        noCurrentTagsLabel.setManaged(false);

        Task<List<Tag>> loadTagsTask = new Task<>() {
            @Override
            protected List<Tag> call() throws Exception {
                return tagService.getTagsForEvent(currentEvent.getId());
            }
        };

        loadTagsTask.setOnSucceeded(_ -> {
            List<Tag> tags = loadTagsTask.getValue();
            Platform.runLater(() -> {
                if (tags == null || tags.isEmpty()) {
                    noCurrentTagsLabel.setVisible(true);
                    noCurrentTagsLabel.setManaged(true);
                } else {
                    for (Tag tag : tags) {
                        currentEventTagsFlowPane.getChildren().add(createTagNode(tag));
                    }
                }
            });
        });

        loadTagsTask.setOnFailed(_ -> {
            Throwable ex = loadTagsTask.getException();
            Platform.runLater(() -> {
                AlertFactory.showErrorAlert("Error Loading Tags", "Could not fetch current tags for the event.", (ex != null ? ex.getMessage() : "Unknown error."));
                if (ex != null) ex.printStackTrace();
                noCurrentTagsLabel.setText("Error loading tags.");
                noCurrentTagsLabel.setVisible(true);
                noCurrentTagsLabel.setManaged(true);
            });
        });
        new Thread(loadTagsTask).start();
    }

    private Node createTagNode(Tag tag) {
        HBox tagNode = new HBox(5);
        tagNode.setAlignment(Pos.CENTER_LEFT);
        tagNode.setStyle("-fx-background-color: #E0E0E0; -fx-padding: 3 5 3 8; -fx-background-radius: 12px;");

        Label tagNameLabel = new Label(tag.getTagName());
        tagNameLabel.setStyle("-fx-text-fill: #333333; -fx-font-size: 11px;");

        Button removeButton = new Button("x");
        removeButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #C62828; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 0 3; -fx-min-width: 15px; -fx-min-height:15px; -fx-cursor:hand;");
        removeButton.setOnAction(_ -> handleRemoveTagAction(tag));
        
        if (currentUser != null && currentEvent != null && currentUser.getId() == currentEvent.getHostUserId()) {
            removeButton.setVisible(true);
            removeButton.setManaged(true);
        } else {
            removeButton.setVisible(false);
            removeButton.setManaged(false);
        }
        
        tagNode.getChildren().addAll(tagNameLabel, removeButton);
        return tagNode;
    }

    @FXML
    private void handleAddTagAction(ActionEvent event) {
        String tagName = newTagTextField.getText().trim();
        if (tagName.isEmpty()) {
            AlertFactory.showWarningAlert("Input Error", "Tag name cannot be empty.", null);
            return;
        }
        if (currentUser == null || currentEvent == null) return;

        addTagButton.setDisable(true);
        addTagButton.setText("Adding...");

        Task<Void> addTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                tagService.applyTagToEvent(currentEvent.getId(), tagName, currentUser);
                return null;
            }
        };

        addTask.setOnSucceeded(_ -> Platform.runLater(() -> {
            newTagTextField.clear();
            loadCurrentTags();
            addTagButton.setDisable(false);
            addTagButton.setText("Add Tag");
        }));

        addTask.setOnFailed(_ -> {
            Throwable ex = addTask.getException();
            Platform.runLater(() -> {
                AlertFactory.showErrorAlert("Error Adding Tag", "Could not apply tag.", (ex != null ? ex.getMessage() : "Unknown error."));
                if (ex != null) ex.printStackTrace();
                addTagButton.setDisable(false);
                addTagButton.setText("Add Tag");
            });
        });
        new Thread(addTask).start();
    }

    private void handleRemoveTagAction(Tag tagToRemove) {
        if (currentUser == null || currentEvent == null || tagToRemove == null) return;
        
        if (currentUser.getId() != currentEvent.getHostUserId()) {
            AlertFactory.showErrorAlert("Permission Denied", "Cannot Remove Tag", "Only the event host can remove tags.");
            return;
        }
        
        Node sourceNode = currentEventTagsFlowPane.getScene().getFocusOwner();
        Button sourceButton = null;
        if (sourceNode instanceof Button) {
            sourceButton = (Button) sourceNode;
            sourceButton.setDisable(true);
        }

        final Button finalSourceButton = sourceButton;

        Task<Void> removeTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                tagService.removeTagFromEvent(currentEvent.getId(), tagToRemove.getTagName(), currentUser);
                return null;
            }
        };
        removeTask.setOnSucceeded(_ -> Platform.runLater(this::loadCurrentTags));
        removeTask.setOnFailed(_ -> {
             Throwable ex = removeTask.getException();
             Platform.runLater(() -> {
                AlertFactory.showErrorAlert("Error Removing Tag", "Could not remove tag.", (ex != null ? ex.getMessage() : "Unknown error."));
                 if (ex != null) ex.printStackTrace();
                 if(finalSourceButton != null) finalSourceButton.setDisable(false);
            });
        });
        new Thread(removeTask).start();
    }

    @FXML
    private void handleDoneAction(ActionEvent event) {
        if (parentController != null) {
            parentController.refreshViewData();
        }
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) rootDialogPane.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}