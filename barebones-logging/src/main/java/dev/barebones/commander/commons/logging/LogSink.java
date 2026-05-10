package dev.barebones.commander.commons.logging;

@FunctionalInterface
public interface LogSink {
    void publish(LogEvent event);
}
