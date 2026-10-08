package com.jackson.migration.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads and writes migration state as UTF-8 JSON under .migration/state.json.
 */
@ApplicationScoped
public class StateStore {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    /**
     * Loads persisted migration state when a migration has already been initialized/executed.
     *
     * @param project application project root
     * @return optional migration state
     * @throws IOException if state exists but cannot be read or parsed
     */
    public Optional<MigrationState> load(Path project) throws IOException {
        Path file = file(project);
        if (!Files.exists(file)) return Optional.empty();
        return Optional.of(json.readValue(file.toFile(), MigrationState.class));
    }

    /**
     * Persists workflow state atomically enough for the single-user CLI execution model.
     *
     * @param project application project root
     * @param state state to persist
     * @throws IOException if state cannot be written
     */
    public void save(Path project, MigrationState state) throws IOException {
        Path file = file(project);
        Files.createDirectories(file.getParent());
        json.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), state);
    }

    /**
     * @param project application project root
     * @return canonical {@code .migration/state.json} location
     */
    public Path file(Path project) { return project.resolve(".migration/state.json"); }
}
