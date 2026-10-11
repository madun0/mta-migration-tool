package com.jackson.migration.util;

import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Path;

/**
 * Resolves the application project root from the process current working directory.
 *
 * <p>The Workbench intentionally has no project-path command-line option. Developers run
 * {@code migrate} from the root directory of the application being migrated, which keeps the
 * execution boundary explicit and avoids platform-specific path translation.</p>
 */
@ApplicationScoped
public class ProjectDirectory {

    /**
     * Returns the current working directory as an absolute normalized path.
     *
     * @return current application project root
     */
    public Path current() {
        return Path.of("").toAbsolutePath().normalize();
    }
}
