package dev.barebones.commander.commons.logging;

import java.lang.System.Logger.Level;
import java.time.Instant;

public final class Logger {

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private final System.Logger delegate;

    Logger(System.Logger delegate) {
        this.delegate = delegate;
    }

    public boolean isTraceEnabled() {
        return isEnabled(Level.TRACE);
    }

    public boolean isDebugEnabled() {
        return isEnabled(Level.DEBUG);
    }

    public boolean isInfoEnabled() {
        return isEnabled(Level.INFO);
    }

    public boolean isWarnEnabled() {
        return isEnabled(Level.WARNING);
    }

    public boolean isErrorEnabled() {
        return isEnabled(Level.ERROR);
    }

    public void trace(String message, Object... arguments) {
        log(Level.TRACE, message, arguments);
    }

    public void debug(String message, Object... arguments) {
        log(Level.DEBUG, message, arguments);
    }

    public void info(String message, Object... arguments) {
        log(Level.INFO, message, arguments);
    }

    public void warn(String message, Object... arguments) {
        log(Level.WARNING, message, arguments);
    }

    public void error(String message, Object... arguments) {
        log(Level.ERROR, message, arguments);
    }

    private boolean isEnabled(Level level) {
        return LoggerFactory.isLoggable(level);
    }

    private void log(Level level, String message, Object... arguments) {
        if (!isEnabled(level)) {
            return;
        }

        Throwable thrown = extractThrowable(message, arguments);
        Object[] formattingArguments = thrown == null ? arguments : copyWithoutLast(arguments);
        String formattedMessage = format(message, formattingArguments);

        if (thrown == null) {
            delegate.log(level, formattedMessage);
        } else {
            delegate.log(level, formattedMessage, thrown);
        }

        LoggerFactory.publish(new LogEvent(Instant.now(), level, delegate.getName(), formattedMessage, thrown, findCaller()));
    }

    static String format(String message, Object... arguments) {
        if (message == null) {
            return "null";
        }
        if (arguments == null || arguments.length == 0) {
            return message;
        }

        StringBuilder formatted = new StringBuilder(message.length() + arguments.length * 16);
        int argumentIndex = 0;
        int cursor = 0;
        int placeholder;
        while ((placeholder = message.indexOf("{}", cursor)) >= 0 && argumentIndex < arguments.length) {
            formatted.append(message, cursor, placeholder);
            formatted.append(arguments[argumentIndex++]);
            cursor = placeholder + 2;
        }
        formatted.append(message, cursor, message.length());

        if (argumentIndex < arguments.length) {
            formatted.append(" [");
            for (int i = argumentIndex; i < arguments.length; i++) {
                if (i > argumentIndex) {
                    formatted.append(", ");
                }
                formatted.append(arguments[i]);
            }
            formatted.append(']');
        }

        return formatted.toString();
    }

    private static Throwable extractThrowable(String message, Object... arguments) {
        if (arguments == null || arguments.length == 0 || !(arguments[arguments.length - 1] instanceof Throwable thrown)) {
            return null;
        }
        return countPlaceholders(message) < arguments.length ? thrown : null;
    }

    private static int countPlaceholders(String message) {
        if (message == null) {
            return 0;
        }

        int count = 0;
        int cursor = 0;
        while ((cursor = message.indexOf("{}", cursor)) >= 0) {
            count++;
            cursor += 2;
        }
        return count;
    }

    private static Object[] copyWithoutLast(Object[] arguments) {
        Object[] copy = new Object[arguments.length - 1];
        System.arraycopy(arguments, 0, copy, 0, copy.length);
        return copy;
    }

    private static StackTraceElement findCaller() {
        return STACK_WALKER.walk(frames -> frames
                .filter(frame -> !frame.getClassName().startsWith("dev.barebones.commander.commons.logging."))
                .findFirst()
                .map(StackWalker.StackFrame::toStackTraceElement)
                .orElse(null));
    }
}
