package com.jackson.migration.workflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Persisted metadata describing a Git-backed migration checkpoint and the workflow state that
 * existed before a migration family ran.
 */
public class Checkpoint {

    /** Sequential workbench checkpoint identifier, for example {@code cp-004}. */
    public String id;

    /** Migration phase active when the checkpoint was created. */
    public String phase;

    /** Migration family about to run when the checkpoint was created. */
    public String family;

    /** Git HEAD fixed as the migration baseline. */
    public String baselineGitHead;

    /** Git object produced by {@code git stash create} representing pre-family working state. */
    public String snapshotCommit;

    /** SHA-256 hash of the Git diff after the family completed. */
    public String postDiffSha256;

    /** Completed-family list captured before the family executed. */
    public List<String> completedFamiliesBefore = new ArrayList<>();

    /** Manual-review backlog captured before the family executed. */
    public List<String> manualReviewFamiliesBefore = new ArrayList<>();
}
