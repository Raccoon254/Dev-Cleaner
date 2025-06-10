package com.kentom.devcleaner.model;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ProjectCache {

    private static final Path CACHE_FILE = Paths.get(System.getProperty("user.home"), ".devcleaner", "cache.ser");

    public static void saveProjects(List<Project> projects) {
        try {
            Files.createDirectories(CACHE_FILE.getParent());
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CACHE_FILE.toFile()))) {
                oos.writeObject(projects);
                LogManager.log("Project cache saved successfully to " + CACHE_FILE);
            }
        } catch (IOException e) {
            LogManager.log("Error saving project cache: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Project> loadProjects() {
        if (!Files.exists(CACHE_FILE)) {
            LogManager.log("Cache file not found. Returning empty project list.");
            return new ArrayList<>();
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(CACHE_FILE.toFile()))) {
            List<Project> projects = (List<Project>) ois.readObject();
            LogManager.log("Project cache loaded successfully. Found " + projects.size() + " projects.");
            return projects;
        } catch (IOException | ClassNotFoundException e) {
            LogManager.log("Error loading project cache: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
