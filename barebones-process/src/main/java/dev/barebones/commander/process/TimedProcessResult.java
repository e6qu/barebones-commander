/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.process;

import java.util.List;

/**
 * Completed result from a short-lived platform helper process.
 */
public final class TimedProcessResult {
    private final List<String> command;
    private final int exitCode;
    private final boolean timedOut;
    private final String stdout;
    private final String stderr;

    TimedProcessResult(List<String> command, int exitCode, boolean timedOut,
            String stdout, String stderr) {
        this.command = List.copyOf(command);
        this.exitCode = exitCode;
        this.timedOut = timedOut;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public List<String> command() {
        return command;
    }

    public int exitCode() {
        return exitCode;
    }

    public boolean timedOut() {
        return timedOut;
    }

    public String stdout() {
        return stdout;
    }

    public String stderr() {
        return stderr;
    }

    public boolean succeeded(int expectedExitCode) {
        return !timedOut && exitCode == expectedExitCode;
    }
}
