package com.kentom.devcleaner;

import com.kentom.devcleaner.model.CleanupTask;
import com.kentom.devcleaner.model.LogManager;
import com.kentom.devcleaner.model.Project;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ProjectDetailsController {

    @FXML private Label projectNameLabel;
    @FXML private Label projectPathLabel;
    @FXML private Label projectDateLabel;
    @FXML private Label projectSizeLabel;
    @FXML private Button cleanButton;

    private Project project;
    private CleanerController cleanerController;

    public void setProject(Project project, CleanerController cleanerController) {
        this.project = project;
        this.cleanerController = cleanerController;
        displayProjectDetails();
    }

    private void displayProjectDetails() {
        projectNameLabel.setText(project.getName());
        projectPathLabel.setText(project.getPath().toString());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
        projectDateLabel.setText("Cached on: " + project.getDateCreated().format(formatter));
        projectSizeLabel.setText("Cleanable Size: " + formatSize(project.getSizeOfCleanableItems()));
        cleanButton.setDisable(project.getCleanableItems().isEmpty());
    }

    @FXML
    private void handleBackAction() {
        cleanerController.hideProjectDetails();
    }

    @FXML
    private void handleCleanProject() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Cleanup");
        confirmation.setHeaderText("Delete " + formatSize(project.getSizeOfCleanableItems()) + " for '" + project.getName() + "'?");
        confirmation.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                new CleanupTask().clean(project.getCleanableItems());
                showAlert(Alert.AlertType.INFORMATION, "Success", "Cleanup completed successfully.");
                cleanerController.removeProject(project);
                handleBackAction();
            } catch (Exception e) {
                LogManager.log("Cleanup failed: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Cleanup Failed", "Could not delete all items. Check file permissions.");
            }
        }
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
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
}
