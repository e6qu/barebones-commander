/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 */
package dev.barebones.commander.test;

import org.junit.jupiter.api.function.Executable;

/**
 * Assertion bridge for legacy tests whose equality methods accepted
 * {@code actual, expected}; JUnit accepts
 * {@code expected, actual}. These wrappers keep existing test intent
 * stable while the suite runs on JUnit 5.
 */
public final class TestAssertions {

    private TestAssertions() {
    }

    public static void assertEquals(Object actual, Object expected) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    public static void assertEquals(Object actual, Object expected, String message) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }

    public static void assertEquals(long actual, long expected) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    public static void assertEquals(long actual, long expected, String message) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }

    public static void assertEquals(int actual, int expected) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    public static void assertEquals(int actual, int expected, String message) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual, message);
    }

    public static void assertEquals(boolean actual, boolean expected) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    public static void assertEquals(byte[] actual, byte[] expected) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual);
    }

    public static void assertEquals(byte[] actual, byte[] expected, String message) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual, message);
    }

    public static void assertEquals(char[] actual, char[] expected) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual);
    }

    public static void assertEquals(char[] actual, char[] expected, String message) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual, message);
    }

    public static void assertNotEquals(Object actual, Object expected) {
        org.junit.jupiter.api.Assertions.assertNotEquals(expected, actual);
    }

    public static void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }

    public static void assertTrue(boolean condition, String message) {
        org.junit.jupiter.api.Assertions.assertTrue(condition, message);
    }

    public static void assertFalse(boolean condition) {
        org.junit.jupiter.api.Assertions.assertFalse(condition);
    }

    public static void assertFalse(boolean condition, String message) {
        org.junit.jupiter.api.Assertions.assertFalse(condition, message);
    }

    public static void assertNull(Object actual) {
        org.junit.jupiter.api.Assertions.assertNull(actual);
    }

    public static void assertNotNull(Object actual) {
        org.junit.jupiter.api.Assertions.assertNotNull(actual);
    }

    public static void assertSame(Object actual, Object expected) {
        org.junit.jupiter.api.Assertions.assertSame(expected, actual);
    }

    public static void assertSame(Object actual, Object expected, String message) {
        org.junit.jupiter.api.Assertions.assertSame(expected, actual, message);
    }

    public static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable) {
        return org.junit.jupiter.api.Assertions.assertThrows(expectedType, executable);
    }

    public static <T extends Throwable> T assertThrows(
            Class<T> expectedType, Executable executable, String message) {
        return org.junit.jupiter.api.Assertions.assertThrows(expectedType, executable, message);
    }
}
