package com.jackson.migration.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the bundled migration catalog is structurally valid and references expected
 * profiles and migration families.
 */
class CatalogStructureTest {

    @Test
    void catalogHasProfilesAndFamilies() throws Exception {
        Path root = Path.of("..").toAbsolutePath().normalize();
        Path file = root.resolve("config/migration-catalog.yaml");
        if (!Files.exists(file)) {
            file = Path.of("config/migration-catalog.yaml").toAbsolutePath();
        }

        MigrationCatalog catalog = new ObjectMapper(new YAMLFactory())
                .findAndRegisterModules()
                .readValue(file.toFile(), MigrationCatalog.class);

        assertTrue(catalog.profiles.containsKey("standard"));
        assertTrue(catalog.families.size() >= 20);
        assertNotNull(catalog.recipeArtifactCoordinates);
        assertEquals("6.46.1", catalog.rewritePluginVersion);
        assertTrue(catalog.rewriteRepositoryUrl == null || catalog.rewriteRepositoryUrl.isBlank());

        MigrationCatalog.FamilySpec platformBaseline = catalog.families.get("platform-baseline");
        assertNotNull(platformBaseline);
        assertFalse(platformBaseline.compileAfterApply);

        List<String> familyOrder = new ArrayList<>(catalog.families.keySet());
        int facesManagedBeansIndex = familyOrder.indexOf("faces-managed-beans");
        int jakartaDependenciesIndex = familyOrder.indexOf("jakarta-dependencies");
        assertTrue(facesManagedBeansIndex >= 0);
        assertTrue(jakartaDependenciesIndex >= 0);
        assertTrue(facesManagedBeansIndex < jakartaDependenciesIndex,
                "faces-managed-beans must run before jakarta-dependencies so legacy javax.faces.bean types remain attributable");

        MigrationCatalog.FamilySpec requestContext = catalog.families.get("request-context");
        assertTrue(requestContext != null && requestContext.always,
                "request-context must always run so supported legacy calls cannot survive when MTA misses the finding");

        MigrationCatalog.FamilySpec jakartaDependencies = catalog.families.get("jakarta-dependencies");
        assertNotNull(jakartaDependencies);
        assertTrue(jakartaDependencies.always);
        assertFalse(jakartaDependencies.compileAfterApply);
        assertTrue(jakartaDependencies.reviewChecklist.stream()
                .anyMatch(item -> item.contains("Defer compilation")));

        MigrationCatalog.FamilySpec jakartaImports = catalog.families.get("jakarta-imports");
        assertNotNull(jakartaImports);
        assertFalse(jakartaImports.compileAfterApply);
        assertTrue(jakartaImports.reviewChecklist.stream()
                .anyMatch(item -> item.contains("During phase validation")));


        assertEquals("ASSISTED", requestContext.automation);
        assertEquals(3, requestContext.reviewChecklist.size());

        MigrationCatalog.FamilySpec fileUpload = catalog.families.get("file-upload");
        assertNotNull(fileUpload);
        assertEquals(4, fileUpload.reviewChecklist.size());

        MigrationCatalog.FamilySpec calendar = catalog.families.get("calendar");
        assertNotNull(calendar);
        assertEquals(4, calendar.reviewChecklist.size());

        MigrationCatalog.FamilySpec inputMask = catalog.families.get("input-mask");
        assertNotNull(inputMask);
        assertEquals("ASSISTED", inputMask.automation);
        assertTrue(inputMask.always,
                "input-mask must always run so missing/empty masks are reviewed even when MTA has no finding");

        MigrationCatalog.FamilySpec schedule = catalog.families.get("schedule");
        MigrationCatalog.FamilySpec timeline = catalog.families.get("timeline");
        assertNotNull(schedule);
        assertNotNull(timeline);
        assertEquals("ASSISTED", schedule.automation);
        assertEquals("ASSISTED", timeline.automation);
    }
}
