/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This file is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This file is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.barebones.commander.commons.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A JUnit test case for {@link OsFamily}.
 */
public class OsFamilyTest {

    @Test
    public void parsesSupportedFamilies() {
        assertEquals(OsFamily.MAC_OS, OsFamily.parseSystemProperty("Mac OS X"));
        assertEquals(OsFamily.LINUX, OsFamily.parseSystemProperty("Linux"));
    }

    @Test
    public void unsupportedFamiliesAreUnknown() {
        assertEquals(OsFamily.UNKNOWN_OS_FAMILY, OsFamily.parseSystemProperty("Windows 11"));
        assertEquals(OsFamily.UNKNOWN_OS_FAMILY, OsFamily.parseSystemProperty("SunOS"));
        assertEquals(OsFamily.UNKNOWN_OS_FAMILY, OsFamily.parseSystemProperty("OS/2"));
        assertEquals(OsFamily.UNKNOWN_OS_FAMILY, OsFamily.parseSystemProperty("OpenVMS"));
    }

    @Test
    public void onlySupportedFamiliesAreUnixBased() {
        assertTrue(OsFamily.MAC_OS.isUnixBased());
        assertTrue(OsFamily.LINUX.isUnixBased());
        assertFalse(OsFamily.UNKNOWN_OS_FAMILY.isUnixBased());
    }
}
