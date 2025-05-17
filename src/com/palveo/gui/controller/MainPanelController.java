package com.palveo.gui.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Consumer;
import com.palveo.MainApp;
import com.palveo.gui.manager.SessionManager;
import com.palveo.gui.util.AlertFactory;
import com.palveo.gui.util.FXMLLoaderWrapper;
import com.palveo.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class MainPanelController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private Label loggedInUserLabel;
    @FXML
    private Button logoutButton;
    @FXML
    private VBox contentArea;
    @FXML
    private Button friendsButton;
    @FXML
    private Button homeNavButton;
    @FXML
    private ImageView homeNavIcon;
    @FXML
    private Button gatheringsNavButton;
    @FXML
    private ImageView gatheringsNavIcon;
    @FXML
    private Button createGatheringNavButton;
    @FXML
    private ImageView createGatheringNavIcon;
    @FXML
    private Button profileNavButton;
    @FXML
    private ImageView profileNavIcon;

    private SessionManager sessionManager;
    private Button currentlyActiveNavButton;
    private ImageView currentlyActiveNavIcon;

    private Stack<String> viewHistory = new Stack<>();
    private String currentViewFxmlPath = "";

    private static final class NavElements {
        final Button button;
        final ImageView icon;

        NavElements(Button button, ImageView icon) {
            this.button = button;
            this.icon = icon;
        }
    }
    private Map<String, NavElements> primaryNavMap;


    private final String ACTIVE_NAV_BUTTON_STYLE =
            "-fx-background-color: transparent; -fx-border-color: #007bff; -fx-border-width: 0 0 2 0; -fx-padding: 8 12 6 12;";
    private final String INACTIVE_NAV_BUTTON_STYLE =
            "-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 8 12 8 12;";
    private final double ACTIVE_ICON_OPACITY = 1.0;
    private final double INACTIVE_ICON_OPACITY = 0.6;

    public MainPanelController() {
        this.sessionManager = SessionManager.getInstance();
    }

    @FXML
    private void initialize() {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser != null) {
            loggedInUserLabel.setText(currentUser.getUsername());
        } else {
            loggedInUserLabel.setText("Error - Not Logged In");
            handleLogoutAction(null);
            return;
        }

        primaryNavMap = new HashMap<>();
        primaryNavMap.put("home_feed.fxml", new NavElements(homeNavButton, homeNavIcon));
        primaryNavMap.put("my_gatherings_panel.fxml", new NavElements(gatheringsNavButton, gatheringsNavIcon));
        primaryNavMap.put("create_gathering.fxml", new NavElements(createGatheringNavButton, createGatheringNavIcon));
        primaryNavMap.put("profile.fxml", new NavElements(profileNavButton, profileNavIcon));
        
        for (NavElements elements : primaryNavMap.values()) {
            setButtonInactiveStyle(elements.button, elements.icon);
        }

        loadView("home_feed.fxml");
    }

    private void updateActiveNavButtonState(String fxmlFileName) {
        NavElements targetElements = primaryNavMap.get(fxmlFileName);

        if (currentlyActiveNavButton != null && currentlyActiveNavIcon != null) {
            setButtonInactiveStyle(currentlyActiveNavButton, currentlyActiveNavIcon);
        }

        if (targetElements != null) {
            setButtonActiveStyle(targetElements.button, targetElements.icon);
            currentlyActiveNavButton = targetElements.button;
            currentlyActiveNavIcon = targetElements.icon;
        } else {
            currentlyActiveNavButton = null;
            currentlyActiveNavIcon = null;
            
        }
    }

    private void setButtonActiveStyle(Button button, ImageView icon) {
        if (button != null) button.setStyle(ACTIVE_NAV_BUTTON_STYLE);
        if (icon != null) icon.setOpacity(ACTIVE_ICON_OPACITY);
    }

    private void setButtonInactiveStyle(Button button, ImageView icon) {
        if (button != null) button.setStyle(INACTIVE_NAV_BUTTON_STYLE);
        if (icon != null) icon.setOpacity(INACTIVE_ICON_OPACITY);
    }

    public String getPreviousViewFromHistoryStack() {
        if (viewHistory.size() > 1) {
            viewHistory.pop();
            if (!viewHistory.isEmpty()) {
                return viewHistory.peek();
            }
        } else if (viewHistory.size() == 1 && !viewHistory.peek().equals(currentViewFxmlPath)){
             return viewHistory.peek();
        }
        return "home_feed.fxml";
    }


    public void setCurrentViewFxmlPath(String fxmlPath) {
        if (this.currentViewFxmlPath != null && !this.currentViewFxmlPath.equals(fxmlPath)
                && !this.currentViewFxmlPath.isEmpty()) {
            if (viewHistory.isEmpty() || !viewHistory.peek().equals(this.currentViewFxmlPath)) {
                viewHistory.push(this.currentViewFxmlPath);
            }
        }
        this.currentViewFxmlPath = fxmlPath;

        final int MAX_HISTORY_SIZE = 10;
        while (viewHistory.size() > MAX_HISTORY_SIZE) {
            viewHistory.remove(0);
        }
    }

    public String getCurrentViewFxmlPath() {
        return this.currentViewFxmlPath;
    }

    public <T> void loadView(String fxmlFile, Consumer<T> controllerInitializer) {
        try {
            String fxmlNavigatingFrom = this.currentViewFxmlPath;
            FXMLLoaderWrapper.FXMLLoaderResult<T> loaderResult =
                    FXMLLoaderWrapper.loadFXMLWithController(fxmlFile);
            Parent viewToLoad = loaderResult.parent;
            T controller = loaderResult.controller;

            setCurrentViewFxmlPath(fxmlFile);
            updateActiveNavButtonState(fxmlFile);

            if (controller instanceof CreateGatheringController) {
                ((CreateGatheringController) controller).setMainPanelController(this);
            } else if (controller instanceof ProfileController) {
                ((ProfileController) controller).setMainPanelController(this);
            } else if (controller instanceof EventDetailsController) {
                ((EventDetailsController) controller).setMainPanelController(this);
                ((EventDetailsController) controller).setPreviousViewFxml(fxmlNavigatingFrom);
            } else if (controller instanceof HomeFeedController) {
                ((HomeFeedController) controller).setMainPanelController(this);
            } else if (controller instanceof MyGatheringsController) {
                ((MyGatheringsController) controller).setMainPanelController(this);
            } else if (controller instanceof OtherProfileController) {
                ((OtherProfileController) controller).setMainPanelController(this);
                ((OtherProfileController) controller).setPreviousViewFxml(fxmlNavigatingFrom);
            } else if (controller instanceof FriendsPanelController) {
                ((FriendsPanelController) controller).setMainPanelController(this);
            } else if (controller instanceof EventParticipantsListController) {
                ((EventParticipantsListController) controller).setMainPanelController(this);
                ((EventParticipantsListController) controller).setPreviousViewFxml(fxmlNavigatingFrom);
            } else if (controller instanceof SettingsPanelController) {
                ((SettingsPanelController) controller).setMainPanelController(this);
            } else if (controller instanceof EditGatheringController) {
                ((EditGatheringController) controller).setMainPanelController(this);
            }

            if (controllerInitializer != null && controller != null) {
                controllerInitializer.accept(controller);
            }
            setContent(viewToLoad);

        } catch (IOException e) {
            System.err.println("Error loading FXML view: " + fxmlFile + " - " + e.getMessage());
            e.printStackTrace();
            AlertFactory.showErrorAlert("Navigation Error", "Could not load view: " + fxmlFile,
                    "Please try again or contact support.");
            setContent(new Label("Error loading view: " + fxmlFile));
        } catch (Exception e) {
            System.err.println("Error processing controller for FXML view: " + fxmlFile + " - "
                    + e.getMessage());
            e.printStackTrace();
            AlertFactory.showErrorAlert("Controller Error",
                    "Could not initialize view controller: " + fxmlFile, "Please contact support.");
        }
    }

    public void loadView(String fxmlFile) {
        loadView(fxmlFile, null);
    }

    public void setContent(Parent node) {
        contentArea.getChildren().clear();
        if (node != null) {
            contentArea.getChildren().add(node);
        }
    }

    @FXML
    private void handleLogoutAction(ActionEvent event) {
        sessionManager.clearSession();
        viewHistory.clear();
        currentlyActiveNavButton = null; 
        currentlyActiveNavIcon = null;
        try {
            MainApp.changeScene("login.fxml", "Palveo - Login", 350, 600);
        } catch (IOException e) {
            AlertFactory.showErrorAlert("Logout Error", "Could not return to login screen.",
                    e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleHomeNavigation(ActionEvent event) {
        loadView("home_feed.fxml");
    }

    @FXML
    private void handleGatheringsNavigation(ActionEvent event) {
        loadView("my_gatherings_panel.fxml");
    }

    @FXML
    private void handleCreateGatheringNavigation(ActionEvent event) {
        loadView("create_gathering.fxml");
    }

    @FXML
    private void handleProfileNavigation(ActionEvent event) {
        loadView("profile.fxml");
    }

    @FXML
    private void handleFriendsNavigation(ActionEvent event) {
        loadView("friends_panel.fxml");
        
        
    }
}