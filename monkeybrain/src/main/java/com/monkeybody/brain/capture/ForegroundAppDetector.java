package com.monkeybody.brain.capture;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;

/** 使用 UsageEvents 查询最近进入前台的应用，不需要无障碍权限。 */
final class ForegroundAppDetector {
    private final UsageStatsManager usageStats;
    ForegroundAppDetector(Context context) {
        usageStats = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
    }

    String currentPackage() {
        long end = System.currentTimeMillis();
        UsageEvents events = usageStats.queryEvents(end - 15_000L, end);
        UsageEvents.Event event = new UsageEvents.Event();
        String foreground = null;
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                foreground = event.getPackageName();
            }
        }
        return foreground;
    }
}
