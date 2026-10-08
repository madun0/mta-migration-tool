package com.jackson.migration.cli;

import com.jackson.migration.util.PathResolver;
import com.jackson.migration.validation.DoctorService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Picocli command that validates the Windows 11/Git Bash migration environment.
 *
 * <p>It checks the project, Java runtime, Git, Maven, MTA CLI, workbench home, and
 * other prerequisites before a migration is allowed to proceed.</p>
 */
@Command(name = "doctor", description = "Validate the Windows/Git Bash migration environment.")
public class DoctorCommand implements Callable<Integer> {

    /** Project directory whose toolchain and repository state should be checked. */
    @Option(names = "--project", defaultValue = ".")
    String project;

    @Inject
    PathResolver paths;

    @Inject
    DoctorService doctor;

    /**
     * Executes environment validation.
     *
     * @return {@code 0} when the environment is ready; {@code 2} when required checks fail
     */
    @Override
    public Integer call() {
        return doctor.run(paths.resolve(project)) ? 0 : 2;
    }
}
