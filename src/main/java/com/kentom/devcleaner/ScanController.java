package com.kentom.devcleaner;

import com.kentom.devcleaner.model.DirectoryScanner;
import com.kentom.devcleaner.model.LogManager;
import com.kentom.devcleaner.model.Project;
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
    @FXML private VBox scanningBox;
    @FXML private Label scanStatusLabel;
    @FXML private StackPane loadingAnimation;

    private ProjectsController projectsController;
    private final DirectoryScanner scanner = new DirectoryScanner();

    public void setProjectsController(ProjectsController projectsController) {
        this.projectsController = projectsController;
    }

    @FXML
    private void initialize() {
        directoryPathField.setText(UserPreferences.getLastScannedPath());
        RotateTransition rt = new RotateTransition(Duration.seconds(2), loadingAnimation);
        rt.setByAngle(360);
        rt.setCycleCount(RotateTransition.INDEFINITE);
        rt.setInterpolator(javafx.animation.Interpolator.LINEAR);
        rt.play();
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

        scanBox.setVisible(false);
        scanningBox.setVisible(true);
        scanStatusLabel.setText("Initializing scan...");
        UserPreferences.setLastScannedPath(pathText);

        Task<List<Project>> scanTask = new Task<>() {
            @Override
            protected List<Project> call() throws IOException {
                Platform.runLater(() -> scanStatusLabel.setText("Scanning " + scanPath.getFileName() + "..."));
                return scanner.scan(scanPath);
            }
        };

        scanTask.setOnSucceeded(event -> {
            List<Project> newProjects = scanTask.getValue();
            Platform.runLater(() -> scanStatusLabel.setText("Found " + newProjects.size() + " projects. Saving..."));
            mergeAndSaveProjects(newProjects);
            // No need to manually refresh - ApplicationDataService will notify all listeners
            Platform.runLater(() -> {
                scanBox.setVisible(true);
                scanningBox.setVisible(false);
                showAlert(Alert.AlertType.INFORMATION, "Scan Complete", "Found and saved " + newProjects.size() + " new or updated projects.");
            });
        });

        scanTask.setOnFailed(event -> {
            LogManager.log("Scan failed: " + scanTask.getException().getMessage());
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Scan Failed", "An error occurred during scanning. Check logs for details.");
                scanBox.setVisible(true);
                scanningBox.setVisible(false);
            });
        });

        new Thread(scanTask).start();
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