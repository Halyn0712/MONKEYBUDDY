package com.monkeybody.brain.api;

/** Monkey Brain 向 Monkey Face 同步的单条统计事件。 */
public final class BrainStatsEvent {
    public static final String TYPE_RECOGNITION = "recognition";
    public static final String TYPE_REMINDER = "reminder";
    public final String eventType;
    public final long timestampMillis;
    public final String category;
    public final String contentType;
    public final String topic;
    public final float confidence;

    public BrainStatsEvent(String eventType, long timestampMillis, String category,
                           String contentType, String topic, float confidence) {
        this.eventType = eventType; this.timestampMillis = timestampMillis; this.category = category;
        this.contentType = contentType; this.topic = topic; this.confidence = confidence;
    }
}
