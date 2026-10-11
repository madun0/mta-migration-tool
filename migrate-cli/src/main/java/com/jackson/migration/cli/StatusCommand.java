package com.jackson.migration.cli;

import com.jackson.migration.util.ProjectDirectory;
import com.jackson.migration.workflow.StateStore;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Picocli command that prints persisted migration progress and review state.
 */
@Command(name = "status", description = "Show migration state and the current/next work item.")
public class StatusCommand implements Callable<Integer> {

    @Inject
    ProjectDirectory projectDirectory;

    @Inject
    StateStore states;

    /**
     * Displays status without modifying migration state.
     *
     * @return {@code 0} when status can be read; {@code 1} on an I/O or parsing failure
     */
    @Override
    public Integer call() {
        try {
            var stored = states.load(projectDirectory.current());
            if (stored.isEmpty()) {
                System.out.println("No migration state exists.");
                return 0;
            }
            var state = stored.get();
            System.out.println("Status: " + state.status);
            System.out.println("Phase: " + state.currentPhase);
            System.out.println("Family: " + state.currentFamily);
            System.out.println("Checkpoint: " + state.lastCheckpoint);
            System.out.println("Completed: " + state.completedFamilies.size());
            System.out.println("Manual review: " + state.manualReviewFamilies);
            if (state.lastError != null) {
                System.out.println("Last error: " + state.lastError);
            }
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
