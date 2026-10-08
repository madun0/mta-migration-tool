package com.jackson.migration.cli;

import com.jackson.migration.util.PathResolver;
import com.jackson.migration.workflow.WorkflowService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Picocli command that resumes a previously started migration from persisted state.
 */
@Command(name = "resume", description = "Continue from the next unfinished migration family.")
public class ResumeCommand implements Callable<Integer> {

    /** Project directory whose migration should be resumed. */
    @Option(names = "--project", defaultValue = ".")
    String project;

    @Inject
    PathResolver paths;

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
            workflow.resume(paths.resolve(project));
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
