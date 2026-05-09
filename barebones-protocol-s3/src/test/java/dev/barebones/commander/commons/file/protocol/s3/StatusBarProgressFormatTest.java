/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.commons.file.protocol.s3;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Pins the locale-free byte formatter used for S3 upload progress
 * status hints. Critical because the formatter runs on AWS SDK
 * Netty threads with no Translator initialised — pulling in
 * {@code SizeFormat} (which calls {@code Translator.get}) blew up
 * the multipart upload before this was made standalone.
 */
public class StatusBarProgressFormatTest {

    @Test
    public void bytesUnderOneKilobyte() {
        assertEquals(S3Object.StatusBarProgressListener.formatBytes(0), "0 B");
        assertEquals(S3Object.StatusBarProgressListener.formatBytes(1), "1 B");
        assertEquals(S3Object.StatusBarProgressListener.formatBytes(1023), "1023 B");
    }

    @Test
    public void kibibytes() {
        assertEquals(S3Object.StatusBarProgressListener.formatBytes(1024), "1.0 KiB");
        assertEquals(S3Object.StatusBarProgressListener.formatBytes(1536), "1.5 KiB");
        assertEquals(S3Object.StatusBarProgressListener.formatBytes(1023L * 1024), "1023.0 KiB");
    }

    @Test
    public void mebibytes() {
        assertEquals(S3Object.StatusBarProgressListener.formatBytes(1024L * 1024), "1.0 MiB");
        assertEquals(S3Object.StatusBarProgressListener.formatBytes(40L * 1024 * 1024), "40.0 MiB");
    }

    @Test
    public void gibibytes() {
        assertEquals(S3Object.StatusBarProgressListener.formatBytes(1024L * 1024 * 1024), "1.0 GiB");
    }

    @Test
    public void tebibytes() {
        assertEquals(S3Object.StatusBarProgressListener.formatBytes(1024L * 1024 * 1024 * 1024), "1.0 TiB");
    }
}
