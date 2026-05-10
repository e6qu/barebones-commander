/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.commons.file.protocol.s3.ui;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static dev.barebones.commander.test.TestAssertions.assertEquals;
import static dev.barebones.commander.test.TestAssertions.assertFalse;
import static dev.barebones.commander.test.TestAssertions.assertThrows;
import static dev.barebones.commander.test.TestAssertions.assertTrue;

class S3EndpointConfigTest {

    @Test
    void parsesPlainHostWithConfiguredSchemeAndPort() throws Exception {
        S3EndpointConfig endpoint = S3EndpointConfig.parse("s3.amazonaws.com", 443, true);

        assertEquals(endpoint.host(), "s3.amazonaws.com");
        assertEquals(endpoint.port(), 443);
        assertTrue(endpoint.useHttps());
    }

    @Test
    void parsesPlainHostPort() throws Exception {
        S3EndpointConfig endpoint = S3EndpointConfig.parse("minio.local:9000", 0, false);

        assertEquals(endpoint.host(), "minio.local");
        assertEquals(endpoint.port(), 9000);
        assertFalse(endpoint.useHttps());
    }

    @Test
    void parsesHttpEndpointUrl() throws Exception {
        S3EndpointConfig endpoint = S3EndpointConfig.parse("http://minio.local:9000", 0, true);

        assertEquals(endpoint.host(), "minio.local");
        assertEquals(endpoint.port(), 9000);
        assertFalse(endpoint.useHttps());
    }

    @Test
    void parsesHttpsEndpointUrlWithConfiguredPortFallback() throws Exception {
        S3EndpointConfig endpoint = S3EndpointConfig.parse("https://r2.example.com", 9443, false);

        assertEquals(endpoint.host(), "r2.example.com");
        assertEquals(endpoint.port(), 9443);
        assertTrue(endpoint.useHttps());
    }

    @Test
    void rejectsEndpointPath() {
        assertThrows(MalformedURLException.class,
            () -> S3EndpointConfig.parse("https://minio.local:9000/base-path", 0, true));
    }

    @Test
    void rejectsUnsupportedScheme() {
        assertThrows(MalformedURLException.class,
            () -> S3EndpointConfig.parse("ftp://minio.local", 0, true));
    }
}
