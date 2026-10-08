package com.jackson.migration.cli;

import com.jackson.migration.util.PathResolver;
import com.jackson.migration.workflow.WorkflowService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Picocli command that converts MTA findings into an ordered migration plan.
 */
@Command(name = "plan", description = "Create an ordered migration plan from MTA findings and the migration catalog.")
public class PlanCommand implements Callable<Integer> {

    /** Project directory whose assessment should be planned. */
    @Option(names = "--project", defaultValue = ".")
    String project;

    @Inject
    PathResolver paths;

    @Inject
    WorkflowService workflow;

    /**
     * Generates the machine-readable and Markdown migration plan.
     *
     * @return {@code 0} on success; {@code 1} when planning fails
     */
    @Override
    public Integer call() {
        try {
            workflow.plan(paths.resolve(project));
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
