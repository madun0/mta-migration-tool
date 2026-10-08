package com.jackson.migration.process;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Executes external tools without shell command concatenation, preserving
 * Windows path safety and optionally streaming output.
 *
 * <p>The runner also protects tools such as the Migration Toolkit for
 * Applications (MTA) from being launched with an input directory that is the
 * same as the process working directory. MTA rejects that configuration with
 * an error similar to {@code "input path ... cannot be the current directory"}.
 * When an {@code --input}, {@code --input=...}, or {@code -i} argument resolves
 * to the requested working directory, the process is launched from the input
 * directory's parent instead.</p>
 */
@ApplicationScoped
public class CommandRunner {

    /**
     * Executes an external command and captures combined standard output/error.
     *
     * @param workingDirectory process working directory
     * @param command executable followed by arguments; no shell parsing is performed
     * @return captured process result
     * @throws IOException if the process cannot be started or read
     * @throws InterruptedException if execution is interrupted
     */
    public CommandResult run(Path workingDirectory, List<String> command)
            throws IOException, InterruptedException {
        return run(workingDirectory, command, Map.of(), false);
    }

    /**
     * Executes an external command while streaming output to the current console
     * and retaining the same output in the returned result.
     *
     * @param workingDirectory process working directory
     * @param command executable followed by arguments
     * @return captured process result
     * @throws IOException if the process cannot be started or read
     * @throws InterruptedException if execution is interrupted
     */
    public CommandResult runStreaming(Path workingDirectory, List<String> command)
            throws IOException, InterruptedException {
        return run(workingDirectory, command, Map.of(), true);
    }

    /**
     * Executes an external process with optional environment overrides and console
     * streaming.
     *
     * <p>If the command contains an input argument whose resolved path is the same
     * as {@code workingDirectory}, a safe parent directory is used as the actual
     * process working directory. This specifically prevents MTA from rejecting a
     * project directory that is simultaneously supplied as its analysis input and
     * process current directory.</p>
     *
     * @param workingDirectory requested process working directory
     * @param command executable followed by arguments
     * @param environment environment variables to add or override
     * @param stream whether process output should also be written to the current console
     * @return captured process result including duration
     * @throws IOException if the process cannot be started or read
     * @throws InterruptedException if execution is interrupted
     */
    public CommandResult run(
            Path workingDirectory,
            List<String> command,
            Map<String, String> environment,
            boolean stream)
            throws IOException, InterruptedException {

        Instant start = Instant.now();

        Path requestedWorkingDirectory =
                workingDirectory.toAbsolutePath().normalize();

        Path effectiveWorkingDirectory =
                resolveSafeWorkingDirectory(
                        requestedWorkingDirectory,
                        command);

        ProcessBuilder builder =
                new ProcessBuilder(new ArrayList<>(command));

        builder.directory(effectiveWorkingDirectory.toFile());
        builder.redirectErrorStream(true);
        builder.environment().putAll(environment);

        Process process = builder.start();

        StringBuilder output = new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     process.getInputStream(),
                                     StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line)
                        .append(System.lineSeparator());

                if (stream) {
                    System.out.println(line);
                }
            }
        }

        int exit = process.waitFor();

        return new CommandResult(
                exit,
                output.toString(),
                Duration.between(start, Instant.now()));
    }

    /**
     * Determines the working directory that should actually be supplied to
     * {@link ProcessBuilder}.
     *
     * <p>For most commands this method returns the requested directory unchanged.
     * If a recognized input argument resolves to the same path, the parent
     * directory is selected. If no parent exists, the JVM temporary directory is
     * used as a neutral fallback.</p>
     *
     * @param requestedWorkingDirectory normalized requested working directory
     * @param command executable and command arguments
     * @return safe process working directory
     */
    private Path resolveSafeWorkingDirectory(
            Path requestedWorkingDirectory,
            List<String> command) {

        Optional<String> inputArgument =
                findInputArgument(command);

        if (inputArgument.isEmpty()) {
            return requestedWorkingDirectory;
        }

        Path inputPath =
                resolveInputPath(
                        requestedWorkingDirectory,
                        inputArgument.get());

        if (!inputPath.equals(requestedWorkingDirectory)) {
            return requestedWorkingDirectory;
        }

        Path parent =
                requestedWorkingDirectory.getParent();

        if (parent != null
                && !parent.equals(requestedWorkingDirectory)) {
            return parent;
        }

        return Path.of(
                        System.getProperty("java.io.tmpdir"))
                .toAbsolutePath()
                .normalize();
    }

    /**
     * Locates a conventional CLI input-path argument.
     *
     * <p>The forms supported are {@code --input path}, {@code --input=path}, and
     * {@code -i path}. These cover the MTA invocation used by the Workbench while
     * keeping the runner independent of MTA-specific classes.</p>
     *
     * @param command executable and arguments
     * @return input-path argument when one is present
     */
    private Optional<String> findInputArgument(
            List<String> command) {

        for (int index = 0;
             index < command.size();
             index++) {

            String argument = command.get(index);

            if ("--input".equals(argument)
                    || "-i".equals(argument)) {

                if (index + 1 < command.size()) {
                    return Optional.ofNullable(
                            command.get(index + 1));
                }

                return Optional.empty();
            }

            if (argument.startsWith("--input=")) {
                String value =
                        argument.substring(
                                "--input=".length());

                if (!value.isBlank()) {
                    return Optional.of(value);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Resolves an input argument to an absolute normalized path.
     *
     * <p>Absolute arguments are preserved. Relative input arguments are resolved
     * against the originally requested working directory because that is how the
     * external process would otherwise interpret them.</p>
     *
     * @param requestedWorkingDirectory requested process working directory
     * @param inputArgument input-path argument
     * @return absolute normalized input path
     */
    private Path resolveInputPath(
            Path requestedWorkingDirectory,
            String inputArgument) {

        Path inputPath =
                Path.of(inputArgument);

        if (!inputPath.isAbsolute()) {
            inputPath =
                    requestedWorkingDirectory.resolve(
                            inputPath);
        }

        return inputPath
                .toAbsolutePath()
                .normalize();
    }
}
