package com.jackson.migration.cli;

import com.jackson.migration.util.ProjectDirectory;
import com.jackson.migration.workflow.WorkflowService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Picocli command that converts MTA findings into an ordered migration plan.
 */
@Command(name = "plan", description = "Create an ordered migration plan from MTA findings and the migration catalog.")
public class PlanCommand implements Callable<Integer> {

    @Inject
    ProjectDirectory projectDirectory;

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
            workflow.plan(projectDirectory.current());
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
