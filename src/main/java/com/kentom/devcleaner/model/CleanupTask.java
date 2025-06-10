package com.kentom.devcleaner.model;

import com.kentom.devcleaner.util.FileUtils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class CleanupTask {
    public void clean(List<Path> paths) throws Exception {
        for (Path path : paths) {
            if (Files.isDirectory(path)) {
                deleteDirectory(path);
            } else {
                FileUtils.delete(path);
            }
            LogManager.log("Deleted: " + path);
        }
    }

    private void deleteDirectory(Path directory) throws Exception {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    FileUtils.delete(file);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                try {
                    FileUtils.delete(dir);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}