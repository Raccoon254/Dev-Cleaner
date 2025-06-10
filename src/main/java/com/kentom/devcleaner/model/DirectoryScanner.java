package com.kentom.devcleaner.model;

import com.kentom.devcleaner.util.FileUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectoryScanner {
    private static final Set<String> CLEANUP_RULES;

    static {
        // Load cleanup rules from a configuration file.
        try {
            String jsonContent = FileUtils.readResourceFile("/com/kentom/devcleaner/config/cleanup-rules.json");
            // Basic parsing of the JSON array of rules.
            CLEANUP_RULES = new HashSet<>(Arrays.asList(jsonContent.split("\"rules\":\\s*\\[")[1].split("]")[0].replaceAll("[\"\\s]", "").split(",")));
        } catch (Exception e) {
            LogManager.log("Critical: Failed to load cleanup rules. Cleanup functionality will be affected. Error: " + e.getMessage());
            throw new RuntimeException("Failed to load cleanup rules", e);
        }
    }

    /**
     * Scans a root directory for projects based on predefined ProjectType markers.
     *
     * @param rootDirectory The directory to start the scan from.
     * @return A list of detected Project objects.
     * @throws IOException if an I/O error occurs.
     */
    public List<Project> scan(Path rootDirectory) throws IOException {
        List<Project> projects = new ArrayList<>();
        Files.walkFileTree(rootDirectory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // **FIX:** The stream of directory entries is now collected into a Set first.
                // This allows us to check for markers for multiple project types without
                // violating the rule that a stream can only be operated on once.
                try {
                    Set<String> fileNames;
                    try (Stream<Path> entries = Files.list(dir)) {
                        fileNames = entries.map(path -> path.getFileName().toString())
                                .collect(Collectors.toSet());
                    }

                    // Now, iterate over project types and check against the collected Set.
                    for (ProjectType type : ProjectType.values()) {
                        // Check if any marker for the current project type exists in the directory.
                        boolean isProject = type.markers.stream().anyMatch(fileNames::contains);

                        if (isProject) {
                            LogManager.log("Detected " + type.displayName + " project at: " + dir);
                            Project project = new Project(dir, type);
                            findCleanableItems(project);
                            projects.add(project);
                            // Skip scanning subdirectories of a detected project.
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                } catch (AccessDeniedException e) {
                    LogManager.log("Access Denied, skipping directory: " + dir);
                    return FileVisitResult.SKIP_SUBTREE; // Skip directories we can't read
                } catch (IOException e) {
                    LogManager.log("Could not access or list directory, skipping: " + dir + " (" + e.getMessage() + ")");
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                if (exc instanceof AccessDeniedException) {
                    LogManager.log("Access Denied, skipping file: " + file);
                } else {
                    LogManager.log("Could not access file, skipping: " + file + " (" + exc.getMessage() + ")");
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return projects;
    }

    /**
     * Scans a given project's directory for items that can be cleaned up
     * based on the CLEANUP_RULES.
     *
     * @param project The project to scan for cleanable items.
     */
    private void findCleanableItems(Project project) {
        try {
            Files.walkFileTree(project.getPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // Avoid re-scanning the project's root directory itself if it matches a rule (e.g., 'vendor')
                    if (dir.equals(project.getPath())) {
                        return FileVisitResult.CONTINUE;
                    }

                    if (CLEANUP_RULES.contains(dir.getFileName().toString())) {
                        try {
                            long size = calculateDirectorySize(dir);
                            project.addCleanableItem(dir, size);
                        } catch (IOException e) {
                            LogManager.log("Could not calculate size for directory, skipping: " + dir + " (" + e.getMessage() + ")");
                        }
                        return FileVisitResult.SKIP_SUBTREE; // Skip contents of already-marked-for-deletion folders
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

    /**
     * Calculates the total size of a directory and its contents.
     */
    private long calculateDirectorySize(Path directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk
                    .filter(p -> p.toFile().isFile())
                    .mapToLong(p -> p.toFile().length())
                    .sum();
        }
    }
}
