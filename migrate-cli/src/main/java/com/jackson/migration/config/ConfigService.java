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
     * Applies profile-aware MTA 8.3 source/target defaults while preserving explicit valid
     * user values and translating Workbench pseudo-selectors emitted by older releases.
     *
     * <p>{@code mta.sources} and {@code mta.targets} are passed directly to MTA, so they must
     * contain native MTA technology selectors. Framework-specific Workbench concepts such as
     * JSF 2, Faces 4, PrimeFaces 6.2, and PrimeFaces 16 are represented by custom rule metadata,
     * not by the MTA source/target CLI arguments.</p>
     */
    private void normalizeMtaScope(MigrationConfig config) {
        if (config.mta.sources == null) config.mta.sources = new java.util.ArrayList<>();
        if (config.mta.targets == null) config.mta.targets = new java.util.ArrayList<>();

        // v21 and earlier wrote Workbench-specific pseudo-sources that are not native MTA 8.3
        // source technologies. Translate them away when older migration.yaml files are loaded.
        config.mta.sources = config.mta.sources.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .filter(value -> !"jsf2".equals(value) && !"primefaces6".equals(value))
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        // Translate only known Workbench pseudo-targets. Preserve all other explicit values so
        // users can select any technology actually exposed by their installed MTA 8.3 CLI.
        java.util.ArrayList<String> normalizedTargets = new java.util.ArrayList<>();
        for (String raw : config.mta.targets) {
            if (raw == null || raw.isBlank()) continue;
            String value = raw.trim();
            switch (value) {
                case "eap82" -> addIfMissing(normalizedTargets, "eap8");
                case "faces4" -> addIfMissing(normalizedTargets, "jakarta-ee");
                case "primefaces16" -> addIfMissing(normalizedTargets, "eap8");
                default -> addIfMissing(normalizedTargets, value);
            }
        }
        config.mta.targets = normalizedTargets;

        if (config.mta.sources.isEmpty()) {
            // JBoss EAP 7 is a native MTA source and is the platform source for this Workbench.
            config.mta.sources.add("eap7");
        }

        if (config.mta.targets.isEmpty()) {
            if (config.mta.target != null && !config.mta.target.isBlank()) {
                String legacy = translateLegacyTarget(config.mta.target.trim());
                addIfMissing(config.mta.targets, legacy);
            }
            if ("primefaces-only".equals(config.profile)) {
                addIfMissing(config.mta.targets, "eap8");
            } else {
                // Native MTA 8.3 targets for the Workbench's EAP 8 / Jakarta / Java 21 goal.
                addIfMissing(config.mta.targets, "eap8");
                addIfMissing(config.mta.targets, "openjdk21");
                addIfMissing(config.mta.targets, "jakarta-ee");
            }
        } else if (!"primefaces-only".equals(config.profile)) {
            // Older generated configurations translated above might otherwise omit the Java 21
            // target. Add it when the configuration clearly came from the Workbench's legacy
            // EAP/Faces/PrimeFaces target set.
            boolean workbenchPlatformTarget = config.mta.targets.contains("eap8")
                    || config.mta.targets.contains("jakarta-ee");
            if (workbenchPlatformTarget) addIfMissing(config.mta.targets, "openjdk21");
        }
    }

    private String translateLegacyTarget(String value) {
        return switch (value) {
            case "eap82", "primefaces16" -> "eap8";
            case "faces4" -> "jakarta-ee";
            default -> value;
        };
    }

    private void addIfMissing(List<String> values, String value) {
        if (value != null && !value.isBlank() && !values.contains(value)) values.add(value);
    }


}
