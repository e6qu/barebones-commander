package dev.barebones.commander.commons.logging;

import java.lang.System.Logger.Level;
import java.time.Instant;

public record LogEvent(
        Instant timestamp,
        Level level,
        String loggerName,
        String message,
        Throwable thrown,
        StackTraceElement source) {
}
