package com.jackson.migration.workflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Persisted state for a migration run. The model is intentionally small because the workbench is
 * a single-user CLI; detailed history is kept in checkpoint metadata and reports.
 */
public class MigrationState {

    /** Profile used for this migration run. */
    public String profile;

    /** Current workflow status such as NEW, IN_PROGRESS, REVIEW_REQUIRED, FAILED, or VERIFIED. */
    public String status = "NEW";

    /** Git HEAD captured before the first migration family executed. */
    public String baselineGitHead;

    /** Currently executing or paused migration phase. */
    public String currentPhase;

    /** Currently executing or paused migration family. */
    public String currentFamily;

    /** Most recently created workbench checkpoint identifier. */
    public String lastCheckpoint;

    /** Last orchestration error message when the workflow stopped with FAILED state. */
    public String lastError;

    /** Migration families successfully completed or intentionally recorded as manual work. */
    public List<String> completedFamilies = new ArrayList<>();

    /** Families that still require developer review before cleanup/final completion. */
    public List<String> manualReviewFamilies = new ArrayList<>();
}
