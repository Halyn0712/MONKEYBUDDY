package com.monkeybody.brain.api;

import java.util.Collections;
import java.util.Map;

/** Monkey Face 数据看板可直接消费的日聚合快照。 */
public final class BrainStatsSnapshot {
    public final long dayStartMillis;
    public final int recognitionCount;
    public final int reminderCount;
    public final int lowValueCount;
    public final int highValueCount;
    public final Map<String, Integer> contentTypeDistribution;

    public BrainStatsSnapshot(long dayStartMillis, int recognitionCount, int reminderCount,
                              int lowValueCount, int highValueCount, Map<String, Integer> distribution) {
        this.dayStartMillis = dayStartMillis; this.recognitionCount = recognitionCount;
        this.reminderCount = reminderCount; this.lowValueCount = lowValueCount; this.highValueCount = highValueCount;
        this.contentTypeDistribution = Collections.unmodifiableMap(distribution);
    }
}
