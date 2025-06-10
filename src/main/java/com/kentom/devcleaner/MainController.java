package com.kentom.devcleaner;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;

public class MainController {

    @FXML private BorderPane mainPane;
    @FXML private StackPane contentPane;

    private Node projectsView;
    private Node scanView;
    private Node settingsView;

    private ProjectsController projectsController;
    private ScanController scanController;

    @FXML
    public void initialize() throws IOException {
        // Pre-load all the views
        FXMLLoader projectsLoader = new FXMLLoader(getClass().getResource("projects-view.fxml"));
        projectsView = projectsLoader.load();
        projectsController = projectsLoader.getController();
        projectsController.setMainController(this);

        FXMLLoader scanLoader = new FXMLLoader(getClass().getResource("scan-view.fxml"));
        scanView = scanLoader.load();
        scanController = scanLoader.getController();
        scanController.setProjectsController(projectsController); // Give scan controller a reference to projects

        // Set initial view
        contentPane.getChildren().add(projectsView);
    }

    @FXML
    private void showProjects() {
        switchView(projectsView);
        projectsController.refreshProjects(); // Refresh projects when switching to the view
    }

    @FXML
    private void showScan() {
        switchView(scanView);
    }

    @FXML
    private void showSettings() {
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