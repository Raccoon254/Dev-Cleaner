package com.kentom.devcleaner.service;

import com.kentom.devcleaner.model.LogManager;
import com.kentom.devcleaner.model.Project;
import com.kentom.devcleaner.model.ProjectCache;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ApplicationDataService {

    private static ApplicationDataService instance;
    private final List<Project> projects = new CopyOnWriteArrayList<>();
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private LocalDateTime lastRefresh;
    private boolean initialLoadComplete = false;

    // Listeners for data changes
    private final List<Runnable> dataChangeListeners = new ArrayList<>();
    private final List<Consumer<String>> loadingStatusListeners = new ArrayList<>();

    private ApplicationDataService() {
        // Private constructor for singleton
    }

    public static ApplicationDataService getInstance() {
        if (instance == null) {
            instance = new ApplicationDataService();
        }
        return instance;
    }

    // Get cached projects immediately (no I/O)
    public List<Project> getProjects() {
        return new ArrayList<>(projects);
    }

    // Check if data is currently loading
    public boolean isLoading() {
        return loading.get();
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    // Get last refresh time
    public LocalDateTime getLastRefresh() {
        return lastRefresh;
    }

    // Check if initial load is complete
    public boolean isInitialLoadComplete() {
        return initialLoadComplete;
    }

    // Load data on app startup (called once)
    public void initializeData() {
        if (initialLoadComplete) {
            LogManager.log("Data already initialized, skipping...");
            return;
        }

        LogManager.log("Initializing application data...");
        refreshData();
    }

    // Fast initialization - load projects without Git caching
    public void initializeDataAsync() {
        if (initialLoadComplete) {
            LogManager.log("Data already initialized, skipping...");
            return;
        }

        LogManager.log("Initializing application data (fast mode)...");
        loading.set(true);
        notifyLoadingStatusListeners("Loading projects...");

        Task<List<Project>> loadTask = new Task<List<Project>>() {
            @Override
            protected List<Project> call() throws Exception {
                // Load from cache (fast - no Git operations)
                List<Project> loadedProjects = ProjectCache.loadProjects();
                return loadedProjects;
            }

            @Override
            protected void succeeded() {
                List<Project> loadedProjects = getValue();

                // Update the cached data
                projects.clear();
                projects.addAll(loadedProjects);
                lastRefresh = LocalDateTime.now();
                initialLoadComplete = true;
                loading.set(false);

                LogManager.log("Fast data load completed. Loaded " + projects.size() + " projects.");

                // Notify listeners - UI can now display
                notifyDataChangeListeners();
                notifyLoadingStatusListeners("Projects loaded");

                // NOW start background Git info caching (non-blocking)
                if (!loadedProjects.isEmpty()) {
                    cacheGitInfoInBackground(loadedProjects);
                }
            }

            @Override
            protected void failed() {
                loading.set(false);
                initialLoadComplete = true;
                LogManager.log("Data load failed: " + getException().getMessage());

                projects.clear();
                lastRefresh = LocalDateTime.now();

                notifyDataChangeListeners();
                notifyLoadingStatusListeners("Failed to load data");
            }
        };

        Thread loadThread = new Thread(loadTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

    // Background Git info caching (doesn't block UI)
    private void cacheGitInfoInBackground(List<Project> projectList) {
        Task<Void> gitCacheTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                LogManager.log("Starting background Git info caching for " + projectList.size() + " projects...");

                for (int i = 0; i < projectList.size(); i++) {
                    Project project = projectList.get(i);
                    try {
                        project.getGitInfo(); // This caches Git info
                    } catch (Exception e) {
                        // Ignore individual project errors
                        LogManager.log("Failed to cache Git info for " + project.getName() + ": " + e.getMessage());
                    }

                    // Small delay to avoid CPU spike
                    if (i % 10 == 0 && i > 0) {
                        Thread.sleep(50);
                    }
                }

                return null;
            }

            @Override
            protected void succeeded() {
                LogManager.log("Background Git info caching completed");
                // Refresh UI to show Git info now that it's cached
                notifyDataChangeListeners();
            }
        };

        Thread gitThread = new Thread(gitCacheTask);
        gitThread.setDaemon(true);
        gitThread.setPriority(Thread.MIN_PRIORITY); // Low priority background task
        gitThread.start();
    }

    // Refresh data from cache/disk (manual or timer-based)
    public void refreshData() {
        if (loading.get()) {
            LogManager.log("Refresh already in progress, skipping...");
            return;
        }

        LogManager.log("Starting data refresh...");
        loading.set(true);
        notifyLoadingStatusListeners("Loading projects from cache...");

        Task<List<Project>> loadTask = new Task<List<Project>>() {
            @Override
            protected List<Project> call() throws Exception {
                // Load from cache
                List<Project> loadedProjects = ProjectCache.loadProjects();

                // Update status periodically
                if (!loadedProjects.isEmpty()) {
                    int total = loadedProjects.size();

                    // Process projects in batches for Git info caching
                    for (int i = 0; i < loadedProjects.size(); i += 10) {
                        final int batchStart = i;
                        final int batchEnd = Math.min(i + 10, loadedProjects.size());

                        Platform.runLater(() ->
                            notifyLoadingStatusListeners(String.format("Processing projects %d-%d of %d...",
                                batchStart + 1, batchEnd, total))
                        );

                        // Pre-load Git info for batch (caches it)
                        for (int j = batchStart; j < batchEnd; j++) {
                            Project project = loadedProjects.get(j);
                            try {
                                project.getGitInfo(); // This caches Git info
                            } catch (Exception e) {
                                // Ignore individual project errors
                            }
                        }

                        // Small delay between batches
                        Thread.sleep(50);
                    }
                }

                return loadedProjects;
            }

            @Override
            protected void succeeded() {
                List<Project> loadedProjects = getValue();

                // Update the cached data
                projects.clear();
                projects.addAll(loadedProjects);
                lastRefresh = LocalDateTime.now();
                initialLoadComplete = true;
                loading.set(false);

                LogManager.log("Data refresh completed. Loaded " + projects.size() + " projects.");

                // Notify all listeners that data has changed
                notifyDataChangeListeners();
                notifyLoadingStatusListeners("Data loaded successfully");
            }

            @Override
            protected void failed() {
                loading.set(false);
                initialLoadComplete = true; // Mark as complete even if failed
                LogManager.log("Data refresh failed: " + getException().getMessage());

                // Clear projects on failure
                projects.clear();
                lastRefresh = LocalDateTime.now();

                // Still notify listeners (they can handle empty data)
                notifyDataChangeListeners();
                notifyLoadingStatusListeners("Failed to load data");
            }
        };

        Thread loadThread = new Thread(loadTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

    // Add/remove projects (called after scanning)
    public void updateProjects(List<Project> newProjects) {
        LogManager.log("Updating projects with " + newProjects.size() + " new projects");

        // Update cache and save to disk
        projects.clear();
        projects.addAll(newProjects);
        lastRefresh = LocalDateTime.now();

        // Save to disk
        ProjectCache.saveProjects(projects);

        // Notify listeners
        notifyDataChangeListeners();
    }

    // Remove a project
    public void removeProject(Project project) {
        LogManager.log("Removing project: " + project.getName());

        projects.remove(project);
        lastRefresh = LocalDateTime.now();

        // Save updated list to disk
        ProjectCache.saveProjects(projects);

        // Notify listeners
        notifyDataChangeListeners();
    }

    // Add listener for data changes
    public void addDataChangeListener(Runnable listener) {
        dataChangeListeners.add(listener);
    }

    // Remove data change listener
    public void removeDataChangeListener(Runnable listener) {
        dataChangeListeners.remove(listener);
    }

    // Add listener for loading status updates
    public void addLoadingStatusListener(Consumer<String> listener) {
        loadingStatusListeners.add(listener);
    }

    // Remove loading status listener
    public void removeLoadingStatusListener(Consumer<String> listener) {
        loadingStatusListeners.remove(listener);
    }

    // Notify all data change listeners
    private void notifyDataChangeListeners() {
        Platform.runLater(() -> {
            for (Runnable listener : dataChangeListeners) {
                try {
                    listener.run();
                } catch (Exception e) {
                    LogManager.log("Error in data change listener: " + e.getMessage());
                }
            }
        });
    }

    // Notify loading status listeners
    private void notifyLoadingStatusListeners(String status) {
        Platform.runLater(() -> {
            for (Consumer<String> listener : loadingStatusListeners) {
                try {
                    listener.accept(status);
                } catch (Exception e) {
                    LogManager.log("Error in loading status listener: " + e.getMessage());
                }
            }
        });
    }

    // Get project count (convenience method)
    public int getProjectCount() {
        return projects.size();
    }

    // Get total cleanable size (convenience method)
    public long getTotalCleanableSize() {
        return projects.stream()
            .mapToLong(Project::getSizeOfCleanableItems)
            .sum();
    }

    // Get total size (convenience method)
    public long getTotalSize() {
        return projects.stream()
            .mapToLong(Project::getTotalSize)
            .sum();
    }

    // Check if data needs refresh (could be used for auto-refresh)
    public boolean needsRefresh() {
        if (lastRefresh == null) return true;

        // Consider data stale after 30 minutes
        return LocalDateTime.now().isAfter(lastRefresh.plusMinutes(30));
    }
}