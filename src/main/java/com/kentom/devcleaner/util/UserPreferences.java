package com.kentom.devcleaner.util;

import java.util.prefs.Preferences;

/**
 * A utility class to manage user-specific settings using the Java Preferences API.
 * This provides a platform-independent way to store simple data like window size,
 * last used paths, etc.
 */
public class UserPreferences {

    // Creates a preference node specific to this application class.
    private static final Preferences prefs = Preferences.userNodeForPackage(UserPreferences.class);

    // Defines the key for storing the last scanned path.
    private static final String LAST_SCANNED_PATH = "lastScannedPath";

    /**
     * Retrieves the last path that was successfully scanned by the user.
     *
     * @return The string of the last scanned path. If no path is stored, it defaults
     * to the user's home directory.
     */
    public static String getLastScannedPath() {
        return prefs.get(LAST_SCANNED_PATH, System.getProperty("user.home"));
    }

    /**
     * Stores the given path as the last scanned path.
     *
     * @param path The directory path to store.
     */
    public static void setLastScannedPath(String path) {
        if (path != null && !path.isEmpty()) {
            prefs.put(LAST_SCANNED_PATH, path);
        }
    }

    // This class can be easily extended to store other preferences.
    // For example:
    // private static final String WINDOW_WIDTH = "windowWidth";
    // public static int getWindowWidth() { return prefs.getInt(WINDOW_WIDTH, 800); }
    // public static void setWindowWidth(int width) { prefs.putInt(WINDOW_WIDTH, width); }
}
