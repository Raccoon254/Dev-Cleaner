package com.kentom.devcleaner;

import com.kentom.devcleaner.model.LogManager;
import com.kentom.devcleaner.model.ProjectCache;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;

import java.util.ArrayList;
import java.util.Optional;

public class SettingsController {

    @FXML private CheckBox autoScanCheckbox;
    @FXML private Button resetButton;

    @FXML
    public void initialize() {
        // Here you would load the preference from UserPreferences
        // e.g., autoScanCheckbox.setSelected(UserPreferences.isAutoScanEnabled());
    }

    @FXML
    private void handleAutoScanToggle() {
        boolean isEnabled = autoScanCheckbox.isSelected();
        // Here you would save the preference
        // e.g., UserPreferences.setAutoScanEnabled(isEnabled);
        System.out.println("Auto-scan on startup: " + (isEnabled ? "Enabled" : "Disabled"));
    }

    @FXML
    private void handleResetProjects() {
        Alert confirmation = new Alert(Alert.AlertType.WARNING);
        confirmation.setTitle("Reset All Projects");
        confirmation.getDialogPane().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        confirmation.setHeaderText("Clear all projects from DevCleaner?");
        confirmation.setContentText("This will remove all projects from the application's cache. It will not delete any files from your disk. Projects can be re-added by scanning again.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ProjectCache.saveProjects(new ArrayList<>());
            LogManager.log("All projects cleared by user reset from settings");
            
            // Show success message
            Platform.runLater(() -> {
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Reset Complete");
                success.getDialogPane().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
                success.setHeaderText("All projects cleared");
                success.setContentText("All projects have been removed from DevCleaner's cache.");
                success.showAndWait();
            });
        }
    }
}