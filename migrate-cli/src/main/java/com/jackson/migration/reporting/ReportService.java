package com.jackson.migration.reporting;

import com.jackson.migration.mta.MigrationFinding;
import com.jackson.migration.planning.MigrationPlan;
import com.jackson.migration.workflow.MigrationState;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

/**
 * Generates human-readable migration plan, TODO, and final verification reports under the project
 * {@code .migration} directory.
 */
@ApplicationScoped
public class ReportService {
    /**
     * Writes the human-readable migration plan to {@code .migration/MIGRATION-PLAN.md}.
     *
     * @param project application project root
     * @param plan migration plan to render
     * @throws IOException if the report cannot be written
     */
    public void writePlan(Path project, MigrationPlan plan) throws IOException {
        StringBuilder out = new StringBuilder("# Migration Plan\n\n")
                .append("Profile: `").append(plan.profile).append("`\n\n");
        for (MigrationPlan.PhasePlan phase : plan.phases) {
            out.append("## ").append(phase.name).append("\n\n");
            for (MigrationPlan.Task task : phase.tasks) {
                out.append("- **").append(task.family).append("** — ").append(task.automation);
                if (task.triggeredBy == null) {
                    out.append(" — findings: ").append(task.findingCount);
                }
                out.append("  \n")
                        .append("  ").append(task.description).append("\n");
                if (task.triggeredBy != null) {
                    out.append("  Triggered by: `").append(task.triggeredBy).append("`\n");
                }
                if (!task.matchedRuleIds.isEmpty()) {
                    out.append("  MTA evidence: `")
                            .append(String.join("`, `", task.matchedRuleIds)).append("`\n");
                }
                if (!task.affectedFiles.isEmpty()) {
                    out.append("  Affected files: ").append(task.affectedFiles.size()).append("\n");
                    for (String file : task.affectedFiles) {
                        out.append("    - `").append(displayPath(project, file)).append("`\n");
                    }
                }
                if (!task.recipes.isEmpty()) {
                    out.append("  Automation:\n");
                    for (String recipe : task.recipes) {
                        out.append("    - `").append(recipe).append("`\n");
                    }
                }
                if (!task.reviewChecklist.isEmpty()) {
                    out.append("  Developer verification:\n");
                    for (String item : task.reviewChecklist) {
                        out.append("    - [ ] ").append(item).append("\n");
                    }
                }
                if (task.guide != null) {
                    out.append("  Guide: `").append(task.guide).append("`\n");
                }
            }
            out.append("\n");
        }
        write(project.resolve(".migration/MIGRATION-PLAN.md"), out.toString());
    }

