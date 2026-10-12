package com.jackson.migration.mta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void workbenchStateIsHiddenDuringMtaAnalysisAndRestoredAfterward() throws Exception {
        Path project = Files.createDirectories(temp.resolve("workspace/verify-app"));
        Path migration = Files.createDirectories(project.resolve(".migration/assessment"));
        Path outputYaml = migration.resolve("output.yaml");
        Files.writeString(outputYaml, "historical assessment");

        try (MtaService.HiddenWorkbenchState ignored = MtaService.hideWorkbenchState(project)) {
            assertFalse(Files.exists(project.resolve(".migration")));
            assertTrue(Files.exists(project.getParent()));
        }

        assertTrue(Files.exists(outputYaml));
        assertEquals("historical assessment", Files.readString(outputYaml));
    }

    @Test
    void mavenBuildOutputIsHiddenDuringMtaAnalysisAndRestoredAfterward() throws Exception {
        Path project = Files.createDirectories(temp.resolve("workspace/multi-module-app"));
        Path rootTarget = Files.createDirectories(project.resolve("target/classes/META-INF"));
        Path moduleTarget = Files.createDirectories(project.resolve("module-a/target/classes"));
        Files.writeString(rootTarget.resolve("persistence.xml"), "legacy generated persistence descriptor");
        Files.writeString(moduleTarget.resolve("generated.txt"), "generated module output");

        try (MtaService.HiddenGeneratedDirectories ignored = MtaService.hideGeneratedDirectories(project)) {
            assertFalse(Files.exists(project.resolve("target")));
            assertFalse(Files.exists(project.resolve("module-a/target")));
            assertTrue(Files.exists(project.resolve("module-a")));
        }

        assertEquals(
                "legacy generated persistence descriptor",
                Files.readString(project.resolve("target/classes/META-INF/persistence.xml")));
        assertEquals(
                "generated module output",
                Files.readString(project.resolve("module-a/target/classes/generated.txt")));
    }


    @Test
    void recreatedMavenBuildOutputIsDiscardedBeforeOriginalIsRestored() throws Exception {
        Path project = Files.createDirectories(temp.resolve("workspace/recreated-target-app"));
        Path originalTarget = Files.createDirectories(project.resolve("target/classes"));
        Files.writeString(originalTarget.resolve("original.txt"), "original build output");

        try (MtaService.HiddenGeneratedDirectories ignored = MtaService.hideGeneratedDirectories(project)) {
            assertFalse(Files.exists(project.resolve("target")));

            Path recreatedTarget = Files.createDirectories(project.resolve("target/mta-generated"));
            Files.writeString(recreatedTarget.resolve("transient.txt"), "transient analysis output");
        }

        assertEquals("original build output", Files.readString(project.resolve("target/classes/original.txt")));
        assertFalse(Files.exists(project.resolve("target/mta-generated/transient.txt")));
    }

    @Test
    void stagedMtaOutputReplacesOnlyRequestedOutputDirectory() throws Exception {
        Path project = Files.createDirectories(temp.resolve("workspace/output-app"));
        Path migration = Files.createDirectories(project.resolve(".migration"));
        Path assessment = Files.createDirectories(migration.resolve("assessment"));
        Files.writeString(assessment.resolve("stale.txt"), "stale");
        Files.writeString(migration.resolve("state.json"), "keep");

        Path staged = Files.createDirectories(temp.resolve("staged-output"));
        Files.writeString(staged.resolve("output.yaml"), "fresh");
        Path report = Files.createDirectories(staged.resolve("static-report"));
        Files.writeString(report.resolve("index.html"), "report");

        MtaService.replaceDirectory(staged, assessment);

        assertFalse(Files.exists(assessment.resolve("stale.txt")));
        assertEquals("fresh", Files.readString(assessment.resolve("output.yaml")));
        assertEquals("report", Files.readString(assessment.resolve("static-report/index.html")));
        assertEquals("keep", Files.readString(migration.resolve("state.json")));
    }
}
