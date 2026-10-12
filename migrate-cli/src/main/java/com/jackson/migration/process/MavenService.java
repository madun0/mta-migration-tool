package com.jackson.migration.process;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Locates and invokes Maven for target-application builds and OpenRewrite execution.
 * The system Maven on PATH is preferred so Workbench execution matches a developer's
 * direct {@code mvn} invocation from the same project directory. A compatible Maven Wrapper
 * is used only when system Maven is unavailable.
 */
@ApplicationScoped
public class MavenService {
    @Inject CommandRunner runner;
    @Inject ToolLocator tools;

    /**
     * Executes Maven from the target application's project root, preferring the system
     * {@code mvn} command on PATH and falling back to a host-compatible project Maven Wrapper.
     * Running from the project root allows Maven to apply the project's {@code .mvn/maven.config},
     * {@code .mvn/jvm.config}, and {@code .mvn/extensions.xml} using Maven's normal discovery.
     *
     * @param project Maven project root
     * @param arguments Maven goals and command-line arguments
     * @param stream whether Maven output should be streamed to the console
     * @return Maven process result
     * @throws IOException if Maven cannot be located or execution cannot start
     * @throws InterruptedException if Maven execution is interrupted
     */
    public CommandResult run(Path project, List<String> arguments, boolean stream) throws IOException, InterruptedException {
        Path executable = tools.systemMaven().orElseGet(() ->
                tools.projectMavenWrapper(project).orElseThrow(() ->
                        new IllegalStateException("Maven not found on PATH and no compatible mvnw/mvnw.cmd exists in project")));

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
