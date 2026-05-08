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
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Backing {@link ServiceRegistration} for {@link LocalBundleContext}.
 * Holds the registered service instance, its types, and a small properties
 * map (just {@code service.id} and {@code objectClass}, plus anything the
 * caller passed in).
 */
final class Registration<S> implements ServiceRegistration<S> {

    private final LocalBundleContext owner;
    private final String[] classes;
    private final S service;
    private final Hashtable<String, Object> properties;
    private final long id;
    private final Ref reference;
    private volatile boolean unregistered;

    @SuppressWarnings({"rawtypes", "unchecked"})
    Registration(LocalBundleContext owner, String[] classes, Object service, Dictionary<String, ?> input, long id) {
        this.owner = owner;
        this.classes = classes;
        this.service = (S) service;
        this.id = id;
        this.properties = new Hashtable<>();
        if (input != null) {
            java.util.Enumeration<String> keys = (java.util.Enumeration<String>) input.keys();
            while (keys.hasMoreElements()) {
                String k = keys.nextElement();
                this.properties.put(k, input.get(k));
            }
        }
        this.properties.put("service.id", id);
        this.properties.put("objectClass", classes);
        this.reference = new Ref();
    }

    String[] classes() { return classes; }
    S service() { return service; }

    @Override
    public ServiceReference<S> getReference() {
        return reference;
    }

    @Override
    public void setProperties(Dictionary<String, ?> properties) {
        // Ignore. No caller in this project mutates properties post-registration.
    }

    @Override
    public void unregister() {
        if (unregistered) return;
        unregistered = true;
        owner.unregister(this);
    }

    /**
     * Inner class so the {@link ServiceReference} carries a back-pointer to
     * the {@link Registration} without exposing it on the public API.
     */
    final class Ref implements ServiceReference<S> {

        final Registration<S> owner = Registration.this;

        @Override
        public Object getProperty(String key) {
            return properties.get(key);
        }

        @Override
        public String[] getPropertyKeys() {
            return Collections.list(properties.keys()).toArray(new String[0]);
        }

        @Override
        public Bundle getBundle() {
            return Registration.this.owner.getBundle();
        }

        @Override
        public Bundle[] getUsingBundles() {
            return null;
        }

        @Override
        public boolean isAssignableTo(Bundle bundle, String className) {
            for (String c : classes) {
                if (c.equals(className)) return true;
            }
            return false;
        }

        @Override
        public int compareTo(Object o) {
            if (!(o instanceof Ref)) return 0;
            return Long.compare(((Ref) o).owner.id, id);
        }

        @Override
        public java.util.Dictionary<String, Object> getProperties() {
            Hashtable<String, Object> copy = new Hashtable<>(properties);
            return copy;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <A> A adapt(Class<A> type) {
            return null;
        }
    }
}
