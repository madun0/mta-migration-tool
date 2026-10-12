package com.jackson.migration.mta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.yaml.snakeyaml.LoaderOptions;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * Parses MTA {@code output.yaml} into exact report incidents plus normalized planning findings.
 *
 * <p>MTA 8.3 analyzer output represents matched rules as entries beneath a
 * {@code violations} object, where the map key is the rule ID. The Workbench treats that
 * structure as authoritative so its findings stay aligned with the MTA report.</p>
 */
@ApplicationScoped
public class MtaOutputParser {
    /**
     * Maximum size accepted for one MTA YAML document, measured in Unicode code points.
     *
     * <p>SnakeYAML defaults to 3 MiB, which is too small for realistic MTA reports because
     * {@code output.yaml} embeds source snippets for thousands of analyzer rules. The Workbench
     * raises that bounded limit to 64 MiB for trusted, locally generated MTA assessment output
     * while retaining SnakeYAML's document-size protection.</p>
     */
    static final int MTA_YAML_CODE_POINT_LIMIT = 64 * 1024 * 1024;

    private final ObjectMapper yaml = createYamlMapper();

    /** Builds a YAML mapper configured for large, locally generated MTA assessment reports. */
    private static ObjectMapper createYamlMapper() {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit(MTA_YAML_CODE_POINT_LIMIT);
        YAMLFactory factory = YAMLFactory.builder()
                .loaderOptions(loaderOptions)
                .build();
        return new ObjectMapper(factory).findAndRegisterModules();
    }

    /**
     * Parses an MTA {@code output.yaml} report and returns normalized planning findings.
     *
     * @param outputYaml path to the MTA YAML report
     * @return normalized findings; an empty list when the report does not exist
     * @throws IOException if the report exists but cannot be parsed
     */
    public List<MigrationFinding> parse(Path outputYaml) throws IOException {
        return parseReport(outputYaml).planningFindings();
    }

    /**
     * Parses MTA insight incidents separately from migration issues. Insights are useful report
     * context but are deliberately not fed into Workbench planning/residual classification.
     */
    public List<MigrationFinding> parseInsights(Path outputYaml) throws IOException {
        return parseReport(outputYaml).insights();
    }

    /**
     * Parses one MTA report once and returns both normalized Workbench data and the raw MTA
     * violation-incident count.
     *
     * <p>The user-facing {@code MTA findings} number is deliberately the number of entries under
     * {@code violations.<rule>.incidents[]} in the MTA report. Normalized findings remain
     * deduplicated for planning, so their list size is not used as the MTA report count.</p>
     *
     * @param outputYaml path to MTA {@code output.yaml}
     * @return parsed report data; empty when the report does not exist
     * @throws IOException if the report exists but cannot be parsed
     */
    public ParsedReport parseReport(Path outputYaml) throws IOException {
        if (!Files.exists(outputYaml)) {
            return new ParsedReport(List.of(), List.of(), List.of(), 0);
        }
        JsonNode root = yaml.readTree(outputYaml.toFile());

        // Keep one record for every MTA violation incident exactly as output.yaml reports it.
        // This list is the source of truth for findings.json and the user-facing MTA count.
        List<MigrationFinding> incidents = new ArrayList<>();
        scanViolationIncidents(root, incidents);

        // Planning is intentionally separate: generated-file noise is removed and duplicate
        // incidents are collapsed so one repeated analyzer record cannot multiply migration work.
        List<MigrationFinding> planningFindings = deduplicate(filterGeneratedFindings(incidents));

        List<MigrationFinding> insights = new ArrayList<>();
        scanInsights(root, insights, new HashSet<>());

        return new ParsedReport(
                List.copyOf(incidents),
                List.copyOf(planningFindings),
                List.copyOf(filterGeneratedFindings(insights)),
                incidents.size());
    }

    /** Parsed MTA report data used by assessment without reparsing a potentially large YAML file. */
    public record ParsedReport(
            List<MigrationFinding> incidents,
            List<MigrationFinding> planningFindings,
            List<MigrationFinding> insights,
            int violationIncidentCount) {
    }

