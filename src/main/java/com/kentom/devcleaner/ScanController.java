package com.kentom.devcleaner;

import com.kentom.devcleaner.model.DirectoryScanner;
import com.kentom.devcleaner.model.LogManager;
import com.kentom.devcleaner.model.Project;
import com.kentom.devcleaner.model.ProjectType;
import com.kentom.devcleaner.model.ScanProgressInfo;
import com.kentom.devcleaner.service.ApplicationDataService;
import com.kentom.devcleaner.util.UserPreferences;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ScanController {

    @FXML private StackPane scanRoot;
    @FXML private TextField directoryPathField;
    @FXML private Button scanButton;
    @FXML private VBox scanBox;
    @FXML private javafx.scene.control.ScrollPane scanningBox;

    // Enhanced scanning UI elements
    @FXML private Label scanPhaseLabel;
    @FXML private Label progressPercentageLabel;
    @FXML private Label progressETALabel;
    @FXML private javafx.scene.control.ProgressBar scanProgressBar;
    @FXML private Label currentFolderLabel;
    @FXML private Label currentFileLabel;

    // Statistics labels
    @FXML private Label directoriesScannedLabel;
    @FXML private Label filesScannedLabel;
    @FXML private Label projectsFoundLabel;
    @FXML private Label cleanableSpaceLabel;
    @FXML private Label totalDataLabel;
    @FXML private Label scanSpeedLabel;
    @FXML private Label elapsedTimeLabel;
    @FXML private Label errorsCountLabel;

    // Dynamic sections
    @FXML private VBox recentDiscoveriesSection;
    @FXML private VBox recentProjectsList;
    @FXML private VBox projectTypesSection;
    @FXML private javafx.scene.layout.HBox projectTypesList;
    @FXML private VBox recentErrorsSection;
    @FXML private VBox recentErrorsList;

    // Control buttons
    @FXML private Button pauseButton;
    @FXML private Button cancelButton;

    private ProjectsController projectsController;
    private final DirectoryScanner scanner = new DirectoryScanner();
    private Task<List<Project>> currentScanTask;
    private javafx.animation.Timeline progressUpdateTimer;

    public void setProjectsController(ProjectsController projectsController) {
        this.projectsController = projectsController;
    }

    @FXML
    private void initialize() {
        directoryPathField.setText(UserPreferences.getLastScannedPath());

        // Initialize progress update timer
        progressUpdateTimer = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(500),
                e -> updateProgressDisplay()
            )
        );
        progressUpdateTimer.setCycleCount(javafx.animation.Timeline.INDEFINITE);

        // Initialize UI state
        resetProgressDisplay();
    }

    @FXML
    private void handleBrowseAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory to Scan");
        File initialDir = new File(directoryPathField.getText());
        if (initialDir.exists() && initialDir.isDirectory()) {
            directoryChooser.setInitialDirectory(initialDir);
        }
        File selectedDirectory = directoryChooser.showDialog(scanRoot.getScene().getWindow());
        if (selectedDirectory != null) {
            directoryPathField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    private void handleScanAction() {
        String pathText = directoryPathField.getText().trim();
        if (pathText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Path Required", "Please enter a directory path to scan.");
            return;
        }

        Path scanPath = Paths.get(pathText);
        if (!scanPath.toFile().isDirectory()) {
            showAlert(Alert.AlertType.ERROR, "Invalid Directory", "The specified path is not a valid directory.");
            return;
        }

        // Switch to scanning UI
        scanBox.setVisible(false);
        scanningBox.setVisible(true);
        UserPreferences.setLastScannedPath(pathText);

        // Reset progress display
        resetProgressDisplay();

        // Start progress update timer
        progressUpdateTimer.play();

        // Create enhanced scan task with progress callback
        currentScanTask = new Task<List<Project>>() {
            @Override
            protected List<Project> call() throws IOException {
                return scanner.scan(scanPath, progressInfo -> {
                    // This callback runs in background thread - update UI via Platform.runLater
                    Platform.runLater(() -> {
                        // Will be called by updateProgressDisplay() on timer
                    });
                });
            }
        };

        currentScanTask.setOnSucceeded(event -> {
            progressUpdateTimer.stop();
            List<Project> newProjects = currentScanTask.getValue();

            Platform.runLater(() -> {
                // Final progress update
                updateProgressDisplay();

                // Save and merge projects
                mergeAndSaveProjects(newProjects);

                // Show completion message and return to scan input
                scanBox.setVisible(true);
                scanningBox.setVisible(false);
                showAlert(Alert.AlertType.INFORMATION, "Scan Complete",
                    "Found and saved " + newProjects.size() + " new or updated projects.");
            });
        });

        currentScanTask.setOnFailed(event -> {
            progressUpdateTimer.stop();
            LogManager.log("Scan failed: " + currentScanTask.getException().getMessage());
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Scan Failed", "An error occurred during scanning. Check logs for details.");
                scanBox.setVisible(true);
                scanningBox.setVisible(false);
            });
        });

        currentScanTask.setOnCancelled(event -> {
            progressUpdateTimer.stop();
            Platform.runLater(() -> {
                scanBox.setVisible(true);
                scanningBox.setVisible(false);
            });
        });

        new Thread(currentScanTask).start();
    }

    @FXML
    private void handlePauseAction() {
        // For now, we'll implement cancel functionality
        // True pause/resume would require more complex state management
        handleCancelAction();
    }

    @FXML
    private void handleCancelAction() {
        if (currentScanTask != null && currentScanTask.isRunning()) {
            scanner.cancel();
            currentScanTask.cancel();
        }
    }

    private void resetProgressDisplay() {
        // Reset all progress labels and bars
        scanPhaseLabel.setText("Initializing...");
        progressPercentageLabel.setText("0%");
        progressETALabel.setText("Estimating...");
        scanProgressBar.setProgress(0);
        currentFolderLabel.setText("/");
        currentFileLabel.setText("Preparing scan...");

        // Reset statistics
        directoriesScannedLabel.setText("0");
        filesScannedLabel.setText("0");
        projectsFoundLabel.setText("0");
        cleanableSpaceLabel.setText("0 B");
        totalDataLabel.setText("0 B");
        scanSpeedLabel.setText("0/s");
        elapsedTimeLabel.setText("Elapsed: 0s");
        errorsCountLabel.setText("Errors: 0");

        // Hide dynamic sections initially
        recentDiscoveriesSection.setVisible(false);
        recentDiscoveriesSection.setManaged(false);
        projectTypesSection.setVisible(false);
        projectTypesSection.setManaged(false);
        recentErrorsSection.setVisible(false);
        recentErrorsSection.setManaged(false);

        // Clear dynamic content
        recentProjectsList.getChildren().clear();
        projectTypesList.getChildren().clear();
        recentErrorsList.getChildren().clear();
    }

    private void updateProgressDisplay() {
        ScanProgressInfo progressInfo = scanner.getProgressInfo();
        if (progressInfo == null) return;

        // Update phase and progress
        scanPhaseLabel.setText(progressInfo.getCurrentPhase().description);
        progressPercentageLabel.setText(String.format("%.1f%%", progressInfo.getProgressPercentage()));
        progressETALabel.setText("ETA: " + progressInfo.getFormattedEstimatedTimeRemaining());
        scanProgressBar.setProgress(progressInfo.getProgressPercentage() / 100.0);

        // Update current scanning info
        Path currentDir = progressInfo.getCurrentDirectory();
        if (currentDir != null) {
            String displayPath = currentDir.toString();
            if (displayPath.length() > 50) {
                displayPath = "..." + displayPath.substring(displayPath.length() - 47);
            }
            currentFolderLabel.setText(displayPath);
        }

        String currentFile = progressInfo.getCurrentFileName();
        if (currentFile != null && !currentFile.isEmpty()) {
            currentFileLabel.setText(currentFile);
        } else {
            currentFileLabel.setText("Scanning directories...");
        }

        // Update statistics
        directoriesScannedLabel.setText(String.format("%,d", progressInfo.getDirectoriesScanned()));
        filesScannedLabel.setText(String.format("%,d", progressInfo.getFilesScanned()));
        projectsFoundLabel.setText(String.format("%,d", progressInfo.getProjectsFound()));
        cleanableSpaceLabel.setText(progressInfo.getFormattedCleanableSpace());
        totalDataLabel.setText(progressInfo.getFormattedBytesProcessed());
        scanSpeedLabel.setText(progressInfo.getDirectoriesPerSecond() + "/s");
        elapsedTimeLabel.setText("Elapsed: " + progressInfo.getFormattedElapsedTime());
        errorsCountLabel.setText("Errors: " + progressInfo.getErrorsEncountered());

        // Update recent discoveries
        List<Project> recentProjects = progressInfo.getRecentProjects();
        if (!recentProjects.isEmpty()) {
            updateRecentDiscoveries(recentProjects);
        }

        // Update project types
        Map<ProjectType, Integer> typeBreakdown = progressInfo.getProjectTypeBreakdown();
        if (!typeBreakdown.isEmpty()) {
            updateProjectTypes(typeBreakdown);
        }

        // Update recent errors
        List<String> recentErrors = progressInfo.getRecentErrors();
        if (!recentErrors.isEmpty()) {
            updateRecentErrors(recentErrors);
        }
    }

    private void updateRecentDiscoveries(List<Project> recentProjects) {
        recentProjectsList.getChildren().clear();

        for (Project project : recentProjects) {
            javafx.scene.layout.HBox projectBox = new javafx.scene.layout.HBox();
            projectBox.setSpacing(8);
            projectBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            projectBox.getStyleClass().add("recent-project-item");

            javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(project.getName());
            nameLabel.getStyleClass().add("project-name");

            javafx.scene.control.Label typeLabel = new javafx.scene.control.Label(project.getType().displayName);
            typeLabel.getStyleClass().add("project-type");

            projectBox.getChildren().addAll(nameLabel, typeLabel);
            recentProjectsList.getChildren().add(projectBox);
        }

        recentDiscoveriesSection.setVisible(true);
        recentDiscoveriesSection.setManaged(true);
    }

    private void updateProjectTypes(Map<ProjectType, Integer> typeBreakdown) {
        projectTypesList.getChildren().clear();

        for (Map.Entry<ProjectType, Integer> entry : typeBreakdown.entrySet()) {
            javafx.scene.layout.VBox typeBox = new javafx.scene.layout.VBox();
            typeBox.setSpacing(4);
            typeBox.setAlignment(javafx.geometry.Pos.CENTER);
            typeBox.getStyleClass().add("project-type-item");

            javafx.scene.control.Label countLabel = new javafx.scene.control.Label(entry.getValue().toString());
            countLabel.getStyleClass().add("type-count");

            javafx.scene.control.Label typeLabel = new javafx.scene.control.Label(entry.getKey().displayName);
            typeLabel.getStyleClass().add("type-name");

            typeBox.getChildren().addAll(countLabel, typeLabel);
            projectTypesList.getChildren().add(typeBox);
        }

        projectTypesSection.setVisible(true);
        projectTypesSection.setManaged(true);
    }

    private void updateRecentErrors(List<String> recentErrors) {
        recentErrorsList.getChildren().clear();

        for (String error : recentErrors) {
            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label(error);
            errorLabel.getStyleClass().add("error-item");
            errorLabel.setWrapText(true);
            recentErrorsList.getChildren().add(errorLabel);
        }

        recentErrorsSection.setVisible(true);
        recentErrorsSection.setManaged(true);
    }

    private void mergeAndSaveProjects(List<Project> newProjects) {
        ApplicationDataService dataService = ApplicationDataService.getInstance();
        List<Project> existingProjects = dataService.getProjects();
        Map<Path, Project> projectMap = existingProjects.stream()
                .collect(Collectors.toMap(Project::getPath, Function.identity()));
        for (Project newProject : newProjects) {
            projectMap.put(newProject.getPath(), newProject);
        }

        // Update data service with merged projects
        dataService.updateProjects(new java.util.ArrayList<>(projectMap.values()));
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.initOwner(scanRoot.getScene().getWindow());
            alert.showAndWait();
        });
    }
}