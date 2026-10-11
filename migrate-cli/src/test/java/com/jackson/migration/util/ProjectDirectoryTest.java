package com.jackson.migration.util;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that CLI commands derive their project root from the current working directory.
 */
class ProjectDirectoryTest {

    @Test
    void currentDirectoryIsAbsoluteAndNormalized() {
        ProjectDirectory resolver = new ProjectDirectory();
        Path expected = Path.of("").toAbsolutePath().normalize();

        assertTrue(resolver.current().isAbsolute());
        assertEquals(expected, resolver.current());
    }
}
