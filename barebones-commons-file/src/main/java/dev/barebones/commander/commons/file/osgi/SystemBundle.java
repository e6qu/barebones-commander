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
package dev.barebones.commander.commons.file.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

/**
 * Stub {@link Bundle} returned by {@link LocalBundleContext#getBundle()}.
 * Most accessors return inert defaults; callers in this project never
 * inspect bundle metadata.
 */
final class SystemBundle implements Bundle {

    private final BundleContext context;

    SystemBundle(BundleContext context) {
        this.context = context;
    }

    @Override public int getState() { return ACTIVE; }
    @Override public void start(int options) { }
    @Override public void start() { }
    @Override public void stop(int options) { }
    @Override public void stop() { }
    @Override public void update() { }
    @Override public void update(InputStream input) { }
    @Override public void uninstall() { }
    @Override public Dictionary<String, String> getHeaders() { return new Hashtable<>(); }
    @Override public long getBundleId() { return 0; }
    @Override public String getLocation() { return "system"; }
    @Override public ServiceReference<?>[] getRegisteredServices() { return new ServiceReference[0]; }
    @Override public ServiceReference<?>[] getServicesInUse() { return new ServiceReference[0]; }
    @Override public boolean hasPermission(Object permission) { return true; }
    @Override public URL getResource(String name) { return getClass().getClassLoader().getResource(name); }
    @Override public Dictionary<String, String> getHeaders(String locale) { return new Hashtable<>(); }
    @Override public String getSymbolicName() { return "barebones-commander"; }
    @Override public Class<?> loadClass(String name) throws ClassNotFoundException { return Class.forName(name); }
    @Override public Enumeration<URL> getResources(String name) throws java.io.IOException { return getClass().getClassLoader().getResources(name); }
    @Override public Enumeration<String> getEntryPaths(String path) { return Collections.emptyEnumeration(); }
    @Override public URL getEntry(String path) { return null; }
    @Override public long getLastModified() { return 0L; }
    @Override public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) { return Collections.emptyEnumeration(); }
    @Override public BundleContext getBundleContext() { return context; }
    @Override public Map<X509Certificate, java.util.List<X509Certificate>> getSignerCertificates(int signersType) { return Collections.emptyMap(); }
    @Override public Version getVersion() { return Version.emptyVersion; }
    @Override public <A> A adapt(Class<A> type) { return null; }
    @Override public java.io.File getDataFile(String filename) { return null; }
    @Override public int compareTo(Bundle o) { return 0; }
}
