/*
 * Copyright (C) 2002-2026 muCommander contributors
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package dev.barebones.commander.ui.main.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.barebones.commander.protocol.ui.ProtocolPanelProvider;
import dev.barebones.commander.ui.dialog.server.ServerConnectDialog;
import dev.barebones.commander.ui.main.DrivePopupButton;

/**
 * Static fan-out for {@link ProtocolPanelProvider} registrations. Was an
 * OSGi {@code ServiceTracker} pre-Phase-2; is now a plain registry that
 * wires the provider into {@link ServerConnectDialog} and (if it has a
 * panel class) {@link DrivePopupButton}.
 */
public final class ProtocolPanelProviderTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolPanelProviderTracker.class);

    private ProtocolPanelProviderTracker() {
    }

    public static void register(ProtocolPanelProvider service) {
        ServerConnectDialog.register(service);
        if (service.getPanelClass() != null) {
            DrivePopupButton.register(service);
        }
        LOGGER.info("ProtocolPanelProvider is registered: " + service);
    }

    public static void unregister(ProtocolPanelProvider service) {
        ServerConnectDialog.unregister(service);
        DrivePopupButton.unregister(service);
        LOGGER.info("ProtocolPanelProvider is unregistered: " + service);
    }
}
