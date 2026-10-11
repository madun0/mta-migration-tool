package com.jackson.migration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads and creates per-application migration configuration stored in migration.yaml.
 */
@ApplicationScoped
public class ConfigService {
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    /**
     * Loads {@code migration.yaml} from an application project, applying safe defaults when
     * the file or optional nested sections are absent.
     *
     * @param project application project root
     * @return normalized migration configuration
     * @throws IOException if an existing configuration cannot be read or parsed
     */
    public MigrationConfig load(Path project) throws IOException {
        Path file = project.resolve("migration.yaml");
        if (!Files.exists(file)) {
            MigrationConfig config = new MigrationConfig();
            normalizeMtaScope(config);
            return config;
        }
        MigrationConfig config = yaml.readValue(file.toFile(), MigrationConfig.class);
        if (config.mta == null) config.mta = new MigrationConfig.MtaSettings();
        if (config.validation == null) config.validation = new MigrationConfig.ValidationSettings();
        if (config.rewrite == null) config.rewrite = new MigrationConfig.RewriteSettings();
        if (config.profile == null || config.profile.isBlank()) config.profile = "standard";
        normalizeMtaScope(config);
        return config;
    }

    /**
     * Creates a default {@code migration.yaml} for an application project.
     *
     * @param project application project root
     * @throws IOException if the file already exists or cannot be written
     */
    public void createDefault(Path project) throws IOException {
        Path file = project.resolve("migration.yaml");
        if (Files.exists(file)) {
            throw new IOException("migration.yaml already exists: " + file);
        }
        MigrationConfig config = new MigrationConfig();
        config.projectName = project.getFileName() == null ? "application" : project.getFileName().toString();
        normalizeMtaScope(config);
        yaml.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), config);
    }
    /**
     * Applies profile-aware native MTA 8.3 source/target defaults while preserving explicit
     * user values exactly as configured.
     *
     * <p>{@code mta.sources} and {@code mta.targets} are passed directly to MTA and therefore
     * must contain technologies accepted by the installed MTA CLI. The Workbench does not
     * translate or accept legacy scalar/pseudo-selector configuration.</p>
     */
    private void normalizeMtaScope(MigrationConfig config) {
        if (config.mta.sources == null) config.mta.sources = new java.util.ArrayList<>();
        if (config.mta.targets == null) config.mta.targets = new java.util.ArrayList<>();

        config.mta.sources = normalizeValues(config.mta.sources);
        config.mta.targets = normalizeValues(config.mta.targets);

        if (config.mta.sources.isEmpty()) {
            config.mta.sources.add("eap7");
        }

        if (config.mta.targets.isEmpty()) {
            if ("primefaces-only".equals(config.profile)) {
                config.mta.targets.add("eap8");
            } else {
                config.mta.targets.add("eap8");
                config.mta.targets.add("openjdk21");
                config.mta.targets.add("jakarta-ee");
            }
        }
    }

    private java.util.ArrayList<String> normalizeValues(List<String> values) {
        return values.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
    }


}
