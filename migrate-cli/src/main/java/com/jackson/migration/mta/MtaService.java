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
import java.util.Comparator;
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
     * <p>The application root is canonicalized with {@link Path#toRealPath()} before MTA starts.
     * MTA is launched from an isolated temporary working directory rather than the application's
     * parent directory. This is particularly important on Windows, where Maven/provider discovery
     * can otherwise observe unrelated sibling directories. After analysis, findings are filtered
     * against the canonical application root so dependency repositories and unrelated filesystem
     * locations cannot become Workbench application findings.</p>
     *
     * @param project application project root to analyze
     * @param output directory where MTA output and normalized findings are stored
     * @param config application migration configuration
     * @return normalized MTA findings scoped to the selected application root
     * @throws IOException if MTA cannot be located, execution fails, or output cannot be persisted
     * @throws InterruptedException if the external MTA process is interrupted
     */
    public List<MigrationFinding> assess(Path project, Path output, MigrationConfig config) throws IOException, InterruptedException {
        Path projectRoot = canonicalProjectRoot(project);
        Path effectiveOutput = resolveOutputPath(project, projectRoot, output);
        Path mta = tools.mta().orElseThrow(() -> new IllegalStateException("mta-cli executable not found. Set MTA_CLI or add it to PATH."));
        Files.createDirectories(effectiveOutput);

        Set<String> sources = normalized(config.mta.sources);
        Set<String> targets = normalized(config.mta.targets);
        // The legacy scalar is only a fallback. Never merge it into an explicit target list.
        if (targets.isEmpty() && config.mta.target != null && !config.mta.target.isBlank()) {
            targets.add(config.mta.target.trim());
        }

        List<String> command = new ArrayList<>(List.of(
                mta.toString(), "analyze",
                "--input", projectRoot.toString(),
                "--output", effectiveOutput.toString(),
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

        Path executionDirectory = Files.createTempDirectory("mta-workbench-").toRealPath();
        System.out.println("MTA application root: " + projectRoot);
        System.out.println("MTA execution directory: " + executionDirectory);
        System.out.println("MTA scope: sources=" + sources + ", targets=" + targets);
        System.out.println("MTA custom ruleset: " + customRuleset);

        CommandResult result;
        try {
            result = runner.runStreaming(executionDirectory, command);
        } finally {
            deleteQuietly(executionDirectory);
        }
        if (!result.success()) throw new IOException("MTA analysis failed with exit code " + result.exitCode());

        Path outputYaml = effectiveOutput.resolve("output.yaml");
        if (!Files.exists(outputYaml)) {
            throw new IOException("MTA analysis completed but did not create " + outputYaml);
        }

        List<MigrationFinding> parsed = parser.parse(outputYaml);
        List<MigrationFinding> findings = parser.filterToProjectRoot(parsed, projectRoot);
        int excluded = parsed.size() - findings.size();
        if (excluded > 0) {
            System.out.println("MTA scope filter excluded " + excluded + " finding(s) outside application root.");
        }
        json.writerWithDefaultPrettyPrinter().writeValue(effectiveOutput.resolve("findings.json").toFile(), findings);
        return findings;
    }

    /**
     * Canonicalizes and validates the application root before an external analyzer is launched.
     */
    static Path canonicalProjectRoot(Path project) throws IOException {
        if (project == null) throw new IllegalArgumentException("Project path is required.");
        Path root = project.toRealPath();
        if (!Files.isDirectory(root)) {
            throw new IOException("Project path is not a directory: " + root);
        }
        if (root.getParent() == null) {
            throw new IOException("Refusing to analyze a filesystem root: " + root);
        }

        String homeValue = System.getProperty("user.home", "");
        if (!homeValue.isBlank()) {
            Path homePath = Path.of(homeValue).toAbsolutePath().normalize();
            if (Files.exists(homePath)) homePath = homePath.toRealPath();
            if (root.equals(homePath)) {
                throw new IOException("Refusing to analyze the user home directory. Select the application project root instead: " + root);
            }
        }
        return root;
    }

    /**
     * Keeps generated output physically under the canonical project when the caller supplied an
     * output path relative to the original (possibly symlinked) project path.
     */
    static Path resolveOutputPath(Path originalProject, Path canonicalProject, Path output) {
        Path original = originalProject.toAbsolutePath().normalize();
        Path requested = output.toAbsolutePath().normalize();
        if (requested.startsWith(original)) {
            return canonicalProject.resolve(original.relativize(requested)).normalize();
        }
        return requested;
    }

    /** Best-effort cleanup for the isolated MTA process working directory. */
    private void deleteQuietly(Path directory) {
        if (directory == null || !Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Temporary working-directory cleanup must not hide the MTA result.
                }
            });
        } catch (IOException ignored) {
            // Best effort only.
        }
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
     * Loads previously normalized findings and enforces the selected application root.
     *
     * @param project application root that owns the assessment
     * @param output MTA assessment directory
     * @return normalized findings scoped to the application root
     * @throws IOException if findings cannot be read or parsed
     */
    public List<MigrationFinding> loadFindings(Path project, Path output) throws IOException {
        Path projectRoot = canonicalProjectRoot(project);
        Path effectiveOutput = resolveOutputPath(project, projectRoot, output);
        Path jsonFile = effectiveOutput.resolve("findings.json");
        List<MigrationFinding> findings;
        if (Files.exists(jsonFile)) {
            findings = json.readValue(
                    jsonFile.toFile(),
                    json.getTypeFactory().constructCollectionType(List.class, MigrationFinding.class));
            findings = parser.filterGeneratedFindings(findings);
        } else {
            findings = parser.parse(effectiveOutput.resolve("output.yaml"));
        }
        return parser.filterToProjectRoot(findings, projectRoot);
    }

    /**
     * Loads findings without project-root enforcement for compatibility with callers that only
     * possess an assessment directory. Workbench workflow code should use
     * {@link #loadFindings(Path, Path)}.
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
