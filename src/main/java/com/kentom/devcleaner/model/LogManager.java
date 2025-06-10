package com.kentom.devcleaner.model;

import com.kentom.devcleaner.util.PlatformConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.*;

public class LogManager {
    private static final Logger LOGGER = Logger.getLogger(LogManager.class.getName());

    static {
        try {
            String logDir = PlatformConfig.getLogDirectory();
            Files.createDirectories(Paths.get(logDir));
            Handler fileHandler = new FileHandler(logDir + "devcleaner.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.INFO);
            //LogManager.getLogManager().readConfiguration(LogManager.class.getResourceAsStream("/com/kentom/devcleaner/config/logging.properties"));
        } catch (IOException e) {
            System.err.println("Failed to configure logging: " + e.getMessage());
        }
    }

    public static void log(String message) {
        LOGGER.info(message);
    }
}