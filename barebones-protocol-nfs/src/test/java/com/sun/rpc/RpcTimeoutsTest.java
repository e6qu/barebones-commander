/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package com.sun.rpc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RpcTimeoutsTest {

    @AfterEach
    void clearProps() {
        System.clearProperty(RpcTimeouts.CONNECT_PROP);
    }

    @Test
    void defaultWhenUnset() {
        assertEquals(RpcTimeouts.DEFAULT_CONNECT_MS, RpcTimeouts.connectMs());
    }

    @Test
    void overrideApplied() {
        System.setProperty(RpcTimeouts.CONNECT_PROP, "10000");
        assertEquals(10_000, RpcTimeouts.connectMs());
    }

    @Test
    void garbageFallsBackToDefault() {
        System.setProperty(RpcTimeouts.CONNECT_PROP, "not-a-number");
        assertEquals(RpcTimeouts.DEFAULT_CONNECT_MS, RpcTimeouts.connectMs());
    }

    @Test
    void zeroOrNegativeFallsBackToDefault() {
        System.setProperty(RpcTimeouts.CONNECT_PROP, "0");
        assertEquals(RpcTimeouts.DEFAULT_CONNECT_MS, RpcTimeouts.connectMs());
        System.setProperty(RpcTimeouts.CONNECT_PROP, "-1");
        assertEquals(RpcTimeouts.DEFAULT_CONNECT_MS, RpcTimeouts.connectMs());
    }
}
