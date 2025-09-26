package com.kentom.devcleaner;

import com.kentom.devcleaner.model.GitInfo;
import com.kentom.devcleaner.model.Project;
import com.kentom.devcleaner.model.ProjectType;
import com.kentom.devcleaner.service.ApplicationDataService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController {

    // Primary stats
    @FXML private Label projectCountLabel;
    @FXML private Label diskUsageLabel;
    @FXML private Label cleanableSpaceLabel;
    @FXML private Label gitProjectsLabel;
    @FXML private Label lastUpdatedLabel;
    
    // Trend and progress
    @FXML private Label projectTrendLabel;
    @FXML private ProgressBar diskUsageProgress;
    @FXML private Label potentialSavingsLabel;
    @FXML private Label gitPercentageLabel;
    
    // Secondary stats
    @FXML private VBox projectTypesBreakdown;
    @FXML private VBox largestProjectsList;
    @FXML private VBox recentActivityBox;
    
    // System health
    @FXML private Label cacheStatusLabel;
    @FXML private Label lastScanLabel;
    @FXML private Label performanceLabel;
    
    // Loading state
    @FXML private VBox loadingStateBox;
    @FXML private VBox mainContentBox;
    @FXML private Label loadingStatusLabel;

    private MainController mainController;
    private ApplicationDataService dataService;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        dataService = ApplicationDataService.getInstance();

        // Listen for data changes and loading status
        dataService.addDataChangeListener(this::updateDashboard);
        dataService.addLoadingStatusListener(this::updateLoadingStatus);

        // Show initial state based on current data
        updateInitialState();
    }

    // Manual refresh - triggers data service refresh
    @FXML
    private void refreshDashboard() {
        dataService.refreshData();
    }

    // Show initial state (loading or data)
    private void updateInitialState() {
        if (dataService.isLoading()) {
            showLoadingState();
        } else if (dataService.isInitialLoadComplete()) {
            hideLoadingState();
            updateDashboard();
        } else {
            showLoadingState();
        }
    }

    // Update dashboard with current cached data
    private void updateDashboard() {
        updateLastUpdated();
        updatePrimaryStats();
        updateProjectTypesBreakdown();
        updateLargestProjects();
        updateRecentActivity();
        updateSystemHealth();
    }

    // Update loading status from data service
    private void updateLoadingStatus(String status) {
        if (loadingStatusLabel != null) {
            loadingStatusLabel.setText(status);
        }

        // Show/hide loading state based on service state
        if (dataService.isLoading()) {
            showLoadingState();
        } else {
            hideLoadingState();
        }
    }
    
    private void showLoadingState() {
        loadingStateBox.setVisible(true);
        loadingStateBox.setManaged(true);
        mainContentBox.setVisible(false);
        mainContentBox.setManaged(false);
    }
    
    private void hideLoadingState() {
        loadingStateBox.setVisible(false);
        loadingStateBox.setManaged(false);
        mainContentBox.setVisible(true);
        mainContentBox.setManaged(true);
    }

    private void updateLastUpdated() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm");
        lastUpdatedLabel.setText("Last updated: " + LocalDateTime.now().format(formatter));
    }

    private void updatePrimaryStats() {
        List<Project> projects = dataService.getProjects();
        
        // Basic counts
        int totalProjects = projects.size();
        long totalSize = 0;
        long cleanableSize = 0;
        int gitProjects = 0;
        
        for (Project project : projects) {
            totalSize += project.getTotalSize();
            cleanableSize += project.getSizeOfCleanableItems();
            
            GitInfo gitInfo = project.getGitInfo();
            if (gitInfo != null && gitInfo.isGitRepository()) {
                gitProjects++;
            }
        }
        
        // Update primary stats
        projectCountLabel.setText(String.valueOf(totalProjects));
        
        // Update trend (simple example)
        if (totalProjects > 0) {
            projectTrendLabel.setText("+" + totalProjects + " discovered");
            projectTrendLabel.getStyleClass().add("trend-up");
        } else {
            projectTrendLabel.setText("Start scanning to discover projects");
        }
        
        // Disk usage with progress
        DecimalFormat df = new DecimalFormat("#.##");
        double totalSizeGB = totalSize / (1024.0 * 1024.0 * 1024.0);
        diskUsageLabel.setText(df.format(totalSizeGB) + " GB");
        
        // Set progress bar (assuming max disk is 100GB for demo)
        double progressValue = Math.min(totalSizeGB / 100.0, 1.0);
        diskUsageProgress.setProgress(progressValue);
        
        // Cleanable space
        double cleanableSizeGB = cleanableSize / (1024.0 * 1024.0 * 1024.0);
        cleanableSpaceLabel.setText(df.format(cleanableSizeGB) + " GB");
        potentialSavingsLabel.setText("Could save " + df.format(cleanableSizeGB) + " GB");
        
        // Git statistics
        gitProjectsLabel.setText(String.valueOf(gitProjects));
        if (totalProjects > 0) {
            double gitPercentage = (gitProjects * 100.0) / totalProjects;
            gitPercentageLabel.setText(String.format("%.0f%% of projects", gitPercentage));
        } else {
            gitPercentageLabel.setText("No projects");
        }
    }
    
    private void updateProjectTypesBreakdown() {
        projectTypesBreakdown.getChildren().clear();

        List<Project> projects = dataService.getProjects();
        if (projects.isEmpty()) {
            Label emptyLabel = new Label("No projects to analyze");
            emptyLabel.getStyleClass().add("empty-breakdown");
            projectTypesBreakdown.getChildren().add(emptyLabel);
            return;
        }
        
        // Count projects by type
        Map<ProjectType, Integer> typeCounts = projects.stream()
            .collect(Collectors.groupingBy(Project::getType, 
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));
        
        // Sort by count descending
        typeCounts.entrySet().stream()
            .sorted(Map.Entry.<ProjectType, Integer>comparingByValue().reversed())
            .forEach(entry -> {
                HBox typeRow = new HBox(10);
                typeRow.setAlignment(Pos.CENTER_LEFT);
                typeRow.getStyleClass().add("type-breakdown-row");
                
                try {
                    ImageView icon = new ImageView(new Image(getClass()
                        .getResourceAsStream("/com/kentom/devcleaner/icons/" + entry.getKey().iconName)));
                    icon.setFitHeight(16);
                    icon.setFitWidth(16);
                    icon.setPreserveRatio(true);
                    typeRow.getChildren().add(icon);
                } catch (Exception e) {
                    // Skip icon if not found
                }
                
                Label typeLabel = new Label(entry.getKey().displayName);
                typeLabel.getStyleClass().add("type-name");
                
                Label countLabel = new Label("(" + entry.getValue() + ")");
                countLabel.getStyleClass().add("type-count");
                
                typeRow.getChildren().addAll(typeLabel, countLabel);
                projectTypesBreakdown.getChildren().add(typeRow);
            });
    }
    
    private void updateLargestProjects() {
        largestProjectsList.getChildren().clear();

        List<Project> projects = dataService.getProjects();
        if (projects.isEmpty()) {
            Label emptyLabel = new Label("No projects to analyze");
            emptyLabel.getStyleClass().add("empty-breakdown");
            largestProjectsList.getChildren().add(emptyLabel);
            return;
        }
        
        // Sort by cleanable size and take top 5
        projects.stream()
            .sorted(Comparator.comparingLong(Project::getSizeOfCleanableItems).reversed())
            .limit(5)
            .forEach(project -> {
                HBox projectRow = new HBox(10);
                projectRow.setAlignment(Pos.CENTER_LEFT);
                projectRow.getStyleClass().add("large-project-row");
                
                Label nameLabel = new Label(project.getName());
                nameLabel.getStyleClass().add("project-name");
                nameLabel.setMaxWidth(120);
                
                DecimalFormat df = new DecimalFormat("#.##");
                double sizeGB = project.getSizeOfCleanableItems() / (1024.0 * 1024.0 * 1024.0);
                Label sizeLabel = new Label(df.format(sizeGB) + " GB");
                sizeLabel.getStyleClass().add("project-size");
                
                projectRow.getChildren().addAll(nameLabel, sizeLabel);
                largestProjectsList.getChildren().add(projectRow);
            });
    }

    private void updateRecentActivity() {
        recentActivityBox.getChildren().clear();

        List<Project> projects = dataService.getProjects();
        
        if (projects.isEmpty()) {
            Label noActivity = new Label("No recent activity");
            noActivity.getStyleClass().add("empty-activity");
            recentActivityBox.getChildren().add(noActivity);
            
            Label suggestion = new Label("Start by scanning for projects");
            suggestion.getStyleClass().add("activity-suggestion");
            recentActivityBox.getChildren().add(suggestion);
        } else {
            // Sort by date created (most recent first) and take top 8
            projects.stream()
                .sorted(Comparator.comparing(Project::getDateCreated).reversed())
                .limit(8)
                .forEach(project -> {
                    HBox activityRow = new HBox(8);
                    activityRow.setAlignment(Pos.CENTER_LEFT);
                    activityRow.getStyleClass().add("activity-row");
                    
                    Label timeLabel = new Label(formatTimeAgo(project.getDateCreated()));
                    timeLabel.getStyleClass().add("activity-time");
                    
                    Label actionLabel = new Label("Discovered");
                    actionLabel.getStyleClass().add("activity-action");
                    
                    Label projectLabel = new Label(project.getName());
                    projectLabel.getStyleClass().add("activity-project");
                    
                    activityRow.getChildren().addAll(timeLabel, actionLabel, projectLabel);
                    recentActivityBox.getChildren().add(activityRow);
                });
        }
    }
    
    private void updateSystemHealth() {
        List<Project> projects = dataService.getProjects();
        
        // Cache Status
        if (!projects.isEmpty()) {
            cacheStatusLabel.setText("Healthy");
            cacheStatusLabel.getStyleClass().removeAll("good", "warning", "error");
            cacheStatusLabel.getStyleClass().add("good");
        } else {
            cacheStatusLabel.setText("Empty");
            cacheStatusLabel.getStyleClass().removeAll("good", "warning", "error");
            cacheStatusLabel.getStyleClass().add("warning");
        }
        
        // Last Scan (simplified)
        if (!projects.isEmpty()) {
            lastScanLabel.setText(formatTimeAgo(projects.get(0).getDateCreated()));
        } else {
            lastScanLabel.setText("Never");
        }
        
        // Performance (simplified)
        if (projects.size() < 50) {
            performanceLabel.setText("Optimal");
            performanceLabel.getStyleClass().removeAll("good", "warning", "error");
            performanceLabel.getStyleClass().add("good");
        } else if (projects.size() < 100) {
            performanceLabel.setText("Good");
            performanceLabel.getStyleClass().removeAll("good", "warning", "error");
            performanceLabel.getStyleClass().add("good");
        } else {
            performanceLabel.setText("Heavy");
            performanceLabel.getStyleClass().removeAll("good", "warning", "error");
            performanceLabel.getStyleClass().add("warning");
        }
    }
    
    private String formatTimeAgo(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(dateTime, now).toMinutes();
        
        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + "m ago";
        } else if (minutes < 1440) { // 24 hours
            return (minutes / 60) + "h ago";
        } else {
            return (minutes / 1440) + "d ago";
        }
    }
}