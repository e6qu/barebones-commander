/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.mount;

import dev.barebones.commander.commons.util.cli.ExternalCommand;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Runs argv lists from a {@link MountCommand} as real OS processes,
 * capturing stdout / stderr / exit code into a {@link MountResult}.
 *
 * The executor never invokes a shell — everything is
 * {@code new ProcessBuilder(List<String>)}. Any user-supplied input
 * (host, share, username) was already validated by {@link MountSpec}
 * and is passed as its own argv entry by {@link MountCommand}.
 */
public final class MountExecutor {

    /** Hard ceiling on a single mount/unmount invocation. */
    private static final long DEFAULT_TIMEOUT_SECONDS = 30L;

    private final MountCommand command;
    private final long timeoutSeconds;

    public MountExecutor(MountCommand command) {
        this(command, DEFAULT_TIMEOUT_SECONDS);
    }

    public MountExecutor(MountCommand command, long timeoutSeconds) {
        this.command = Objects.requireNonNull(command, "command");
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be positive");
        }
        this.timeoutSeconds = timeoutSeconds;
    }

    public MountResult mount(MountSpec spec) throws IOException, InterruptedException {
        ensureMountpointExists(spec);
        return run(command.mountArgv(spec));
    }

    /**
     * Mount with bounded retry. NFS in particular is prone to
     * transient portmap / rpcbind failures right after a server
     * restart; a fresh attempt 500 ms later usually succeeds. This
     * method retries iff the previous attempt:
     * <ul>
     *   <li>returned a non-zero exit code, or</li>
     *   <li>threw an {@link IOException} (which {@link
     *       ExternalCommand} raises on timeout).</li>
     * </ul>
     * Backoff doubles between attempts (500 → 1 000 → 2 000 …).
     * On final failure, returns the last {@link MountResult} (or
     * rethrows the last IOException) so the caller can surface
     * the underlying stderr to the user.
     */
    public MountResult mountWithRetry(MountSpec spec, int attempts, long baseBackoffMs)
            throws IOException, InterruptedException {
        if (attempts <= 0) {
            throw new IllegalArgumentException("attempts must be positive");
        }
        if (baseBackoffMs < 0) {
            throw new IllegalArgumentException("baseBackoffMs must be non-negative");
        }
        ensureMountpointExists(spec);
        MountResult last = null;
        IOException lastIo = null;
        long backoff = baseBackoffMs;
        for (int i = 0; i < attempts; i++) {
            try {
                last = run(command.mountArgv(spec));
                lastIo = null;
                if (last.ok()) {
                    return last;
                }
            } catch (IOException io) {
                lastIo = io;
                last = null;
            }
            if (i < attempts - 1) {
                Thread.sleep(backoff);
                backoff = Math.min(backoff * 2, TimeUnit.SECONDS.toMillis(30));
            }
        }
        if (lastIo != null) {
            throw lastIo;
        }
        return last;
    }

    public MountResult unmount(MountSpec spec) throws IOException, InterruptedException {
        return run(command.unmountArgv(spec));
    }

    private static void ensureMountpointExists(MountSpec spec) throws IOException {
        Files.createDirectories(spec.mountpoint());
    }

    private MountResult run(List<String> argv) throws IOException, InterruptedException {
        ExternalCommand.Result r = ExternalCommand.run(argv, timeoutSeconds, TimeUnit.SECONDS);
        return new MountResult(r.exitCode(), r.stdout(), r.stderr());
    }
}
