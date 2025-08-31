package com.kentom.devcleaner;

import com.kentom.devcleaner.model.LogManager;
import com.kentom.devcleaner.model.Project;
import com.kentom.devcleaner.model.ProjectCache;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectsController {

    @FXML private TilePane projectTilePane;
    @FXML private VBox emptyStateBox;
    @FXML private Button resetButton;


    private MainController mainController;
    private List<Project> activeProjects;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        refreshProjects();
    }

    public void refreshProjects() {
        activeProjects = ProjectCache.loadProjects();
        boolean projectsExist = !activeProjects.isEmpty();

        // When projects exist, the empty state is made invisible AND unmanaged,
        // which removes it from the layout bounds completely.
        emptyStateBox.setVisible(!projectsExist);
        emptyStateBox.setManaged(!projectsExist);

        // The project grid is made visible and managed only when projects exist.
        projectTilePane.setVisible(projectsExist);
        projectTilePane.setManaged(projectsExist);

        // Show reset button only when projects exist
        resetButton.setVisible(projectsExist);
        resetButton.setManaged(projectsExist);

        populateGrid();
    }

    private void populateGrid() {
        Platform.runLater(() -> {
            projectTilePane.getChildren().clear();
            if (activeProjects.isEmpty()) {
                return; // Nothing to populate
            }

            for (Project project : activeProjects) {
                projectTilePane.getChildren().add(createProjectTile(project));
            }
        });
    }

    private Node createProjectTile(Project project) {
        VBox tile = new VBox(5);
        tile.getStyleClass().add("project-tile");
        tile.setAlignment(Pos.CENTER);

        ImageView icon = new ImageView(project.getIcon());
        icon.setFitHeight(48);
        icon.setFitWidth(48);
        icon.setPreserveRatio(true);

        Label name = new Label(formatProjectName(project.getName()));
        name.getStyleClass().add("tile-title");

        Label type = new Label(project.getType().displayName);
        type.getStyleClass().add("tile-subtitle");

        tile.getChildren().addAll(icon, name, type);
        tile.setOnMouseClicked(event -> showProjectDetails(project));

        FadeTransition ft = new FadeTransition(Duration.millis(500), tile);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();

        return tile;
    }

    private String formatProjectName(String name) {
        StringBuilder formattedName = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : name.toCharArray()) {
            if (c == '-') {
                formattedName.append(' ');
                capitalizeNext = true;
            } else if (Character.isWhitespace(c)) {
                formattedName.append(c);
                capitalizeNext = true;
            } else {
                if (capitalizeNext) {
                    formattedName.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    formattedName.append(c);
                }
            }
        }

        String[] words = formattedName.toString().split(" ");
        if (words.length > 2) {
            formattedName = new StringBuilder(words[0] + " " + words[1] + "...");
        }

        return formattedName.toString();
    }

    public void removeProject(Project project) {
        activeProjects.remove(project);
        ProjectCache.saveProjects(activeProjects);
        refreshProjects();
    }

    public void showProjectDetails(Project project) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("project-details-view.fxml"));
            Node detailsView = loader.load();
            ProjectDetailsController controller = loader.getController();
            controller.setProject(project, this, mainController);
            mainController.showProjectDetails(detailsView);
        } catch (IOException e) {
            LogManager.log("Failed to load project details view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleResetProjects() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Reset All Projects");
        confirmation.getDialogPane().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        confirmation.setHeaderText("Clear all projects from DevCleaner?");
        confirmation.setContentText("This will remove all projects from the application's cache. It will not delete any files from your disk. Projects can be re-added by scanning again.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            activeProjects.clear();
            ProjectCache.saveProjects(new ArrayList<>());
            LogManager.log("All projects cleared by user reset");
            refreshProjects();
            
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
