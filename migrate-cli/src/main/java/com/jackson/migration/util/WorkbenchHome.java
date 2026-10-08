package com.jackson.migration.util;

import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves the installed workbench home used to find rules, configuration, recipe artifacts, and documentation.
 */
@ApplicationScoped
public class WorkbenchHome {
    /**
     * Resolves the workbench installation root from {@code MTA_MIGRATE_HOME}, launcher metadata,
     * or the current working directory fallback.
     *
     * @return workbench home directory
     */
    public Path path() {
        String configured = System.getenv("MTA_MIGRATE_HOME");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }

        Path candidate = Path.of("").toAbsolutePath().normalize();
        for (Path p = candidate; p != null; p = p.getParent()) {
            if (Files.exists(p.resolve("config/migration-catalog.yaml"))) {
                return p;
            }
        }
        return candidate;
    }
}
