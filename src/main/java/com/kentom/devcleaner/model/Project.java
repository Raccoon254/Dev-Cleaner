package com.kentom.devcleaner.model;

import javafx.scene.image.Image;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Project implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String pathString; // Store path as String for serialization
    private final ProjectType type;
    private final LocalDateTime dateCreated;
    private final List<String> cleanableItemsPaths = new ArrayList<>();
    private long sizeOfCleanableItems = 0;

    private transient Image icon; // Exclude from serialization

    public Project(Path path, ProjectType type) {
        this.name = path.getFileName().toString();
        this.pathString = path.toAbsolutePath().toString();
        this.type = type;
        this.dateCreated = LocalDateTime.now();
        loadIcon();
    }

    // Getters
    public String getName() { return name; }
    public Path getPath() { return Paths.get(pathString); }
    public ProjectType getType() { return type; }
    public Image getIcon() {
        if (icon == null) {
            loadIcon();
        }
        return icon;
    }
    public List<Path> getCleanableItems() {
        List<Path> paths = new ArrayList<>();
        for (String p : cleanableItemsPaths) {
            paths.add(Paths.get(p));
        }
        return paths;
    }
    public long getSizeOfCleanableItems() { return sizeOfCleanableItems; }
    public LocalDateTime getDateCreated() { return dateCreated; }

    // Public methods
    public void addCleanableItem(Path item, long size) {
        this.cleanableItemsPaths.add(item.toAbsolutePath().toString());
        this.sizeOfCleanableItems += size;
    }

    private void loadIcon() {
        try {
            this.icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/kentom/devcleaner/icons/" + type.iconName)));
        } catch (Exception e) {
            this.icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/kentom/devcleaner/icons/default.png")));
        }
    }

    @Serial
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }

    @Serial
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        loadIcon();
    }
}
