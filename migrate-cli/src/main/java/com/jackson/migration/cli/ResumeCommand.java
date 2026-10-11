package com.jackson.migration.cli;

import com.jackson.migration.util.ProjectDirectory;
import com.jackson.migration.workflow.WorkflowService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Picocli command that resumes a previously started migration from persisted state.
 */
@Command(name = "resume", description = "Continue from the next unfinished migration family.")
public class ResumeCommand implements Callable<Integer> {

    @Inject
    ProjectDirectory projectDirectory;

    @Inject
    WorkflowService workflow;

    /**
     * Resumes the workflow. If manual review was required, invoking resume confirms that the
     * developer has completed the documented review and allows cleanup to continue.
     *
     * @return {@code 0} on success; {@code 1} when resume cannot proceed
     */
    @Override
    public Integer call() {
        try {
            workflow.resume(projectDirectory.current());
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