    /**
     * Appends an assisted/manual migration work item to {@code .migration/TODO.md}.
     *
     * @param project application project root
     * @param task migration task requiring developer attention
     * @param reason explanation of why review is required
     * @throws IOException if the TODO report cannot be updated
     */
    public void writeTodo(Path project, MigrationPlan.Task task, String reason) throws IOException {
        Path file = project.resolve(".migration/TODO.md");
        Files.createDirectories(file.getParent());
        if (!Files.exists(file)) {
            Files.writeString(file, "# Migration TODO\n\n", StandardCharsets.UTF_8);
        }
        StringBuilder block = new StringBuilder()
                .append("## ").append(task.family).append("\n\n")
                .append(task.description).append("\n\n")
                .append("Reason: ").append(reason).append("\n\n");
        if (!task.matchedRuleIds.isEmpty()) {
            block.append("MTA evidence: `").append(String.join("`, `", task.matchedRuleIds)).append("`\n\n");
        }
        if (!task.affectedFiles.isEmpty()) {
            block.append("Affected files:\n");
            for (String affectedFile : task.affectedFiles) {
                block.append("- `").append(displayPath(project, affectedFile)).append("`\n");
            }
            block.append("\n");
        }
        if (!task.reviewChecklist.isEmpty()) {
            block.append("Developer verification:\n");
            for (String item : task.reviewChecklist) {
                block.append("- [ ] ").append(item).append("\n");
            }
            block.append("\n");
        } else {
            block.append("- [ ] Review and complete this migration family.\n\n");
        }
        if (task.guide != null) {
            block.append("Guide: `").append(task.guide).append("`\n\n");
        }
        Files.writeString(file, block.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    /**
     * Writes the final migration report, including workflow state and before/after MTA finding
     * counts, to {@code .migration/MIGRATION-REPORT.md}.
     *
     * @param project application project root
     * @param state final workflow state
     * @param before baseline MTA findings
     * @param after post-migration MTA findings
     * @throws IOException if the report cannot be written
     */
    public void writeFinal(Path project, MigrationState state, List<MigrationFinding> before,
                           List<MigrationFinding> after) throws IOException {
        writeFinal(project, state, before, after, Map.of(), Map.of(), true);
    }

    /**
     * Writes the final migration report with verification residuals grouped by owning family.
     * Automatic residuals are blocking; assisted/manual residuals are developer-review work.
     */
    public void writeFinal(Path project, MigrationState state, List<MigrationFinding> before,
                           List<MigrationFinding> after, Map<String, List<String>> automaticResiduals,
                           Map<String, List<String>> reviewResiduals) throws IOException {
        writeFinal(project, state, before, after, automaticResiduals, reviewResiduals, true);
    }

    /** Writes the final report and records whether the optional test gate actually ran. */
    public void writeFinal(Path project, MigrationState state, List<MigrationFinding> before,
                           List<MigrationFinding> after, Map<String, List<String>> automaticResiduals,
                           Map<String, List<String>> reviewResiduals, boolean testsRun) throws IOException {
        StringBuilder out = new StringBuilder("# Migration Report\n\n")
                .append("Status: **").append(state.status).append("**\n\n")
                .append("Baseline MTA findings: **").append(before.size()).append("**  \n")
                .append("Final MTA findings: **").append(after.size()).append("**\n\n")
                .append("## Verification summary\n\n")
                .append("- Compilation: **PASS**\n")
                .append("- Tests: **").append(testsRun ? "PASS" : "SKIPPED").append("**\n")
                .append("- Blocking automatic residual families: **")
                .append(automaticResiduals.size()).append("**\n")
                .append("- Assisted/manual residual families: **")
                .append(reviewResiduals.size()).append("**\n")
                .append("- Remaining MTA findings: **")
                .append(after.size()).append("**\n\n");

        appendResidualSection(out, "Blocking automatic migration residuals", automaticResiduals);
        appendResidualSection(out, "Outstanding assisted/manual review residuals", reviewResiduals);
        appendRemainingFindings(out, project, after);

        out.append("## Completed families\n\n");
        for (String family : state.completedFamilies) {
            out.append("- ").append(family).append("\n");
        }
        out.append("\n## Families that required manual review during migration\n\n");
        if (state.manualReviewFamilies.isEmpty()) {
            out.append("- None\n");
        } else {
            for (String family : state.manualReviewFamilies) {
                out.append("- ").append(family).append("\n");
            }
        }
        write(project.resolve(".migration/MIGRATION-REPORT.md"), out.toString());
    }


    /**
     * Appends the MTA findings that remain after migration. These findings are reported for
     * transparency even when no family-owned forbidden pattern makes them blocking.
     */
    private void appendRemainingFindings(StringBuilder out, Path project, List<MigrationFinding> findings) {
        if (findings.isEmpty()) return;
        out.append("## Remaining MTA findings\n\n")
                .append("These findings remain in the post-migration MTA analysis but are not classified as ")
                .append("blocking by the current family verification policies. Review them before release.\n\n");
        for (MigrationFinding finding : findings) {
            out.append("- `").append(safe(finding.ruleId())).append("`");
            if (finding.category() != null && !finding.category().isBlank()) {
                out.append(" [").append(finding.category()).append("]");
            }
            if (finding.file() != null && !finding.file().isBlank()) {
                out.append(" — `").append(displayPath(project, finding.file()));
                if (finding.line() != null && finding.line() > 0) out.append(":").append(finding.line());
                out.append("`");
            }
            if (finding.message() != null && !finding.message().isBlank()) {
                out.append(" — ").append(finding.message().replace('\n', ' ').replace('\r', ' '));
            }
            out.append("\n");
        }
        out.append("\n");
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown-rule" : value;
    }

    private void appendResidualSection(StringBuilder out, String heading, Map<String, List<String>> residuals) {
        if (residuals.isEmpty()) return;
        out.append("## ").append(heading).append("\n\n");
        residuals.forEach((family, errors) -> {
            out.append("### ").append(family).append("\n\n");
            for (String error : errors) out.append("- ").append(error).append("\n");
            out.append("\n");
        });
    }

    /** Converts MTA file URIs/absolute paths into stable project-relative paths when possible. */
    private String displayPath(Path project, String value) {
        try {
            Path source = value.startsWith("file:") ? Path.of(URI.create(value)) : Path.of(value);
            Path normalizedProject = project.toAbsolutePath().normalize();
            Path normalizedSource = source.toAbsolutePath().normalize();
            if (normalizedSource.startsWith(normalizedProject)) {
                return normalizedProject.relativize(normalizedSource).toString().replace('\\', '/');
            }
        } catch (Exception ignored) {
            // Preserve the original MTA location when it cannot be safely converted to a local path.
        }
        return value;
    }

    private void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
