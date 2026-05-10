/*
 * Copyright (C) 2026 barebones-commander contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 */
package dev.barebones.commander.commons.runtime;

/**
 * Central defaults for runtime delays and thresholds that may need tuning in
 * tests or large deployments. Values can be overridden with matching
 * {@code -Dbarebones.tunables.*} system properties.
 */
public final class Tunables {
    public static final int JOB_CURRENT_FILE_LABEL_REFRESH_MS =
        positiveInteger("jobs.currentFileLabelRefreshMs", 100);
    public static final int JOB_MAIN_REFRESH_TICKS =
        positiveInteger("jobs.mainRefreshTicks", 10);
    public static final int JOB_FINISHED_REMOVE_MS =
        positiveInteger("jobs.finishedRemoveMs", 1_500);

    public static final int FOLDER_MONITOR_WAIT_MULTIPLIER =
        positiveInteger("folderMonitor.waitMultiplier", 50);
    public static final int FOLDER_MONITOR_TICK_MS =
        positiveInteger("folderMonitor.tickMs", 300);

    public static final int SHORTCUT_EDITING_CLICKS =
        positiveInteger("shortcuts.editingClicks", 2);
    public static final int SHORTCUT_EDITING_TIMEOUT_MS =
        positiveInteger("shortcuts.editingTimeoutMs", 3_000);

    public static final long NOTIFIER_UPDATE_ICON_INTERVAL_MS =
        positiveLong("notifier.updateIconIntervalMs", 1_000L);
    public static final int LOCATION_BAR_BREADCRUMB_SHOW_DELAY_MS =
        positiveInteger("locationBar.breadcrumbShowDelayMs", 250);
    public static final long DIRECTORY_SIZE_REFRESH_MS =
        positiveLong("directorySize.refreshMs", 300L);

    public static final int S3_UPLOAD_SPILL_THRESHOLD_BYTES =
        positiveInteger("s3.uploadSpillThresholdBytes", 32 * 1024 * 1024);
    public static final long S3_PROGRESS_PUBLISH_INTERVAL_NANOS =
        positiveLong("s3.progressPublishIntervalNanos", 250_000_000L);

    private static final String PREFIX = "barebones.tunables.";

    private Tunables() {
    }

    private static int positiveInteger(String name, int defaultValue) {
        int value = Integer.getInteger(PREFIX + name, defaultValue);
        return value > 0 ? value : defaultValue;
    }

    private static long positiveLong(String name, long defaultValue) {
        long value = Long.getLong(PREFIX + name, defaultValue);
        return value > 0 ? value : defaultValue;
    }
}
