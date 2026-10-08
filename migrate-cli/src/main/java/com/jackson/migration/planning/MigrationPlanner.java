package com.jackson.migration.planning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jackson.migration.catalog.MigrationCatalog;
import com.jackson.migration.mta.MigrationFinding;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps normalized MTA findings to migration families and produces an ordered plan for the selected
 * profile. A finding is assigned to the first matching family in catalog order so overlapping
 * built-in and custom rules become one developer-facing migration task rather than duplicate work.
 */
@ApplicationScoped
public class MigrationPlanner {
    private static final String UNMAPPED_FAMILY = "unmapped-mta-review";
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    /**
     * Builds an ordered migration plan for a profile by matching MTA rule identifiers to catalog
     * families and grouping selected work by phase.
     *
     * @param catalog migration knowledge catalog
     * @param profileName profile to apply
     * @param findings normalized MTA findings
     * @return ordered migration plan
     * @throws IllegalArgumentException if the requested profile does not exist
     */
    public MigrationPlan build(MigrationCatalog catalog, String profileName, List<MigrationFinding> findings) {
        MigrationCatalog.ProfileSpec profile = catalog.profiles.get(profileName);
        if (profile == null) throw new IllegalArgumentException("Unknown profile: " + profileName);

        Mapping mapping = assignFindings(catalog, findings);
        MigrationPlan plan = new MigrationPlan();
        plan.profile = profileName;

        for (String phaseName : profile.phases) {
            MigrationPlan.PhasePlan phase = new MigrationPlan.PhasePlan();
            phase.name = phaseName;

            if ("CLEANUP".equals(phaseName)) {
                addCleanupTasks(catalog, mapping.byFamily, phase);
            } else {
                addMigrationTasks(catalog, mapping.byFamily, phaseName, phase);
            }

            if ("MANUAL_REVIEW".equals(phaseName) && !mapping.unmapped.isEmpty()) {
                phase.tasks.add(unmappedTask(mapping.unmapped));
            }

            if (!phase.tasks.isEmpty()) plan.phases.add(phase);
        }
        return plan;
    }

    /** Assigns each finding to at most one family, honoring catalog order as precedence. */
    private Mapping assignFindings(MigrationCatalog catalog, List<MigrationFinding> findings) {
        Map<String, List<MigrationFinding>> byFamily = new LinkedHashMap<>();
        catalog.families.keySet().forEach(key -> byFamily.put(key, new ArrayList<>()));
        List<MigrationFinding> unmapped = new ArrayList<>();

        for (MigrationFinding finding : findings) {
            String familyKey = firstMatchingFamily(catalog, finding.ruleId());
            if (familyKey == null) {
                unmapped.add(finding);
            } else {
                byFamily.get(familyKey).add(finding);
            }
        }
        return new Mapping(byFamily, unmapped);
    }

    /** Returns the first family whose exact IDs or curated prefixes match the rule identifier. */
    private String firstMatchingFamily(MigrationCatalog catalog, String ruleId) {
        if (ruleId == null) return null;
        for (Map.Entry<String, MigrationCatalog.FamilySpec> entry : catalog.families.entrySet()) {
            MigrationCatalog.FamilySpec family = entry.getValue();
            if (family.mtaRuleIds != null && family.mtaRuleIds.contains(ruleId)) return entry.getKey();
            if (family.mtaRulePrefixes != null
                    && family.mtaRulePrefixes.stream().anyMatch(ruleId::startsWith)) return entry.getKey();
        }
        return null;
    }

    /** Adds normal migration tasks for the requested phase when findings or an always-on family select them. */
    private void addMigrationTasks(MigrationCatalog catalog, Map<String, List<MigrationFinding>> byFamily,
                                   String phaseName, MigrationPlan.PhasePlan phase) {
        for (Map.Entry<String, MigrationCatalog.FamilySpec> entry : catalog.families.entrySet()) {
            MigrationCatalog.FamilySpec family = entry.getValue();
            if (!phaseName.equals(family.phase)) continue;
            List<MigrationFinding> evidence = byFamily.getOrDefault(entry.getKey(), List.of());
            if (!family.always && evidence.isEmpty()) continue;
            phase.tasks.add(task(entry.getKey(), family, evidence, family.recipes));
        }
    }

