package com.kentom.devcleaner.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class GitInfo {
    private final boolean isGitRepository;
    private final String currentBranch;
    private final String remoteUrl;
    private final long gitFolderSize;
    private final int commitCount;
    private final boolean hasUncommittedChanges;

    private GitInfo(boolean isGitRepository, String currentBranch, String remoteUrl, 
                   long gitFolderSize, int commitCount, boolean hasUncommittedChanges) {
        this.isGitRepository = isGitRepository;
        this.currentBranch = currentBranch;
        this.remoteUrl = remoteUrl;
        this.gitFolderSize = gitFolderSize;
        this.commitCount = commitCount;
        this.hasUncommittedChanges = hasUncommittedChanges;
    }

    public static GitInfo analyze(Path projectPath) {
        Path gitFolder = projectPath.resolve(".git");
        
        if (!Files.exists(gitFolder)) {
            return new GitInfo(false, null, null, 0, 0, false);
        }

        try {
            long gitSize = calculateDirectorySize(gitFolder);
            String branch = getCurrentBranch(projectPath);
            String remote = getRemoteUrl(projectPath);
            int commits = getCommitCount(projectPath);
            boolean hasChanges = hasUncommittedChanges(projectPath);
            
            return new GitInfo(true, branch, remote, gitSize, commits, hasChanges);
        } catch (Exception e) {
            LogManager.log("Error analyzing Git repository at " + projectPath + ": " + e.getMessage());
            return new GitInfo(true, "unknown", null, 0, 0, false);
        }
    }

    private static long calculateDirectorySize(Path directory) {
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk
                    .filter(p -> p.toFile().isFile())
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            return 0;
        }
    }

    private static String getCurrentBranch(Path projectPath) {
        return executeGitCommand(projectPath, "git", "branch", "--show-current")
                .trim();
    }

    private static String getRemoteUrl(Path projectPath) {
        String remote = executeGitCommand(projectPath, "git", "remote", "get-url", "origin");
        return remote.isEmpty() ? null : remote.trim();
    }

    private static int getCommitCount(Path projectPath) {
        String count = executeGitCommand(projectPath, "git", "rev-list", "--count", "HEAD");
        try {
            return count.isEmpty() ? 0 : Integer.parseInt(count.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean hasUncommittedChanges(Path projectPath) {
        String status = executeGitCommand(projectPath, "git", "status", "--porcelain");
        return !status.trim().isEmpty();
    }

    private static String executeGitCommand(Path workingDirectory, String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDirectory.toFile());
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            process.waitFor();
            return output.toString();
            
        } catch (IOException | InterruptedException e) {
            return "";
        }
    }

    // Getters
    public boolean isGitRepository() { return isGitRepository; }
    public String getCurrentBranch() { return currentBranch; }
    public String getRemoteUrl() { return remoteUrl; }
    public long getGitFolderSize() { return gitFolderSize; }
    public int getCommitCount() { return commitCount; }
    public boolean hasUncommittedChanges() { return hasUncommittedChanges; }

    public String getFormattedGitSize() {
        if (gitFolderSize < 1024) return gitFolderSize + " B";
        int z = (63 - Long.numberOfLeadingZeros(gitFolderSize)) / 10;
        return String.format("%.1f %sB", (double) gitFolderSize / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    public String getRepositoryName() {
        if (remoteUrl == null) return null;
        
        // Extract repository name from URL
        String url = remoteUrl;
        if (url.endsWith(".git")) {
            url = url.substring(0, url.length() - 4);
        }
        
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1);
        }
        
        return null;
    }
}