/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.auth;

import dev.barebones.commander.commons.file.AbstractFile;
import dev.barebones.commander.commons.logging.Logger;
import dev.barebones.commander.commons.logging.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

final class CredentialsFilePermissions {
    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialsFilePermissions.class);
    private static final Set<PosixFilePermission> OWNER_READ_WRITE = Set.of(OWNER_READ, OWNER_WRITE);

    private CredentialsFilePermissions() {
    }

    static boolean secure(AbstractFile file) {
        try {
            return secure(Path.of(file.getAbsolutePath()));
        } catch (InvalidPathException e) {
            LOGGER.warn("Credentials file path is invalid: {}", file, e);
            return false;
        }
    }

    static boolean secure(Path path) {
        try {
            Files.setPosixFilePermissions(path, OWNER_READ_WRITE);
            return true;
        } catch (UnsupportedOperationException e) {
            LOGGER.warn("Credentials file system does not support POSIX permissions: {}", path, e);
            return false;
        } catch (IOException | SecurityException e) {
            LOGGER.warn("Credentials file permissions could not be set on {}", path, e);
            return false;
        }
    }

    static Set<PosixFilePermission> ownerReadWritePermissions() {
        return OWNER_READ_WRITE;
    }
}
