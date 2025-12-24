package com.kentom.devcleaner;

import com.kentom.devcleaner.service.ApplicationDataService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

public class MainController {

    @FXML private BorderPane mainPane;
    @FXML private StackPane contentPane;

    private Node dashboardView;
    private Node projectsView;
    private Node scanView;
    private Node settingsView;
    private Node aboutView;

    private DashboardController dashboardController;
    private ProjectsController projectsController;
    private ScanController scanController;

    @FXML
    public void initialize() throws IOException {
        ApplicationDataService service = ApplicationDataService.getInstance();

        // Always load dashboard immediately (with cached data if available)
        loadDashboardView();

        // Start background data loading/refresh
        if (!service.isLoading() && !service.isInitialLoadComplete()) {
            service.initializeDataAsync();
        }
    }

    private Node createLoadingView() {
        // Simple loading indicator (VBox with ProgressIndicator + Label)
        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.getStyleClass().add("loading-container");
        loadingBox.setMaxWidth(Double.MAX_VALUE);
        loadingBox.setMaxHeight(Double.MAX_VALUE);

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.getStyleClass().add("loading-spinner");
        spinner.setMaxSize(60, 60);

        Label label = new Label("Loading workspace...");
        label.getStyleClass().add("loading-text");

        loadingBox.getChildren().addAll(spinner, label);
        return loadingBox;
    }

    private void loadDashboardView() throws IOException {
        if (dashboardView == null) {
            FXMLLoader dashboardLoader = new FXMLLoader(getClass().getResource("dashboard-view.fxml"));
            dashboardView = dashboardLoader.load();
            dashboardController = dashboardLoader.getController();
            dashboardController.setMainController(this);
        }

        // Also pre-load scan view (needed often)
        if (scanView == null) {
            FXMLLoader scanLoader = new FXMLLoader(getClass().getResource("scan-view.fxml"));
            scanView = scanLoader.load();
            scanController = scanLoader.getController();
            // We'll set the projects controller reference when projects view is loaded
        }

        switchView(dashboardView);
    }

    @FXML
    private void showDashboard() {
        if (dashboardView == null) {
            try {
                loadDashboardView();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        switchView(dashboardView);
    }

    @FXML
    public void showProjects() {
        // Lazy-load projects view
        if (projectsView == null) {
            try {
                FXMLLoader projectsLoader = new FXMLLoader(getClass().getResource("projects-view.fxml"));
                projectsView = projectsLoader.load();
                projectsController = projectsLoader.getController();
                projectsController.setMainController(this);

                // Now that projects controller is loaded, connect it to scan controller if needed
                if (scanController != null) {
                    scanController.setProjectsController(projectsController);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        switchView(projectsView);
    }

    @FXML
    public void showScan() {
        // Lazy-load scan view if not already loaded
        if (scanView == null) {
            try {
                FXMLLoader scanLoader = new FXMLLoader(getClass().getResource("scan-view.fxml"));
                scanView = scanLoader.load();
                scanController = scanLoader.getController();

                // Connect to projects controller if it's already loaded
                if (projectsController != null) {
                    scanController.setProjectsController(projectsController);
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        switchView(scanView);
    }

    @FXML
    public void showSettings() {
        try {
            if (settingsView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("settings-view.fxml"));
                settingsView = loader.load();
            }
            switchView(settingsView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showAbout() {
        try {
            if (aboutView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("about-view.fxml"));
                aboutView = loader.load();
            }
            switchView(aboutView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showProjectDetails(Node detailsView) {
        detailsView.setOpacity(0);
        contentPane.getChildren().add(detailsView);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), detailsView);
        fadeIn.setToValue(1.0);
        fadeIn.play();
        contentPane.getChildren().get(0).setEffect(new javafx.scene.effect.GaussianBlur(10));
    }

    public void hideProjectDetails(Node detailsView) {
        contentPane.getChildren().get(0).setEffect(null);
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), detailsView);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> contentPane.getChildren().remove(detailsView));
        fadeOut.play();
    }

    private void switchView(Node newView) {
        // Handle case where contentPane is empty (first view)
        if (contentPane.getChildren().isEmpty()) {
            contentPane.getChildren().add(newView);
            newView.setOpacity(1);
            return;
        }

        // Clean up any extra views (like project details overlays)
        if (contentPane.getChildren().size() > 1) {
            while (contentPane.getChildren().size() > 1) {
                contentPane.getChildren().remove(1);
            }
            contentPane.getChildren().get(0).setEffect(null);
        }

        // Switch to new view with fade animation
        if (!contentPane.getChildren().contains(newView)) {
            Node currentView = contentPane.getChildren().get(0);
            contentPane.getChildren().add(newView);
            newView.setOpacity(0);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), currentView);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> contentPane.getChildren().remove(currentView));

            FadeTransition fadeIn = new FadeTransition(Duration.millis(250), newView);
            fadeIn.setToValue(1);

            fadeOut.play();
            fadeIn.play();
        }
    }
}