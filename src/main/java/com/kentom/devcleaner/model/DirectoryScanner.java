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
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // Check if the directory is a project
                for (ProjectType type : ProjectType.values()) {
                    try (Stream<Path> entries = Files.list(dir)) {
                        if (entries.anyMatch(path -> type.markers.contains(path.getFileName().toString()))) {
                            Project project = new Project(dir, type);
                            findCleanableItems(project);
                            projects.add(project);
                            // Don't scan inside a project for more projects
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // Log or handle permissions errors
                return FileVisitResult.CONTINUE;
            }
        });
        return projects;
    }

    private void findCleanableItems(Project project) throws IOException {
        Files.walkFileTree(project.getPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (CLEANUP_RULES.contains(dir.getFileName().toString())) {
                    long size = Files.walk(dir).mapToLong(p -> p.toFile().length()).sum();
                    project.addCleanableItem(dir, size);
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
        });
    }
}