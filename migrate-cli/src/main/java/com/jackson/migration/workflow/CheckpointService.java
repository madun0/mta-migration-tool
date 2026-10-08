package com.jackson.migration.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Creates, completes, loads, and safely rolls back Git-backed checkpoints for individual migration families.
 */
@ApplicationScoped
public class CheckpointService {
    @Inject GitService git;
    @Inject StateStore states;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    /**
     * Creates a checkpoint before a migration family runs. The checkpoint records the baseline Git
     * HEAD, an object-only Git stash snapshot, and the workflow lists required for state rollback.
     *
     * @param project application project root
     * @param state current workflow state
     * @param phase phase containing the family
     * @param family migration family about to execute
     * @return persisted checkpoint metadata
     * @throws IOException if checkpoint metadata or Git state cannot be read/written
     * @throws InterruptedException if a Git operation is interrupted
     */
    public Checkpoint create(Path project, MigrationState state, String phase, String family)
            throws IOException, InterruptedException {
        String currentHead = git.head(project);
        if (!currentHead.equals(state.baselineGitHead)) {
            throw new IllegalStateException("Git HEAD changed since migration started. Refusing checkpoint creation.");
        }
        Path dir = project.resolve(".migration/checkpoints");
        Files.createDirectories(dir);
        int next;
        try (var entries = Files.list(dir)) {
            next = entries.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(s -> s.matches("cp-\\d+"))
                    .mapToInt(s -> Integer.parseInt(s.substring(3)))
                    .max().orElse(0) + 1;
        }

        Checkpoint cp = new Checkpoint();
        cp.id = "cp-%03d".formatted(next);
        cp.phase = phase;
        cp.family = family;
        cp.baselineGitHead = state.baselineGitHead;
        cp.snapshotCommit = git.stashCreate(project, "migrate " + cp.id + " before " + family);
        cp.completedFamiliesBefore.addAll(state.completedFamilies);
        cp.manualReviewFamiliesBefore.addAll(state.manualReviewFamilies);
        save(project, cp);
        state.lastCheckpoint = cp.id;
        states.save(project, state);
        return cp;
    }

    /**
     * Marks a checkpoint complete by recording the SHA-256 hash of the resulting Git diff.
     *
     * @param project application project root
     * @param cp checkpoint to finalize
     * @throws IOException if the diff or checkpoint cannot be persisted
     * @throws InterruptedException if Git execution is interrupted
     */
    public void complete(Path project, Checkpoint cp) throws IOException, InterruptedException {
        cp.postDiffSha256 = git.diffHash(project);
        save(project, cp);
    }

    /**
     * Loads checkpoint metadata by identifier.
     *
     * @param project application project root
     * @param id checkpoint identifier such as {@code cp-004}
     * @return checkpoint metadata
     * @throws IOException if the checkpoint cannot be read
     */
    public Checkpoint load(Path project, String id) throws IOException {
        return json.readValue(project.resolve(".migration/checkpoints").resolve(id).resolve("checkpoint.json").toFile(),
                Checkpoint.class);
    }

    /**
     * Finds the lexically latest sequential workbench checkpoint.
     *
     * @param project application project root
     * @return latest checkpoint identifier
     * @throws IOException if no checkpoint exists or the checkpoint directory cannot be read
     */
    public String latestId(Path project) throws IOException {
        Path dir = project.resolve(".migration/checkpoints");
        if (!Files.exists(dir)) throw new IOException("No checkpoints exist");
        try (var entries = Files.list(dir)) {
            return entries.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(s -> s.matches("cp-\\d+"))
                    .max(Comparator.naturalOrder())
                    .orElseThrow(() -> new IOException("No checkpoints exist"));
        }
    }

    /**
     * Restores the source tree and workflow state captured by a checkpoint. Rollback is refused if
     * Git HEAD changed or the current diff no longer matches the workbench-recorded post-change diff.
     *
     * @param project application project root
     * @param checkpointId checkpoint to restore
     * @throws IOException if rollback safety checks fail or Git/state restoration fails
     * @throws InterruptedException if Git execution is interrupted
     */
    public void rollback(Path project, String checkpointId) throws IOException, InterruptedException {
        Checkpoint cp = load(project, checkpointId);
        MigrationState state = states.load(project)
                .orElseThrow(() -> new IOException("No migration state exists"));
        if (!git.head(project).equals(cp.baselineGitHead)) {
            throw new IllegalStateException("Git HEAD changed after migration started. Rollback is unsafe and was refused.");
        }
        if (cp.postDiffSha256 != null && !cp.postDiffSha256.equals(git.diffHash(project))) {
            throw new IllegalStateException(
                    "Working tree differs from the recorded workbench state. Commit/stash manual edits before rollback.");
        }
        git.resetHard(project, cp.baselineGitHead);
        git.applyStashCommit(project, cp.snapshotCommit);
        state.completedFamilies.clear();
        state.completedFamilies.addAll(cp.completedFamiliesBefore);
        state.manualReviewFamilies.clear();
        state.manualReviewFamilies.addAll(cp.manualReviewFamiliesBefore);
        state.currentPhase = cp.phase;
        state.currentFamily = cp.family;
        state.lastCheckpoint = cp.id;
        state.status = "ROLLED_BACK";
        state.lastError = null;
        states.save(project, state);
    }

    /** Persists checkpoint metadata beneath {@code .migration/checkpoints/<id>}. */
    private void save(Path project, Checkpoint cp) throws IOException {
        Path dir = project.resolve(".migration/checkpoints").resolve(cp.id);
        Files.createDirectories(dir);
        json.writerWithDefaultPrettyPrinter().writeValue(dir.resolve("checkpoint.json").toFile(), cp);
    }
}
