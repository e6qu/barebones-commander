package dev.barebones.commander.commons.logging;

import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LoggerFactory {

    private static final String APPLICATION_LOGGER_NAME = "dev.barebones";
    private static final java.util.logging.Handler CONSOLE_HANDLER = new java.util.logging.ConsoleHandler();
    private static final CopyOnWriteArrayList<LogSink> SINKS = new CopyOnWriteArrayList<>();

    private static volatile Level minimumLevel = Level.INFO;

    static {
        configureApplicationLogger(Level.INFO);
    }

    private LoggerFactory() {
    }

    public static Logger getLogger(Class<?> type) {
        return getLogger(type.getName());
    }

    public static Logger getLogger(String name) {
        return new Logger(System.getLogger(name));
    }

    public static Level getMinimumLevel() {
        return minimumLevel;
    }

    public static void setMinimumLevel(Level level) {
        minimumLevel = Objects.requireNonNull(level, "level");
        configureApplicationLogger(level);
    }

    public static void addSink(LogSink sink) {
        SINKS.addIfAbsent(Objects.requireNonNull(sink, "sink"));
    }

    public static void removeSink(LogSink sink) {
        SINKS.remove(sink);
    }

    static boolean isLoggable(Level level) {
        Level currentLevel = minimumLevel;
        return currentLevel != Level.OFF
                && level != Level.OFF
                && level.getSeverity() >= currentLevel.getSeverity();
    }

    static void publish(LogEvent event) {
        for (LogSink sink : SINKS) {
            sink.publish(event);
        }
    }

    private static java.util.logging.Level toJulLevel(Level level) {
        return switch (level) {
            case ALL -> java.util.logging.Level.ALL;
            case TRACE -> java.util.logging.Level.FINER;
            case DEBUG -> java.util.logging.Level.FINE;
            case INFO -> java.util.logging.Level.INFO;
            case WARNING -> java.util.logging.Level.WARNING;
            case ERROR -> java.util.logging.Level.SEVERE;
            case OFF -> java.util.logging.Level.OFF;
        };
    }

    private static synchronized void configureApplicationLogger(Level level) {
        java.util.logging.Level julLevel = toJulLevel(level);
        java.util.logging.Logger appLogger = java.util.logging.Logger.getLogger(APPLICATION_LOGGER_NAME);
        appLogger.setLevel(julLevel);
        appLogger.setUseParentHandlers(false);
        CONSOLE_HANDLER.setLevel(julLevel);
        if (!hasConsoleHandler(appLogger)) {
            appLogger.addHandler(CONSOLE_HANDLER);
        }
    }

    private static boolean hasConsoleHandler(java.util.logging.Logger appLogger) {
        for (java.util.logging.Handler handler : appLogger.getHandlers()) {
            if (handler == CONSOLE_HANDLER) {
                return true;
            }
        }
        return false;
    }
}
