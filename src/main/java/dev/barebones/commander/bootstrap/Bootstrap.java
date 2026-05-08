/*
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
package dev.barebones.commander.bootstrap;

import dev.barebones.commander.commons.file.osgi.LocalBundleContext;
import org.osgi.framework.BundleActivator;

import java.util.Map;

/**
 * Replacement for the Apache Felix container.
 *
 * Creates an in-process {@link LocalBundleContext} from a property map
 * (typically built from CLI args), then walks each {@code Activator}
 * class and calls its {@code start(BundleContext)} in dependency order.
 *
 * Activator class names are listed by FQN and resolved with
 * {@link Class#forName(String)}, so this launcher only needs the
 * subproject jars on the runtime classpath — it does not have a
 * compile-time dep on each subproject.
 *
 * The order matters: {@code commons-file}'s Activator opens the file
 * service trackers first so subsequent producer Activators register
 * services into a context that is already listening; the {@code core}
 * Activator runs last because its {@code start()} eventually shows the
 * Swing UI.
 */
public final class Bootstrap {

    /**
     * Activators in dependency order. Anything not on the runtime
     * classpath is silently skipped — that lets the Linux Activator and
     * macOS Activator both appear in the list with only one of them
     * actually loading on a given JVM (e.g., the macOS jar is excluded
     * from the Linux installer).
     */
    private static final String[] ACTIVATOR_CLASSES = {
            // commons-file first so trackers are listening before producers register
            "dev.barebones.commander.commons.file.osgi.Activator",
            // basic services
            "dev.barebones.commander.text.Activator",
            "dev.barebones.commander.conf.Activator",
            "dev.barebones.commander.preload.Activator",
            // protocols
            "dev.barebones.commander.commons.file.protocol.sftp.Activator",
            "dev.barebones.commander.commons.file.protocol.s3.Activator",
            "dev.barebones.commander.commons.file.protocol.nfs.Activator",
            // archive formats
            "dev.barebones.commander.commons.file.archive.zip.Activator",
            "dev.barebones.commander.commons.file.archive.tar.Activator",
            "dev.barebones.commander.commons.file.archive.gzip.Activator",
            "dev.barebones.commander.commons.file.archive.bzip2.Activator",
            "dev.barebones.commander.commons.file.archive.xz.Activator",
            // text viewer
            "dev.barebones.commander.viewer.text.Activator",
            // os adapters (one per OS — load-class will fail silently for the wrong one)
            "dev.barebones.commander.desktop.linux.Activator",
            "dev.barebones.commander.desktop.macos.Activator",
            // core last — its start() ends up showing the UI
            "dev.barebones.commander.Activator",
    };

    private Bootstrap() {
    }

    /**
     * Creates a {@link LocalBundleContext} from {@code properties} and
     * runs every Activator's {@code start(BundleContext)} in order.
     *
     * @return the live bundle context (kept by the caller for an
     *     orderly shutdown via {@link #stop(LocalBundleContext)}).
     */
    public static LocalBundleContext start(Map<String, String> properties) throws Exception {
        LocalBundleContext context = new LocalBundleContext(properties);
        for (String className : ACTIVATOR_CLASSES) {
            BundleActivator activator;
            try {
                activator = (BundleActivator) Class.forName(className).getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException notFound) {
                // Module not on the runtime classpath (e.g. the wrong-OS adapter). Skip silently.
                continue;
            }
            activator.start(context);
        }
        return context;
    }

    /**
     * Best-effort shutdown. Each Activator's {@code stop} is invoked in
     * reverse order; exceptions are swallowed so a failing module does
     * not prevent the rest from cleaning up.
     */
    public static void stop(LocalBundleContext context) {
        for (int i = ACTIVATOR_CLASSES.length - 1; i >= 0; i--) {
            try {
                BundleActivator activator = (BundleActivator) Class.forName(ACTIVATOR_CLASSES[i]).getDeclaredConstructor().newInstance();
                activator.stop(context);
            } catch (Throwable ignored) {
                // best-effort
            }
        }
    }
}
