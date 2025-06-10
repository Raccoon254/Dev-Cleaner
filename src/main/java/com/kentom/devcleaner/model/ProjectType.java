package com.kentom.devcleaner.model;

import java.util.Set;

public enum ProjectType {
    NODE("Node.js", "node.png", Set.of("package.json", "node_modules")),
    MAVEN("Maven", "maven.png", Set.of("pom.xml")),
    GRADLE("Gradle", "gradle.png", Set.of("build.gradle", ".gradle")),
    PHP_COMPOSER("Composer", "php.png", Set.of("composer.json", "vendor")),
    PYTHON("Python", "python.png", Set.of("requirements.txt", "venv", "__pycache__")),
    ANDROID("Android", "android.png", Set.of("build.gradle", "app"));

    public final String displayName;
    public final String iconName;
    public final Set<String> markers;

    ProjectType(String displayName, String iconName, Set<String> markers) {
        this.displayName = displayName;
        this.iconName = iconName;
        this.markers = markers;
    }
}