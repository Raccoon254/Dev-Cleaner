package com.kentom.devcleaner.model;

import com.kentom.devcleaner.util.FileUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectoryScanner {
    private static final Set<String> CLEANUP_RULES;

    // Progress tracking
    private ScanProgressInfo progressInfo;
    private Consumer<ScanProgressInfo> progressCallback;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    // Performance settings
    private int updateFrequency = 10; // Update progress every N directories
    
    // Directories that should be excluded from project detection (build/generated directories)
    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
        ".next", "obj", "bin", "target", "build", "dist", "out",
        "Debug", "Release", "x64", "x86", "classes", "generated",
        ".gradle", ".idea", ".vscode", ".vs", "node_modules",
        "__pycache__", ".git", ".svn", ".hg"
    );

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
        return scan(rootDirectory, null);
    }

    /**
     * Scans a root directory for projects with progress tracking.
     *
     * @param rootDirectory The directory to start the scan from.
     * @param progressCallback Callback for progress updates (can be null).
     * @return A list of detected Project objects.
     * @throws IOException if an I/O error occurs.
     */
    public List<Project> scan(Path rootDirectory, Consumer<ScanProgressInfo> progressCallback) throws IOException {
        // Initialize progress tracking
        this.progressInfo = new ScanProgressInfo();
        this.progressCallback = progressCallback;
        this.cancelled.set(false);

        updateProgress(ScanProgressInfo.ScanPhase.INITIALIZING);

        // Phase 1: Estimate total directories for progress calculation
        updateProgress(ScanProgressInfo.ScanPhase.ESTIMATING);
        try {
            long estimatedDirs = estimateDirectoryCount(rootDirectory);
            progressInfo.setEstimatedTotalDirectories(estimatedDirs);
            LogManager.log("Estimated " + estimatedDirs + " directories to scan");
        } catch (Exception e) {
            LogManager.log("Could not estimate directory count: " + e.getMessage());
            progressInfo.setEstimatedTotalDirectories(1000); // Fallback estimate
        }

        // Phase 2: Scan for projects
        updateProgress(ScanProgressInfo.ScanPhase.SCANNING_DIRECTORIES);
        List<Project> projects = scanForProjects(rootDirectory);

        // Phase 3: Analyze and calculate sizes
        updateProgress(ScanProgressInfo.ScanPhase.ANALYZING_PROJECTS);
        for (Project project : projects) {
            if (cancelled.get()) break;

            progressInfo.updateCurrentDirectory(project.getPath());
            findCleanableItems(project);
            calculateTotalProjectSize(project);
            progressInfo.addProjectFound(project);
            updateProgress();
        }

        // Phase 4: Finalize
        updateProgress(ScanProgressInfo.ScanPhase.FINALIZING);

        if (cancelled.get()) {
            updateProgress(ScanProgressInfo.ScanPhase.CANCELLED);
            LogManager.log("Scan cancelled by user");
        } else {
            updateProgress(ScanProgressInfo.ScanPhase.COMPLETED);
            LogManager.log("Scan completed successfully. Found " + projects.size() + " projects.");
        }

        return projects;
    }

    /**
     * Original scan method implementation, now extracted for progress tracking.
     */
    private List<Project> scanForProjects(Path rootDirectory) throws IOException {
        List<Project> projects = new ArrayList<>();
        Files.walkFileTree(rootDirectory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                // Check for cancellation
                if (cancelled.get()) {
                    return FileVisitResult.TERMINATE;
                }

                // Update progress
                progressInfo.updateCurrentDirectory(dir);
                progressInfo.incrementDirectoriesScanned();

                // Update callback periodically for performance
                if (progressInfo.getDirectoriesScanned() % updateFrequency == 0) {
                    updateProgress();
                }

                // Skip excluded directories (build/generated directories)
                if (shouldSkipDirectory(dir)) {
                    LogManager.log("Skipping excluded directory: " + dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }

                try {
                    Set<String> fileNames;
                    try (Stream<Path> entries = Files.list(dir)) {
                        fileNames = entries.map(path -> path.getFileName().toString())
                                .collect(Collectors.toSet());
                    }

                    // Check for project types, prioritizing certain types
                    ProjectType detectedType = null;
                    
                    // First check for more specific project types
                    for (ProjectType type : ProjectType.values()) {
                        boolean isProject = type.markers.stream().anyMatch(fileNames::contains);
                        if (isProject) {
                            detectedType = type;
                            break;
                        }
                    }

                    if (detectedType != null) {
                        LogManager.log("Detected " + detectedType.displayName + " project at: " + dir);
                        Project project = new Project(dir, detectedType);
                        findCleanableItems(project);
                        calculateTotalProjectSize(project);
                        projects.add(project);
                        // Skip scanning subdirectories of a detected project.
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                } catch (AccessDeniedException e) {
                    String error = "Access Denied, skipping directory: " + dir;
                    LogManager.log(error);
                    progressInfo.addError(error);
                    return FileVisitResult.SKIP_SUBTREE; // Skip directories we can't read
                } catch (IOException e) {
                    String error = "Could not access or list directory, skipping: " + dir + " (" + e.getMessage() + ")";
                    LogManager.log(error);
                    progressInfo.addError(error);
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                String error;
                if (exc instanceof AccessDeniedException) {
                    error = "Access Denied, skipping file: " + file;
                } else {
                    error = "Could not access file, skipping: " + file + " (" + exc.getMessage() + ")";
                }
                LogManager.log(error);
                progressInfo.addError(error);
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
        progressInfo.updatePhase(ScanProgressInfo.ScanPhase.CALCULATING_SIZES);
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
     * Determines if a directory should be skipped during project detection.
     */
    private boolean shouldSkipDirectory(Path dir) {
        String dirName = dir.getFileName().toString();
        
        // Skip if the directory name itself is excluded
        if (EXCLUDED_DIRECTORIES.contains(dirName)) {
            return true;
        }
        
        // Skip if we're inside a build/generated directory (check if path contains these directories)
        String pathString = dir.toString().toLowerCase();
        for (String excluded : EXCLUDED_DIRECTORIES) {
            if (pathString.contains("/" + excluded.toLowerCase() + "/") || 
                pathString.contains("\\" + excluded.toLowerCase() + "\\")) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Calculates the total size of a project directory (excluding cleanable items to avoid double counting).
     */
    private void calculateTotalProjectSize(Project project) {
        try {
            long totalSize = calculateDirectorySize(project.getPath());
            project.setTotalSize(totalSize);
            LogManager.log("Calculated total size for " + project.getName() + ": " + totalSize + " bytes");
        } catch (IOException e) {
            LogManager.log("Could not calculate total size for project " + project.getName() + ": " + e.getMessage());
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

    /**
     * Estimates the total number of directories to scan for progress calculation.
     */
    private long estimateDirectoryCount(Path rootDirectory) throws IOException {
        final AtomicLong count = new AtomicLong(0);

        Files.walkFileTree(rootDirectory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (cancelled.get()) {
                    return FileVisitResult.TERMINATE;
                }

                count.incrementAndGet();

                // Skip excluded directories for estimation too
                if (shouldSkipDirectory(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // Continue estimation even if some files can't be accessed
                return FileVisitResult.CONTINUE;
            }
        });

        return count.get();
    }

    /**
     * Updates progress and calls callback if available.
     */
    private void updateProgress() {
        updateProgress(progressInfo.getCurrentPhase());
    }

    /**
     * Updates progress with specific phase and calls callback if available.
     */
    private void updateProgress(ScanProgressInfo.ScanPhase phase) {
        if (progressInfo != null) {
            progressInfo.updatePhase(phase);
        }

        if (progressCallback != null && progressInfo != null) {
            try {
                progressCallback.accept(progressInfo);
            } catch (Exception e) {
                LogManager.log("Error in progress callback: " + e.getMessage());
            }
        }
    }

    /**
     * Cancels the current scan operation.
     */
    public void cancel() {
        cancelled.set(true);
        LogManager.log("Scan cancellation requested");
    }

    /**
     * Returns the current progress info.
     */
    public ScanProgressInfo getProgressInfo() {
        return progressInfo;
    }

    /**
     * Sets the update frequency for progress callbacks.
     */
    public void setUpdateFrequency(int frequency) {
        this.updateFrequency = Math.max(1, frequency);
    }
}
