package com.kentom.devcleaner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class DevCleanerApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(DevCleanerApp.class.getResource("main-view.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 1024, 768);

        // Add all icon sizes using correct resource path
        stage.getIcons().addAll(
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/logo-16x16.png"))),
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/logo-32x32.png"))),
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/logo-64x64.png"))),
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/logo-128x128.png"))),
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/logo-256x256.png"))),
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/logo-512x512.png")))
        );

        stage.setTitle("Dev Cleaner Pro");
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}