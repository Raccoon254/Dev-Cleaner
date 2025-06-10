package com.kentom.devcleaner.model;

import com.kentom.devcleaner.util.FileUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

public class DirectoryScanner {
    private static final Set<String> CLEANUP_RULES;

    static {
        // Load cleanup rules from JSON
        try {
            String jsonContent = FileUtils.readResourceFile("/com/kentom/devcleaner/config/cleanup-rules.json");
            CLEANUP_RULES = new HashSet<>(Arrays.asList(jsonContent.split("\"rules\":\\s*\\[")[1].split("]")[0].replaceAll("[\"\\s]", "").split(",")));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load cleanup rules", e);
        }
    }

    public List<Project> scan(Path rootDirectory) throws IOException {
        List<Project> projects = new ArrayList<>();
        Files.walkFileTree(rootDirectory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // *** FIX: Wrap directory access in a try-catch block to handle permissions errors. ***
                try (Stream<Path> entries = Files.list(dir)) {
                    // Check if the directory is a project by looking for marker files.
                    for (ProjectType type : ProjectType.values()) {
                        if (entries.anyMatch(path -> type.markers.contains(path.getFileName().toString()))) {
                            Project project = new Project(dir, type);
                            findCleanableItems(project);
                            projects.add(project);
                            // Don't scan inside a detected project for more projects.
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                } catch (IOException e) {
                    // *** FIX: If an error occurs (e.g., AccessDeniedException), log it and skip the directory. ***
                    LogManager.log("Could not access directory, skipping: " + dir + " (" + e.getMessage() + ")");
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // This handles cases where a file cannot be accessed.
                LogManager.log("Could not access file, skipping: " + file + " (" + exc.getMessage() + ")");
                return FileVisitResult.CONTINUE;
            }
        });
        return projects;
    }

    private void findCleanableItems(Project project) {
        try {
            Files.walkFileTree(project.getPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (CLEANUP_RULES.contains(dir.getFileName().toString())) {
                        try {
                            long size = Files.walk(dir).mapToLong(p -> p.toFile().length()).sum();
                            project.addCleanableItem(dir, size);
                        } catch (IOException e) {
                            LogManager.log("Could not calculate size for directory, skipping: " + dir);
                        }
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (CLEANUP_RULES.contains(file.getFileName().toString())) {
                        project.addCleanableItem(file, attrs.size());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    LogManager.log("Could not access file during cleanup scan, skipping: " + file);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LogManager.log("Error scanning for cleanable items in project " + project.getName() + ": " + e.getMessage());
        }
    }
}