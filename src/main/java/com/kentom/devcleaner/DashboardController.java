package com.kentom.devcleaner;

import com.kentom.devcleaner.model.DirectoryScanner;
import com.kentom.devcleaner.model.LogManager;
import com.kentom.devcleaner.model.Project;
import com.kentom.devcleaner.model.ProjectCache;
import com.kentom.devcleaner.util.UserPreferences;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private StackPane dashboardRoot;
    @FXML private TextField directoryPathField;
    @FXML private Button scanButton;
    @FXML private TilePane projectTilePane;
    @FXML private ProgressIndicator scanProgressIndicator;

    private MainController mainController;
    private final DirectoryScanner scanner = new DirectoryScanner();
    private List<Project> activeProjects;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        directoryPathField.setText(UserPreferences.getLastScannedPath());
        scanProgressIndicator.setVisible(false);
        loadCachedProjects();
    }

    private void loadCachedProjects() {
        activeProjects = ProjectCache.loadProjects();
        populateGrid();
    }

    private void populateGrid() {
        Platform.runLater(() -> {
            projectTilePane.getChildren().clear();
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

        Label name = new Label(project.getName());
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

    @FXML
    private void handleBrowseAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory to Scan");
        File initialDir = new File(directoryPathField.getText());
        if (initialDir.exists() && initialDir.isDirectory()) {
            directoryChooser.setInitialDirectory(initialDir);
        }
        File selectedDirectory = directoryChooser.showDialog(dashboardRoot.getScene().getWindow());
        if (selectedDirectory != null) {
            directoryPathField.setText(selectedDirectory.getAbsolutePath());
            handleScanAction();
        }
    }

    @FXML
    private void handleScanAction() {
        String pathText = directoryPathField.getText().trim();
        if (pathText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Path Required", "Please enter a directory path to scan.");
            return;
        }

        Path scanPath = Paths.get(pathText);
        if (!scanPath.toFile().isDirectory()) {
            showAlert(Alert.AlertType.ERROR, "Invalid Directory", "The specified path is not a valid directory.");
            return;
        }

        scanProgressIndicator.setVisible(true);
        scanButton.setDisable(true);
        UserPreferences.setLastScannedPath(pathText);

        Task<List<Project>> scanTask = new Task<>() {
            @Override
            protected List<Project> call() throws IOException {
                return scanner.scan(scanPath);
            }
        };

        scanTask.setOnSucceeded(event -> {
            List<Project> newProjects = scanTask.getValue();
            mergeAndSaveProjects(newProjects);
            populateGrid();
            scanProgressIndicator.setVisible(false);
            scanButton.setDisable(false);
        });

        scanTask.setOnFailed(event -> {
            LogManager.log("Scan failed: " + scanTask.getException().getMessage());
            showAlert(Alert.AlertType.ERROR, "Scan Failed", "An error occurred during scanning.");
            scanProgressIndicator.setVisible(false);
            scanButton.setDisable(false);
        });

        new Thread(scanTask).start();
    }

    private void mergeAndSaveProjects(List<Project> newProjects) {
        Map<Path, Project> projectMap = activeProjects.stream()
                .collect(Collectors.toMap(Project::getPath, Function.identity()));
        for (Project newProject : newProjects) {
            projectMap.put(newProject.getPath(), newProject);
        }
        activeProjects = new java.util.ArrayList<>(projectMap.values());
        ProjectCache.saveProjects(activeProjects);
    }

    public void removeProject(Project project) {
        activeProjects.remove(project);
        ProjectCache.saveProjects(activeProjects);
        populateGrid();
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

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}