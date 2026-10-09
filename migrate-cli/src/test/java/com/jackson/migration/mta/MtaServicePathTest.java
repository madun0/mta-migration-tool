package com.jackson.migration.mta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies canonical MTA application-root and output-path handling. */
class MtaServicePathTest {

    @TempDir
    Path temp;

    @Test
    void canonicalProjectRootResolvesRealDirectory() throws Exception {
        Path project = Files.createDirectories(temp.resolve("workspace/app"));
        assertEquals(project.toRealPath(), MtaService.canonicalProjectRoot(project));
    }

    @Test
    void filesystemRootIsRejected() {
        Path root = temp.toAbsolutePath().getRoot();
        assertThrows(Exception.class, () -> MtaService.canonicalProjectRoot(root));
    }

    @Test
    void outputUnderOriginalProjectIsRemappedUnderCanonicalProject() throws Exception {
        Path original = temp.resolve("logical-app").toAbsolutePath().normalize();
        Path canonical = Files.createDirectories(temp.resolve("real-app")).toRealPath();
        Path output = original.resolve(".migration/assessment");

        assertEquals(
                canonical.resolve(".migration/assessment"),
                MtaService.resolveOutputPath(original, canonical, output));
    }
}
