package com.jackson.migration.mta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * Parses MTA {@code output.yaml} and normalizes rule incidents into deduplicated migration findings.
 *
 * <p>MTA analyzer output represents matched rules primarily as entries beneath a
 * {@code violations} object, where the map key is the rule ID. Older/custom output can also
 * expose a {@code ruleID} field directly. This parser supports both shapes so that planning does
 * not silently lose valid MTA findings.</p>
 */
@ApplicationScoped
public class MtaOutputParser {
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    /**
     * Parses an MTA {@code output.yaml} report and recursively extracts deduplicated incidents.
     *
     * @param outputYaml path to the MTA YAML report
     * @return normalized findings; an empty list when the report does not exist
     * @throws IOException if the report exists but cannot be parsed
     */
    public List<MigrationFinding> parse(Path outputYaml) throws IOException {
        if (!Files.exists(outputYaml)) return List.of();
        JsonNode root = yaml.readTree(outputYaml.toFile());
        List<MigrationFinding> findings = new ArrayList<>();
        scan(root, findings, new HashSet<>());
        return filterGeneratedFindings(findings);
    }

    /** Recursively visits arbitrary MTA report nodes and extracts rule incidents. */
    private void scan(JsonNode node, List<MigrationFinding> out, Set<String> seen) {
        if (node == null) return;
        if (node.isObject()) {
            JsonNode violations = node.get("violations");
            if (violations != null && violations.isObject()) {
                Set<Map.Entry<String, JsonNode>> properties = violations.properties();
                Iterator<Map.Entry<String, JsonNode>> iterator = properties.iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    extractViolation(entry.getKey(), entry.getValue(), out, seen);
                }
            }

            String ruleId = text(node, "ruleID", "ruleId", "rule");
            if (ruleId != null) {
                extractViolation(ruleId, node, out, seen);
            }

            Set<Map.Entry<String, JsonNode>> properties = node.properties();
            Iterator<Map.Entry<String, JsonNode>> iterator = properties.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> field = iterator.next();
                if (!"violations".equals(field.getKey())) {
                    scan(field.getValue(), out, seen);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) scan(child, out, seen);
        }
    }

    /** Extracts one MTA violation node whose rule ID may have come from a map key. */
    private void extractViolation(String ruleId, JsonNode rule, List<MigrationFinding> out, Set<String> seen) {
        JsonNode incidents = rule.get("incidents");
        if (incidents != null && incidents.isArray()) {
            for (JsonNode incident : incidents) add(out, seen, finding(ruleId, rule, incident));
        } else {
            // Some custom/legacy output represents the rule node itself as the incident.
            add(out, seen, finding(ruleId, rule, rule));
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
     * Removes findings that point at Workbench-generated or build-generated files rather than
     * application source. MTA output can contain embedded source snippets, and when the assessment
     * directory lives beneath the analyzed project a later assessment can rediscover those snippets
     * as if they were application files.
     *
     * @param findings findings to filter
     * @return a new list containing only application findings
     */
    public List<MigrationFinding> filterGeneratedFindings(List<MigrationFinding> findings) {
        if (findings == null || findings.isEmpty()) return List.of();
        return findings.stream()
                .filter(finding -> !isGeneratedPath(finding.file()))
                .toList();
    }

    /** Returns true when a finding path belongs to generated Workbench/build metadata. */
    private boolean isGeneratedPath(String file) {
        if (file == null || file.isBlank()) return false;
        String normalized = file.replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.contains("/.migration/")
                || normalized.contains("/target/")
                || normalized.contains("/.git/");
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
