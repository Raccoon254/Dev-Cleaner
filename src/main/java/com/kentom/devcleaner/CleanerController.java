package com.kentom.devcleaner;

import com.kentom.devcleaner.model.CleanupTask;
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
import javafx.scene.effect.GaussianBlur;
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

public class CleanerController {

    @FXML private StackPane rootStackPane;
    @FXML private VBox mainView;
    @FXML private TextField directoryPathField;
    @FXML private Button scanButton;
    @FXML private TilePane projectTilePane;
    @FXML private ScrollPane scrollPane;
    @FXML private ProgressIndicator scanProgressIndicator;

    private final DirectoryScanner scanner = new DirectoryScanner();
    private List<Project> activeProjects;

    @FXML
    private void initialize() {
        directoryPathField.setText(UserPreferences.getLastScannedPath());
        scanProgressIndicator.setVisible(false);
        // Load cached projects on startup
        loadCachedProjects();
    }

    private void loadCachedProjects() {
        activeProjects = ProjectCache.loadProjects();
        if (activeProjects.isEmpty()) {
            LogManager.log("No cached projects found. Ready for a new scan.");
        }
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
        VBox tile = new VBox(10);
        tile.getStyleClass().add("project-tile");
        tile.setAlignment(Pos.CENTER);

        ImageView icon = new ImageView(project.getIcon());
        icon.setFitHeight(50);
        icon.setFitWidth(50);
        icon.setPreserveRatio(true);

        Label name = new Label(project.getName());
        name.getStyleClass().add("tile-title");

        Label type = new Label(project.getType().displayName + " Project");
        type.getStyleClass().add("tile-subtitle");

        tile.getChildren().addAll(icon, name, type);
        tile.setOnMouseClicked(event -> showProjectDetails(project));

        FadeTransition ft = new FadeTransition(Duration.millis(700), tile);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();

        return tile;
    }

    @FXML
    private void handleBrowseAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select a Directory to Scan");
        File initialDir = new File(directoryPathField.getText());
        if (initialDir.exists() && initialDir.isDirectory()) {
            directoryChooser.setInitialDirectory(initialDir);
        }
        File selectedDirectory = directoryChooser.showDialog(rootStackPane.getScene().getWindow());
        if (selectedDirectory != null) {
            directoryPathField.setText(selectedDirectory.getAbsolutePath());
            handleScanAction();
        }
    }

    @FXML
    private void handleScanAction() {
        String pathText = directoryPathField.getText().trim();
        if (pathText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Path Required", "Please enter or browse to a directory path to scan.");
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
            protected List<Project> call() throws Exception {
                LogManager.log("Starting scan of directory: " + pathText);
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
            Throwable ex = scanTask.getException();
            String errorMessage = (ex != null) ? ex.getMessage() : "An unknown error occurred.";
            LogManager.log("Scan failed: " + errorMessage);
            showAlert(Alert.AlertType.ERROR, "Scan Failed", "An error occurred during scanning:\n" + errorMessage);
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
        LogManager.log("Projects merged and cache updated.");
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
            detailsView.setId("detailsView");
            ProjectDetailsController controller = loader.getController();
            controller.setProject(project, this);
            FadeTransition ft = new FadeTransition(Duration.millis(300), detailsView);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            mainView.setEffect(new GaussianBlur(10));
            rootStackPane.getChildren().add(detailsView);
            ft.play();
        } catch (IOException e) {
            LogManager.log("Failed to load project details view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void hideProjectDetails() {
        mainView.setEffect(null);
        rootStackPane.getChildren().removeIf(node -> "detailsView".equals(node.getId()));
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