    /** Adds post-review cleanup tasks for families that expose cleanup recipes. */
    private void addCleanupTasks(MigrationCatalog catalog, Map<String, List<MigrationFinding>> byFamily,
                                 MigrationPlan.PhasePlan phase) {
        for (Map.Entry<String, MigrationCatalog.FamilySpec> entry : catalog.families.entrySet()) {
            MigrationCatalog.FamilySpec family = entry.getValue();
            if (family.cleanupRecipes == null || family.cleanupRecipes.isEmpty()) continue;
            List<MigrationFinding> evidence = byFamily.getOrDefault(entry.getKey(), List.of());
            if (!family.always && evidence.isEmpty()) continue;
            MigrationPlan.Task task = task(entry.getKey() + "-cleanup", family, List.of(), family.cleanupRecipes);
            task.description = "Finalize reviewed " + entry.getKey() + " migration.";
            task.automation = "AUTOMATIC";
            task.triggeredBy = entry.getKey();
            task.compileAfterApply = true;
            task.cleanupRecipes.clear();
            phase.tasks.add(task);
        }
    }

    /** Creates a plan task and consolidates rule/file evidence for the selected family. */
    private MigrationPlan.Task task(String key, MigrationCatalog.FamilySpec family,
                                    List<MigrationFinding> evidence, List<String> recipes) {
        MigrationPlan.Task task = new MigrationPlan.Task();
        task.family = key;
        task.description = family.description;
        task.automation = family.automation;
        addEvidence(task, evidence);
        if (recipes != null) task.recipes.addAll(recipes);
        if (family.cleanupRecipes != null) task.cleanupRecipes.addAll(family.cleanupRecipes);
        task.guide = family.guide;
        task.compileAfterApply = family.compileAfterApply;
        if (family.reviewChecklist != null) task.reviewChecklist.addAll(family.reviewChecklist);
        if (family.forbiddenPatterns != null) task.forbiddenPatterns.addAll(family.forbiddenPatterns);
        return task;
    }

    /** Builds a visible safety-net task for findings that are not yet represented in the catalog. */
    private MigrationPlan.Task unmappedTask(List<MigrationFinding> findings) {
        MigrationPlan.Task task = new MigrationPlan.Task();
        task.family = UNMAPPED_FAMILY;
        task.description = "Review MTA findings that are not yet mapped to a Workbench migration family. "
                + "These findings are preserved so assessment coverage is never silently discarded.";
        task.automation = "MANUAL";
        addEvidence(task, findings);
        return task;
    }

    /** Copies distinct rule IDs and file locations from normalized findings into a task. */
    private void addEvidence(MigrationPlan.Task task, List<MigrationFinding> findings) {
        task.findingCount = findings.size();
        Set<String> rules = new LinkedHashSet<>();
        Set<String> files = new LinkedHashSet<>();
        for (MigrationFinding finding : findings) {
            if (finding.ruleId() != null && !finding.ruleId().isBlank()) rules.add(finding.ruleId());
            if (finding.file() != null && !finding.file().isBlank()) files.add(finding.file());
        }
        task.matchedRuleIds.addAll(rules);
        task.affectedFiles.addAll(files);
    }

    /**
     * Persists a migration plan as JSON.
     *
     * @param plan plan to persist
     * @param file destination path
     * @throws IOException if the plan cannot be written
     */
    public void save(MigrationPlan plan, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        json.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), plan);
    }

    /**
     * Loads a previously generated migration plan.
     *
     * @param file plan JSON file
     * @return deserialized migration plan
     * @throws IOException if the plan cannot be read or parsed
     */
    public MigrationPlan load(Path file) throws IOException {
        return json.readValue(file.toFile(), MigrationPlan.class);
    }

    /** Immutable result of assigning normalized findings to migration families. */
    private record Mapping(Map<String, List<MigrationFinding>> byFamily, List<MigrationFinding> unmapped) { }
}
