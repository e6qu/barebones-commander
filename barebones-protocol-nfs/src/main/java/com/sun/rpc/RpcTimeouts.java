/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Configuration helper bolted onto the vendored com.sun.rpc package.
 * Lives in the same package so the package-private connection
 * classes can read it without exposing a public API.
 */
package com.sun.rpc;

/**
 * Operator-tunable timeouts for the vendored Sun RPC transport.
 *
 * <ul>
 *   <li>{@code barebones.rpc.connectTimeoutMs} — TCP SYN cap on
 *       {@link ConnectSocket}. The bare {@code new Socket(host, port)}
 *       constructor blocks indefinitely on an unreachable host;
 *       this gives it a deadline. Default 30 000.</li>
 * </ul>
 *
 * Garbage / out-of-range values silently fall back to the default.
 */
final class RpcTimeouts {

    static final String CONNECT_PROP = "barebones.rpc.connectTimeoutMs";
    static final int DEFAULT_CONNECT_MS = 30_000;

    private RpcTimeouts() {
    }

    static int connectMs() {
        String raw = System.getProperty(CONNECT_PROP);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_CONNECT_MS;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            return v > 0 ? v : DEFAULT_CONNECT_MS;
        } catch (NumberFormatException e) {
            return DEFAULT_CONNECT_MS;
        }
    }
}
