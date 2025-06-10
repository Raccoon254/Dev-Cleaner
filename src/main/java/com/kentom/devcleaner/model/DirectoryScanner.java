package com.kentom.devcleaner.model;

import com.kentom.devcleaner.util.FileUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DirectoryScanner {
    private static final Set<String> CLEANUP_RULES = new HashSet<>();

    static {
        try {
            String jsonContent = FileUtils.readResourceFile("/com/kentom/devcleaner/config/cleanup-rules.json");
            String[] rules = jsonContent.split("\"rules\":\\s*\\[")[1].split("]")[0].replaceAll("[\"\\s]", "").split(",");
            for (String rule : rules) {
                if (!rule.isEmpty()) {
                    CLEANUP_RULES.add(rule);
                }
            }
        } catch (Exception e) {
            LogManager.log("Failed to load cleanup rules: " + e.getMessage());
        }
    }

    public List<Path> scan(Path directory) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IOException("Invalid directory: " + directory);
        }

        List<Path> filesToClean = new ArrayList<>();
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (CLEANUP_RULES.contains(dir.getFileName().toString())) {
                    filesToClean.add(dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (CLEANUP_RULES.contains(file.getFileName().toString())) {
                    filesToClean.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LogManager.log("Failed to access: " + file + " - " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
        return filesToClean;
    }
}