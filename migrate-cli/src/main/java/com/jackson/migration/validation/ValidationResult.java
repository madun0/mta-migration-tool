package com.jackson.migration.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects errors and warnings produced by non-build migration validation checks.
 */
public class ValidationResult {

    /** Overall validation state; set to {@code false} when one or more blocking errors exist. */
    public boolean success = true;

    /** Blocking validation failures. */
    public List<String> errors = new ArrayList<>();

    /** Non-blocking observations that should be reviewed. */
    public List<String> warnings = new ArrayList<>();
}
