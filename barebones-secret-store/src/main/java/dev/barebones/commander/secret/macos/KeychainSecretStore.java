/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.secret.macos;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFDataRef;
import com.sun.jna.platform.mac.CoreFoundation.CFIndex;
import com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.ptr.PointerByReference;

import dev.barebones.commander.secret.SecretRef;
import dev.barebones.commander.secret.SecretStore;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/**
 * macOS Keychain-backed {@link SecretStore} via JNA bindings to
 * {@code Security.framework}'s generic-password SecItem API.
 *
 * Each {@link SecretRef} maps to one keychain item identified by the
 * (service, account) pair. On first store the user may see the
 * standard "barebones-commander wants to use your keychain" prompt;
 * on subsequent reads the keychain remembers our app and lets us
 * through silently.
 */
public final class KeychainSecretStore implements SecretStore {

    /**
     * Probe: try a no-op lookup to confirm the framework loads and
     * the user has a keychain. Throws {@link UnsatisfiedLinkError}
     * if Security.framework can't be loaded (only happens off-macOS).
     */
    public static boolean isAvailable() {
        try {
            // Just touching INSTANCE forces the native load — if we're
            // not on macOS or the framework isn't present this throws.
            return SecurityFramework.INSTANCE != null
                && SecurityFramework.Constants.SEC_CLASS != null;
        } catch (LinkageError | RuntimeException e) {
            return false;
        }
    }

    @Override
    public void store(SecretRef ref, char[] secret) throws IOException {
        byte[] password = toUtf8Bytes(secret);
        try (SecItemQuery addQuery = SecItemQuery.base(ref).withPasswordData(password);
             SecItemQuery updateQuery = SecItemQuery.passwordData(password)) {
            int status = SecurityFramework.INSTANCE.SecItemAdd(addQuery.dictionary(), null);
            if (status == SecurityFramework.DUPLICATE_ITEM) {
                try (SecItemQuery baseQuery = SecItemQuery.base(ref)) {
                    status = SecurityFramework.INSTANCE.SecItemUpdate(
                        baseQuery.dictionary(), updateQuery.dictionary());
                }
            }
            if (status != SecurityFramework.OK) {
                throw new IOException(
                    "SecItemAdd/SecItemUpdate failed: status=" + status);
            }
        } finally {
            Arrays.fill(password, (byte) 0);
        }
    }

    @Override
    public Optional<char[]> lookup(SecretRef ref) throws IOException {
        PointerByReference result = new PointerByReference();
        try (SecItemQuery query = SecItemQuery.base(ref)
            .withMatchLimitOne()
            .withReturnData()) {
            int status = SecurityFramework.INSTANCE.SecItemCopyMatching(
                query.dictionary(), result);
            if (status == SecurityFramework.ITEM_NOT_FOUND) {
                return Optional.empty();
            }
            if (status != SecurityFramework.OK) {
                throw new IOException("SecItemCopyMatching failed: status=" + status);
            }
            CFDataRef data = new CFDataRef(result.getValue());
            byte[] bytes = data.getBytePtr().getByteArray(0, data.getLength());
            char[] chars = utf8BytesToChars(bytes);
            Arrays.fill(bytes, (byte) 0);
            return Optional.of(chars);
        } finally {
            Pointer p = result.getValue();
            if (p != null) {
                SecurityFramework.release(new CFTypeRef(p));
            }
        }
    }

    @Override
    public void delete(SecretRef ref) throws IOException {
        try (SecItemQuery query = SecItemQuery.base(ref)) {
            int status = SecurityFramework.INSTANCE.SecItemDelete(query.dictionary());
            if (status != SecurityFramework.OK && status != SecurityFramework.ITEM_NOT_FOUND) {
                throw new IOException(
                    "SecItemDelete failed: status=" + status);
            }
        }
    }

    @Override
    public String backendName() {
        return "macos-keychain";
    }

    private static byte[] toUtf8Bytes(char[] chars) {
        ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        byte[] out = new byte[bb.remaining()];
        bb.get(out);
        return out;
    }

    private static char[] utf8BytesToChars(byte[] bytes) {
        CharBuffer cb = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));
        char[] out = new char[cb.remaining()];
        cb.get(out);
        return out;
    }

    static byte[] utf8BytesForTest(char[] chars) {
        return toUtf8Bytes(chars);
    }

    static char[] utf8CharsForTest(byte[] bytes) {
        return utf8BytesToChars(bytes);
    }

    private static final class SecItemQuery implements AutoCloseable {
        private final CFMutableDictionaryRef dictionary;
        private final java.util.List<CFTypeRef> ownedRefs = new java.util.ArrayList<>();

        private SecItemQuery() {
            dictionary = CoreFoundation.INSTANCE.CFDictionaryCreateMutable(
                null, new CFIndex(0), null, null);
            if (dictionary == null || dictionary.getPointer() == null) {
                throw new IllegalStateException("CFDictionaryCreateMutable returned NULL");
            }
        }

        static SecItemQuery base(SecretRef ref) {
            return new SecItemQuery()
                .put(SecurityFramework.Constants.SEC_CLASS,
                    SecurityFramework.Constants.SEC_CLASS_GENERIC_PASSWORD)
                .putString(SecurityFramework.Constants.ATTR_SERVICE, ref.service())
                .putString(SecurityFramework.Constants.ATTR_ACCOUNT, ref.account());
        }

        static SecItemQuery passwordData(byte[] password) {
            return new SecItemQuery().withPasswordData(password);
        }

        SecItemQuery withPasswordData(byte[] password) {
            return putData(SecurityFramework.Constants.VALUE_DATA, password);
        }

        SecItemQuery withReturnData() {
            return put(SecurityFramework.Constants.RETURN_DATA,
                SecurityFramework.Constants.CF_BOOLEAN_TRUE);
        }

        SecItemQuery withMatchLimitOne() {
            return put(SecurityFramework.Constants.MATCH_LIMIT,
                SecurityFramework.Constants.MATCH_LIMIT_ONE);
        }

        CFMutableDictionaryRef dictionary() {
            return dictionary;
        }

        private SecItemQuery put(CFStringRef key, CFTypeRef value) {
            dictionary.setValue(key, value);
            return this;
        }

        private SecItemQuery putString(CFStringRef key, String value) {
            CFStringRef ref = CFStringRef.createCFString(value);
            ownedRefs.add(ref);
            dictionary.setValue(key, ref);
            return this;
        }

        private SecItemQuery putData(CFStringRef key, byte[] value) {
            Pointer data = null;
            if (value.length > 0) {
                Memory memory = new Memory(value.length);
                memory.write(0, value, 0, value.length);
                data = memory;
            }
            CFDataRef ref = CoreFoundation.INSTANCE.CFDataCreate(
                null, data, new CFIndex(value.length));
            if (ref == null || ref.getPointer() == null) {
                throw new IllegalStateException("CFDataCreate returned NULL");
            }
            ownedRefs.add(ref);
            dictionary.setValue(key, ref);
            return this;
        }

        @Override
        public void close() {
            for (int i = ownedRefs.size() - 1; i >= 0; i--) {
                SecurityFramework.release(ownedRefs.get(i));
            }
            SecurityFramework.release(dictionary);
        }
    }
}
