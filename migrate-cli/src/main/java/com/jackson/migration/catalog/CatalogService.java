package com.jackson.migration.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jackson.migration.util.WorkbenchHome;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Loads the workbench migration catalog from the configured workbench home directory and deserializes it into the domain model.
 */
@ApplicationScoped
public class CatalogService {
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    @Inject
    WorkbenchHome home;

    /**
     * Loads the installed {@code migration-catalog.yaml}.
     *
     * @return deserialized migration catalog used by planning and execution
     * @throws IOException if the catalog cannot be read or parsed
     */
    public MigrationCatalog load() throws IOException {
        Path path = home.path().resolve("config").resolve("migration-catalog.yaml");
        return yaml.readValue(path.toFile(), MigrationCatalog.class);
    }
}
