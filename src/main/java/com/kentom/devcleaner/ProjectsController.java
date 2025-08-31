package com.kentom.devcleaner;

import com.kentom.devcleaner.model.GitInfo;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;

public class ProjectsController {

    @FXML private TilePane projectTilePane;
    @FXML private VBox emptyStateBox;
    @FXML private VBox projectStatsBox;
    @FXML private Label totalProjectsLabel;
    @FXML private Label totalCleanableLabel;
    @FXML private Label projectTypesLabel;
    @FXML private Button sortNameBtn;
    @FXML private Button sortSizeBtn;
    @FXML private Button sortDateBtn;
    @FXML private Button sortTypeBtn;

    private MainController mainController;
    private List<Project> activeProjects;
    private SortType currentSortType = SortType.NAME;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    private void initialize() {
        updateSortButtons();
        setupResponsiveTilePane();
        refreshProjects();
    }
    
    private void setupResponsiveTilePane() {
        // Make TilePane responsive - calculate columns based on available width
        projectTilePane.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double tileWidth = 220; // tile width + gap
            double availableWidth = newWidth.doubleValue() - 40; // subtract padding
            int columns = Math.max(1, (int) (availableWidth / tileWidth));
            projectTilePane.setPrefColumns(columns);
        });
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

        // Show project stats only when projects exist
        projectStatsBox.setVisible(projectsExist);
        projectStatsBox.setManaged(projectsExist);

        updateHeaderStats();
        sortAndRefreshProjects();
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
        VBox tile = new VBox(8);
        tile.getStyleClass().add("project-tile");
        tile.setAlignment(Pos.CENTER);

        // Project icon
        ImageView icon = new ImageView(project.getIcon());
        icon.setFitHeight(56);
        icon.setFitWidth(56);
        icon.setPreserveRatio(true);

        // Project name (centered)
        Label name = new Label(formatProjectName(project.getName()));
        name.getStyleClass().add("tile-title");
        name.setWrapText(true);
        name.setMaxWidth(180);
        name.setAlignment(Pos.CENTER);

        // Project info section
        VBox infoSection = new VBox(4);
        infoSection.setAlignment(Pos.CENTER);
        
        // Type and size in one row
        HBox typeAndSize = new HBox(8);
        typeAndSize.setAlignment(Pos.CENTER);
        
        Label type = new Label(project.getType().displayName);
        type.getStyleClass().add("tile-subtitle");
        
        Label separator = new Label("•");
        separator.getStyleClass().add("tile-separator");
        
        Label size = new Label(formatSize(project.getSizeOfCleanableItems()));
        size.getStyleClass().add("tile-size");
        
        typeAndSize.getChildren().addAll(type, separator, size);
        
        // Date info
        Label dateInfo = new Label("Updated " + formatDate(project.getDateCreated()));
        dateInfo.getStyleClass().add("tile-date");
        
        infoSection.getChildren().addAll(typeAndSize, dateInfo);
        
        // Add spacer to push powered by section to bottom
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        // Powered by section at bottom
        HBox poweredBySection = new HBox(4);
        poweredBySection.setAlignment(Pos.CENTER);
        poweredBySection.getStyleClass().add("powered-by-section");
        
        GitInfo gitInfo = project.getGitInfo();
        if (gitInfo.isGitRepository()) {
            Label poweredByLabel = new Label("powered by");
            poweredByLabel.getStyleClass().add("powered-by-label");
            
            ImageView gitIcon = new ImageView();
            try {
                gitIcon.setImage(new javafx.scene.image.Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/kentom/devcleaner/icons/github.png"))));
                gitIcon.setFitHeight(12);
                gitIcon.setFitWidth(12);
                gitIcon.setPreserveRatio(true);
                gitIcon.getStyleClass().add("powered-by-icon");
            } catch (Exception e) {
                // Fallback if icon not found
                gitIcon = null;
            }
            
            Label gitLabel = new Label("Git");
            gitLabel.getStyleClass().add("powered-by-text");
            
            poweredBySection.getChildren().add(poweredByLabel);
            if (gitIcon != null) {
                poweredBySection.getChildren().add(gitIcon);
            }
            poweredBySection.getChildren().add(gitLabel);
        }

        tile.getChildren().addAll(icon, name, infoSection, spacer, poweredBySection);
        tile.setOnMouseClicked(event -> showProjectDetails(project));

        FadeTransition ft = new FadeTransition(Duration.millis(500), tile);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();

        return tile;
    }
    
    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
    }
    
    private String formatDate(java.time.LocalDateTime date) {
        java.time.temporal.ChronoUnit unit = java.time.temporal.ChronoUnit.DAYS;
        long daysAgo = unit.between(date.toLocalDate(), java.time.LocalDate.now());
        
        if (daysAgo == 0) {
            return "today";
        } else if (daysAgo == 1) {
            return "yesterday";
        } else if (daysAgo < 30) {
            return daysAgo + " days ago";
        } else if (daysAgo < 365) {
            long monthsAgo = daysAgo / 30;
            return monthsAgo + " month" + (monthsAgo > 1 ? "s" : "") + " ago";
        } else {
            long yearsAgo = daysAgo / 365;
            return yearsAgo + " year" + (yearsAgo > 1 ? "s" : "") + " ago";
        }
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

    private void updateHeaderStats() {
        if (activeProjects == null || activeProjects.isEmpty()) {
            return;
        }
        
        // Calculate total projects
        totalProjectsLabel.setText(String.valueOf(activeProjects.size()));
        
        // Calculate total cleanable size
        long totalCleanable = activeProjects.stream()
                .mapToLong(Project::getSizeOfCleanableItems)
                .sum();
        
        // Format size in GB
        double cleanableGB = totalCleanable / (1024.0 * 1024.0 * 1024.0);
        totalCleanableLabel.setText(String.format("%.1f GB", cleanableGB));
        
        // Calculate unique project types
        long uniqueTypes = activeProjects.stream()
                .map(Project::getType)
                .distinct()
                .count();
        
        projectTypesLabel.setText(String.valueOf(uniqueTypes));
    }
    
    @FXML
    private void sortByName() {
        currentSortType = SortType.NAME;
        updateSortButtons();
        sortAndRefreshProjects();
    }
    
    @FXML
    private void sortBySize() {
        currentSortType = SortType.SIZE;
        updateSortButtons();
        sortAndRefreshProjects();
    }
    
    @FXML
    private void sortByDate() {
        currentSortType = SortType.DATE;
        updateSortButtons();
        sortAndRefreshProjects();
    }
    
    @FXML
    private void sortByType() {
        currentSortType = SortType.TYPE;
        updateSortButtons();
        sortAndRefreshProjects();
    }
    
    private void sortAndRefreshProjects() {
        if (activeProjects == null || activeProjects.isEmpty()) {
            return;
        }
        
        switch (currentSortType) {
            case NAME:
                activeProjects.sort(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER));
                break;
            case SIZE:
                activeProjects.sort(Comparator.comparingLong(Project::getSizeOfCleanableItems).reversed());
                break;
            case DATE:
                activeProjects.sort(Comparator.comparing(Project::getDateCreated).reversed());
                break;
            case TYPE:
                activeProjects.sort(Comparator.comparing(p -> p.getType().displayName));
                break;
        }
        
        populateGrid();
    }
    
    private void updateSortButtons() {
        // Reset all button styles
        sortNameBtn.getStyleClass().removeAll("sort-button-active");
        sortSizeBtn.getStyleClass().removeAll("sort-button-active");
        sortDateBtn.getStyleClass().removeAll("sort-button-active");
        sortTypeBtn.getStyleClass().removeAll("sort-button-active");
        
        // Activate current sort button
        switch (currentSortType) {
            case NAME:
                sortNameBtn.getStyleClass().add("sort-button-active");
                break;
            case SIZE:
                sortSizeBtn.getStyleClass().add("sort-button-active");
                break;
            case DATE:
                sortDateBtn.getStyleClass().add("sort-button-active");
                break;
            case TYPE:
                sortTypeBtn.getStyleClass().add("sort-button-active");
                break;
        }
    }
    
    private enum SortType {
        NAME, SIZE, DATE, TYPE
    }
}
