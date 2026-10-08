package com.jackson.migration.cli;

import com.jackson.migration.util.PathResolver;
import com.jackson.migration.workflow.WorkflowService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Picocli command that runs an MTA assessment for a target application.
 *
 * <p>The command delegates all orchestration to {@link WorkflowService} and returns a
 * process-friendly exit code for use from Git Bash and CI scripts.</p>
 */
@Command(name = "assess", description = "Run MTA analysis and capture normalized findings.")
public class AssessCommand implements Callable<Integer> {

    /** Project directory to assess. */
    @Option(names = "--project", defaultValue = ".")
    String project;

    @Inject
    PathResolver paths;

    @Inject
    WorkflowService workflow;

    /**
     * Executes the assessment command.
     *
     * @return {@code 0} on success; {@code 1} when assessment fails
     */
    @Override
    public Integer call() {
        try {
            workflow.assess(paths.resolve(project));
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
