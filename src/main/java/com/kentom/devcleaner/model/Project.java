package com.kentom.devcleaner.model;

import javafx.scene.image.Image;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Project {
    private final String name;
    private final Path path;
    private final ProjectType type;
    private final Image icon;
    private final List<Path> cleanableItems = new ArrayList<>();
    private long sizeOfCleanableItems = 0;

    public Project(Path path, ProjectType type) {
        this.name = path.getFileName().toString();
        this.path = path;
        this.type = type;
        this.icon = new Image(getClass().getResourceAsStream("/com/kentom/devcleaner/icons/" + type.iconName));
    }

    // Getters
    public String getName() { return name; }
    public Path getPath() { return path; }
    public ProjectType getType() { return type; }
    public Image getIcon() { return icon; }
    public List<Path> getCleanableItems() { return cleanableItems; }
    public long getSizeOfCleanableItems() { return sizeOfCleanableItems; }

    // Public methods
    public void addCleanableItem(Path item, long size) {
        this.cleanableItems.add(item);
        this.sizeOfCleanableItems += size;
    }
}