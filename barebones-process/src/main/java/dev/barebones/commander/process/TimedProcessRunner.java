/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.process;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runs short-lived platform helper commands with bounded execution time.
 */
public final class TimedProcessRunner {
    public static final String TIMEOUT_PROPERTY = "barebones.process.helperTimeoutMs";
    public static final long DEFAULT_TIMEOUT_MS = 5_000L;

    private static final long DESTROY_GRACE_MS = 250L;

    private TimedProcessRunner() {
    }

    public static TimedProcessResult run(List<String> command) throws IOException {
        return run(command, null, defaultTimeout(), Charset.defaultCharset());
    }

    public static TimedProcessResult run(List<String> command, Duration timeout,
            Charset charset) throws IOException {
        return run(command, null, timeout, charset);
    }

    public static TimedProcessResult run(List<String> command, File directory,
            Duration timeout, Charset charset) throws IOException {
        List<String> safeCommand = List.copyOf(command);
        if (safeCommand.isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (charset == null) {
            throw new IllegalArgumentException("charset must not be null");
        }

        ProcessBuilder builder = new ProcessBuilder(safeCommand);
        if (directory != null) {
            builder.directory(directory);
        }

        Process process = builder.start();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            Future<byte[]> stdout = executor.submit(
                () -> readAvailableBytes(process.getInputStream()));
            Future<byte[]> stderr = executor.submit(
                () -> readAvailableBytes(process.getErrorStream()));

            boolean exited = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!exited) {
                stop(process);
                closeOutputs(process);
                return result(safeCommand, -1, true, stdout, stderr, charset);
            }

            return result(safeCommand, process.exitValue(), false, stdout, stderr, charset);
        } catch (InterruptedException e) {
            stopAfterInterrupt(process);
            closeOutputs(process);
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while running " + safeCommand, e);
        } finally {
            executor.shutdownNow();
        }
    }

    public static TimedProcessResult runTokenized(String command) throws IOException {
        return runTokenized(command, defaultTimeout(), Charset.defaultCharset());
    }

    public static TimedProcessResult runTokenized(String command, Duration timeout,
            Charset charset) throws IOException {
        return run(tokenize(command), null, timeout, charset);
    }

    public static Duration defaultTimeout() {
        long timeoutMs = Long.getLong(TIMEOUT_PROPERTY, DEFAULT_TIMEOUT_MS);
        if (timeoutMs <= 0) {
            timeoutMs = DEFAULT_TIMEOUT_MS;
        }
        return Duration.ofMillis(timeoutMs);
    }

    private static TimedProcessResult result(List<String> command, int exitCode,
            boolean timedOut, Future<byte[]> stdout, Future<byte[]> stderr,
            Charset charset) throws IOException, InterruptedException {
        return new TimedProcessResult(
            command,
            exitCode,
            timedOut,
            new String(await(stdout), charset),
            new String(await(stderr), charset));
    }

    private static byte[] await(Future<byte[]> output)
            throws IOException, InterruptedException {
        try {
            return output.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to read process output", cause);
        }
    }

    private static List<String> tokenize(String command) {
        StringTokenizer parser = new StringTokenizer(command);
        List<String> tokens = new ArrayList<>(parser.countTokens());
        while (parser.hasMoreTokens()) {
            tokens.add(parser.nextToken());
        }
        return tokens;
    }

    private static byte[] readAvailableBytes(InputStream input) throws IOException {
        try (input) {
            return input.readAllBytes();
        }
    }

    private static void stop(Process process) throws InterruptedException {
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        if (!process.waitFor(DESTROY_GRACE_MS, TimeUnit.MILLISECONDS)) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            process.waitFor();
        }
    }

    private static void stopAfterInterrupt(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private static void closeOutputs(Process process) {
        closeQuietly(process.getInputStream());
        closeQuietly(process.getErrorStream());
    }

    private static void closeQuietly(InputStream input) {
        try {
            input.close();
        } catch (IOException ignored) {
            // Best effort cleanup after process timeout/interruption.
        }
    }
}
