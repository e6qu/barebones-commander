/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.mount;

import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class MountRetryTest {

    /** /bin/true → always succeeds. /bin/false → always fails. */
    private static Path locate(String name) {
        for (String p : new String[]{"/usr/bin/" + name, "/bin/" + name}) {
            if (new File(p).canExecute()) {
                return Path.of(p);
            }
        }
        throw new IllegalStateException("no /bin/" + name + " on this platform");
    }

    private static MountSpec specFor(Path mountpoint) {
        return new MountSpec(MountKind.NFSV4, "h", "/x", mountpoint, null, 0);
    }

    @Test
    public void firstAttemptSucceedsNoRetry() throws Exception {
        Path truePath = locate("true");
        AtomicInteger calls = new AtomicInteger();
        MountCommand cmd = stubMountCommand(spec -> {
            calls.incrementAndGet();
            return List.of(truePath.toString());
        });
        MountExecutor exec = new MountExecutor(cmd);
        Path mp = Files.createTempDirectory("mount-retry-ok-");
        try {
            MountResult r = exec.mountWithRetry(specFor(mp), 3, 10);
            assertTrue(r.ok());
            assertEquals(calls.get(), 1, "successful first attempt should not retry");
        } finally {
            Files.deleteIfExists(mp);
        }
    }

    @Test
    public void retriesUntilSuccess() throws Exception {
        Path truePath = locate("true");
        Path falsePath = locate("false");
        AtomicInteger calls = new AtomicInteger();
        MountCommand cmd = stubMountCommand(spec -> {
            int n = calls.incrementAndGet();
            return List.of((n < 3 ? falsePath : truePath).toString());
        });
        MountExecutor exec = new MountExecutor(cmd);
        Path mp = Files.createTempDirectory("mount-retry-recover-");
        try {
            long t0 = System.currentTimeMillis();
            MountResult r = exec.mountWithRetry(specFor(mp), 3, 10);
            long elapsed = System.currentTimeMillis() - t0;
            assertTrue(r.ok());
            assertEquals(calls.get(), 3);
            // 10 + 20 = 30 ms minimum backoff between 3 attempts.
            assertTrue(elapsed >= 30, "elapsed should include backoff: " + elapsed);
        } finally {
            Files.deleteIfExists(mp);
        }
    }

    @Test
    public void allAttemptsFailReturnsLastResult() throws Exception {
        Path falsePath = locate("false");
        AtomicInteger calls = new AtomicInteger();
        MountCommand cmd = stubMountCommand(spec -> {
            calls.incrementAndGet();
            return List.of(falsePath.toString());
        });
        MountExecutor exec = new MountExecutor(cmd);
        Path mp = Files.createTempDirectory("mount-retry-fail-");
        try {
            MountResult r = exec.mountWithRetry(specFor(mp), 3, 1);
            assertFalse(r.ok(), "all attempts failed → result is non-ok");
            assertEquals(r.exitCode(), 1);
            assertEquals(calls.get(), 3);
        } finally {
            Files.deleteIfExists(mp);
        }
    }

    @Test
    public void rejectsBadParams() throws Exception {
        Path truePath = locate("true");
        MountExecutor exec = new MountExecutor(
            stubMountCommand(s -> List.of(truePath.toString())));
        Path mp = Files.createTempDirectory("mount-retry-args-");
        try {
            assertThrows(IllegalArgumentException.class,
                () -> exec.mountWithRetry(specFor(mp), 0, 10));
            assertThrows(IllegalArgumentException.class,
                () -> exec.mountWithRetry(specFor(mp), 3, -1));
        } finally {
            Files.deleteIfExists(mp);
        }
    }

    private interface ArgvFn {
        List<String> apply(MountSpec spec);
    }

    private static MountCommand stubMountCommand(ArgvFn fn) {
        return new MountCommand() {
            @Override
            public List<String> mountArgv(MountSpec spec) {
                return fn.apply(spec);
            }
            @Override
            public List<String> unmountArgv(MountSpec spec) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