    /**
     * Recursively extracts exactly the incidents reported beneath MTA 8.3 {@code violations}
     * maps. No Workbench deduplication or path scoping is applied here.
     */
    private void scanViolationIncidents(JsonNode node, List<MigrationFinding> out) {
        if (node == null) return;
        if (node.isObject()) {
            JsonNode violations = node.get("violations");
            if (violations != null && violations.isObject()) {
                for (Map.Entry<String, JsonNode> entry : violations.properties()) {
                    JsonNode rule = entry.getValue();
                    JsonNode ruleIncidents = rule.get("incidents");
                    if (ruleIncidents != null && ruleIncidents.isArray()) {
                        for (JsonNode incident : ruleIncidents) {
                            out.add(finding(entry.getKey(), rule, incident));
                        }
                    }
                }
            }
            for (Map.Entry<String, JsonNode> field : node.properties()) {
                if (!"violations".equals(field.getKey())) {
                    scanViolationIncidents(field.getValue(), out);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) scanViolationIncidents(child, out);
        }
    }

    /** Recursively extracts entries beneath MTA's insights maps. */
    private void scanInsights(JsonNode node, List<MigrationFinding> out, Set<String> seen) {
        if (node == null) return;
        if (node.isObject()) {
            JsonNode insights = node.get("insights");
            if (insights != null && insights.isObject()) {
                for (Map.Entry<String, JsonNode> entry : insights.properties()) {
                    JsonNode rule = entry.getValue();
                    JsonNode incidents = rule.get("incidents");
                    if (incidents != null && incidents.isArray()) {
                        for (JsonNode incident : incidents) {
                            MigrationFinding finding = finding(entry.getKey(), rule, incident);
                            add(out, seen, new MigrationFinding(
                                    finding.ruleId(), finding.message(), finding.file(), finding.line(), "insight"));
                        }
                    }
                }
            }
            for (Map.Entry<String, JsonNode> field : node.properties()) {
                if (!"insights".equals(field.getKey())) scanInsights(field.getValue(), out, seen);
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) scanInsights(child, out, seen);
        }
    }

    /** Builds one normalized finding from a rule node and an incident node. */
    private MigrationFinding finding(String ruleId, JsonNode rule, JsonNode incident) {
        String message = text(incident, "message", "description");
        if (message == null) message = text(rule, "message", "description");
        String file = text(incident, "uri", "file", "path");
        Integer line = integer(incident, "lineNumber", "line");
        String category = text(rule, "category");
        return new MigrationFinding(ruleId, message == null ? "" : message, file == null ? "" : file, line, category == null ? "" : category);
    }

    /**
     * Removes findings that point at Workbench-generated or build-generated files. Dependency findings are intentionally preserved here; MTA itself determines analysis scope.
     * MTA output can contain embedded source snippets, and when the assessment
     * directory lives beneath the analyzed project a later assessment can rediscover those snippets
     * as if they were application files.
     *
     * @param findings findings to filter
     * @return a new list with Workbench/build-generated findings removed
     */
    public List<MigrationFinding> filterGeneratedFindings(List<MigrationFinding> findings) {
        if (findings == null || findings.isEmpty()) return List.of();
        return findings.stream()
                .filter(finding -> !isGeneratedPath(finding.file()))
                .toList();
    }

    /** Returns true when a finding path belongs to Workbench- or build-generated metadata. */
    private boolean isGeneratedPath(String file) {
        if (file == null || file.isBlank()) return false;
        String normalized = file.replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.contains("/.migration/")
                || normalized.contains("/target/")
                || normalized.contains("/.git/");
    }

    /** Deduplicates findings by rule/file/line/message while preserving first-seen order. */
    private List<MigrationFinding> deduplicate(List<MigrationFinding> findings) {
        List<MigrationFinding> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (MigrationFinding finding : findings) add(out, seen, finding);
        return out;
    }

    /** Adds a finding only when its rule/file/line/message identity has not been seen. */
    private void add(List<MigrationFinding> out, Set<String> seen, MigrationFinding f) {
        String key = f.ruleId() + "|" + f.file() + "|" + f.line() + "|" + f.message();
        if (seen.add(key)) out.add(f);
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isValueNode()) return value.asText();
        }
        return null;
    }

    private Integer integer(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.canConvertToInt()) return value.asInt();
        }
        return null;
    }
}
