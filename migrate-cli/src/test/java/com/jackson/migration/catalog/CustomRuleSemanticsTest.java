package com.jackson.migration.catalog;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protects custom MTA rule severity and InputMask matching semantics. */
class CustomRuleSemanticsTest {
    @Test
    void reviewRulesDoNotTreatValidStaticInputMaskAsFindingOrScheduleTimelineAsBlocking() throws Exception {
        Path root = Path.of("..").toAbsolutePath().normalize();
        if (!Files.exists(root.resolve("rules/migration/primefaces.yaml"))) root = Path.of(".").toAbsolutePath();
        String primefaces = Files.readString(root.resolve("rules/migration/primefaces.yaml"));
        String extra = Files.readString(root.resolve("rules/migration/workbench-extra.yaml"));

        int inputStart = primefaces.indexOf("ruleID: primefaces-input-mask-review-0001");
        int inputEnd = primefaces.indexOf("- ruleID:", inputStart + 10);
        String inputRule = primefaces.substring(inputStart, inputEnd);
        assertFalse(inputRule.contains("pattern: \"<p:inputMask\""));
        assertTrue(inputRule.contains("#\\\\{"));
        assertTrue(inputRule.contains("validateMask"));

        String scheduleRule = block(extra, "primefaces-schedule-0001");
        String timelineRule = block(extra, "primefaces-timeline-0001");
        assertTrue(scheduleRule.contains("category: potential"));
        assertTrue(timelineRule.contains("category: potential"));

        // Post-migration Java types remain valid in PrimeFaces 16. Do not flag
        // mere presence of modern Schedule/Timeline model classes.
        assertFalse(scheduleRule.contains("DefaultScheduleEvent|LazyScheduleModel"));
        assertFalse(timelineRule.contains("TimelineEvent|TimelineModel"));

        // Java findings must be limited to legacy migration signatures.
        assertTrue(scheduleRule.contains("new\\\\s+DefaultScheduleEvent"));
        assertTrue(scheduleRule.contains("loadEvents\\\\s*"));
        assertTrue(timelineRule.contains("new\\\\s+TimelineEvent"));
    }

    @Test
    void reservedMtaSourceAndTargetLabelsUseNativeMtaTechnologies() throws Exception {
        Path root = Path.of("..").toAbsolutePath().normalize();
        if (!Files.exists(root.resolve("rules/migration/primefaces.yaml"))) root = Path.of(".").toAbsolutePath();

        String combined = Files.readString(root.resolve("rules/migration/faces4.yaml"))
                + Files.readString(root.resolve("rules/migration/primefaces.yaml"))
                + Files.readString(root.resolve("rules/migration/workbench-extra.yaml"));

        assertFalse(combined.contains("konveyor.io/source=jsf2"));
        assertFalse(combined.contains("konveyor.io/source=primefaces6"));
        assertFalse(combined.contains("konveyor.io/target=faces4"));
        assertFalse(combined.contains("konveyor.io/target=primefaces16"));
        assertTrue(combined.contains("konveyor.io/source=eap7"));
        assertTrue(combined.contains("konveyor.io/target=eap8"));
        assertTrue(combined.contains("workbench.io/source-framework=jsf2"));
        assertTrue(combined.contains("workbench.io/source-framework=primefaces6"));
    }

    private String block(String yaml, String ruleId) {
        int start = yaml.indexOf("ruleID: " + ruleId);
        int end = yaml.indexOf("- ruleID:", start + 10);
        return yaml.substring(start, end < 0 ? yaml.length() : end);
    }
}
