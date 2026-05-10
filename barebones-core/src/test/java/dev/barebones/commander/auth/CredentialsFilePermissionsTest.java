/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.auth;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.GROUP_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;
import static java.nio.file.attribute.PosixFilePermission.OTHERS_READ;
import static dev.barebones.commander.test.TestAssertions.assertEquals;
import static dev.barebones.commander.test.TestAssertions.assertTrue;

public class CredentialsFilePermissionsTest {

    @Test
    public void ownerReadWritePermissionsAre0600() {
        assertEquals(CredentialsFilePermissions.ownerReadWritePermissions(),
            Set.of(OWNER_READ, OWNER_WRITE));
    }

    @Test
    public void securePathRemovesGroupAndOtherPermissions() throws Exception {
        Path file = Files.createTempFile("barebones-credentials", ".xml");
        try {
            try {
                Files.getPosixFilePermissions(file);
            } catch (UnsupportedOperationException e) {
                return;
            }

            Files.setPosixFilePermissions(file,
                Set.of(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ));

            assertTrue(CredentialsFilePermissions.secure(file));
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file);
            assertEquals(permissions, Set.of(OWNER_READ, OWNER_WRITE));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
