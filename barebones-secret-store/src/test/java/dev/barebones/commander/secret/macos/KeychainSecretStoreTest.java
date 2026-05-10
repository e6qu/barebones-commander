/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.secret.macos;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static dev.barebones.commander.test.TestAssertions.assertEquals;

public class KeychainSecretStoreTest {

    @Test
    public void utf8RoundTripPreservesNonAsciiSecrets() {
        char[] secret = "pässwörd-日本語".toCharArray();

        byte[] bytes = KeychainSecretStore.utf8BytesForTest(secret);
        assertEquals(new String(bytes, StandardCharsets.UTF_8), "pässwörd-日本語");

        assertEquals(KeychainSecretStore.utf8CharsForTest(bytes), secret);
    }
}
