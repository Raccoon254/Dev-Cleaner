package com.kentom.devcleaner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class DevCleanerApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;

        // Show main application directly (skip splash screen)
        showMainApplication();
    }
    
    private void showMainApplication() {
        try {
            // Load main application
            FXMLLoader mainLoader = new FXMLLoader(DevCleanerApp.class.getResource("main-view.fxml"));
            Parent mainRoot = mainLoader.load();
            Scene mainScene = new Scene(mainRoot, 1380, 900);

            // Set dark background immediately to prevent white screen
            mainScene.setFill(Color.rgb(12, 12, 12)); // #0C0C0C

            // Apply stylesheet immediately
            mainScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styles.css")).toExternalForm());

            // Configure primary stage
            addAppIcons(primaryStage);
            primaryStage.setTitle("Dev Cleaner");
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(650);
            primaryStage.setScene(mainScene);

            // Start maximized
            //primaryStage.setMaximized(true);

            // Force CSS application before showing
            mainRoot.applyCss();
            mainRoot.layout();

            // Show main application after everything is ready
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void addAppIcons(Stage stage) {
        try {
            stage.getIcons().addAll(
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/logo-16x16.png"))),
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/logo-32x32.png"))),
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/logo-64x64.png"))),
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/logo-128x128.png"))),
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/logo-256x256.png"))),
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/logo-512x512.png")))
            );
        } catch (Exception e) {
            // If icons can't be loaded, continue without them
            System.err.println("Warning: Could not load application icons");
        }
    }

    public static void main(String[] args) {
        launch();
    }
}