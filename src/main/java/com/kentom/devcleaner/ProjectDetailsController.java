package com.kentom.devcleaner;

import com.kentom.devcleaner.model.CleanupTask;
import com.kentom.devcleaner.model.LogManager;
import com.kentom.devcleaner.model.Project;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;

public class ProjectDetailsController {

    @FXML private VBox detailsView;
    @FXML private Label projectNameLabel;
    @FXML private Label projectPathLabel;
    @FXML private Label projectDateLabel;
    @FXML private Label projectSizeLabel;
    @FXML private ImageView projectIcon;
    @FXML private ListView<String> cleanableFilesListView;
    @FXML private Button cleanButton;

    private Project project;
    private DashboardController dashboardController;
    private MainController mainController;

    public void setProject(Project project, DashboardController dashboardController, MainController mainController) {
        this.project = project;
        this.dashboardController = dashboardController;
        this.mainController = mainController;
        displayProjectDetails();
    }

    private void displayProjectDetails() {
        projectNameLabel.setText(project.getName());
        projectPathLabel.setText(project.getPath().toString());
        projectIcon.setImage(project.getIcon());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
        projectDateLabel.setText("Cached on: " + project.getDateCreated().format(formatter));
        projectSizeLabel.setText(String.format("Cleanable Size: %s", formatSize(project.getSizeOfCleanableItems())));

        cleanableFilesListView.setItems(FXCollections.observableArrayList(
                project.getCleanableItems().stream()
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .collect(Collectors.toList())
        ));

        cleanButton.setDisable(project.getCleanableItems().isEmpty());
    }

    @FXML
    private void handleBackAction() {
        mainController.hideProjectDetails(detailsView);
    }

    @FXML
    private void handleCleanProject() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Cleanup");
        confirmation.setHeaderText("Delete " + formatSize(project.getSizeOfCleanableItems()) + " for '" + project.getName() + "'?");
        confirmation.setContentText("This action permanently deletes files and cannot be undone.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                new CleanupTask().clean(project.getCleanableItems());
                showAlert(Alert.AlertType.INFORMATION, "Success", "Cleanup completed. The project has been removed from the list.");
                dashboardController.removeProject(project);
                handleBackAction();
            } catch (Exception e) {
                LogManager.log("Cleanup failed: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Cleanup Failed", "Could not delete all items. Check logs and file permissions.");
            }
        }
    }

    @FXML
    private void handleForgetProject() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Forget Project");
        confirmation.setHeaderText("Remove '" + project.getName() + "' from DevCleaner?");
        confirmation.setContentText("This will remove the project from the application's cache. It will not delete any files from your disk. The project can be re-added by scanning again.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if(result.isPresent() && result.get() == ButtonType.OK) {
            dashboardController.removeProject(project);
            showAlert(Alert.AlertType.INFORMATION, "Project Forgotten", "The project has been removed from the cache.");
            handleBackAction();
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