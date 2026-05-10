/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class TimedProcessRunnerTest {
    @Test
    void capturesStdoutStderrAndExitCode() throws IOException {
        TimedProcessResult result = TimedProcessRunner.run(
            List.of("/bin/sh", "-c", "printf out; printf err >&2; exit 7"),
            Duration.ofSeconds(2),
            StandardCharsets.UTF_8);

        assertFalse(result.timedOut());
        assertEquals(7, result.exitCode());
        assertEquals("out", result.stdout());
        assertEquals("err", result.stderr());
    }

    @Test
    void stopsProcessAfterTimeout() throws IOException {
        long start = System.nanoTime();

        TimedProcessResult result = TimedProcessRunner.run(
            List.of("/bin/sh", "-c", "sleep 2"),
            Duration.ofMillis(100),
            StandardCharsets.UTF_8);

        long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
        assertTrue(result.timedOut());
        assertEquals(-1, result.exitCode());
        assertTrue(elapsedMs < 1_500L);
    }
}
