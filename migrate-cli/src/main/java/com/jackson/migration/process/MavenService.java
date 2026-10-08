package com.jackson.migration.process;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Locates and invokes the target application Maven wrapper or system Maven for builds and OpenRewrite execution.
 */
@ApplicationScoped
public class MavenService {
    @Inject CommandRunner runner;
    @Inject ToolLocator tools;

    /**
     * Executes Maven for the target application, preferring a host-compatible Maven Wrapper and
     * falling back to a host-compatible system Maven installation.
     *
     * @param project Maven project root
     * @param arguments Maven goals and command-line arguments
     * @param stream whether Maven output should be streamed to the console
     * @return Maven process result
     * @throws IOException if Maven cannot be located or execution cannot start
     * @throws InterruptedException if Maven execution is interrupted
     */
    public CommandResult run(Path project, List<String> arguments, boolean stream) throws IOException, InterruptedException {
        Path executable = tools.projectMavenWrapper(project).orElseGet(() ->
                tools.systemMaven().orElseThrow(() -> new IllegalStateException("Maven not found and no mvnw/mvnw.cmd exists in project")));

        List<String> command = new ArrayList<>();
        String name = executable.getFileName().toString().toLowerCase();
        if (name.endsWith(".cmd") || name.endsWith(".bat")) {
            if (!tools.isWindowsHost()) {
                throw new IOException("Windows Maven launcher selected on a non-Windows host: " + executable);
            }
            command.add("cmd.exe");
            command.add("/d");
            command.add("/s");
            command.add("/c");
            command.add(executable.toString());
        } else {
            command.add(executable.toString());
        }
        command.addAll(arguments);
        return stream ? runner.runStreaming(project, command) : runner.run(project, command);
    }
}
