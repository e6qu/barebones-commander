/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.secret.linux;

import com.sun.jna.Pointer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Bounds every libsecret D-Bus call by a configurable timeout.
 *
 * libsecret's {@code _sync} entry points block until the Secret
 * Service answers. If the keyring daemon is wedged (deadlocked on a
 * UI prompt, hung on its own filesystem, etc.), every credential
 * lookup hangs forever. {@code GCancellable} is libsecret's
 * idiomatic out: a fuse object we cancel from another thread to
 * make the in-flight sync call return with an error.
 *
 * Default timeout: 5 000 ms. Override via
 * {@code -Dbarebones.secretStore.libsecretTimeoutMs=N}.
 */
final class LibsecretTimeout {

    static final String TIMEOUT_PROP = "barebones.secretStore.libsecretTimeoutMs";
    static final long DEFAULT_TIMEOUT_MS = 5_000L;

    private static final ScheduledExecutorService TIMER =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "libsecret-cancel-timer");
            t.setDaemon(true);
            return t;
        });

    private LibsecretTimeout() {
    }

    /**
     * Runs {@code action} with a fresh {@code GCancellable}. If the
     * action does not return within {@link #timeoutMs()} ms, the
     * cancellable is cancelled — the in-flight libsecret sync call
     * returns with {@code G_IO_ERROR_CANCELLED} which the caller
     * surfaces as an IOException.
     *
     * Always unrefs the cancellable in a finally block, regardless
     * of normal return, exception, or timeout-fired-late race.
     */
    static <R> R withCancellable(Function<Pointer, R> action) {
        Pointer cancellable = Libsecret.INSTANCE.g_cancellable_new();
        ScheduledFuture<?> fuse = TIMER.schedule(
            () -> Libsecret.INSTANCE.g_cancellable_cancel(cancellable),
            timeoutMs(), TimeUnit.MILLISECONDS);
        try {
            return action.apply(cancellable);
        } finally {
            fuse.cancel(false);
            Libsecret.INSTANCE.g_object_unref(cancellable);
        }
    }

    static long timeoutMs() {
        String raw = System.getProperty(TIMEOUT_PROP);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_TIMEOUT_MS;
        }
        try {
            long v = Long.parseLong(raw.trim());
            return v > 0 ? v : DEFAULT_TIMEOUT_MS;
        } catch (NumberFormatException e) {
            return DEFAULT_TIMEOUT_MS;
        }
    }
}
