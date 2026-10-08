package com.jackson.migration.workflow;

import com.jackson.migration.process.CommandResult;
import com.jackson.migration.process.CommandRunner;
import com.jackson.migration.process.ToolLocator;
import com.jackson.migration.util.Hashing;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Small Git adapter used for clean-tree checks, checkpoint snapshots, diff hashing, and guarded rollback operations.
 */
@ApplicationScoped
public class GitService {
    @Inject ToolLocator tools;
    @Inject CommandRunner runner;

    private Path git() {
        return tools.git().orElseThrow(() -> new IllegalStateException("git.exe not found on PATH"));
    }

    /**
     * @param project Git repository root
     * @return current {@code HEAD} commit identifier
     * @throws IOException if Git fails
     * @throws InterruptedException if Git execution is interrupted
     */
    public String head(Path project) throws IOException, InterruptedException {
        return run(project, "rev-parse", "HEAD").output().trim();
    }

    /**
     * Checks whether the working tree is clean, ignoring workbench-owned {@code .migration} files.
     *
     * @param project Git repository root
     * @return {@code true} when no application changes are pending
     * @throws IOException if Git fails
     * @throws InterruptedException if Git execution is interrupted
     */
    public boolean isClean(Path project) throws IOException, InterruptedException {
        String output = run(project, "status", "--porcelain").output();
        return output.lines()
                .filter(line -> !line.contains(".migration/") && !line.contains(".migration\\"))
                .findAny()
                .isEmpty();
    }

    /**
     * Creates an object-only Git stash snapshot without modifying the working tree.
     *
     * @param project Git repository root
     * @param message snapshot description
     * @return snapshot commit identifier; may be blank for an otherwise clean tree
     * @throws IOException if Git fails
     * @throws InterruptedException if Git execution is interrupted
     */
    public String stashCreate(Path project, String message) throws IOException, InterruptedException {
        return run(project, "stash", "create", message).output().trim();
    }

    /**
     * @param project Git repository root
     * @return binary-safe diff between the working tree and {@code HEAD}
     * @throws IOException if Git fails
     * @throws InterruptedException if Git execution is interrupted
     */
    public String diff(Path project) throws IOException, InterruptedException {
        return run(project, "diff", "--binary", "HEAD").output();
    }

    /**
     * Computes a stable hash of the current Git diff for rollback safety checks.
     *
     * @param project Git repository root
     * @return SHA-256 hash of the binary Git diff
     * @throws IOException if Git fails
     * @throws InterruptedException if Git execution is interrupted
     */
    public String diffHash(Path project) throws IOException, InterruptedException {
        return Hashing.sha256(diff(project));
    }

    /**
     * Resets the repository index and working tree to a specific commit. This destructive operation
     * is used only after checkpoint safety checks have succeeded.
     *
     * @param project Git repository root
     * @param commit commit to restore
     * @throws IOException if Git fails
     * @throws InterruptedException if Git execution is interrupted
     */
    public void resetHard(Path project, String commit) throws IOException, InterruptedException {
        requireSuccess(run(project, "reset", "--hard", commit), "git reset --hard");
    }

    /**
     * Applies a checkpoint snapshot commit when one exists.
     *
     * @param project Git repository root
     * @param commit stash/snapshot commit identifier
     * @throws IOException if Git fails
     * @throws InterruptedException if Git execution is interrupted
     */
    public void applyStashCommit(Path project, String commit) throws IOException, InterruptedException {
        if (commit == null || commit.isBlank()) return;
        requireSuccess(run(project, "stash", "apply", commit), "git stash apply");
    }

    /** Executes Git with the supplied arguments in the application repository. */
    private CommandResult run(Path project, String... args) throws IOException, InterruptedException {
        java.util.ArrayList<String> cmd = new java.util.ArrayList<>();
        cmd.add(git().toString());
        cmd.addAll(List.of(args));
        return runner.run(project, cmd);
    }

    /** Converts a failed Git process result into an IOException with captured output. */
    private void requireSuccess(CommandResult result, String operation) throws IOException {
        if (!result.success()) {
            throw new IOException(operation + " failed:\n" + result.output());
        }
    }
}
