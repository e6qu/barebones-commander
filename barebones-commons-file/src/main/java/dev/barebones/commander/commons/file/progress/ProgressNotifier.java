/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.commons.file.progress;

import java.util.function.Consumer;

/**
 * Static SPI for surfacing a single short progress hint to wherever
 * the host process wants to show it (status bar, JProgressBar, log
 * line). Lives in {@code barebones-commons-file} so producers
 * (protocols, jobs) can publish without depending on the UI layer.
 *
 * Default sink is a no-op. The host process (barebones-core's UI
 * activator) installs a real sink that posts to the status bar.
 *
 * Single-slot — there is one global hint at a time. This is
 * deliberately a coarse surface; per-job progress dialogs remain the
 * right place for fine-grained byte counts. The hint exists so
 * operations that don't go through {@code FileJob} (S3 multipart
 * upload during {@code OutputStream.close()} is the motivating case)
 * have a place to say "I'm not stuck, I'm uploading".
 */
public final class ProgressNotifier {

    private static volatile Consumer<String> sink = msg -> { /* no-op */ };

    private ProgressNotifier() {
    }

    /** Install a sink. Replaces any previous one. */
    public static void install(Consumer<String> newSink) {
        sink = newSink == null ? msg -> { } : newSink;
    }

    /** Publish {@code message} to the installed sink. Null clears. */
    public static void post(String message) {
        sink.accept(message);
    }

    /** Convenience: clear any showing hint. */
    public static void clear() {
        sink.accept(null);
    }
}
