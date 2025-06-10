package com.kentom.devcleaner;

import com.kentom.devcleaner.model.CleanupTask;
import com.kentom.devcleaner.model.DirectoryScanner;
import com.kentom.devcleaner.model.LogManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CleanerController {
    @FXML
    private TextField directoryPathField;
    @FXML
    private Button scanButton;
    @FXML
    private TextArea scanResultsArea;
    @FXML
    private Button cleanButton;

    private DirectoryScanner scanner;
    private CleanupTask cleanupTask;
    private List<Path> filesToClean;

    @FXML
    private void initialize() {
        scanner = new DirectoryScanner();
        cleanupTask = new CleanupTask();
        cleanButton.setDisable(true);
        LogManager.log("Application initialized.");
    }

    @FXML
    private void handleScanAction() {
        String directoryPath = directoryPathField.getText().trim();
        if (directoryPath.isEmpty()) {
            showAlert("Error", "Please enter a directory path.");
            return;
        }

        Path path = Paths.get(directoryPath);
        scanResultsArea.clear();
        LogManager.log("Starting scan of directory: " + directoryPath);

        try {
            filesToClean = scanner.scan(path);
            if (filesToClean.isEmpty()) {
                scanResultsArea.setText("No files or directories found to clean.");
                cleanButton.setDisable(true);
            } else {
                StringBuilder results = new StringBuilder("Files and directories to clean:\n");
                filesToClean.forEach(p -> results.append(p.toString()).append("\n"));
                scanResultsArea.setText(results.toString());
                cleanButton.setDisable(false);
                LogManager.log("Scan completed. Found " + filesToClean.size() + " items.");
            }
        } catch (Exception e) {
            scanResultsArea.setText("Error during scan: " + e.getMessage());
            LogManager.log("Scan failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleCleanAction() {
        if (filesToClean == null || filesToClean.isEmpty()) {
            showAlert("Error", "No files to clean.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Cleanup");
        confirmation.setHeaderText("Are you sure you want to delete the listed files?");
        confirmation.setContentText("This action cannot be undone.");
        confirmation.showAndWait().ifPresent(response -> {
            if (response.getText().equals("OK")) {
                try {
                    cleanupTask.clean(filesToClean);
                    scanResultsArea.setText("Cleanup completed successfully.");
                    cleanButton.setDisable(true);
                    LogManager.log("Cleanup completed for " + filesToClean.size() + " items.");
                    filesToClean.clear();
                } catch (Exception e) {
                    scanResultsArea.setText("Error during cleanup: " + e.getMessage());
                    LogManager.log("Cleanup failed: " + e.getMessage());
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}