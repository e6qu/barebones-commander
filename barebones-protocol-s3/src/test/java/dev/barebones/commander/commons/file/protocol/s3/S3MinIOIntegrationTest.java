/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.commons.file.protocol.s3;

import dev.barebones.commander.commons.file.AbstractFile;
import dev.barebones.commander.commons.file.AuthenticationType;
import dev.barebones.commander.commons.file.Credentials;
import dev.barebones.commander.commons.file.DefaultSchemeHandler;
import dev.barebones.commander.commons.file.DefaultSchemeParser;
import dev.barebones.commander.commons.file.FileFactory;
import dev.barebones.commander.commons.file.FileURL;
import dev.barebones.commander.commons.file.SchemeHandler;
import dev.barebones.commander.commons.file.osgi.FileProtocolService;
import dev.barebones.commander.commons.file.osgi.FileProtocolServiceTracker;
import dev.barebones.commander.commons.file.protocol.ProtocolProvider;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static dev.barebones.commander.test.TestAssertions.assertEquals;
import static dev.barebones.commander.test.TestAssertions.assertFalse;
import static dev.barebones.commander.test.TestAssertions.assertTrue;

/**
 * End-to-end tests against MinIO, not LocalStack, to verify the
 * S3-compatible/path-style endpoint behavior used by self-hosted
 * deployments.
 */
public class S3MinIOIntegrationTest {

    private static final DockerImageName MINIO_IMAGE =
        DockerImageName.parse("minio/minio:RELEASE.2025-09-07T16-13-09Z");
    private static final int MINIO_API_PORT = 9000;
    private static final String ACCESS_KEY = "minioadmin";
    private static final String SECRET_KEY = "minioadmin";
    private static final String REGION = "us-east-1";

    private GenericContainer<?> container;
    private S3ProtocolProvider provider;

    @BeforeAll
    public void startMinIO() throws Exception {
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            throw new TestAbortedException(
                "Docker is not available on this runner; skipping MinIO S3 integration tests.");
        }

        container = new GenericContainer<>(MINIO_IMAGE)
            .withExposedPorts(MINIO_API_PORT)
            .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
            .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
            .withCommand("server", "/data", "--address", ":9000")
            .waitingFor(Wait.forHttp("/minio/health/ready").forPort(MINIO_API_PORT));
        container.start();

        SchemeHandler handler = new DefaultSchemeHandler(
            new DefaultSchemeParser(), 443, "/",
            AuthenticationType.AUTHENTICATION_REQUIRED, null);
        Method m = FileURL.class.getDeclaredMethod("registerHandler",
            String.class, SchemeHandler.class);
        m.setAccessible(true);
        m.invoke(null, "s3", handler);

        provider = new S3ProtocolProvider();
        FileProtocolServiceTracker.register(new FileProtocolService() {
            @Override public String getSchema() { return "s3"; }
            @Override public ProtocolProvider getProtocolProvider() { return provider; }
            @Override public SchemeHandler getSchemeHandler() { return handler; }
        });
    }

    @AfterAll
    public void stopMinIO() {
        if (provider != null) {
            provider.close();
        }
        if (container != null) {
            container.stop();
        }
    }

    private FileURL urlFor(String path) throws Exception {
        FileURL url = FileURL.getFileURL("s3://" + container.getHost() + path);
        url.setPort(container.getMappedPort(MINIO_API_PORT));
        url.setCredentials(new Credentials(ACCESS_KEY, SECRET_KEY));
        url.setProperty(S3ProtocolProvider.PROPERTY_REGION, REGION);
        url.setProperty(S3ProtocolProvider.PROPERTY_PATH_STYLE, "true");
        url.setProperty(S3ProtocolProvider.PROPERTY_USE_HTTPS, "false");
        return url;
    }

    @Test
    public void minioPathStyleLifecycle() throws Exception {
        String bucketName = "minio-" + UUID.randomUUID().toString().substring(0, 8);
        AbstractFile bucket = FileFactory.getFile(urlFor("/" + bucketName + "/"));
        assertTrue(bucket instanceof S3Bucket);
        bucket.mkdir();
        assertTrue(bucket.exists(), "bucket should exist after mkdir");

        AbstractFile object = FileFactory.getFile(urlFor("/" + bucketName + "/hello.txt"));
        byte[] payload = "hello minio world".getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = object.getOutputStream()) {
            os.write(payload);
        }
        assertTrue(object.exists(), "object should exist after upload");
        assertEquals(object.getSize(), payload.length);

        try (InputStream is = object.getInputStream()) {
            assertEquals(is.readAllBytes(), payload);
        }

        AbstractFile dir = FileFactory.getFile(urlFor("/" + bucketName + "/sub/"));
        dir.mkdir();
        AbstractFile nested = FileFactory.getFile(urlFor("/" + bucketName + "/sub/inner.txt"));
        byte[] nestedPayload = "nested".getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = nested.getOutputStream()) {
            os.write(nestedPayload);
        }

        Set<String> bucketNames = names(bucket.ls());
        assertTrue(bucketNames.contains("hello.txt"), "bucket listing should include object " + bucketNames);
        assertTrue(bucketNames.contains("sub"), "bucket listing should include prefix " + bucketNames);

        AbstractFile[] innerListing = dir.ls();
        assertEquals(innerListing.length, 1);
        assertEquals(innerListing[0].getName(), "inner.txt");

        nested.delete();
        assertFalse(nested.exists(), "nested object should be gone after delete");
        object.delete();
        assertFalse(object.exists(), "object should be gone after delete");
    }

    @Test
    public void minioRenameCopiesAndDeletesSource() throws Exception {
        String bucketName = "rename-" + UUID.randomUUID().toString().substring(0, 8);
        AbstractFile bucket = FileFactory.getFile(urlFor("/" + bucketName + "/"));
        bucket.mkdir();

        AbstractFile src = FileFactory.getFile(urlFor("/" + bucketName + "/source.txt"));
        try (OutputStream os = src.getOutputStream()) {
            os.write("rename me".getBytes(StandardCharsets.UTF_8));
        }

        AbstractFile dest = FileFactory.getFile(urlFor("/" + bucketName + "/dest.txt"));
        src.renameTo(dest);

        Set<String> names = names(bucket.ls());
        assertTrue(names.contains("dest.txt"));
        assertFalse(names.contains("source.txt"), "source.txt should be gone after rename, got " + names);

        try (InputStream is = dest.getInputStream()) {
            assertEquals(new String(is.readAllBytes(), StandardCharsets.UTF_8), "rename me");
        }
    }

    private static Set<String> names(AbstractFile[] files) {
        Set<String> names = new HashSet<>();
        for (AbstractFile file : files) {
            names.add(file.getName());
        }
        return names;
    }
}
