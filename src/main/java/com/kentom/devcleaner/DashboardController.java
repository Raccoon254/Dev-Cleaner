package com.kentom.devcleaner;

import com.kentom.devcleaner.model.Project;
import com.kentom.devcleaner.model.ProjectCache;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;

import java.text.DecimalFormat;
import java.util.List;

public class DashboardController {

    @FXML private GridPane statsGrid;
    @FXML private Label projectCountLabel;
    @FXML private Label diskUsageLabel;
    @FXML private Label cleanableSpaceLabel;
    @FXML private VBox recentActivityBox;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        refreshDashboard();
    }

    public void refreshDashboard() {
        Platform.runLater(() -> {
            updateStats();
            updateRecentActivity();
        });
    }

    private void updateStats() {
        List<Project> projects = ProjectCache.loadProjects();
        
        // Update project count
        projectCountLabel.setText(String.valueOf(projects.size()));
        
        // Calculate cleanable space (only cleanable size is available in the model)
        long cleanableSize = 0;
        
        for (Project project : projects) {
            cleanableSize += project.getSizeOfCleanableItems();
        }
        
        // Format sizes in GB
        DecimalFormat df = new DecimalFormat("#.##");
        diskUsageLabel.setText("N/A"); // Total project size not available
        cleanableSpaceLabel.setText(df.format(cleanableSize / (1024.0 * 1024.0 * 1024.0)) + " GB");
    }

    private void updateRecentActivity() {
        recentActivityBox.getChildren().clear();
        
        List<Project> projects = ProjectCache.loadProjects();
        
        if (projects.isEmpty()) {
            Label noActivity = new Label("No recent activity. Start by scanning for projects.");
            noActivity.setStyle("-fx-text-fill: #666666;");
            recentActivityBox.getChildren().add(noActivity);
        } else {
            // Show up to 5 most recent projects
            int count = Math.min(5, projects.size());
            for (int i = 0; i < count; i++) {
                Project project = projects.get(i);
                Label activityItem = new Label("Found project: " + project.getName());
                activityItem.setStyle("-fx-text-fill: #333333; -fx-font-size: 12px;");
                recentActivityBox.getChildren().add(activityItem);
            }
        }
    }
}