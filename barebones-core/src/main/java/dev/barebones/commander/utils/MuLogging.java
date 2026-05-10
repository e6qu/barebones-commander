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

package dev.barebones.commander.utils;

import java.io.IOException;

import dev.barebones.commander.commons.logging.LoggerFactory;
import dev.barebones.commander.conf.MuConfigurations;
import dev.barebones.commander.conf.MuPreference;
import dev.barebones.commander.conf.MuPreferences;
import dev.barebones.commander.ui.dialog.debug.DebugConsoleAppender;

/**
 * This class manages logging issues within mucommander
 *
 * @author Maxence Bernard, Arik Hadas
 */
public class MuLogging {

    /** Levels of log printings */
    public enum LogLevel {
        OFF,
        SEVERE,
        WARNING,
        INFO,
        CONFIG,
        FINE,
        FINER,
        FINEST;

        /**
         * This method maps mucommander log levels to JDK logging levels.
         *
         * @return JDK level corresponding to this <code>LogLevel</code>
         */
        public java.lang.System.Logger.Level toSystemLevel() {
            return switch (this) {
                case SEVERE -> java.lang.System.Logger.Level.ERROR;
                case WARNING -> java.lang.System.Logger.Level.WARNING;
                case INFO, CONFIG -> java.lang.System.Logger.Level.INFO;
                case FINE, FINER -> java.lang.System.Logger.Level.DEBUG;
                case FINEST -> java.lang.System.Logger.Level.TRACE;
                case OFF -> java.lang.System.Logger.Level.OFF;
            };
        }
    }

    /** Appender that writes log printings to the debug console dialog */
    private static DebugConsoleAppender debugConsoleAppender;

    /**
     * Sets the level of all muCommander loggers.
     *
     * @param level the new log level
     */
    private static void updateLogLevel(LogLevel level) {
        LoggerFactory.setMinimumLevel(level.toSystemLevel());
    }

    /**
     * Returns the current log level used by all application loggers.
     *
     * @return the current log level used by all application loggers.
     */
    public static LogLevel getLogLevel() {
        return LogLevel.valueOf(MuConfigurations.getPreferences().getVariable(MuPreference.LOG_LEVEL, MuPreferences.DEFAULT_LOG_LEVEL));
    }

    /**
     * Sets the new log level to be used by all application loggers, and persists it in the
     * application preferences.
     *
     * @param level the new log level to be used by all application loggers.
     */
    public static void setLogLevel(LogLevel level) {
        MuConfigurations.getPreferences().setVariable(MuPreference.LOG_LEVEL, level.toString());
        updateLogLevel(level);
    }

    public static DebugConsoleAppender getDebugConsoleAppender() {
        return debugConsoleAppender;
    }

    public static void configureLogging() throws IOException {
        configureLogging(getLogLevel());
    }

    public static void configureLogging(LogLevel level) throws IOException {
        debugConsoleAppender = createDebugConsoleAppender();
        LoggerFactory.addSink(debugConsoleAppender);
        updateLogLevel(level);
    }

    private static DebugConsoleAppender createDebugConsoleAppender() {
        return new DebugConsoleAppender();
    }
}
