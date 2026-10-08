package com.jackson.migration.mta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jackson.migration.config.MigrationConfig;
import com.jackson.migration.process.CommandResult;
import com.jackson.migration.process.CommandRunner;
import com.jackson.migration.process.ToolLocator;
import com.jackson.migration.util.WorkbenchHome;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Runs the MTA CLI with the workbench custom rules and persists normalized findings for planning and verification.
 */
@ApplicationScoped
public class MtaService {
    @Inject ToolLocator tools;
    @Inject CommandRunner runner;
    @Inject MtaOutputParser parser;
    @Inject WorkbenchHome home;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    /**
     * Executes {@code mta-cli analyze} with a migration-specific source/target scope and the
     * workbench Faces and PrimeFaces custom rules. Normalized findings are persisted as
     * {@code findings.json} beneath the requested output directory for later planning and
     * before/after comparison.
     *
     * @param project application project root to analyze
     * @param output directory where MTA output and normalized findings are stored
     * @param config application migration configuration
     * @return normalized MTA findings
     * @throws IOException if MTA cannot be located, execution fails, or output cannot be persisted
     * @throws InterruptedException if the external MTA process is interrupted
     */
    public List<MigrationFinding> assess(Path project, Path output, MigrationConfig config) throws IOException, InterruptedException {
        Path mta = tools.mta().orElseThrow(() -> new IllegalStateException("mta-cli executable not found. Set MTA_CLI or add it to PATH."));
        Files.createDirectories(output);

        Set<String> sources = normalized(config.mta.sources);
        Set<String> targets = normalized(config.mta.targets);
        // The legacy scalar is only a fallback. Never merge it into an explicit target list.
        if (targets.isEmpty() && config.mta.target != null && !config.mta.target.isBlank()) {
            targets.add(config.mta.target.trim());
        }

        List<String> command = new ArrayList<>(List.of(
                mta.toString(), "analyze",
                "--input", project.toString(),
                "--output", output.toString(),
                "--overwrite"
        ));

        for (String source : sources) {
            command.add("--source");
            command.add(source);
        }
        for (String target : targets) {
            command.add("--target");
            command.add(target);
        }

        Path customRuleset = customRuleset();
        command.add("--rules");
        command.add(customRuleset.toString());

        if (config.mta.mode != null && !config.mta.mode.isBlank()) {
            command.add("--mode");
            command.add(config.mta.mode);
        }

        System.out.println("MTA scope: sources=" + sources + ", targets=" + targets);
        System.out.println("MTA custom ruleset: " + customRuleset);
        CommandResult result = runner.runStreaming(project, command);
        if (!result.success()) throw new IOException("MTA analysis failed with exit code " + result.exitCode());

        Path outputYaml = output.resolve("output.yaml");
        if (!Files.exists(outputYaml)) {
            throw new IOException("MTA analysis completed but did not create " + outputYaml);
        }

        List<MigrationFinding> findings = parser.parse(outputYaml);
        json.writerWithDefaultPrettyPrinter().writeValue(output.resolve("findings.json").toFile(), findings);
        return findings;
    }


    /**
     * Returns the single Workbench custom MTA ruleset directory.
     *
     * <p>MTA documents a directory containing one {@code ruleset.yaml} as the supported way to
     * supply multiple custom rule files. The Workbench therefore passes exactly one {@code --rules}
     * argument. Source and target labels remain on the individual rules so profile-specific
     * selection continues to work for Faces-only or PrimeFaces-only analyses.</p>
     *
     * @return Workbench custom ruleset directory
     * @throws IOException if the ruleset or one of its required rule files is missing
     */
    private Path customRuleset() throws IOException {
        Path ruleset = home.path().resolve("rules/migration");
        List<Path> requiredFiles = List.of(
                ruleset.resolve("ruleset.yaml"),
                ruleset.resolve("faces4.yaml"),
                ruleset.resolve("primefaces.yaml"),
                ruleset.resolve("workbench-extra.yaml")
        );

        for (Path requiredFile : requiredFiles) {
            if (!Files.isRegularFile(requiredFile)) {
                throw new IOException("Required custom MTA ruleset file not found: " + requiredFile);
            }
        }
        return ruleset;
    }

    /** Loads non-blank values into a deterministic, deduplicated set. */
    private Set<String> normalized(List<String> values) {
        Set<String> result = new LinkedHashSet<>();
        if (values == null) return result;
        for (String value : values) {
            if (value != null && !value.isBlank()) result.add(value.trim());
        }
        return result;
    }

    /**
     * Loads previously normalized findings, falling back to parsing MTA {@code output.yaml}
     * when {@code findings.json} is not present.
     *
     * @param output MTA assessment directory
     * @return normalized findings
     * @throws IOException if findings cannot be read or parsed
     */
    public List<MigrationFinding> loadFindings(Path output) throws IOException {
        Path jsonFile = output.resolve("findings.json");
        if (Files.exists(jsonFile)) {
            List<MigrationFinding> findings = json.readValue(
                    jsonFile.toFile(),
                    json.getTypeFactory().constructCollectionType(List.class, MigrationFinding.class));
            return parser.filterGeneratedFindings(findings);
        }
        return parser.parse(output.resolve("output.yaml"));
    }
}
