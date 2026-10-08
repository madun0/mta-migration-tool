package com.jackson.migration.cli;

import com.jackson.migration.util.PathResolver;
import com.jackson.migration.workflow.WorkflowService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Picocli command that executes an ordered migration plan.
 *
 * <p>A migration can run the complete configured profile or be restricted to a single
 * phase or migration family for controlled troubleshooting and remediation.</p>
 */
@Command(name = "migrate", description = "Execute the migration plan with checkpoints and per-family validation.")
public class MigrateCommand implements Callable<Integer> {

    /** Project directory to migrate. */
    @Option(names = "--project", defaultValue = ".")
    String project;

    /** Optional phase name to execute exclusively. */
    @Option(names = "--phase")
    String phase;

    /** Optional migration family key to execute exclusively. */
    @Option(names = "--family")
    String family;

    @Inject
    PathResolver paths;

    @Inject
    WorkflowService workflow;

    /**
     * Executes migration orchestration for the requested scope.
     *
     * @return {@code 0} when orchestration completes normally; {@code 1} on an unrecoverable command error
     */
    @Override
    public Integer call() {
        try {
            workflow.migrate(paths.resolve(project), phase, family);
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
