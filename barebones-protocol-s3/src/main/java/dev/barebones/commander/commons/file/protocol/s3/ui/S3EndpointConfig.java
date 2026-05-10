/*
 * Copyright (C) 2002-2026 muCommander contributors
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.commons.file.protocol.s3.ui;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

record S3EndpointConfig(String host, int port, boolean useHttps) {

    static S3EndpointConfig parse(String endpoint, int configuredPort, boolean configuredUseHttps)
            throws MalformedURLException {
        if (endpoint == null || endpoint.isBlank()) {
            throw new MalformedURLException("S3 endpoint must not be blank");
        }

        String trimmed = endpoint.trim();
        if (trimmed.contains("://")) {
            return parseAbsoluteEndpoint(trimmed, configuredPort);
        }
        return parseHostEndpoint(trimmed, configuredPort, configuredUseHttps);
    }

    private static S3EndpointConfig parseAbsoluteEndpoint(String endpoint, int configuredPort)
            throws MalformedURLException {
        URI uri;
        try {
            uri = new URI(endpoint);
        } catch (URISyntaxException e) {
            MalformedURLException malformed = new MalformedURLException("Invalid S3 endpoint URL: " + endpoint);
            malformed.initCause(e);
            throw malformed;
        }

        String scheme = uri.getScheme();
        boolean useHttps;
        if ("https".equalsIgnoreCase(scheme)) {
            useHttps = true;
        } else if ("http".equalsIgnoreCase(scheme)) {
            useHttps = false;
        } else {
            throw new MalformedURLException("S3 endpoint URL must use http or https");
        }
        if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new MalformedURLException("S3 endpoint URL must not include user info, query, or fragment");
        }
        String path = uri.getPath();
        if (path != null && !path.isEmpty() && !"/".equals(path)) {
            throw new MalformedURLException("S3 endpoint URL must not include a path; use the Bucket field instead");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new MalformedURLException("S3 endpoint URL must include a host");
        }
        int port = uri.getPort() > 0 ? uri.getPort() : configuredPort;
        return new S3EndpointConfig(host, port, useHttps);
    }

    private static S3EndpointConfig parseHostEndpoint(String endpoint, int configuredPort, boolean configuredUseHttps)
            throws MalformedURLException {
        URI uri;
        try {
            uri = new URI("s3://" + endpoint);
        } catch (URISyntaxException e) {
            MalformedURLException malformed = new MalformedURLException("Invalid S3 endpoint host: " + endpoint);
            malformed.initCause(e);
            throw malformed;
        }
        if (uri.getPath() != null && !uri.getPath().isEmpty()) {
            throw new MalformedURLException("S3 endpoint host must not include a path; use the Bucket field instead");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new MalformedURLException("S3 endpoint host must not be blank");
        }
        int port = uri.getPort() > 0 ? uri.getPort() : configuredPort;
        return new S3EndpointConfig(host, port, configuredUseHttps);
    }
}
