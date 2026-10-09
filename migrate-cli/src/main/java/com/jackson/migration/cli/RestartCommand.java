package com.jackson.migration.cli;

import com.jackson.migration.util.PathResolver;
import com.jackson.migration.workflow.WorkflowService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Starts a fresh migration run from the project's current Git HEAD while preserving migration.yaml.
 */
@Command(name = "restart", description = "Clear prior migration run state/checkpoints and start fresh from current Git HEAD.")
public class RestartCommand implements Callable<Integer> {

    /** Project directory whose migration run should be restarted. */
    @Option(names = "--project", defaultValue = ".")
    String project;

    @Inject PathResolver paths;
    @Inject WorkflowService workflow;

    /**
     * Clears Workbench-owned run state after verifying that application changes are committed/stashed.
     *
     * @return {@code 0} on success; {@code 1} when restart is unsafe or fails
     */
    @Override
    public Integer call() {
        try {
            workflow.restart(paths.resolve(project));
            return 0;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }
}
