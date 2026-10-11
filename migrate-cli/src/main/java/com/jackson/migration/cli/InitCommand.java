package com.jackson.migration.cli;

import com.jackson.migration.util.ProjectDirectory;
import com.jackson.migration.workflow.WorkflowService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Picocli command that initializes a legacy application for workbench-managed migration.
 */
@Command(name = "init", description = "Create migration.yaml and initialize the workbench directory.")
public class InitCommand implements Callable<Integer> {

    @Inject
    ProjectDirectory projectDirectory;

    @Inject
    WorkflowService workflow;

    /**
     * Creates the migration configuration and local workbench directory.
     *
     * @return {@code 0} on success; {@code 1} if initialization fails
     */
    @Override
    public Integer call() {
        try {
            workflow.init(projectDirectory.current());
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
