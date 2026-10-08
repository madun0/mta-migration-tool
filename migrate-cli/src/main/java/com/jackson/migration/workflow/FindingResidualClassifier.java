package com.jackson.migration.workflow;

import com.jackson.migration.catalog.MigrationCatalog;
import com.jackson.migration.mta.MigrationFinding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Classifies post-migration MTA findings according to the owning Workbench family's automation contract. */
public final class FindingResidualClassifier {
    private FindingResidualClassifier() {}

    /** Result of classifying post-migration findings into blocking automatic or review residuals. */
    public record Result(Map<String, List<String>> automatic, Map<String, List<String>> review) {}

    /**
     * Maps findings to catalog families. AUTOMATIC families are blocking; ASSISTED/MANUAL families
     * remain review work regardless of the raw MTA category. Unmapped findings are review-only.
     */
    public static Result classify(MigrationCatalog catalog, List<MigrationFinding> findings) {
        Map<String, List<String>> automatic = new LinkedHashMap<>();
        Map<String, List<String>> review = new LinkedHashMap<>();
        for (MigrationFinding finding : findings) {
            String family = owner(catalog, finding.ruleId());
            String detail = describe(finding);
            if (family == null) {
                review.computeIfAbsent("unmapped-mta-findings", ignored -> new ArrayList<>()).add(detail);
                continue;
            }
            MigrationCatalog.FamilySpec spec = catalog.families.get(family);
            Map<String, List<String>> target = "AUTOMATIC".equalsIgnoreCase(spec.automation) ? automatic : review;
            target.computeIfAbsent(family, ignored -> new ArrayList<>()).add(detail);
        }
        return new Result(automatic, review);
    }

    private static String owner(MigrationCatalog catalog, String ruleId) {
        if (ruleId == null) return null;
        for (Map.Entry<String, MigrationCatalog.FamilySpec> entry : catalog.families.entrySet()) {
            MigrationCatalog.FamilySpec spec = entry.getValue();
            if (spec.mtaRuleIds != null && spec.mtaRuleIds.contains(ruleId)) return entry.getKey();
            if (spec.mtaRulePrefixes != null) {
                for (String prefix : spec.mtaRulePrefixes) {
                    if (prefix != null && !prefix.isBlank() && ruleId.startsWith(prefix)) return entry.getKey();
                }
            }
        }
        return null;
    }

    private static String describe(MigrationFinding finding) {
        StringBuilder out = new StringBuilder(finding.ruleId() == null ? "unknown-rule" : finding.ruleId());
        if (finding.file() != null && !finding.file().isBlank()) {
            out.append(" — ").append(finding.file());
            if (finding.line() != null && finding.line() > 0) out.append(":").append(finding.line());
        }
        if (finding.message() != null && !finding.message().isBlank()) {
            out.append(" — ").append(finding.message().replace('\n', ' ').replace('\r', ' '));
        }
        return out.toString();
    }
}
