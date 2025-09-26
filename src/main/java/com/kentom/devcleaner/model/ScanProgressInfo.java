package com.kentom.devcleaner.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ScanProgressInfo {

    // Current scanning state
    private volatile Path currentDirectory;
    private volatile String currentFileName;
    private volatile ScanPhase currentPhase = ScanPhase.INITIALIZING;

    // Progress counters
    private final AtomicInteger directoriesScanned = new AtomicInteger(0);
    private final AtomicInteger filesScanned = new AtomicInteger(0);
    private final AtomicInteger projectsFound = new AtomicInteger(0);
    private final AtomicLong totalBytesProcessed = new AtomicLong(0);
    private final AtomicLong cleanableSpaceFound = new AtomicLong(0);

    // Timing information
    private LocalDateTime scanStartTime;
    private volatile LocalDateTime lastUpdateTime;
    private final AtomicLong estimatedTotalDirectories = new AtomicLong(0);

    // Recently discovered projects (thread-safe list)
    private final List<Project> recentProjects = new CopyOnWriteArrayList<>();

    // Project type breakdown
    private final Map<ProjectType, Integer> projectTypeBreakdown = new java.util.concurrent.ConcurrentHashMap<>();

    // Performance metrics
    private final AtomicInteger filesPerSecond = new AtomicInteger(0);
    private final AtomicInteger directoriesPerSecond = new AtomicInteger(0);

    // Error tracking
    private final AtomicInteger errorsEncountered = new AtomicInteger(0);
    private final List<String> recentErrors = new CopyOnWriteArrayList<>();

    public enum ScanPhase {
        INITIALIZING("Preparing to scan..."),
        ESTIMATING("Estimating scan size..."),
        SCANNING_DIRECTORIES("Scanning directories..."),
        ANALYZING_PROJECTS("Analyzing projects..."),
        CALCULATING_SIZES("Calculating sizes..."),
        FINALIZING("Finalizing results..."),
        COMPLETED("Scan completed"),
        CANCELLED("Scan cancelled"),
        ERROR("Scan failed");

        public final String description;

        ScanPhase(String description) {
            this.description = description;
        }
    }

    public ScanProgressInfo() {
        this.scanStartTime = LocalDateTime.now();
        this.lastUpdateTime = LocalDateTime.now();
    }

    // Current state getters
    public Path getCurrentDirectory() { return currentDirectory; }
    public String getCurrentFileName() { return currentFileName; }
    public ScanPhase getCurrentPhase() { return currentPhase; }

    // Progress counters
    public int getDirectoriesScanned() { return directoriesScanned.get(); }
    public int getFilesScanned() { return filesScanned.get(); }
    public int getProjectsFound() { return projectsFound.get(); }
    public long getTotalBytesProcessed() { return totalBytesProcessed.get(); }
    public long getCleanableSpaceFound() { return cleanableSpaceFound.get(); }

    // Timing
    public LocalDateTime getScanStartTime() { return scanStartTime; }
    public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
    public long getElapsedTimeMs() {
        return Duration.between(scanStartTime, LocalDateTime.now()).toMillis();
    }

    // Progress calculation
    public double getProgressPercentage() {
        long estimated = estimatedTotalDirectories.get();
        if (estimated == 0) return 0.0;
        return Math.min(100.0, (directoriesScanned.get() * 100.0) / estimated);
    }

    public long getEstimatedTimeRemainingMs() {
        double progress = getProgressPercentage();
        if (progress <= 0) return 0;

        long elapsed = getElapsedTimeMs();
        return (long) ((elapsed / progress) * (100 - progress));
    }

    // Recent discoveries
    public List<Project> getRecentProjects() {
        return recentProjects.size() > 5 ?
            recentProjects.subList(Math.max(0, recentProjects.size() - 5), recentProjects.size()) :
            recentProjects;
    }

    public Map<ProjectType, Integer> getProjectTypeBreakdown() {
        return new java.util.HashMap<>(projectTypeBreakdown);
    }

    // Performance metrics
    public int getFilesPerSecond() { return filesPerSecond.get(); }
    public int getDirectoriesPerSecond() { return directoriesPerSecond.get(); }

    // Error tracking
    public int getErrorsEncountered() { return errorsEncountered.get(); }
    public List<String> getRecentErrors() {
        return recentErrors.size() > 3 ?
            recentErrors.subList(Math.max(0, recentErrors.size() - 3), recentErrors.size()) :
            recentErrors;
    }

    // Update methods (called by scanner)
    public void updateCurrentDirectory(Path directory) {
        this.currentDirectory = directory;
        this.lastUpdateTime = LocalDateTime.now();
        updatePerformanceMetrics();
    }

    public void updateCurrentFile(String fileName) {
        this.currentFileName = fileName;
        this.lastUpdateTime = LocalDateTime.now();
    }

    public void updatePhase(ScanPhase phase) {
        this.currentPhase = phase;
        this.lastUpdateTime = LocalDateTime.now();
    }

    public void incrementDirectoriesScanned() {
        directoriesScanned.incrementAndGet();
        updatePerformanceMetrics();
    }

    public void incrementFilesScanned() {
        filesScanned.incrementAndGet();
        updatePerformanceMetrics();
    }

    public void addBytesProcessed(long bytes) {
        totalBytesProcessed.addAndGet(bytes);
    }

    public void addProjectFound(Project project) {
        projectsFound.incrementAndGet();
        recentProjects.add(project);

        // Update type breakdown
        ProjectType type = project.getType();
        projectTypeBreakdown.merge(type, 1, Integer::sum);

        // Add to cleanable space
        cleanableSpaceFound.addAndGet(project.getSizeOfCleanableItems());

        this.lastUpdateTime = LocalDateTime.now();
    }

    public void setEstimatedTotalDirectories(long estimate) {
        estimatedTotalDirectories.set(estimate);
    }

    public void addError(String error) {
        errorsEncountered.incrementAndGet();
        recentErrors.add(error);

        // Keep only recent errors
        if (recentErrors.size() > 10) {
            recentErrors.remove(0);
        }
    }

    private void updatePerformanceMetrics() {
        long elapsedSeconds = getElapsedTimeMs() / 1000;
        if (elapsedSeconds > 0) {
            filesPerSecond.set((int) (filesScanned.get() / elapsedSeconds));
            directoriesPerSecond.set((int) (directoriesScanned.get() / elapsedSeconds));
        }
    }

    // Utility methods for display
    public String getFormattedElapsedTime() {
        long seconds = getElapsedTimeMs() / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public String getFormattedEstimatedTimeRemaining() {
        long remainingMs = getEstimatedTimeRemainingMs();
        if (remainingMs <= 0) return "Unknown";

        long seconds = remainingMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public String getFormattedBytesProcessed() {
        return formatBytes(totalBytesProcessed.get());
    }

    public String getFormattedCleanableSpace() {
        return formatBytes(cleanableSpaceFound.get());
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
        return String.format("%.1f %sB", (double) bytes / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    // Reset for new scan
    public void reset() {
        currentDirectory = null;
        currentFileName = null;
        currentPhase = ScanPhase.INITIALIZING;

        directoriesScanned.set(0);
        filesScanned.set(0);
        projectsFound.set(0);
        totalBytesProcessed.set(0);
        cleanableSpaceFound.set(0);
        estimatedTotalDirectories.set(0);

        recentProjects.clear();
        projectTypeBreakdown.clear();

        filesPerSecond.set(0);
        directoriesPerSecond.set(0);

        errorsEncountered.set(0);
        recentErrors.clear();

        scanStartTime = LocalDateTime.now();
        lastUpdateTime = LocalDateTime.now();
    }
}