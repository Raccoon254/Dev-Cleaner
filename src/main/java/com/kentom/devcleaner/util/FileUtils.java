package com.kentom.devcleaner.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {
    public static void delete(Path path) throws Exception {
        Files.delete(path);
    }

    public static String readResourceFile(String resourcePath) throws Exception {
        try (InputStream is = FileUtils.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new Exception("Resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}