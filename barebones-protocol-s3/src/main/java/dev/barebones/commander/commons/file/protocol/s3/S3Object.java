/*
 * Copyright (C) 2002-2026 muCommander contributors
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.commons.file.protocol.s3;

import dev.barebones.commander.commons.file.AbstractFile;
import dev.barebones.commander.commons.file.FileURL;
import dev.barebones.commander.commons.file.UnsupportedFileOperationException;
import dev.barebones.commander.commons.file.progress.ProgressNotifier;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

import dev.barebones.commander.commons.logging.Logger;
import dev.barebones.commander.commons.logging.LoggerFactory;
import dev.barebones.commander.commons.runtime.Tunables;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;

/**
 * One S3 object (or a "directory" prefix). Path shape:
 * {@code s3://endpoint/BUCKET/path/to/object[/]}.
 *
 * Two flavours:
 *   - object (key does not end in '/') — backed by a real S3 object
 *   - directory (key ends in '/') — pure prefix; HEAD/GET don't work
 *
 * The flavour can be set explicitly by the listing path
 * (see {@link #setListingMetadata}) so a {@code ls()} call doesn't
 * pay an extra HEAD round-trip per child.
 */
public class S3Object extends S3File {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Object.class);

    private boolean metadataKnown;
    private boolean directory;
    private long size;
    private long lastModified;
    private IOException lastMetadataFailure;

    public S3Object(FileURL url, S3Connection connection) {
        super(url, connection);
        // If the URL ends with '/', the object is a directory by construction.
        if (parsed.key().endsWith("/")) {
            this.directory = true;
            this.metadataKnown = true;
        }
    }

    /**
     * Stash the metadata from a parent listing so we can answer
     * isDirectory / getSize / getDate without re-HEADing the object.
     */
    void setListingMetadata(long size, long lastModified, boolean directory) {
        this.size = size;
        this.lastModified = lastModified;
        this.directory = directory;
        this.metadataKnown = true;
    }

    private void ensureMetadata() throws IOException {
        if (metadataKnown) return;
        try {
            HeadObjectResponse h = connection.client().headObject(
                HeadObjectRequest.builder()
                    .bucket(parsed.bucket())
                    .key(parsed.key())
                    .build());
            this.size = h.contentLength() != null ? h.contentLength() : 0L;
            this.lastModified = h.lastModified() != null
                ? h.lastModified().toEpochMilli() : 0L;
            this.directory = false;
            this.metadataKnown = true;
            this.lastMetadataFailure = null;
        } catch (NoSuchKeyException missing) {
            this.lastMetadataFailure = null;
            this.metadataKnown = true; // exists() answers via this state
        } catch (S3Exception e) {
            IOException failure = toIOException(e, fileURL);
            this.lastMetadataFailure = failure;
            throw failure;
        }
    }

    private void logMetadataFailure(String operation, IOException failure) {
        if (failure == lastMetadataFailure) {
            LOGGER.warn("S3 metadata lookup failed during {} for {}", operation, getURL(), failure);
        } else {
            LOGGER.warn("S3 metadata state unavailable during {} for {}", operation, getURL(), failure);
        }
    }

    @Override
    public boolean isDirectory() {
        try {
            ensureMetadata();
        } catch (IOException e) {
            logMetadataFailure("isDirectory", e);
            return false;
        }
        return directory;
    }

    @Override
    public boolean exists() {
        try {
            ensureMetadata();
        } catch (IOException e) {
            logMetadataFailure("exists", e);
            return false;
        }
        // metadataKnown == true after a HEAD; if directory or non-zero
        // size or non-zero lastModified, we got a real response.
        return directory || size > 0 || lastModified > 0;
    }

    @Override
    public long getDate() {
        try {
            ensureMetadata();
        } catch (IOException e) {
            logMetadataFailure("getDate", e);
        }
        return lastModified;
    }

    @Override
    public long getSize() {
        try {
            ensureMetadata();
        } catch (IOException e) {
            logMetadataFailure("getSize", e);
        }
        return size;
    }

    @Override
    public AbstractFile[] ls() throws IOException {
        if (!isDirectory()) {
            throw new IOException("not a directory: " + fileURL);
        }
        String prefix = parsed.key();
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return S3Listing.listChildrenAsFiles(connection, fileURL, parsed.bucket(), prefix);
    }

    @Override
    public void mkdir() throws IOException {
        // S3 has no real directories; create an empty object whose
        // key ends with '/'. That's what the AWS Console does and
        // it's what subsequent ListObjectsV2 with delimiter='/' picks
        // up as a CommonPrefix.
        String key = parsed.key();
        if (!key.endsWith("/")) key = key + "/";
        try {
            connection.client().putObject(
                PutObjectRequest.builder()
                    .bucket(parsed.bucket())
                    .key(key)
                    .build(),
                RequestBody.empty());
            this.directory = true;
            this.metadataKnown = true;
        } catch (S3Exception e) {
            throw toIOException(e, fileURL);
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            ResponseInputStream<GetObjectResponse> stream = connection.client().getObject(
                GetObjectRequest.builder()
                    .bucket(parsed.bucket())
                    .key(parsed.key())
                    .build());
            return stream;
        } catch (S3Exception e) {
            throw toIOException(e, fileURL);
        }
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        // OutputStream contract: caller writes whatever, then close().
        // Two strategies bounded by Tunables.S3_UPLOAD_SPILL_THRESHOLD_BYTES:
        //   - Small writes buffer in memory and
        //     PUT in a single request on close().
        //   - Large writes spill to a temp file beyond the threshold
        //     and on close() the file is uploaded via S3TransferManager
        //     (multipart). The temp file is deleted regardless.
        // This keeps memory bounded for arbitrary-size uploads without
        // forcing every small file through TransferManager.
        return new SpillingPutOutputStream();
    }

    @Override
    public void delete() throws IOException {
        try {
            connection.client().deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(parsed.bucket())
                    .key(parsed.key())
                    .build());
            size = 0L;
            lastModified = 0L;
            directory = false;
            metadataKnown = true;
            lastMetadataFailure = null;
        } catch (S3Exception e) {
            throw toIOException(e, fileURL);
        }
    }

    @Override
    public void renameTo(AbstractFile destFile) throws IOException {
        if (!(destFile instanceof S3Object dest)) {
            throw new UnsupportedFileOperationException(
                dev.barebones.commander.commons.file.FileOperation.RENAME);
        }
        // S3 has no native rename — copy + delete.
        try {
            connection.client().copyObject(
                CopyObjectRequest.builder()
                    .sourceBucket(parsed.bucket())
                    .sourceKey(parsed.key())
                    .destinationBucket(dest.parsed.bucket())
                    .destinationKey(dest.parsed.key())
                    .build());
        } catch (S3Exception e) {
            throw toIOException(e, fileURL);
        }
        delete();
    }

    /**
     * OutputStream that uploads on close. Stays in-memory for small
     * payloads; spills to a temp file once a threshold is exceeded
     * and uploads from that file via {@link
     * software.amazon.awssdk.transfer.s3.S3TransferManager} (multipart).
     */
    private final class SpillingPutOutputStream extends OutputStream {

        /** Spill to disk when the in-memory buffer would exceed this. */
        private ByteArrayOutputStream memory = new ByteArrayOutputStream();
        private long bytesWritten;
        private Path spillFile;
        private OutputStream spillStream;
        private boolean closed;

        @Override
        public void write(int b) throws IOException {
            ensureCapacityForOneByte();
            target().write(b);
            bytesWritten++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (len <= 0) return;
            ensureCapacityForExtra(len);
            target().write(b, off, len);
            bytesWritten += len;
        }

        private OutputStream target() {
            return spillStream != null ? spillStream : memory;
        }

        private void ensureCapacityForOneByte() throws IOException {
            ensureCapacityForExtra(1);
        }

        private void ensureCapacityForExtra(int len) throws IOException {
            if (spillStream != null) return;
            if (bytesWritten + len <= Tunables.S3_UPLOAD_SPILL_THRESHOLD_BYTES) return;
            // Switch to temp-file mode; copy current memory buffer into it.
            spillFile = Files.createTempFile("barebones-s3-upload-", ".bin");
            spillStream = Files.newOutputStream(spillFile);
            memory.writeTo(spillStream);
            memory = null;
        }

        @Override
        public void close() throws IOException {
            if (closed) return;
            closed = true;
            try {
                if (spillStream == null) {
                    putFromMemory();
                } else {
                    spillStream.close();
                    uploadSpilledFile();
                }
                // Whichever path: refresh local metadata.
                size = bytesWritten;
                lastModified = System.currentTimeMillis();
                directory = false;
                metadataKnown = true;
            } finally {
                if (spillFile != null) {
                    try {
                        Files.deleteIfExists(spillFile);
                    } catch (IOException e) {
                        LOGGER.warn("Failed to delete S3 upload spill file {}", spillFile, e);
                    }
                }
            }
        }

        private void putFromMemory() throws IOException {
            byte[] payload = memory.toByteArray();
            try {
                connection.client().putObject(
                    PutObjectRequest.builder()
                        .bucket(parsed.bucket())
                        .key(parsed.key())
                        .contentLength((long) payload.length)
                        .build(),
                    RequestBody.fromBytes(payload));
            } catch (S3Exception e) {
                throw toIOException(e, fileURL);
            }
        }

        private void uploadSpilledFile() throws IOException {
            String key = parsed.key();
            String prefix = "Uploading to s3://" + parsed.bucket() + "/" + key + ": ";
            // LoggingTransferListener emits one log line at each
            // 10 % milestone. ProgressNotifier surfaces byte-accurate
            // progress to the status bar via a TransferListener that
            // fires on AWS SDK threads — without it, the FileJob
            // progress dialog reaches 100 % at end-of-spill and then
            // the upload runs invisibly during close().
            LOGGER.info("S3 multipart upload starting: {} ({} bytes) → s3://{}/{}",
                spillFile, bytesWritten, parsed.bucket(), key);
            ProgressNotifier.post(prefix + "starting");
            try {
                connection.transferManager()
                    .uploadFile(UploadFileRequest.builder()
                        .source(spillFile)
                        .addTransferListener(LoggingTransferListener.create())
                        .addTransferListener(new StatusBarProgressListener(prefix))
                        .putObjectRequest(PutObjectRequest.builder()
                            .bucket(parsed.bucket())
                            .key(key)
                            .build())
                        .build())
                    .completionFuture()
                    .join();
                LOGGER.info("S3 multipart upload complete: s3://{}/{} ({} bytes)",
                    parsed.bucket(), key, bytesWritten);
            } catch (CompletionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                if (cause instanceof S3Exception se) {
                    throw toIOException(se, fileURL);
                }
                throw new IOException("S3 multipart upload failed: " + cause.getMessage(), cause);
            } finally {
                ProgressNotifier.clear();
            }
        }
    }

    /**
     * Routes AWS SDK byte-transferred events into {@link ProgressNotifier}
     * with human-readable byte counts. Throttled to one publish per 250 ms
     * so a fast LAN upload doesn't spam the status bar (the hint flickers
     * unreadably otherwise). Always publishes the final 100 %.
     *
     * Fires on AWS SDK Netty threads; the message is unlocalised on
     * purpose — {@code SizeFormat} pulls in {@code Translator} which is
     * not initialised in test contexts (LocalStack tests run without
     * the UI Activator). Bytes are formatted in-place.
     */
    static final class StatusBarProgressListener implements TransferListener {

        private final String prefix;
        private long lastPublishNanos;

        StatusBarProgressListener(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void bytesTransferred(Context.BytesTransferred ctx) {
            ctx.progressSnapshot().totalBytes().ifPresent(total -> {
                long sent = ctx.progressSnapshot().transferredBytes();
                long now = System.nanoTime();
                boolean atEnd = sent >= total;
                if (!atEnd && now - lastPublishNanos < Tunables.S3_PROGRESS_PUBLISH_INTERVAL_NANOS) {
                    return;
                }
                lastPublishNanos = now;
                int pct = total > 0 ? (int) (sent * 100L / total) : 0;
                ProgressNotifier.post(prefix + formatBytes(sent) + " / "
                    + formatBytes(total) + " (" + pct + "%)");
            });
        }

        /** No-locale, no-allocation byte formatter. KiB / MiB / GiB. */
        static String formatBytes(long bytes) {
            if (bytes < 1024L) {
                return bytes + " B";
            }
            String[] units = {"KiB", "MiB", "GiB", "TiB"};
            double v = bytes;
            int u = -1;
            do {
                v /= 1024.0;
                u++;
            } while (v >= 1024.0 && u < units.length - 1);
            // One decimal place; English-style decimal separator. The
            // status-bar string is operator-facing, not localised.
            long whole = (long) v;
            long tenths = (long) ((v - whole) * 10);
            return whole + "." + tenths + " " + units[u];
        }
    }
}
