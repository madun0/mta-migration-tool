package com.jackson.migration.mta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.yaml.snakeyaml.LoaderOptions;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Pattern;
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
     * Removes findings that point at Workbench-generated or build-generated files. Dependency
     * findings are intentionally preserved here and are scoped later according to MTA mode.
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

    /**
     * Keeps only findings that belong to the selected application root.
     *
     * <p>This is the {@code source-only} finding boundary. Full mode intentionally does not apply
     * this filter because dependency, parent-model, and reactor-module incidents are part of the
     * requested result. File-less/global findings are retained because they cannot be scoped to a
     * source path.</p>
     *
     * @param findings normalized MTA findings
     * @param projectRoot canonical application project root
     * @return findings whose source path is inside {@code projectRoot}, plus file-less findings
     */
    public List<MigrationFinding> filterToProjectRoot(List<MigrationFinding> findings, Path projectRoot) {
        if (findings == null || findings.isEmpty()) return List.of();
        String root = normalizePathKey(projectRoot.toAbsolutePath().normalize().toString());
        return findings.stream()
                .filter(finding -> finding.file() == null
                        || finding.file().isBlank()
                        || isWithinProjectPath(root, finding.file()))
                .toList();
    }

    /**
     * Tests whether an MTA file reference is inside an application root.
     *
     * <p>The comparison understands native Windows paths, {@code file:///C:/...} URIs, Git Bash
     * {@code /c/...} paths, and POSIX paths. Windows drive paths are compared case-insensitively
     * and path-segment boundaries prevent a project such as {@code C:/work/app} from accepting
     * {@code C:/work/app-other}.</p>
     */
    static boolean isWithinProjectPath(String projectRoot, String findingFile) {
        String root = normalizePathKey(projectRoot);
        String file = normalizePathKey(findingFile);
        if (root.isBlank() || file.isBlank()) return false;

        if (!isAbsolutePathKey(file)) {
            file = normalizePathKey(root + "/" + file);
        }

        return file.equals(root) || file.startsWith(root + "/");
    }

    /** Converts URI/native/Git-Bash path spellings into a stable comparison key. */
    static String normalizePathKey(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String value = raw.trim();

        if (value.regionMatches(true, 0, "file:", 0, 5)) {
            try {
                URI uri = URI.create(value);
                if ("file".equalsIgnoreCase(uri.getScheme())) {
                    String authority = uri.getAuthority();
                    String path = uri.getPath();
                    value = authority == null || authority.isBlank()
                            ? path
                            : "//" + authority + path;
                }
            } catch (IllegalArgumentException ignored) {
                // Fall through to conservative string normalization.
            }
        }

        value = value.replace('\\', '/');
        value = value.replaceAll("/{2,}", "/");

        // file:///C:/... becomes /C:/... after URI decoding.
        if (value.matches("^/[A-Za-z]:/.*")) {
            value = value.substring(1);
        }

        // Git Bash /c/work/app -> C:/work/app.
        if (value.matches("^/[A-Za-z]/.*")) {
            value = Character.toUpperCase(value.charAt(1)) + ":" + value.substring(2);
        }

        while (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }

        if (WINDOWS_DRIVE.matcher(value).matches()) {
            value = value.toLowerCase(Locale.ROOT);
        }
        return value;
    }

    private static final Pattern WINDOWS_DRIVE = Pattern.compile("^[A-Za-z]:/.*");

    private static boolean isAbsolutePathKey(String value) {
        return value.startsWith("/") || WINDOWS_DRIVE.matcher(value).matches();
    }

    /** Returns true when a finding path belongs to Workbench- or build-generated metadata. */
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
