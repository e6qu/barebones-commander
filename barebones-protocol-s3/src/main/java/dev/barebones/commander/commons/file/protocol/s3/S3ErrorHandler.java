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

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Single point of translation between AWS S3 SDK exceptions and the
 * VFS exception hierarchy.
 *
 * Why distinct types matter:
 * <ul>
 *   <li>401 / 403 → {@link AuthException}. The credentials dialog
 *       knows to re-prompt for these; a generic IOException makes
 *       it look like a transport error.</li>
 *   <li>404 → {@link FileNotFoundException}. Listing / open paths
 *       can distinguish "user typed a wrong key" from "S3 is down".</li>
 *   <li>5xx and other → {@link IOException}. Generic transport
 *       failure; retry / surface to user as transient.</li>
 * </ul>
 *
 * Every translation logs at WARN with status, AWS error code, URL
 * and message — no credentials, no payload — so an operator
 * triaging a user report has the load-bearing context.
 */
final class S3ErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ErrorHandler.class);

    private S3ErrorHandler() {
    }

    /**
     * Convert any {@link AwsServiceException} into the most specific
     * VFS exception type. The returned exception always carries the
     * SDK exception as its cause.
     */
    static IOException toIOException(AwsServiceException e, FileURL url) {
        int status = e.statusCode();
        String awsCode = e.awsErrorDetails() != null
            ? e.awsErrorDetails().errorCode() : "(none)";
        String message = e.awsErrorDetails() != null
            ? e.awsErrorDetails().errorMessage()
            : e.getMessage();

        LOGGER.warn("S3 error: status={} awsCode={} url={} msg={}",
            status, awsCode, url, message);

        if (status == 401 || status == 403) {
            return new AuthException(url, message);
        }
        if (status == 404 || "NoSuchKey".equals(awsCode) || "NoSuchBucket".equals(awsCode)) {
            FileNotFoundException fnf = new FileNotFoundException(url + ": " + message);
            fnf.initCause(e);
            return fnf;
        }
        return new IOException(message, e);
    }

    /** Convenience overload — {@link S3Exception} is the SDK's
     *  concrete subtype, but the translation rule is identical. */
    static IOException toIOException(S3Exception e, FileURL url) {
        return toIOException((AwsServiceException) e, url);
    }
}
