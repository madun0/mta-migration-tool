package com.jackson.migration.validation;

import com.jackson.migration.process.CommandResult;
import com.jackson.migration.process.MavenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Runs Maven compile/test gates and scans source files for forbidden legacy patterns after transformations.
 */
@ApplicationScoped
public class ValidationService {
    @Inject MavenService maven;

    private static final Set<String> TEXT_EXTENSIONS = Set.of(".java", ".xml", ".xhtml", ".js", ".properties", ".yaml", ".yml");

    /**
     * Runs the target application's Maven compile gate.
     *
     * @param project application project root
     * @throws IOException if Maven cannot be executed or compilation fails
     * @throws InterruptedException if Maven execution is interrupted
     */
    public void compile(Path project) throws IOException, InterruptedException {
        CommandResult result = maven.run(project, List.of("-DskipTests", "compile"), true);
        if (!result.success()) throw new IOException("Maven compile failed");
    }

    /**
     * Runs the target application's Maven test gate.
     *
     * @param project application project root
     * @throws IOException if Maven cannot be executed or tests fail
     * @throws InterruptedException if Maven execution is interrupted
     */
    public void test(Path project) throws IOException, InterruptedException {
        CommandResult result = maven.run(project, List.of("test"), true);
        if (!result.success()) throw new IOException("Maven tests failed");
    }

    /**
     * Scans text source/configuration files for legacy patterns declared forbidden by completed
     * migration families.
     *
     * @param project application project root
     * @param patterns literal patterns that should no longer remain
     * @return validation result containing every residual match
     * @throws IOException if the project tree cannot be traversed
     */
    public ValidationResult forbiddenPatterns(Path project, List<String> patterns) throws IOException {
        ValidationResult result = new ValidationResult();
        if (patterns == null || patterns.isEmpty()) return result;
        try (var stream = Files.walk(project)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> !path.toString().contains(".git") && !path.toString().contains(".migration") && !path.toString().contains("target"))
                    .filter(this::isText)
                    .forEach(path -> scan(path, project, patterns, result));
        }
        result.success = result.errors.isEmpty();
        return result;
    }

    private boolean isText(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return TEXT_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    /** Scans one UTF-8 text file and records line-oriented residual pattern matches. */
    private void scan(Path file, Path project, List<String> patterns, ValidationResult result) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                for (String pattern : patterns) {
                    if (!pattern.isBlank() && lines.get(i).contains(pattern)) {
                        result.errors.add(project.relativize(file) + ":" + (i + 1) + " contains " + pattern);
                    }
                }
            }
        } catch (IOException ignored) {
            result.warnings.add("Could not scan " + file);
        }
    }
}
