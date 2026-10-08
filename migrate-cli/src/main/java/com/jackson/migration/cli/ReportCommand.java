package com.jackson.migration.cli;

import com.jackson.migration.util.PathResolver;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Picocli command that reports which workbench-generated migration documents currently exist.
 */
@Command(name = "report", description = "Print locations of current plan/TODO/final report artifacts.")
public class ReportCommand implements Callable<Integer> {

    /** Project directory containing the {@code .migration} workspace. */
    @Option(names = "--project", defaultValue = ".")
    String project;

    @Inject
    PathResolver paths;

    /**
     * Prints paths for the current plan, TODO list, and final report.
     *
     * @return always {@code 0}; missing reports are shown as unavailable rather than treated as errors
     */
    @Override
    public Integer call() {
        Path migration = paths.resolve(project).resolve(".migration");
        for (String file : List.of("MIGRATION-PLAN.md", "TODO.md", "MIGRATION-REPORT.md")) {
            Path path = migration.resolve(file);
            System.out.println((Files.exists(path) ? "[OK] " : "[--] ") + path);
        }
        return 0;
    }
}
