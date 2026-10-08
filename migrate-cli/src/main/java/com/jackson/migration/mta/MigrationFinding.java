package com.jackson.migration.mta;

/**
 * Normalized MTA finding used by the planner independently of the raw MTA report schema.
 *
 * @param ruleId MTA rule identifier used to map the finding to a migration family
 * @param message human-readable incident message
 * @param file source/configuration file associated with the incident
 * @param line one-based source line when available
 * @param category optional MTA category/severity grouping
 */
public record MigrationFinding(
        String ruleId,
        String message,
        String file,
        Integer line,
        String category) {
}
