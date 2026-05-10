/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.secret;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static dev.barebones.commander.test.TestAssertions.assertEquals;
import static dev.barebones.commander.test.TestAssertions.assertThrows;

public class LegacyXorCodecTest {

    /**
     * Fixtures produced by reapplying upstream muCommander's
     * {@code XORCipher.encryptXORBase64} byte-for-byte (same key,
     * same default-encoding bytes, same Base64). The decoder must
     * be byte-compatible so existing credentials.xml files migrate
     * cleanly.
     */
    @Test
    public void decodesAsciiPassword() throws IOException {
        assertEquals(LegacyXorCodec.decryptXorBase64("yanyONTcCg=="), "hunter2");
    }

    @Test
    public void decodesPasswordWithSymbols() throws IOException {
        assertEquals(LegacyXorCodec.decryptXorBase64("0ejvP8aeSkFD"), "p4ssw0rd!");
    }

    @Test
    public void decodesUtf8Password() throws IOException {
        assertEquals(LegacyXorCodec.decryptXorBase64("cWNM/GAu6Juy5jGf"), "пароль");
    }

    @Test
    public void rejectsNonBase64() {
        assertThrows(IOException.class,
            () -> LegacyXorCodec.decryptXorBase64("not!valid!base64"));
    }
}
