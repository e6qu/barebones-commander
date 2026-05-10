/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.secret.linux;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static dev.barebones.commander.test.TestAssertions.assertEquals;

/**
 * Pure-config tests. The live cancellable behaviour requires
 * libsecret + a running Secret Service which our CI runners
 * (macOS) don't have, so the runtime path is not exercised here.
 */
public class LibsecretTimeoutTest {

    @AfterEach
    public void clearProps() {
        System.clearProperty(LibsecretTimeout.TIMEOUT_PROP);
    }

    @Test
    public void defaultTimeoutWhenUnset() {
        assertEquals(LibsecretTimeout.timeoutMs(), LibsecretTimeout.DEFAULT_TIMEOUT_MS);
    }

    @Test
    public void overrideApplied() {
        System.setProperty(LibsecretTimeout.TIMEOUT_PROP, "1500");
        assertEquals(LibsecretTimeout.timeoutMs(), 1500L);
    }

    @Test
    public void garbageFallsBackToDefault() {
        System.setProperty(LibsecretTimeout.TIMEOUT_PROP, "not-a-number");
        assertEquals(LibsecretTimeout.timeoutMs(), LibsecretTimeout.DEFAULT_TIMEOUT_MS);
    }

    @Test
    public void zeroOrNegativeFallsBackToDefault() {
        System.setProperty(LibsecretTimeout.TIMEOUT_PROP, "0");
        assertEquals(LibsecretTimeout.timeoutMs(), LibsecretTimeout.DEFAULT_TIMEOUT_MS);
        System.setProperty(LibsecretTimeout.TIMEOUT_PROP, "-1");
        assertEquals(LibsecretTimeout.timeoutMs(), LibsecretTimeout.DEFAULT_TIMEOUT_MS);
    }
}
