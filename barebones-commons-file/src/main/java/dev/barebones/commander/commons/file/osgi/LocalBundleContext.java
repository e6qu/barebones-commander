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
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-process replacement for the OSGi container's {@link BundleContext}.
 *
 * Implements just enough of the OSGi service-registry API to satisfy the
 * existing {@link org.osgi.framework.BundleActivator} + {@link org.osgi.util.tracker.ServiceTracker}
 * pattern used throughout barebones-commander, so we can drop Apache Felix
 * without rewriting every Activator and Tracker. Bundle / framework
 * lifecycle methods are stubbed.
 *
 * Filter handling is intentionally minimal: only the {@code (objectClass=...)}
 * shape produced by {@code ServiceTracker(BundleContext, Class<?>, ...)}
 * is recognised. That is the only filter shape any tracker in this
 * project uses.
 */
public final class LocalBundleContext implements BundleContext {

    private final Map<String, String> properties;
    private final List<Registration<?>> services = new CopyOnWriteArrayList<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong nextServiceId = new AtomicLong(1);
    private final Bundle bundle;

    public LocalBundleContext(Map<String, String> properties) {
        this.properties = new ConcurrentHashMap<>(properties);
        this.bundle = new SystemBundle(this);
    }

    public LocalBundleContext() {
        this(Collections.emptyMap());
    }

    @Override
    public String getProperty(String key) {
        String v = properties.get(key);
        return v != null ? v : System.getProperty(key);
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public Bundle getBundle(long id) {
        return id == 0 ? bundle : null;
    }

    @Override
    public Bundle getBundle(String location) {
        return null;
    }

    @Override
    public Bundle[] getBundles() {
        return new Bundle[]{bundle};
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
        Registration<S> reg = new Registration<>(this, new String[]{clazz.getName()}, service, properties, nextServiceId.getAndIncrement());
        services.add(reg);
        fire(new ServiceEvent(ServiceEvent.REGISTERED, reg.getReference()));
        return reg;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) {
        return registerService(new String[]{clazz}, service, properties);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ServiceRegistration<?> registerService(String[] classes, Object service, Dictionary<String, ?> properties) {
        Registration reg = new Registration(this, classes.clone(), service, properties, nextServiceId.getAndIncrement());
        services.add(reg);
        fire(new ServiceEvent(ServiceEvent.REGISTERED, reg.getReference()));
        return reg;
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, org.osgi.framework.ServiceFactory<S> factory, Dictionary<String, ?> properties) {
        throw new UnsupportedOperationException("ServiceFactory not supported by LocalBundleContext");
    }

    @Override
    public ServiceReference<?>[] getServiceReferences(String clazz, String filter) {
        return matching(clazz).stream().map(Registration::getReference).toArray(ServiceReference<?>[]::new);
    }

    @Override
    public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) {
        return getServiceReferences(clazz, filter);
    }

    @Override
    public ServiceReference<?> getServiceReference(String clazz) {
        ServiceReference<?>[] refs = getServiceReferences(clazz, (String) null);
        return refs.length == 0 ? null : refs[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
        return (ServiceReference<S>) getServiceReference(clazz.getName());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter) {
        List<ServiceReference<S>> out = new ArrayList<>();
        for (Registration<?> r : matching(clazz.getName())) {
            out.add((ServiceReference<S>) r.getReference());
        }
        return out;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> S getService(ServiceReference<S> reference) {
        return ((Registration<S>) ((Registration.Ref) reference).owner).service();
    }

    @Override
    public boolean ungetService(ServiceReference<?> reference) {
        return true;
    }

    @Override
    public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) {
        throw new UnsupportedOperationException("ServiceObjects not supported by LocalBundleContext");
    }

    @Override
    public void addServiceListener(ServiceListener listener) {
        listeners.add(new Listener(listener, null));
    }

    @Override
    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        listeners.add(new Listener(listener, parseClassFilter(filter)));
    }

    @Override
    public void removeServiceListener(ServiceListener listener) {
        listeners.removeIf(l -> l.delegate == listener);
    }

    @Override public void addBundleListener(BundleListener listener) {}
    @Override public void removeBundleListener(BundleListener listener) {}
    @Override public void addFrameworkListener(FrameworkListener listener) {}
    @Override public void removeFrameworkListener(FrameworkListener listener) {}

    @Override
    public Filter createFilter(String filter) {
        final String objectClass = parseClassFilter(filter);
        return new Filter() {
            @Override public boolean match(ServiceReference<?> reference) {
                if (objectClass == null) return true;
                Object oc = reference.getProperty("objectClass");
                if (oc instanceof String[]) {
                    for (String s : (String[]) oc) if (objectClass.equals(s)) return true;
                }
                return false;
            }
            @Override public boolean match(Dictionary<String, ?> dictionary) { return false; }
            @Override public boolean matchCase(Dictionary<String, ?> dictionary) { return false; }
            @Override public boolean matches(Map<String, ?> map) { return false; }
            @Override public String toString() { return filter; }
        };
    }

    @Override public Bundle installBundle(String s) { throw new UnsupportedOperationException(); }
    @Override public Bundle installBundle(String s, InputStream stream) { throw new UnsupportedOperationException(); }
    @Override public File getDataFile(String s) { return null; }

    void unregister(Registration<?> reg) {
        if (services.remove(reg)) {
            fire(new ServiceEvent(ServiceEvent.UNREGISTERING, reg.getReference()));
        }
    }

    private void fire(ServiceEvent event) {
        for (Listener l : listeners) {
            if (l.objectClass == null || matchesObjectClass(event.getServiceReference(), l.objectClass)) {
                try {
                    l.delegate.serviceChanged(event);
                } catch (RuntimeException e) {
                    // Listener exceptions must not break registration of other listeners.
                }
            }
        }
    }

    private boolean matchesObjectClass(ServiceReference<?> ref, String objectClass) {
        Object oc = ref.getProperty("objectClass");
        if (oc instanceof String[]) {
            for (String s : (String[]) oc) if (objectClass.equals(s)) return true;
        }
        return false;
    }

    private List<Registration<?>> matching(String clazz) {
        if (clazz == null) {
            return new ArrayList<>(services);
        }
        List<Registration<?>> out = new ArrayList<>();
        for (Registration<?> r : services) {
            for (String c : r.classes()) {
                if (c.equals(clazz)) {
                    out.add(r);
                    break;
                }
            }
        }
        return out;
    }

    /**
     * Parses {@code (objectClass=foo.Bar)} → {@code "foo.Bar"}. Returns null
     * for any other shape (unrecognised filters match everything, which is
     * fine because no caller in this project uses other filter shapes).
     */
    private static String parseClassFilter(String filter) {
        if (filter == null) return null;
        String f = filter.trim();
        if (f.startsWith("(objectClass=") && f.endsWith(")")) {
            return f.substring("(objectClass=".length(), f.length() - 1);
        }
        return null;
    }

    private static final class Listener {
        final ServiceListener delegate;
        final String objectClass;
        Listener(ServiceListener delegate, String objectClass) {
            this.delegate = delegate;
            this.objectClass = objectClass;
        }
    }
}
