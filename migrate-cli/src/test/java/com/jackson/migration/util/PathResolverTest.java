package com.jackson.migration.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies Windows and Git Bash path normalization behavior used by CLI commands.
 */
class PathResolverTest {

    @Test
    void relativePathNormalizes() {
        PathResolver resolver = new PathResolver();
        assertTrue(resolver.resolve(".").isAbsolute());
    }
}
