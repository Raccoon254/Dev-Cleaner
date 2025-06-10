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
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;

public class ProjectsController {

    @FXML private TilePane projectTilePane;
    @FXML private VBox emptyStateBox;


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
}
