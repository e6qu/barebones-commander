/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.commons.file.protocol.s3;

import dev.barebones.commander.commons.file.AuthException;
import dev.barebones.commander.commons.file.FileURL;

import org.testng.annotations.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class S3ErrorHandlerTest {

    private static FileURL s3url() throws Exception {
        return FileURL.getFileURL("s3://example.s3.amazonaws.com/bucket/key.txt");
    }

    private static S3Exception build(int statusCode, String awsErrorCode, String message) {
        AwsErrorDetails details = AwsErrorDetails.builder()
            .errorCode(awsErrorCode)
            .errorMessage(message)
            .serviceName("S3")
            .build();
        return (S3Exception) S3Exception.builder()
            .statusCode(statusCode)
            .awsErrorDetails(details)
            .message(message)
            .build();
    }

    @Test
    public void status401MapsToAuthException() throws Exception {
        IOException io = S3ErrorHandler.toIOException(
            build(401, "InvalidAccessKeyId", "bad key"), s3url());
        assertTrue(io instanceof AuthException, "expected AuthException, got " + io.getClass());
    }

    @Test
    public void status403MapsToAuthException() throws Exception {
        IOException io = S3ErrorHandler.toIOException(
            build(403, "AccessDenied", "forbidden"), s3url());
        assertTrue(io instanceof AuthException);
    }

    @Test
    public void status404MapsToFileNotFoundException() throws Exception {
        S3Exception sdk = build(404, "NoSuchKey", "the specified key does not exist");
        IOException io = S3ErrorHandler.toIOException(sdk, s3url());
        assertTrue(io instanceof FileNotFoundException,
            "expected FileNotFoundException, got " + io.getClass());
        assertSame(io.getCause(), sdk, "FNF must carry the SDK exception as cause");
    }

    @Test
    public void noSuchBucketCodeMapsToFileNotFound() throws Exception {
        // 404 may be reported with status=404 but on some S3-compatible
        // servers as 200+code; the awsErrorCode rule should still fire.
        IOException io = S3ErrorHandler.toIOException(
            build(404, "NoSuchBucket", "bucket gone"), s3url());
        assertTrue(io instanceof FileNotFoundException);
    }

    @Test
    public void status500MapsToGenericIOException() throws Exception {
        IOException io = S3ErrorHandler.toIOException(
            build(500, "InternalError", "we broke it"), s3url());
        assertEquals(io.getClass(), IOException.class,
            "5xx should land as plain IOException, not subtype");
    }

    @Test
    public void status503MapsToGenericIOException() throws Exception {
        IOException io = S3ErrorHandler.toIOException(
            build(503, "SlowDown", "throttle"), s3url());
        assertEquals(io.getClass(), IOException.class);
    }
}
