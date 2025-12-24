package com.kentom.devcleaner;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class AboutController {

    @FXML private VBox aboutRoot;
    @FXML private ImageView appIcon;
    @FXML private Label versionLabel;
    @FXML private Label platformLabel;
    @FXML private Label buildInfoLabel;

    @FXML
    private void initialize() {
        // Set version from package manifest
        try {
            String version = getClass().getPackage().getImplementationVersion();
            if (version != null) {
                versionLabel.setText("v" + version);
            } else {
                versionLabel.setText("v1.0-SNAPSHOT");
            }
        } catch (Exception e) {
            versionLabel.setText("v1.0-SNAPSHOT");
        }

        // Set platform info
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        platformLabel.setText(String.format("%s %s (%s)", osName, osVersion, osArch));

        // Set build info
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version", "21");
        buildInfoLabel.setText(String.format("Java %s • JavaFX %s", javaVersion, javafxVersion));
    }
}
