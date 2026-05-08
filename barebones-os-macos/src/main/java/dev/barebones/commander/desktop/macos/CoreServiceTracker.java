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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dev.barebones.commander.desktop.macos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.barebones.commander.os.api.CoreService;

/**
 * Static holder for the {@link CoreService} singleton. Was an OSGi
 * {@code ServiceTracker} pre-Phase-2; is now a plain holder.
 */
public final class CoreServiceTracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreServiceTracker.class);

    private static volatile CoreService service;

    private CoreServiceTracker() {
    }

    public static void register(CoreService coreService) {
        service = coreService;
        LOGGER.info("CoreService is registered: " + coreService);
    }

    public static void unregister(CoreService coreService) {
        if (service == coreService) {
            service = null;
        }
        LOGGER.info("CoreService is unregistered: " + coreService);
    }

    public static CoreService getCoreService() {
        return service;
    }
}
