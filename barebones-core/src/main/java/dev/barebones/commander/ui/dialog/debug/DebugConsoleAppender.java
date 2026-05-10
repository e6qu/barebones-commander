/*
 * This file is part of muCommander, http://www.mucommander.com
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.barebones.commander.ui.dialog.debug;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import dev.barebones.commander.commons.logging.LogEvent;
import dev.barebones.commander.commons.logging.LogSink;
import dev.barebones.commander.conf.MuConfigurations;
import dev.barebones.commander.conf.MuPreference;
import dev.barebones.commander.conf.MuPreferences;
import dev.barebones.commander.utils.MuLogging.LogLevel;

/**
 * This <code>java.util.logging</code> <code>Handler</code> collects the last log messages that were published by
 * the different muCommander loggers, so they can be displayed at any time in the {@link DebugConsoleDialog}.
 * Log records are kept in memory as a sliding window. The number of log records is controlled by the
 * {@link MuPreferences#LOG_BUFFER_SIZE} configuration variable: the more records, the more memory is used.
 *
 * @see DebugConsoleDialog
 * @see MuPreferences#LOG_BUFFER_SIZE
 * @author Maxence Bernard, Arik Hadas
 */
public class DebugConsoleAppender implements LogSink {

    /** Maximum number of log records to keep in memory */
    private int bufferSize;

    /** Contains the last LogRecord instances. */
    private List<JdkLoggingEvent> loggingEventsList;
    
    /** The layout of the logging event representation */
    private CustomLoggingLayout loggingEventLayout;

    /**
     * Creates a new <code>DebugConsoleHandler</code>. This constructor is automatically by
     * <code>java.util.logging</code> when it is configured and should never be called directly.
     */
    public DebugConsoleAppender() {
        this.loggingEventLayout = new CustomLoggingLayout();

        bufferSize = MuConfigurations.getPreferences().getVariable(MuPreference.LOG_BUFFER_SIZE, MuPreferences.DEFAULT_LOG_BUFFER_SIZE);
        loggingEventsList = new LinkedList<>();
    }

    /**
     * Returns the last records that were collected by this handler.
     *
     * @return the last records that were collected by this handler.
     */
    public synchronized LoggingEvent[] getLogRecords() {
        JdkLoggingEvent[] records = new JdkLoggingEvent[0];
        records = loggingEventsList.toArray(records);

        return records;
    }


    @Override
    public synchronized void publish(LogEvent record) {
        if (loggingEventsList.size() == bufferSize) {
            loggingEventsList.remove(0);
        }

        loggingEventsList.add(new JdkLoggingEvent(record));
    }

    /**
     * Wraps a {@link LogEvent} and overrides {@link #toString()} to have it return a properly formatted string
     * representation of it so that it can be displayed in a {@link javax.swing.JList} or {@link javax.swing.JTable} and
     * pasted to the clipboard.
     * It also implements the LoggingEvent interface so that the logging event can be presented in the debug console.
     */
    public class JdkLoggingEvent implements LoggingEvent {

        /** The logging event */
        private LogEvent loggingEvent;

        /** The log level of the event in mucommander's terms */
        private LogLevel logLevel;

        JdkLoggingEvent(LogEvent lr) {
            this.loggingEvent = lr;
        }

        /**
         * Returns a properly formatted string representation of the {@link LogEvent}.
         * 
         * @return a properly formatted string representation of the {@link LogEvent}.
         */
        @Override
        public String toString() {
            return loggingEventLayout.format(loggingEvent);
        }
        
        
        ///////////////////////////////////////
        /// LogRecordListItem Implementation //
        ///////////////////////////////////////
        
        public boolean isLevelEqualOrHigherThan(LogLevel level) {
            return getLevel().compareTo(level) <= 0;
        }
        
        public LogLevel getLevel() {
            if (logLevel == null) {
                logLevel = getLogLevel(loggingEvent);
            }
            return logLevel;
        }
    }

    /**
     * Returns the log level, in mucommander terms, that matches the level of a JDK logging event.
     *
     * @param loggingEvent JDK logging event
     * @return log level, in mucommander terms, that matches the level of the given event
     */
    public static LogLevel getLogLevel(LogEvent loggingEvent) {
        return switch (loggingEvent.level()) {
            case OFF -> LogLevel.OFF;
            case ERROR -> LogLevel.SEVERE;
            case WARNING -> LogLevel.WARNING;
            case INFO -> LogLevel.INFO;
            case DEBUG -> LogLevel.FINE;
            case TRACE, ALL -> LogLevel.FINEST;
        };
    }

    private static class CustomLoggingLayout {

        private final static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        public String format(LogEvent event) {
            StackTraceElement stackTraceElement = event.source();

            StringBuilder sbuf = new StringBuilder(128);
            sbuf.append("[");
            sbuf.append(SIMPLE_DATE_FORMAT.format(Date.from(event.timestamp())));
            sbuf.append("] ");
            sbuf.append(getLogLevel(event));
            sbuf.append(" ");
            if (stackTraceElement != null) {
                sbuf.append(stackTraceElement.getFileName());
                sbuf.append("#");
                sbuf.append(stackTraceElement.getMethodName());
                sbuf.append(",");
                sbuf.append(stackTraceElement.getLineNumber());
            } else {
                sbuf.append(event.loggerName());
            }
            sbuf.append(" ");
            sbuf.append(event.message());
            sbuf.append(System.lineSeparator());

            Throwable thrown = event.thrown();
            if (thrown != null) {
                StringWriter stackTrace = new StringWriter();
                thrown.printStackTrace(new PrintWriter(stackTrace));
                sbuf.append(stackTrace);
                sbuf.append(System.lineSeparator());
            }
            return sbuf.toString();
        }
    }
}
