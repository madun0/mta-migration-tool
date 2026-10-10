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
     * can otherwise observe unrelated sibling directories. Finding scope then follows the mode from
     * {@code migration.yaml}: {@code source-only} enforces the canonical application root, while
     * {@code full} intentionally preserves external dependency findings.</p>
     *
     * @param project application project root to analyze
     * @param output directory where MTA output and normalized findings are stored
     * @param config application migration configuration
     * @return normalized MTA findings scoped according to the configured MTA mode
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

        String analysisMode = effectiveAnalysisMode(config.mta.mode);
        command.add("--mode");
        command.add(analysisMode);

        boolean analyzeKnownLibraries = "full".equals(analysisMode) && config.mta.analyzeKnownLibraries;
        if (analyzeKnownLibraries) {
            command.add("--analyze-known-libraries");
        }

        Path mavenSettings = resolveMavenSettings(projectRoot, config.mta.mavenSettings);
        if (mavenSettings != null) {
            command.add("--maven-settings");
            command.add(mavenSettings.toString());
        }
        if (config.mta.disableMavenSearch) {
            command.add("--disable-maven-search=true");
        }

        Path executionDirectory = Files.createTempDirectory("mta-workbench-").toRealPath();
        System.out.println("MTA application root: " + projectRoot);
        System.out.println("MTA execution directory: " + executionDirectory);
        System.out.println("MTA scope: sources=" + sources + ", targets=" + targets);
        System.out.println("MTA analysis mode: " + analysisMode + " (from migration.yaml)");
        System.out.println("MTA analyze known libraries: " + analyzeKnownLibraries);
        System.out.println("MTA Maven settings: " + (mavenSettings == null ? "<MTA/Maven default>" : mavenSettings));
        System.out.println("MTA disable Maven search: " + config.mta.disableMavenSearch);
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
        List<MigrationFinding> insights = parser.parseInsights(outputYaml);
        List<MigrationFinding> findings = scopeFindings(parsed, projectRoot, analysisMode);
        int external = parsed.size() - parser.filterToProjectRoot(parsed, projectRoot).size();
        if ("source-only".equals(analysisMode) && external > 0) {
            System.out.println("MTA source-only scope excluded " + external + " finding(s) outside application root.");
        } else if ("full".equals(analysisMode) && external > 0) {
            System.out.println("MTA full mode retained " + external + " external dependency finding(s).");
        }
        json.writerWithDefaultPrettyPrinter().writeValue(effectiveOutput.resolve("findings.json").toFile(), findings);
        json.writerWithDefaultPrettyPrinter().writeValue(effectiveOutput.resolve("insights.json").toFile(), insights);
        System.out.println("MTA actionable findings captured: " + findings.size());
        System.out.println("MTA insights captured separately: " + insights.size());
        if (Files.exists(effectiveOutput.resolve("dependencies.yaml"))) {
            System.out.println("MTA dependency inventory: " + effectiveOutput.resolve("dependencies.yaml"));
        }
        return findings;
    }

    /**
     * Resolves the MTA analysis mode configured in {@code migration.yaml}.
     *
     * <p>The mode is intentionally YAML-only: {@code source-only} limits normalized findings to
     * the selected application root, while {@code full} preserves dependency findings emitted by
     * MTA, including local Maven repository and parent/reactor-module incidents. No CLI option
     * overrides this setting.</p>
     *
     * @param requestedMode configured MTA mode
     * @return {@code source-only} or {@code full}
     */
    static String effectiveAnalysisMode(String requestedMode) {
        String mode = requestedMode == null ? "" : requestedMode.trim().toLowerCase();
        if (mode.isBlank()) return "source-only";
        if (!"source-only".equals(mode) && !"full".equals(mode)) {
            throw new IllegalArgumentException(
                    "Unsupported MTA analysis mode '" + requestedMode
                            + "'. Expected 'source-only' or 'full' in migration.yaml.");
        }
        return mode;
    }

    /** Applies the finding boundary associated with the configured MTA mode. */
    List<MigrationFinding> scopeFindings(List<MigrationFinding> findings, Path projectRoot, String analysisMode) {
        if ("full".equals(analysisMode)) {
            return findings == null ? List.of() : List.copyOf(findings);
        }
        return parser.filterToProjectRoot(findings, projectRoot);
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

    /** Resolves an optional Maven settings file configured in migration.yaml. */
    static Path resolveMavenSettings(Path projectRoot, String configured) throws IOException {
        if (configured == null || configured.isBlank()) return null;
        Path candidate = Path.of(configured.trim());
        if (!candidate.isAbsolute()) candidate = projectRoot.resolve(candidate);
        candidate = candidate.toAbsolutePath().normalize();
        if (!Files.exists(candidate)) {
            throw new IOException("Configured MTA Maven settings file does not exist: " + candidate);
        }
        if (!Files.isRegularFile(candidate)) {
            throw new IOException("Configured MTA Maven settings path is not a file: " + candidate);
        }
        return candidate.toRealPath();
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
     * Loads previously normalized findings using the same mode policy as assessment.
     *
     * <p>In {@code full} mode the raw MTA YAML is preferred when available. This intentionally
     * recovers external dependency findings from assessments produced by v19 and earlier, where
     * {@code findings.json} was incorrectly reduced to the application root. In
     * {@code source-only} mode the selected application root remains the hard boundary.</p>
     *
     * @param project application root that owns the assessment
     * @param output MTA assessment directory
     * @param config migration configuration loaded from {@code migration.yaml}
     * @return normalized findings consistent with the configured MTA mode
     * @throws IOException if findings cannot be read or parsed
     */
    public List<MigrationFinding> loadFindings(Path project, Path output, MigrationConfig config) throws IOException {
        Path projectRoot = canonicalProjectRoot(project);
        Path effectiveOutput = resolveOutputPath(project, projectRoot, output);
        String analysisMode = effectiveAnalysisMode(config == null || config.mta == null ? null : config.mta.mode);
        Path outputYaml = effectiveOutput.resolve("output.yaml");
        Path jsonFile = effectiveOutput.resolve("findings.json");

        List<MigrationFinding> findings;
        if ("full".equals(analysisMode) && Files.exists(outputYaml)) {
            findings = parser.parse(outputYaml);
        } else if (Files.exists(jsonFile)) {
            findings = json.readValue(
                    jsonFile.toFile(),
                    json.getTypeFactory().constructCollectionType(List.class, MigrationFinding.class));
            findings = parser.filterGeneratedFindings(findings);
        } else {
            findings = parser.parse(outputYaml);
        }
        return scopeFindings(findings, projectRoot, analysisMode);
    }

    /**
     * Backward-compatible source-only loader. Workflow code must use the configuration-aware
     * overload so dependency findings are preserved when {@code mta.mode: full}.
     */
    @Deprecated
    public List<MigrationFinding> loadFindings(Path project, Path output) throws IOException {
        MigrationConfig config = new MigrationConfig();
        config.mta.mode = "source-only";
        return loadFindings(project, output, config);
    }

    /**
     * Loads findings without project-root enforcement for compatibility with callers that only
     * possess an assessment directory. Workbench workflow code should use
     * {@link #loadFindings(Path, Path, MigrationConfig)}.
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
