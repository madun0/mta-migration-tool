package com.jackson.migration.cli;

import com.jackson.migration.util.ProjectDirectory;
import com.jackson.migration.validation.DoctorService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * Picocli command that validates the Windows 11/Git Bash migration environment.
 *
 * <p>It checks the project, Java runtime, Git, Maven, MTA CLI, workbench home, and
 * other prerequisites before a migration is allowed to proceed.</p>
 */
@Command(name = "doctor", description = "Validate the Windows/Git Bash migration environment.")
public class DoctorCommand implements Callable<Integer> {

    @Inject
    ProjectDirectory projectDirectory;

    @Inject
    DoctorService doctor;

    /**
     * Executes environment validation.
     *
     * @return {@code 0} when the environment is ready; {@code 2} when required checks fail
     */
    @Override
    public Integer call() {
        return doctor.run(projectDirectory.current()) ? 0 : 2;
    }
}
