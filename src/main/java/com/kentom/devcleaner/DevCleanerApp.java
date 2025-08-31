package com.kentom.devcleaner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

public class DevCleanerApp extends Application {
    
    private Stage primaryStage;
    private Stage splashStage;
    
    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;
        
        // Show splash screen first
        showSplashScreen();
    }
    
    private void showSplashScreen() throws IOException {
        // Load splash screen
        FXMLLoader splashLoader = new FXMLLoader(DevCleanerApp.class.getResource("splash-screen.fxml"));
        Parent splashRoot = splashLoader.load();
        SplashController splashController = splashLoader.getController();
        
        // Create splash stage
        splashStage = new Stage();
        splashStage.setTitle("DevCleaner");
        splashStage.initStyle(StageStyle.UNDECORATED); // No window decorations
        splashStage.setResizable(false);
        splashStage.setAlwaysOnTop(true);
        
        // Add app icons to splash stage
        addAppIcons(splashStage);
        
        // Create splash scene with better dimensions
        Scene splashScene = new Scene(splashRoot, 600, 400);
        splashScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styles.css")).toExternalForm());
        
        splashStage.setScene(splashScene);
        splashStage.centerOnScreen();
        splashStage.show();
        
        // Set callback to load main application when splash is complete
        splashController.setOnLoadComplete(this::showMainApplication);
        
        // Start the loading process
        splashController.startLoading();
    }
    
    private void showMainApplication() {
        try {
            // Load main application
            FXMLLoader mainLoader = new FXMLLoader(DevCleanerApp.class.getResource("main-view.fxml"));
            Parent mainRoot = mainLoader.load();
            Scene mainScene = new Scene(mainRoot, 1024, 768);
            
            // Configure primary stage
            addAppIcons(primaryStage);
            primaryStage.setTitle("Dev Cleaner");
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(650);
            primaryStage.setScene(mainScene);
            
            // Position main window at center of screen
            primaryStage.centerOnScreen();
            
            // Show main application and hide splash
            primaryStage.show();
            splashStage.close();
            
        } catch (IOException e) {
            e.printStackTrace();
            // If main app fails to load, at least close splash screen
            splashStage.close();
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