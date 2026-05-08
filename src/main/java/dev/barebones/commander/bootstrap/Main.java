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

import com.beust.jcommander.JCommander;
import dev.barebones.commander.commons.file.osgi.LocalBundleContext;
import dev.barebones.commander.main.Configuration;
import dev.barebones.commander.main.UserPreferencesDir;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * New non-Felix entrypoint for barebones-commander.
 *
 * Parses CLI arguments via JCommander (same {@link Configuration} the
 * Felix launcher used), assembles a property map, and hands off to
 * {@link Bootstrap#start(Map)} which walks every Activator's
 * {@code start(BundleContext)} on a {@link LocalBundleContext}.
 *
 * The Felix-specific pieces of the old launcher (Felix config, framework
 * init/start/wait, AutoProcessor.process) are gone. Felix is no longer
 * on the runtime classpath.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        Configuration configuration = new Configuration();
        JCommander jCommander = new JCommander(configuration);
        jCommander.parse(args);

        if (configuration.help) {
            jCommander.setProgramName("barebones-commander");
            jCommander.usage();
            return;
        }

        if (configuration.version) {
            String version = Main.class.getPackage().getImplementationVersion();
            jCommander.getConsole().println(version != null ? version : "0.1.0-SNAPSHOT");
            return;
        }

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Error: no graphical environment detected.");
            return;
        }

        // Resolve the user preferences folder before Bootstrap so logback can
        // pick up MUCOMMANDER_USER_PREFERENCES on first init.
        File preferencesFolder = configuration.preferences != null
                ? new File(configuration.preferences)
                : UserPreferencesDir.getDefaultPreferencesFolder();
        configuration.preferences = preferencesFolder.getAbsolutePath();
        System.setProperty("MUCOMMANDER_USER_PREFERENCES", configuration.preferences);

        // Build the property map the Activators read via BundleContext.getProperty.
        Map<String, String> properties = new HashMap<>();
        properties.putAll(new AbstractMap<String, String>() {
            @Override
            public java.util.Set<Map.Entry<String, String>> entrySet() {
                return configuration.entrySet();
            }
        });

        LocalBundleContext context = Bootstrap.start(properties);

        Runtime.getRuntime().addShutdownHook(new Thread("barebones-commander-shutdown") {
            @Override
            public void run() {
                try {
                    Bootstrap.stop(context);
                } catch (Throwable ignored) {
                    // best-effort
                }
            }
        });
    }
}
