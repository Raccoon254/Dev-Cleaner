package com.kentom.devcleaner;

import com.kentom.devcleaner.model.CleanableItem;
import com.kentom.devcleaner.model.CleanupTask;
import com.kentom.devcleaner.model.GitInfo;
import com.kentom.devcleaner.model.LogManager;
import com.kentom.devcleaner.model.Project;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectDetailsController {

    @FXML private VBox detailsView;
    @FXML private Label projectNameLabel;
    @FXML private Label projectPathLabel;
    @FXML private Label projectDateLabel;
    @FXML private Label projectSizeLabel;
    @FXML private Label itemCountLabel;
    @FXML private ImageView projectIcon;
    @FXML private Button cleanButton;
    @FXML private HBox projectActionsBox;
    @FXML private VBox cleanableItemsContainer;
    @FXML private VBox gitInfoSection;
    @FXML private Label gitBranchLabel;
    @FXML private Label gitStatusLabel;
    @FXML private Label gitRemoteLabel;
    @FXML private Label gitSizeLabel;
    @FXML private Label gitCommitsLabel;

    private Project project;
    private ProjectsController projectController;
    private MainController mainController;

    public void setProject(Project project, ProjectsController projectController, MainController mainController) {
        this.project = project;
        this.projectController = projectController;
        this.mainController = mainController;
        displayProjectDetails();
    }

    private void displayProjectDetails() {
        projectNameLabel.setText(project.getName());
        projectPathLabel.setText(project.getPath().toString());
        projectIcon.setImage(project.getIcon());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
        projectDateLabel.setText("Cached on: " + project.getDateCreated().format(formatter));
        projectSizeLabel.setText(String.format("Cleanable Size: %s", formatSize(project.getSizeOfCleanableItems())));

        // Setup project actions
        setupProjectActions();
        
        // Setup Git info
        setupGitInfo();
        
        // Setup enhanced cleanable items view
        setupCleanableItemsView();

        cleanButton.setDisable(project.getCleanableItems().isEmpty());
    }

    private void setupProjectActions() {
        projectActionsBox.getChildren().clear();
        
        // Add project type-specific actions
        switch (project.getType()) {
            case NODE:
                addActionButton("npm install", "js-logo.png", "action-button-warning", this::runNpmInstall);
                addActionButton("npm audit", "security.png", "action-button-warning", this::runNpmAudit);
                break;
            case MAVEN:
                addActionButton("mvn clean", "maven.png", "project-action-button", this::runMavenClean);
                addActionButton("mvn install", "maven.png", "action-button-success", this::runMavenInstall);
                break;
            case GRADLE:
                addActionButton("gradle clean", "gradle.png", "project-action-button", this::runGradleClean);
                addActionButton("gradle build", "gradle.png", "action-button-success", this::runGradleBuild);
                break;
            case PYTHON:
                addActionButton("pip install", "python.png", "action-button-success", this::runPipInstall);
                break;
        }
        
        // Always add open in terminal
        addActionButton("Open Terminal", "terminal.png", "project-action-button", this::openTerminal);
        
        // Add Git actions if it's a Git repository
        GitInfo gitInfo = project.getGitInfo();
        if (gitInfo.isGitRepository()) {
            addActionButton("git status", "github.png", "project-action-button", this::runGitStatus);
            addActionButton("git pull", "github.png", "action-button-success", this::runGitPull);
        }
    }

    private void addActionButton(String text, String iconName, String styleClass, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("project-action-button", styleClass);
        
        try {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/com/kentom/devcleaner/icons/" + iconName)));
            icon.setFitHeight(16);
            icon.setFitWidth(16);
            icon.setPreserveRatio(true);
            button.setGraphic(icon);
        } catch (Exception e) {
            // Icon not found, continue without icon
        }
        
        button.setOnAction(e -> action.run());
        projectActionsBox.getChildren().add(button);
    }

    private void setupGitInfo() {
        GitInfo gitInfo = project.getGitInfo();
        
        if (!gitInfo.isGitRepository()) {
            gitInfoSection.setVisible(false);
            gitInfoSection.setManaged(false);
            return;
        }
        
        gitInfoSection.setVisible(true);
        gitInfoSection.setManaged(true);
        
        // Branch and status
        String branchText = "Branch: " + (gitInfo.getCurrentBranch() != null ? gitInfo.getCurrentBranch() : "unknown");
        gitBranchLabel.setText(branchText);
        
        String statusText = gitInfo.hasUncommittedChanges() ? "Uncommitted changes" : "Clean";
        String statusClass = gitInfo.hasUncommittedChanges() ? "git-status-dirty" : "git-status-clean";
        gitStatusLabel.setText(statusText);
        gitStatusLabel.getStyleClass().removeAll("git-status-dirty", "git-status-clean");
        gitStatusLabel.getStyleClass().add(statusClass);
        
        // Remote URL
        String remote = gitInfo.getRepositoryName();
        if (remote != null) {
            gitRemoteLabel.setText("Remote: " + remote);
        } else if (gitInfo.getRemoteUrl() != null) {
            gitRemoteLabel.setText("Remote: " + gitInfo.getRemoteUrl());
        } else {
            gitRemoteLabel.setText("No remote configured");
        }
        
        // Git folder size and commit count
        gitSizeLabel.setText(".git size: " + gitInfo.getFormattedGitSize());
        gitCommitsLabel.setText("Commits: " + gitInfo.getCommitCount());
    }

    private void setupCleanableItemsView() {
        cleanableItemsContainer.getChildren().clear();
        
        List<CleanableItem> items = createCleanableItems();
        Map<String, List<CleanableItem>> groupedItems = groupDuplicateItems(items);
        
        itemCountLabel.setText(String.format("(%d items, %s total)", 
            groupedItems.size(), formatSize(project.getSizeOfCleanableItems())));
        
        for (List<CleanableItem> itemGroup : groupedItems.values()) {
            VBox itemCard = createItemCard(itemGroup);
            cleanableItemsContainer.getChildren().add(itemCard);
        }
    }

    private List<CleanableItem> createCleanableItems() {
        return project.getCleanableItems().stream()
                .map(path -> {
                    try {
                        long size = Files.isDirectory(path) ? calculateDirectorySize(path) : Files.size(path);
                        return new CleanableItem(path, size);
                    } catch (IOException e) {
                        return new CleanableItem(path, 0);
                    }
                })
                .collect(Collectors.toList());
    }

    private Map<String, List<CleanableItem>> groupDuplicateItems(List<CleanableItem> items) {
        return items.stream()
                .collect(Collectors.groupingBy(CleanableItem::getName));
    }

    private VBox createItemCard(List<CleanableItem> itemGroup) {
        VBox card = new VBox(8);
        card.getStyleClass().add("cleanable-item");
        
        CleanableItem primary = itemGroup.get(0);
        
        // Main info row
        HBox mainInfo = new HBox(12);
        mainInfo.setAlignment(Pos.CENTER_LEFT);
        mainInfo.getStyleClass().add("item-main-info");
        
        // Icon
        ImageView icon = createItemIcon(primary);
        mainInfo.getChildren().add(icon);
        
        // Name and size
        VBox textInfo = new VBox(2);
        Label nameLabel = new Label(primary.getName());
        nameLabel.getStyleClass().add("item-name");
        
        long totalSize = itemGroup.stream().mapToLong(CleanableItem::getSize).sum();
        Label sizeLabel = new Label(formatSize(totalSize));
        sizeLabel.getStyleClass().add("item-size");
        
        textInfo.getChildren().addAll(nameLabel, sizeLabel);
        HBox.setHgrow(textInfo, Priority.ALWAYS);
        mainInfo.getChildren().add(textInfo);
        
        // Stack indicator if duplicates
        if (itemGroup.size() > 1) {
            Label stackLabel = new Label(String.valueOf(itemGroup.size()));
            stackLabel.getStyleClass().add("stack-indicator");
            mainInfo.getChildren().add(stackLabel);
        }
        
        card.getChildren().add(mainInfo);
        
        // Path info
        if (itemGroup.size() == 1) {
            String relativePath = project.getPath().relativize(primary.getPath()).toString();
            Label pathLabel = new Label(relativePath);
            pathLabel.getStyleClass().add("item-path");
            card.getChildren().add(pathLabel);
        } else {
            // Show up to 5 paths for duplicates in a responsive grid
            TilePane pathsPane = new TilePane();
            pathsPane.setPrefColumns(2);
            pathsPane.setHgap(10);
            pathsPane.setVgap(5);
            pathsPane.getStyleClass().add("paths-grid");

            List<Label> allPathLabels = new ArrayList<>();
            for (CleanableItem item : itemGroup) {
                String relativePath = project.getPath().relativize(item.getPath()).toString();
                Label pathLabel = new Label(relativePath);
                pathLabel.getStyleClass().add("item-path");
                allPathLabels.add(pathLabel);
            }

            // Add first 5
            int maxVisible = 5;
            for (int i = 0; i < Math.min(maxVisible, allPathLabels.size()); i++) {
                pathsPane.getChildren().add(allPathLabels.get(i));
            }

            // If more than 5, add a "Show more" button
            if (itemGroup.size() > maxVisible) {
                Button showMoreButton = new Button("Show more (" + (itemGroup.size() - maxVisible) + " more)");
                showMoreButton.getStyleClass().add("show-more-button");
                showMoreButton.setOnAction(e -> {
                    pathsPane.getChildren().clear();
                    for (Label label : allPathLabels) {
                        pathsPane.getChildren().add(label);
                    }
                    Button showLessButton = new Button("Show less");
                    showLessButton.getStyleClass().add("show-less-button");
                    showLessButton.setOnAction(ev -> {
                        pathsPane.getChildren().clear();
                        for (int i = 0; i < Math.min(maxVisible, allPathLabels.size()); i++) {
                            pathsPane.getChildren().add(allPathLabels.get(i));
                        }
                        pathsPane.getChildren().add(showMoreButton);
                    });
                    pathsPane.getChildren().add(showLessButton);
                });
                pathsPane.getChildren().add(showMoreButton);
            }

            card.getChildren().add(pathsPane);
        }
        
        return card;
    }

    private ImageView createItemIcon(CleanableItem item) {
        ImageView icon = new ImageView();
        icon.setFitHeight(24);
        icon.setFitWidth(24);
        icon.setPreserveRatio(true);
        icon.getStyleClass().add("item-icon");
        
        try {
            String iconPath = "/com/kentom/devcleaner/icons/" + item.getType().iconName;
            icon.setImage(new Image(getClass().getResourceAsStream(iconPath)));
        } catch (Exception e) {
            // Fallback to generic icon
            try {
                String fallback = item.isDirectory() ? 
                    "/com/kentom/devcleaner/icons/folder.png" : 
                    "/com/kentom/devcleaner/icons/file.png";
                icon.setImage(new Image(getClass().getResourceAsStream(fallback)));
            } catch (Exception ex) {
                // No icon available
            }
        }
        
        return icon;
    }

    private long calculateDirectorySize(Path directory) {
        try {
            return Files.walk(directory)
                    .filter(p -> p.toFile().isFile())
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }

    @FXML
    private void handleBackAction() {
        //mainController.hideProjectDetails(detailsView);
        mainController.showProjects();
    }

    @FXML
    private void handleCleanProject() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Cleanup");
        // load css file for styling
        confirmation.getDialogPane().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        confirmation.setHeaderText("Delete " + formatSize(project.getSizeOfCleanableItems()) + " for '" + project.getName() + "'?");
        confirmation.setContentText("This action permanently deletes files and cannot be undone.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                new CleanupTask().clean(project.getCleanableItems());
                showAlert(Alert.AlertType.INFORMATION, "Success", "Cleanup completed. The project has been removed from the list.");
                projectController.removeProject(project);
                handleBackAction();
            } catch (Exception e) {
                LogManager.log("Cleanup failed: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Cleanup Failed", "Could not delete all items. Check logs and file permissions.");
            }
        }
    }

    @FXML
    private void handleForgetProject() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Forget Project");
        confirmation.getDialogPane().getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        confirmation.setHeaderText("Remove '" + project.getName() + "' from DevCleaner?");
        confirmation.setContentText("This will remove the project from the application's cache. It will not delete any files from your disk. The project can be re-added by scanning again.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if(result.isPresent() && result.get() == ButtonType.OK) {
            projectController.removeProject(project);
            showAlert(Alert.AlertType.INFORMATION, "Project Forgotten", "The project has been removed from the cache.");
            handleBackAction();
        }
    }

    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
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

    @FXML
    private void handleCopyPath() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(project.getPath().toString());
        clipboard.setContent(content);
        
        showAlert(Alert.AlertType.INFORMATION, "Path Copied", "Project path has been copied to clipboard.");
    }

    // Action methods for project commands
    private void runNpmInstall() {
        runCommand("npm install", "Installing npm dependencies...");
    }
    
    private void runNpmAudit() {
        runCommand("npm audit", "Running npm security audit...");
    }
    
    private void runMavenClean() {
        runCommand("mvn clean", "Cleaning Maven project...");
    }
    
    private void runMavenInstall() {
        runCommand("mvn install", "Building Maven project...");
    }
    
    private void runGradleClean() {
        runCommand("gradle clean", "Cleaning Gradle project...");
    }
    
    private void runGradleBuild() {
        runCommand("gradle build", "Building Gradle project...");
    }
    
    private void runPipInstall() {
        runCommand("pip install -r requirements.txt", "Installing Python dependencies...");
    }
    
    private void runGitStatus() {
        runCommand("git status", "Checking Git status...");
    }
    
    private void runGitPull() {
        runCommand("git pull", "Pulling latest changes...");
    }
    
    private void openTerminal() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            
            if (os.contains("mac")) {
                pb = new ProcessBuilder("open", "-a", "Terminal", project.getPath().toString());
            } else if (os.contains("windows")) {
                pb = new ProcessBuilder("cmd", "/c", "start", "cmd", "/k", "cd", "/d", project.getPath().toString());
            } else {
                // Linux/Unix
                pb = new ProcessBuilder("gnome-terminal", "--working-directory=" + project.getPath().toString());
            }
            
            pb.start();
            LogManager.log("Opened terminal for project: " + project.getName());
        } catch (IOException e) {
            LogManager.log("Failed to open terminal: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Terminal Error", "Could not open terminal for this project.");
        }
    }
    
    private void runCommand(String command, String statusMessage) {
        showAlert(Alert.AlertType.INFORMATION, "Command Started", statusMessage + "\nCheck your terminal for output.");
        
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            
            if (os.contains("windows")) {
                pb = new ProcessBuilder("cmd", "/c", command);
            } else {
                pb = new ProcessBuilder("sh", "-c", command);
            }
            
            pb.directory(project.getPath().toFile());
            pb.inheritIO(); // This will show output in the parent process terminal
            
            Process process = pb.start();
            LogManager.log("Started command '" + command + "' for project: " + project.getName());
            
            // Optional: Wait for completion in background thread
            new Thread(() -> {
                try {
                    int exitCode = process.waitFor();
                    Platform.runLater(() -> {
                        if (exitCode == 0) {
                            LogManager.log("Command completed successfully: " + command);
                        } else {
                            LogManager.log("Command failed with exit code " + exitCode + ": " + command);
                        }
                    });
                } catch (InterruptedException e) {
                    LogManager.log("Command interrupted: " + command);
                }
            }).start();
            
        } catch (IOException e) {
            LogManager.log("Failed to run command '" + command + "': " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Command Error", "Failed to run command: " + command);
        }
    }

    public void handleScanProject(ActionEvent actionEvent) {
    }
}



