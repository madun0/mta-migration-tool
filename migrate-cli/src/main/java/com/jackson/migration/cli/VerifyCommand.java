package com.jackson.migration.cli;

import com.jackson.migration.util.PathResolver;
import com.jackson.migration.workflow.WorkflowService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Picocli command that executes final build, residual-pattern, and MTA verification.
 */
@Command(name = "verify", description = "Validate the migrated application and generate the final migration report.")
public class VerifyCommand implements Callable<Integer> {

    /** Project directory to verify. */
    @Option(names = "--project", defaultValue = ".")
    String project;

    @Inject
    PathResolver paths;

    @Inject
    WorkflowService workflow;

    /**
     * Runs final migration verification.
     *
     * @return {@code 0} when verification executes successfully; {@code 1} on command failure
     */
    @Override
    public Integer call() {
        try {
            workflow.verify(paths.resolve(project));
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
