package com.kentom.devcleaner.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CleanableItem {
    private final String name;
    private final Path path;
    private final boolean isDirectory;
    private final long size;
    private final ItemType type;
    private final List<Path> duplicatePaths = new ArrayList<>();
    private int stackCount = 1;

    public enum ItemType {
        NODE_MODULES("node_modules", "node.png"),
        TARGET("target", "java.png"),
        BUILD("build", "gradle.png"),
        DIST("dist", "build.png"),
        OUT("out", "build.png"),
        BIN("bin", "build.png"),
        OBJ("obj", "csharp.png"),
        CACHE("cache", "cache.png"),
        TEMP("temp", "temp.png"),
        LOGS("logs", "log.png"),
        VENDOR("vendor", "php.png"),
        VENV("venv", "python.png"),
        PYCACHE("__pycache__", "python.png"),
        DOT_GRADLE(".gradle", "gradle.png"),
        DOT_NEXT(".next", "react.png"),
        GIT(".git", "git.png"),
        FOLDER("folder", "folder.png"),
        FILE("file", "file.png");

        public final String displayName;
        public final String iconName;

        ItemType(String displayName, String iconName) {
            this.displayName = displayName;
            this.iconName = iconName;
        }

        public static ItemType fromFileName(String fileName) {
            String lowerName = fileName.toLowerCase();
            for (ItemType type : values()) {
                if (type.displayName.toLowerCase().equals(lowerName)) {
                    return type;
                }
            }
            return Files.isDirectory(Path.of(fileName)) ? FOLDER : FILE;
        }
    }

    public CleanableItem(Path path, long size) {
        this.path = path;
        this.name = path.getFileName().toString();
        this.size = size;
        this.isDirectory = Files.isDirectory(path);
        this.type = ItemType.fromFileName(name);
    }

    // Getters
    public String getName() { return name; }
    public Path getPath() { return path; }
    public boolean isDirectory() { return isDirectory; }
    public long getSize() { return size; }
    public ItemType getType() { return type; }
    public List<Path> getDuplicatePaths() { return duplicatePaths; }
    public int getStackCount() { return stackCount; }

    // Stack management
    public void addDuplicate(Path duplicatePath) {
        duplicatePaths.add(duplicatePath);
        stackCount++;
    }

    public boolean hasDuplicates() {
        return stackCount > 1;
    }

    public String getFormattedSize() {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double) size / (1L << (z * 10)), " KMGTPE".charAt(z));
    }
}