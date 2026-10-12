package com.jackson.migration.mta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jackson.migration.config.MigrationConfig;
import com.jackson.migration.process.CommandResult;
import com.jackson.migration.process.CommandRunner;
import com.jackson.migration.process.ToolLocator;
import com.jackson.migration.util.ElapsedTime;
import com.jackson.migration.util.WorkbenchHome;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Runs the MTA CLI with Workbench custom rules while preserving MTA report incidents exactly and deriving separate planning findings.
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
     * workbench Faces and PrimeFaces custom rules. Raw MTA violation incidents are persisted as
     * {@code findings.json}; normalized planning findings are persisted as {@code planning-findings.json} beneath the requested output directory for later planning and
     * before/after comparison.
     *
     * <p>The application root is canonicalized with {@link Path#toRealPath()} before MTA starts.
     * MTA is launched from an isolated temporary working directory rather than the application's
     * parent directory. This is particularly important on Windows, where Maven/provider discovery
     * can otherwise observe unrelated sibling directories. Workbench-owned {@code .migration}
     * state and Maven {@code target/} directories are temporarily moved outside the application
     * root while MTA runs so previous reports, checkpoints, and generated build copies cannot be
     * rediscovered as application source.
     * MTA remains authoritative for the source/dependency scope selected by {@code migration.yaml}.</p>
     *
     * @param project application project root to analyze
     * @param output directory where MTA output and Workbench artifacts are stored
     * @param config application migration configuration
     * @return normalized planning findings derived from the MTA report
     * @throws IOException if MTA cannot be located, execution fails, or output cannot be persisted
     * @throws InterruptedException if the external MTA process is interrupted
     */
    public List<MigrationFinding> assess(Path project, Path output, MigrationConfig config) throws IOException, InterruptedException {
        Path projectRoot = canonicalProjectRoot(project);
        Path effectiveOutput = resolveOutputPath(project, projectRoot, output);
        Path mta = tools.mta().orElseThrow(() -> new IllegalStateException("mta-cli executable not found. Set MTA_CLI or add it to PATH."));

        Set<String> sources = normalized(config.mta.sources);
        Set<String> targets = normalized(config.mta.targets);

        validateMtaTechnologyScope(mta, projectRoot, sources, targets);

        Path analysisOutput = Files.createTempDirectory("mta-workbench-output-").toRealPath();
        List<String> command = new ArrayList<>(List.of(
                mta.toString(), "analyze",
                "--input", projectRoot.toString(),
                "--output", analysisOutput.toString(),
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
        // Make MTA's default-ruleset behavior explicit so the Workbench and a direct
        // mta-cli invocation have the same rule baseline.
        command.add("--enable-default-rulesets=true");

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
        System.out.println("MTA report staging directory: " + analysisOutput);
        System.out.println("MTA persisted output: " + effectiveOutput);
        System.out.println("MTA scope: sources=" + sources + ", targets=" + targets);
        System.out.println("MTA analysis mode: " + analysisMode + " (from migration.yaml)");
        System.out.println("MTA analyze known libraries: " + analyzeKnownLibraries);
        System.out.println("MTA Maven settings: " + (mavenSettings == null ? "<MTA/Maven default>" : mavenSettings));
        System.out.println("MTA disable Maven search: " + config.mta.disableMavenSearch);
        System.out.println("MTA custom ruleset: " + customRuleset);
        System.out.println("MTA command: " + formatCommand(command));

        try {
            CommandResult result;
            Instant mtaStart = Instant.now();
            try {
                try (HiddenWorkbenchState ignored = hideWorkbenchState(projectRoot);
                     HiddenGeneratedDirectories ignoredBuildOutput = hideGeneratedDirectories(projectRoot)) {
                    result = runner.runStreaming(executionDirectory, command);
                }
            } finally {
                deleteQuietly(executionDirectory);
                System.out.println("[TIMER] MTA analyze: " + ElapsedTime.format(ElapsedTime.since(mtaStart)));
            }
            if (!result.success()) throw new IOException("MTA analysis failed with exit code " + result.exitCode());

            Path stagedOutputYaml = analysisOutput.resolve("output.yaml");
            if (!Files.exists(stagedOutputYaml)) {
                throw new IOException("MTA analysis completed but did not create " + stagedOutputYaml);
            }

            replaceDirectory(analysisOutput, effectiveOutput);
            Path outputYaml = effectiveOutput.resolve("output.yaml");
            MtaOutputParser.ParsedReport report = parser.parseReport(outputYaml);
            List<MigrationFinding> incidents = report.incidents();
            List<MigrationFinding> planningFindings = report.planningFindings();
            List<MigrationFinding> insights = report.insights();

            // MTA is authoritative for source-only versus full scope. findings.json mirrors the
            // violation incidents in output.yaml one-for-one; Workbench-only cleanup/deduplication is
            // persisted separately for planning and residual classification.
            json.writerWithDefaultPrettyPrinter().writeValue(effectiveOutput.resolve("findings.json").toFile(), incidents);
            json.writerWithDefaultPrettyPrinter().writeValue(
                    effectiveOutput.resolve("planning-findings.json").toFile(), planningFindings);
            json.writerWithDefaultPrettyPrinter().writeValue(effectiveOutput.resolve("insights.json").toFile(), insights);
            json.writerWithDefaultPrettyPrinter().writeValue(
                    effectiveOutput.resolve("assessment-summary.json").toFile(),
                    Map.of("mtaFindings", incidents.size()));
            System.out.println(formatFindingCount(incidents.size()));
            if (Files.exists(effectiveOutput.resolve("dependencies.yaml"))) {
                System.out.println("MTA dependency inventory: " + effectiveOutput.resolve("dependencies.yaml"));
            }
            return planningFindings;
        } finally {
            deleteQuietly(analysisOutput);
        }
    }


    /**
     * Temporarily moves Workbench-owned state outside the application root so MTA cannot analyze
     * its own previous reports, findings, checkpoints, or migration metadata as application source.
     * The state is restored automatically when the returned guard is closed.
     */
    static HiddenWorkbenchState hideWorkbenchState(Path projectRoot) throws IOException {
        Path migrationDirectory = projectRoot.resolve(".migration");
        if (!Files.exists(migrationDirectory)) {
            return new HiddenWorkbenchState(migrationDirectory, null);
        }

        Path hiddenDirectory = projectRoot.resolveSibling(
                "." + projectRoot.getFileName() + "-mta-workbench-state-" + java.util.UUID.randomUUID());
        Files.move(migrationDirectory, hiddenDirectory);
        return new HiddenWorkbenchState(migrationDirectory, hiddenDirectory);
    }

    /**
     * Temporarily moves Maven build-output directories outside the analyzed application tree.
     * MTA scans the full input directory recursively and can otherwise report migration findings
     * from generated copies such as {@code target/classes/META-INF/persistence.xml}. Those files
     * are build products, not migration sources, and can duplicate or stale-shadow the real
     * descriptor under {@code src/main}.
     */
    static HiddenGeneratedDirectories hideGeneratedDirectories(Path projectRoot) throws IOException {
        List<Path> candidates;
        try (var paths = Files.walk(projectRoot)) {
            candidates = paths
                    .filter(Files::isDirectory)
                    .filter(path -> !path.equals(projectRoot))
                    .filter(path -> path.getFileName() != null
                            && "target".equalsIgnoreCase(path.getFileName().toString()))
                    .sorted(Comparator.comparingInt(Path::getNameCount))
                    .toList();
        }

        // If a target directory contains another target directory, moving the outer directory is
        // sufficient. Keep only the shallowest generated roots.
        List<Path> selected = new ArrayList<>();
        for (Path candidate : candidates) {
            boolean nestedInSelected = selected.stream().anyMatch(candidate::startsWith);
            if (!nestedInSelected) selected.add(candidate);
        }
        if (selected.isEmpty()) return new HiddenGeneratedDirectories(null, List.of());

        Path stagingRoot = projectRoot.resolveSibling(
                "." + projectRoot.getFileName() + "-mta-build-output-" + java.util.UUID.randomUUID());
        Files.createDirectories(stagingRoot);
        List<MovedDirectory> moved = new ArrayList<>();
        try {
            for (Path original : selected) {
                Path hidden = stagingRoot.resolve(projectRoot.relativize(original));
                Path parent = hidden.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.move(original, hidden);
                moved.add(new MovedDirectory(original, hidden));
            }
            return new HiddenGeneratedDirectories(stagingRoot, List.copyOf(moved));
        } catch (IOException failure) {
            restoreMovedDirectories(moved);
            deleteQuietly(stagingRoot);
            throw failure;
        }
    }

    private record MovedDirectory(Path original, Path hidden) {}

    /** Restores Maven build output that was hidden while MTA analyzed the source tree. */
    static final class HiddenGeneratedDirectories implements AutoCloseable {
        private final Path stagingRoot;
        private final List<MovedDirectory> moved;
        private boolean restored;

        HiddenGeneratedDirectories(Path stagingRoot, List<MovedDirectory> moved) {
            this.stagingRoot = stagingRoot;
            this.moved = moved;
        }

        @Override
        public void close() throws IOException {
            if (restored) return;
            restoreMovedDirectories(moved);
            deleteQuietly(stagingRoot);
            restored = true;
        }
    }

    private static void restoreMovedDirectories(List<MovedDirectory> moved) throws IOException {
        IOException first = null;
        for (int i = moved.size() - 1; i >= 0; i--) {
            MovedDirectory entry = moved.get(i);
            try {
                if (!Files.exists(entry.hidden())) continue;
                if (Files.exists(entry.original())) {
                    // The original build output was moved away before MTA started. Anything that
                    // reappears at the original path while analysis is running is transient output
                    // created by MTA/Maven and must not replace the caller's pre-existing build tree.
                    deleteRecursively(entry.original());
                }
                Path parent = entry.original().getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.move(entry.hidden(), entry.original());
            } catch (IOException e) {
                if (first == null) first = e;
                else first.addSuppressed(e);
            }
        }
        if (first != null) throw first;
    }

    /** Restores a temporarily hidden Workbench state directory. */
    static final class HiddenWorkbenchState implements AutoCloseable {
        private final Path originalDirectory;
        private final Path hiddenDirectory;
        private boolean restored;

        HiddenWorkbenchState(Path originalDirectory, Path hiddenDirectory) {
            this.originalDirectory = originalDirectory;
            this.hiddenDirectory = hiddenDirectory;
        }

        @Override
        public void close() throws IOException {
            if (restored || hiddenDirectory == null || !Files.exists(hiddenDirectory)) {
                restored = true;
                return;
            }
            if (Files.exists(originalDirectory)) {
                throw new IOException("Cannot restore Workbench state because the application recreated "
                        + originalDirectory + " while MTA was running.");
            }
            Files.move(hiddenDirectory, originalDirectory);
            restored = true;
        }
    }

    /** Replaces a persisted MTA output directory with the contents of an isolated staging directory. */
    static void replaceDirectory(Path source, Path target) throws IOException {
        deleteTree(target);
        Files.createDirectories(target);
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path relative = source.relativize(path);
                if (relative.toString().isEmpty()) continue;
                Path destination = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Path parent = destination.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    Files.copy(path, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /** Deletes a directory tree and propagates failures when replacement must be reliable. */
    private static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }


    /** Formats the single user-facing assessment findings count. */
    static String formatFindingCount(int count) {
        return "MTA findings: " + count;
    }

    /** Renders the exact MTA argument vector for troubleshooting/comparison with direct CLI runs. */
    static String formatCommand(List<String> command) {
        return command.stream()
                .map(value -> value.matches(".*\\s+.*") ? '"' + value + '"' : value)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    /**
     * Returns the MTA report's raw violation-incident count for an existing assessment directory.
     * A persisted summary is preferred; older assessments transparently fall back to output.yaml.
     */
    public int loadReportedIncidentCount(Path output) throws IOException {
        Path summary = output.resolve("assessment-summary.json");
        if (Files.exists(summary)) {
            JsonNode node = json.readTree(summary.toFile());
            JsonNode count = node.get("mtaFindings");
            if (count != null && count.canConvertToInt()) return count.intValue();
        }
        return parser.parseReport(output.resolve("output.yaml")).violationIncidentCount();
    }

    /**
     * Validates the configured source and target technologies against the installed MTA CLI.
     *
     * <p>MTA 8.2+ exposes the authoritative technology lists through
     * {@code mta-cli rules list-sources} and {@code mta-cli rules list-targets}. The Workbench
     * intentionally validates against the executable it will actually invoke rather than
     * maintaining a second hard-coded catalog that can drift from MTA 8.3 patch releases.</p>
     */
    void validateMtaTechnologyScope(
            Path mta,
            Path workingDirectory,
            Set<String> sources,
            Set<String> targets) throws IOException, InterruptedException {

        Set<String> availableSources = queryMtaTechnologies(mta, workingDirectory, "list-sources");
        Set<String> availableTargets = queryMtaTechnologies(mta, workingDirectory, "list-targets");

        List<String> invalidSources = sources.stream()
                .filter(value -> !availableSources.contains(value.toLowerCase(Locale.ROOT)))
                .toList();
        List<String> invalidTargets = targets.stream()
                .filter(value -> !availableTargets.contains(value.toLowerCase(Locale.ROOT)))
                .toList();

        if (!invalidSources.isEmpty() || !invalidTargets.isEmpty()) {
            StringBuilder message = new StringBuilder("migration.yaml contains MTA technologies not accepted by the installed CLI.");
            if (!invalidSources.isEmpty()) message.append(" Invalid sources: ").append(invalidSources).append('.');
            if (!invalidTargets.isEmpty()) message.append(" Invalid targets: ").append(invalidTargets).append('.');
            message.append(" Run 'mta-cli rules list-sources' and 'mta-cli rules list-targets' to see the accepted MTA 8.3 values.");
            throw new IllegalArgumentException(message.toString());
        }
    }

    private Set<String> queryMtaTechnologies(
            Path mta,
            Path workingDirectory,
            String subcommand) throws IOException, InterruptedException {
        CommandResult result = runner.run(
                workingDirectory,
                List.of(mta.toString(), "rules", subcommand));
        if (!result.success()) {
            throw new IOException("Unable to query MTA technologies with 'mta-cli rules "
                    + subcommand + "' (exit code " + result.exitCode() + ").");
        }
        Set<String> values = parseTechnologyNames(result.output());
        if (values.isEmpty()) {
            throw new IOException("MTA returned no technology names for 'mta-cli rules "
                    + subcommand + "'.");
        }
        return values;
    }

    private static final Pattern ANSI_ESCAPE = Pattern.compile("\\u001B\\[[;\\d]*[ -/]*[@-~]");
    private static final Pattern TECHNOLOGY_TOKEN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:+-]*");

    /**
     * Extracts technology identifiers from MTA's human-readable list output. The parser is
     * intentionally tolerant of headings, tables, bullets, ANSI color, and whitespace so it
     * remains compatible with the MTA 8.3 CLI presentation format.
     */
    static Set<String> parseTechnologyNames(String output) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (output == null || output.isBlank()) return values;
        String clean = ANSI_ESCAPE.matcher(output).replaceAll("");
        var matcher = TECHNOLOGY_TOKEN.matcher(clean);
        while (matcher.find()) {
            values.add(matcher.group().toLowerCase(Locale.ROOT));
        }
        return values;
    }

    /**
     * Resolves the MTA analysis mode configured in {@code migration.yaml}.
     *
     * <p>The mode is intentionally YAML-only and is passed directly to MTA. The Workbench does
     * not re-scope MTA output after analysis. No CLI option overrides this setting.</p>
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

    /** Deletes a file or directory tree and propagates filesystem failures. */
    private static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) return;
        if (!Files.isDirectory(path)) {
            Files.deleteIfExists(path);
            return;
        }
        try (var paths = Files.walk(path)) {
            for (Path candidate : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(candidate);
            }
        }
    }

    private static void deleteQuietly(Path directory) {
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
     * Loads Workbench planning findings derived from an existing MTA assessment.
     *
     * <p>MTA's own output remains authoritative for analysis scope. Planning findings are only
     * deduplicated and cleaned of Workbench/build-generated paths.</p>
     *
     * @param project application root that owns the assessment
     * @param output MTA assessment directory
     * @param config migration configuration (retained as part of the workflow contract)
     * @return normalized planning findings
     * @throws IOException if findings cannot be read or parsed
     */
    public List<MigrationFinding> loadFindings(Path project, Path output, MigrationConfig config) throws IOException {
        Path projectRoot = canonicalProjectRoot(project);
        Path effectiveOutput = resolveOutputPath(project, projectRoot, output);
        Path planningFile = effectiveOutput.resolve("planning-findings.json");
        Path outputYaml = effectiveOutput.resolve("output.yaml");

        if (Files.exists(planningFile)) {
            return json.readValue(
                    planningFile.toFile(),
                    json.getTypeFactory().constructCollectionType(List.class, MigrationFinding.class));
        }
        return parser.parse(outputYaml);
    }

    /** Convenience loader for callers that do not need the configuration object. */
    public List<MigrationFinding> loadFindings(Path project, Path output) throws IOException {
        return loadFindings(project, output, null);
    }

    /**
     * Loads planning findings for callers that only possess an assessment directory. Workbench workflow code should use
     * {@link #loadFindings(Path, Path, MigrationConfig)}.
     */
    public List<MigrationFinding> loadFindings(Path output) throws IOException {
        Path planningFile = output.resolve("planning-findings.json");
        if (Files.exists(planningFile)) {
            return json.readValue(
                    planningFile.toFile(),
                    json.getTypeFactory().constructCollectionType(List.class, MigrationFinding.class));
        }
        return parser.parse(output.resolve("output.yaml"));
    }
}
