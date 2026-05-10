/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.commons.file.protocol.sftp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static dev.barebones.commander.test.TestAssertions.assertEquals;
import static dev.barebones.commander.test.TestAssertions.assertFalse;
import static dev.barebones.commander.test.TestAssertions.assertTrue;

public class HostKeyPrompterTest {

    private HostKeyPrompter saved;

    @BeforeEach
    public void capture() {
        saved = HostKeyPrompter.current();
    }

    @AfterEach
    public void restore() {
        HostKeyPrompter.setDefault(saved);
    }

    @Test
    public void setDefaultReplacesCurrent() {
        AtomicReference<String> seen = new AtomicReference<>();
        HostKeyPrompter rejecter = msg -> { seen.set(msg); return false; };
        HostKeyPrompter.setDefault(rejecter);

        boolean accepted = HostKeyPrompter.current().shouldAcceptHostKey("hello");
        assertFalse(accepted);
        assertEquals(seen.get(), "hello");
    }

    @Test
    public void promptAccepts() {
        HostKeyPrompter.setDefault(msg -> true);
        assertTrue(HostKeyPrompter.current().shouldAcceptHostKey("anything"));
    }

    @Test
    public void promptRejects() {
        HostKeyPrompter.setDefault(msg -> false);
        assertFalse(HostKeyPrompter.current().shouldAcceptHostKey("anything"));
    }
}
