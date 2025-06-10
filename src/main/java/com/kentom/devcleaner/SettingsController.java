package com.kentom.devcleaner;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

public class SettingsController {

    @FXML
    private CheckBox autoScanCheckbox;

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
}