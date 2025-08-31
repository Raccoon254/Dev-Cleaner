package com.kentom.devcleaner;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SplashController {

    @FXML private VBox splashRoot;
    @FXML private ImageView appIcon;
    @FXML private Label versionLabel;
    @FXML private Label loadingLabel;
    @FXML private ProgressBar progressBar;

    private Runnable onLoadComplete;

    @FXML
    private void initialize() {
        // Set version from system properties or manifest
        try {
            String version = getClass().getPackage().getImplementationVersion();
            if (version != null) {
                versionLabel.setText("v" + version);
            }
        } catch (Exception e) {
            // Keep default version
        }
    }

    public void setOnLoadComplete(Runnable onLoadComplete) {
        this.onLoadComplete = onLoadComplete;
    }

    public void startLoading() {
        // Create a background task to simulate loading
        Task<Void> loadingTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String[] loadingSteps = {
                    "Initializing application...",
                    "Loading configuration...",
                    "Scanning for projects...",
                    "Preparing interface...",
                    "Almost ready..."
                };

                for (int i = 0; i < loadingSteps.length; i++) {
                    final int step = i;
                    final double progress = (double) (i + 1) / loadingSteps.length;
                    
                    Platform.runLater(() -> {
                        loadingLabel.setText(loadingSteps[step]);
                        progressBar.setProgress(progress);
                    });
                    
                    // Simulate some loading time
                    Thread.sleep(400 + (long) (Math.random() * 600)); // 400-1000ms per step
                }

                return null;
            }
        };

        loadingTask.setOnSucceeded(e -> {
            // Add a small delay before transitioning to main app
            Timeline delay = new Timeline(new KeyFrame(Duration.millis(300), event -> {
                if (onLoadComplete != null) {
                    onLoadComplete.run();
                }
            }));
            delay.play();
        });

        loadingTask.setOnFailed(e -> {
            // Handle loading failure
            Platform.runLater(() -> {
                loadingLabel.setText("Failed to load application");
                progressBar.setProgress(0);
            });
        });

        // Start the loading task in background thread
        Thread loadingThread = new Thread(loadingTask);
        loadingThread.setDaemon(true);
        loadingThread.start();
    }
}