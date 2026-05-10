package dev.barebones.commander.commons.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class LoggerTest {

    @Test
    public void formatsPlaceholders() {
        assertEquals(Logger.format("{} opened {}", "user", "file.txt"), "user opened file.txt");
    }

    @Test
    public void appendsExtraArguments() {
        assertEquals(Logger.format("value={}", 1, 2, 3), "value=1 [2, 3]");
    }

    @Test
    public void leavesUnmatchedPlaceholders() {
        assertEquals(Logger.format("{} -> {}", "left"), "left -> {}");
    }

    @Test
    public void debugLevelDoesNotLowerJulRootLogger() {
        java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
        java.util.logging.Level originalRootLevel = rootLogger.getLevel();
        List<java.util.logging.Level> originalHandlerLevels = Arrays.stream(rootLogger.getHandlers())
                .map(java.util.logging.Handler::getLevel)
                .toList();

        LoggerFactory.setMinimumLevel(System.Logger.Level.DEBUG);

        assertEquals(java.util.logging.Level.FINE, java.util.logging.Logger.getLogger("dev.barebones").getLevel());
        assertEquals(originalRootLevel, rootLogger.getLevel());
        assertEquals(originalHandlerLevels, Arrays.stream(rootLogger.getHandlers())
                .map(java.util.logging.Handler::getLevel)
                .toList());
        assertFalse(java.util.logging.Logger.getLogger("java.awt.Component").isLoggable(java.util.logging.Level.FINE));

        LoggerFactory.setMinimumLevel(System.Logger.Level.INFO);
    }
}
