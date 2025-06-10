package com.kentom.devcleaner.util;

public class PlatformConfig {
    public static String getLogDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return System.getenv("APPDATA") + "\\DevCleaner\\logs\\";
        } else {
            return System.getProperty("user.home") + "/.devcleaner/logs/";
        }
    }
}