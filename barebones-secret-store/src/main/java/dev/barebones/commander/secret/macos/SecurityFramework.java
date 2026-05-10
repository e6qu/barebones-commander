/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.secret.macos;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.platform.mac.CoreFoundation.CFBooleanRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.ptr.PointerByReference;

/**
 * JNA binding for the subset of macOS {@code Security.framework}
 * that we need for keychain-backed credential storage.
 */
public interface SecurityFramework extends Library {

    SecurityFramework INSTANCE = Native.load("Security", SecurityFramework.class);

    /** errSecSuccess */
    int OK = 0;
    /** errSecItemNotFound */
    int ITEM_NOT_FOUND = -25300;
    /** errSecDuplicateItem */
    int DUPLICATE_ITEM = -25299;

    int SecItemAdd(CFDictionaryRef attributes, PointerByReference result);

    int SecItemCopyMatching(CFDictionaryRef query, PointerByReference result);

    int SecItemUpdate(CFDictionaryRef query, CFDictionaryRef attributesToUpdate);

    int SecItemDelete(CFDictionaryRef query);

    final class Constants {
        static final CFStringRef SEC_CLASS = securityString("kSecClass");
        static final CFStringRef SEC_CLASS_GENERIC_PASSWORD = securityString("kSecClassGenericPassword");
        static final CFStringRef ATTR_SERVICE = securityString("kSecAttrService");
        static final CFStringRef ATTR_ACCOUNT = securityString("kSecAttrAccount");
        static final CFStringRef VALUE_DATA = securityString("kSecValueData");
        static final CFStringRef RETURN_DATA = securityString("kSecReturnData");
        static final CFStringRef MATCH_LIMIT = securityString("kSecMatchLimit");
        static final CFStringRef MATCH_LIMIT_ONE = securityString("kSecMatchLimitOne");
        static final CFBooleanRef CF_BOOLEAN_TRUE = coreFoundationBoolean("kCFBooleanTrue");

        private Constants() {
        }

        private static CFStringRef securityString(String symbol) {
            return new CFStringRef(
                NativeLibrary.getInstance("Security")
                    .getGlobalVariableAddress(symbol)
                    .getPointer(0));
        }

        private static CFBooleanRef coreFoundationBoolean(String symbol) {
            return new CFBooleanRef(
                NativeLibrary.getInstance("CoreFoundation")
                    .getGlobalVariableAddress(symbol)
                    .getPointer(0));
        }
    }

    static void release(CFTypeRef ref) {
        if (ref != null) {
            ref.release();
        }
    }
}
