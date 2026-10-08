package com.jackson.migration.process;

import java.time.Duration;

/**
 * Immutable result of an external process execution.
 *
 * @param exitCode operating-system process exit code
 * @param output combined standard output and standard error captured as UTF-8 text
 * @param duration elapsed process execution time
 */
public record CommandResult(int exitCode, String output, Duration duration) {

    /**
     * Indicates whether the process exited successfully.
     *
     * @return {@code true} when {@link #exitCode()} is zero
     */
    public boolean success() {
        return exitCode == 0;
    }
}
