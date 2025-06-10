package com.kentom.devcleaner;

import com.kentom.devcleaner.model.CleanupTask;
import com.kentom.devcleaner.model.DirectoryScanner;
import com.kentom.devcleaner.model.LogManager;
import com.kentom.devcleaner.model.Project;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class CleanerController {

    @FXML
    private TextField directoryPathField;
    @FXML
    private Button scanButton;
    @FXML
    private ListView<Project> projectListView;
    @FXML
    private VBox rootVBox; // Make sure you have fx:id="rootVBox" on your root VBox in FXML
    @FXML
    private ProgressIndicator scanProgressIndicator;

    private final DirectoryScanner scanner = new DirectoryScanner();
    private final CleanupTask cleanupTask = new CleanupTask();

    @FXML
    private void initialize() {
        // Set a default path for user convenience
        directoryPathField.setText(System.getProperty("user.home"));
        scanProgressIndicator.setVisible(false);

        // Custom cell factory to render our Project objects
        projectListView.setCellFactory(param -> new ProjectCell());

        // Add a nice fade-in effect for the list
        projectListView.setItems(FXCollections.observableArrayList());

        LogManager.log("Application initialized.");
    }

    @FXML
    private void handleBrowseAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select a Directory to Scan");
        File initialDir = new File(directoryPathField.getText());
        if (initialDir.exists() && initialDir.isDirectory()) {
            directoryChooser.setInitialDirectory(initialDir);
        }
        File selectedDirectory = directoryChooser.showDialog(rootVBox.getScene().getWindow());
        if (selectedDirectory != null) {
            directoryPathField.setText(selectedDirectory.getAbsolutePath());
            handleScanAction();
        }
    }

    @FXML
    private void handleScanAction() {
        String directoryPath = directoryPathField.getText().trim();
        if (directoryPath.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Path Required", "Please enter or browse to a directory path to scan.");
            return;
        }

        Path path = Paths.get(directoryPath);
        if (!path.toFile().exists() || !path.toFile().isDirectory()){
            showAlert(Alert.AlertType.ERROR, "Invalid Path", "The specified path does not exist or is not a directory.");
            return;
        }

        projectListView.getItems().clear();
        scanProgressIndicator.setVisible(true);
        scanButton.setDisable(true);

        // Run the scan on a background thread to keep the UI responsive
        Task<List<Project>> scanTask = new Task<>() {
            @Override
            protected List<Project> call() throws Exception {
                LogManager.log("Starting scan of directory: " + directoryPath);
                return scanner.scan(path);
            }
        };

        scanTask.setOnSucceeded(event -> {
            List<Project> projects = scanTask.getValue();
            projectListView.setItems(FXCollections.observableArrayList(projects));
            if (projects.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Scan Complete", "No recognizable projects found in the selected directory.");
            }
            LogManager.log("Scan completed. Found " + projects.size() + " projects.");
            scanProgressIndicator.setVisible(false);
            scanButton.setDisable(false);
        });

        scanTask.setOnFailed(event -> {
            Throwable e = scanTask.getException();
            showAlert(Alert.AlertType.ERROR, "Scan Failed", "An error occurred during the scan: " + e.getMessage());
            LogManager.log("Scan failed: " + e.getMessage());
            scanProgressIndicator.setVisible(false);
            scanButton.setDisable(false);
        });

        new Thread(scanTask).start();
    }

    private void handleCleanAction(Project project) {
        if (project.getCleanableItems().isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Nothing to Clean", "This project has no items that match the cleanup rules.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Cleanup");
        confirmation.setHeaderText("Delete " + project.getCleanableItems().size() + " items for '" + project.getName() + "'?");
        String details = "This will permanently delete " + formatSize(project.getSizeOfCleanableItems()) + " of data. This action cannot be undone.";
        confirmation.setContentText(details);

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                cleanupTask.clean(project.getCleanableItems());
                projectListView.getItems().remove(project);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Cleanup completed for '" + project.getName() + "'.");
                LogManager.log("Cleanup completed for " + project.getName());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Cleanup Failed", "An error occurred during cleanup: " + e.getMessage());
                LogManager.log("Cleanup failed: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double)size / (1L << (z*10)), " KMGTPE".charAt(z));
    }

    /**
     * Custom ListCell for displaying project details in a styled card format.
     */
    private class ProjectCell extends ListCell<Project> {
        private final HBox content = new HBox(15);
        private final ImageView icon = new ImageView();
        private final Text name = new Text();
        private final Text path = new Text();
        private final Text cleanableInfo = new Text();
        private final Button cleanButton = new Button("Clean");
        private final VBox projectDetails = new VBox(5);
        private final Region spacer = new Region();

        public ProjectCell() {
            super();
            // Configure the icon
            icon.setFitWidth(40);
            icon.setFitHeight(40);
            icon.setPreserveRatio(true);
            icon.setEffect(new DropShadow(5, Color.BLACK));

            // Apply style classes to text elements
            name.getStyleClass().add("project-name");
            path.getStyleClass().add("project-path");
            cleanableInfo.getStyleClass().add("project-info");

            // Build the layout
            projectDetails.getChildren().addAll(name, path, cleanableInfo);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            content.setAlignment(Pos.CENTER_LEFT);
            content.getChildren().addAll(icon, projectDetails, spacer, cleanButton);

            // Add a fade-in transition when the cell appears
            FadeTransition ft = new FadeTransition(Duration.millis(500), content);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }

        @Override
        protected void updateItem(Project project, boolean empty) {
            super.updateItem(project, empty);
            if (empty || project == null) {
                setGraphic(null);
            } else {
                icon.setImage(project.getIcon());
                name.setText(project.getName());
                path.setText(project.getPath().toString());
                cleanableInfo.setText(project.getType().displayName + " Project • " + formatSize(project.getSizeOfCleanableItems()) + " to clean");

                // Set the action for the button for this specific project
                cleanButton.setOnAction(e -> handleCleanAction(project));

                // Disable button if there's nothing to clean
                cleanButton.setDisable(project.getCleanableItems().isEmpty());

                setGraphic(content);
            }
        }
    }
}
