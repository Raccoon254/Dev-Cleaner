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

    private Node dashboardView;
    private DashboardController dashboardController;

    @FXML
    public void initialize() throws IOException {
        // Load the initial view (Dashboard)
        FXMLLoader loader = new FXMLLoader(getClass().getResource("dashboard-view.fxml"));
        dashboardView = loader.load();
        dashboardController = loader.getController();
        dashboardController.setMainController(this);
        contentPane.getChildren().add(dashboardView);
    }

    @FXML
    private void showDashboard() {
        switchView(dashboardView);
    }

    @FXML
    private void showSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("settings-view.fxml"));
            Node settingsView = loader.load();
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
    }

    public void hideProjectDetails(Node detailsView) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), detailsView);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> contentPane.getChildren().remove(detailsView));
        fadeOut.play();
    }

    private void switchView(Node view) {
        if (contentPane.getChildren().get(0) != view) {
            Node currentView = contentPane.getChildren().get(0);
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentView);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                if(view != dashboardView) { // Avoid re-adding dashboard view if it's already there
                    contentPane.getChildren().set(0, view);
                } else if(!contentPane.getChildren().contains(dashboardView)) {
                    contentPane.getChildren().set(0, dashboardView);
                }
                view.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), view);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            fadeOut.play();
        }
    }
}