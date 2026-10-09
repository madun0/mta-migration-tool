package com.jackson.migration.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;

/**
 * Top-level {@code migrate} command for the migration workbench.
 *
 * <p>The root command intentionally contains no migration logic; each subcommand delegates to
 * application services so orchestration remains testable independently of Picocli.</p>
 */
@TopCommand
@Command(
        name = "migrate",
        mixinStandardHelpOptions = true,
        description = "Guided MTA + OpenRewrite migration workbench for Windows 11 / Git Bash.",
        subcommands = {
                InitCommand.class, DoctorCommand.class, AssessCommand.class, PlanCommand.class,
                MigrateCommand.class, StatusCommand.class, ResumeCommand.class, RestartCommand.class,
                RollbackCommand.class, VerifyCommand.class, ReportCommand.class
        })
public class RootCommand implements Runnable {

    /** Prints the help hint when the root command is invoked without a subcommand. */
    @Override
    public void run() {
        System.out.println("Use 'migrate --help' to see available commands.");
    }
}
