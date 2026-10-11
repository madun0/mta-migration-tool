package com.jackson.migration.cli;

import com.jackson.migration.util.ProjectDirectory;
import com.jackson.migration.workflow.WorkflowService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Picocli command that restores the source tree to a workbench checkpoint.
 */
@Command(name = "rollback", description = "Restore the source tree to a workbench checkpoint.")
public class RollbackCommand implements Callable<Integer> {

    /** Optional checkpoint identifier; the most recent checkpoint is used when omitted. */
    @Option(names = "--checkpoint", description = "Checkpoint id such as cp-004. Defaults to latest.")
    String checkpoint;

    @Inject
    ProjectDirectory projectDirectory;

    @Inject
    WorkflowService workflow;

    /**
     * Performs a safety-checked rollback using the selected workbench checkpoint.
     *
     * @return {@code 0} on success; {@code 1} when rollback is unsafe or fails
     */
    @Override
    public Integer call() {
        try {
            workflow.rollback(projectDirectory.current(), checkpoint);
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
