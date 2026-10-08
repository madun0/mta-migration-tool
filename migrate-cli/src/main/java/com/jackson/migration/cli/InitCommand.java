package com.jackson.migration.cli;

import com.jackson.migration.util.PathResolver;
import com.jackson.migration.workflow.WorkflowService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Picocli command that initializes a legacy application for workbench-managed migration.
 */
@Command(name = "init", description = "Create migration.yaml and initialize the workbench directory.")
public class InitCommand implements Callable<Integer> {

    /** Project directory to initialize. */
    @Option(names = "--project", defaultValue = ".")
    String project;

    @Inject
    PathResolver paths;

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
            workflow.init(paths.resolve(project));
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
