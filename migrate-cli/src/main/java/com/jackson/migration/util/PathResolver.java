package com.jackson.migration.util;

import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;

/**
 * Normalizes CLI project arguments, including Git Bash /c/... paths, into Windows-compatible java.nio.file.Path values.
 */
@ApplicationScoped
public class PathResolver {
    /**
     * Resolves a CLI path into an absolute normalized Windows-compatible path. Git Bash paths such
     * as {@code /c/work/app} are translated when running on Windows.
     *
     * @param input user-supplied project path
     * @return absolute normalized path
     */
    public Path resolve(String input) {
        String value = input == null || input.isBlank() ? "." : input;
        if (isWindows() && value.matches("^/[A-Za-z]/.*")) {
            char drive = Character.toUpperCase(value.charAt(1));
            value = drive + ":\\" + value.substring(3).replace('/', '\\');
        }
        return Path.of(value).toAbsolutePath().normalize();
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
