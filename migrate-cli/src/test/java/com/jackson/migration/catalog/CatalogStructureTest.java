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

        MigrationCatalog.FamilySpec platformBaseline = catalog.families.get("platform-baseline");
        assertNotNull(platformBaseline);
        assertFalse(platformBaseline.compileAfterApply);

        List<String> familyOrder = new ArrayList<>(catalog.families.keySet());
        int facesManagedBeansIndex = familyOrder.indexOf("faces-managed-beans");
        int jakartaImportsIndex = familyOrder.indexOf("jakarta-imports");
        int jakartaDependenciesIndex = familyOrder.indexOf("jakarta-dependencies");
        assertTrue(facesManagedBeansIndex >= 0);
        assertTrue(jakartaImportsIndex >= 0);
        assertTrue(jakartaDependenciesIndex >= 0);
        assertTrue(facesManagedBeansIndex < jakartaImportsIndex,
                "faces-managed-beans must run before generic namespace migration so removed javax.faces.bean APIs receive semantic migration");
        assertTrue(jakartaImportsIndex < jakartaDependenciesIndex,
                "jakarta-imports must run before jakarta-dependencies so legacy javax types remain attributable while imports are rewritten");

        MigrationCatalog.FamilySpec requestContext = catalog.families.get("request-context");
        assertTrue(requestContext != null && requestContext.always,
                "request-context must always run so supported legacy calls cannot survive when MTA misses the finding");

        MigrationCatalog.FamilySpec jakartaDependencies = catalog.families.get("jakarta-dependencies");
        assertNotNull(jakartaDependencies);
        assertTrue(jakartaDependencies.always);
        assertFalse(jakartaDependencies.compileAfterApply);
        assertTrue(jakartaDependencies.reviewChecklist.stream()
                .anyMatch(item -> item.contains("Defer compilation")));
        assertTrue(jakartaDependencies.reviewChecklist.stream()
                .anyMatch(item -> item.contains("jsf.version")));
        assertTrue(jakartaDependencies.forbiddenPatterns.contains("<jsf.version>2."));
        assertTrue(jakartaDependencies.forbiddenPatterns.contains("<jsf.version>3."));

        MigrationCatalog.FamilySpec jakartaImports = catalog.families.get("jakarta-imports");
        assertNotNull(jakartaImports);
        assertTrue(jakartaImports.always,
                "jakarta-imports must always run because MTA may not report every migratable javax namespace");
        assertFalse(jakartaImports.compileAfterApply);
        assertTrue(jakartaImports.forbiddenPatterns.contains("import javax.inject."));
        assertFalse(jakartaImports.forbiddenPatterns.contains("import javax.faces."),
                "generic Faces residual scanning must not claim javax.faces.bean, which is owned by the assisted managed-bean family");
        assertTrue(jakartaImports.reviewChecklist.stream()
                .anyMatch(item -> item.contains("before replacing legacy javax API dependencies")));

        MigrationCatalog.FamilySpec jakartaDescriptors = catalog.families.get("jakarta-descriptors");
        assertNotNull(jakartaDescriptors);
        assertTrue(jakartaDescriptors.always,
                "jakarta-descriptors must always run because descriptor migration cannot depend on MTA emitting every XML finding");
        assertFalse(jakartaDescriptors.compileAfterApply);
        assertTrue(jakartaDescriptors.forbiddenPatterns.contains("http://java.sun.com/xml/ns/javaee"));
        assertTrue(jakartaDescriptors.forbiddenPatterns.contains("web-facesconfig_2_2.xsd"));
        assertTrue(jakartaDescriptors.forbiddenPatterns.contains("beans_2_0.xsd"));
        assertTrue(jakartaDescriptors.forbiddenPatterns.contains("persistence_3_0.xsd"));
        assertTrue(jakartaDescriptors.reviewChecklist.stream()
                .anyMatch(item -> item.contains("bean-discovery-mode")));

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
