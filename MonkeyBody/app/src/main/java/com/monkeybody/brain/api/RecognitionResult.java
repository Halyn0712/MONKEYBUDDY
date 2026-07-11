package com.monkeybody.brain.api;

/** 与算法实现解耦的最小识别结果模型。 */
public final class RecognitionResult {
    public final String category;
    public final float confidence;
    public final long timestampMillis;
    public final String topic;
    public final String contentType;
    public final int informationDensity;
    public final long inferenceMillis;
    public final String pageType;

    public RecognitionResult(String category, float confidence, long timestampMillis) {
        this(category, confidence, timestampMillis, "", "", 0, 0L, "DETAIL");
    }

    public RecognitionResult(String category, float confidence, long timestampMillis,
                             String topic, String contentType, int informationDensity, long inferenceMillis) {
        this(category, confidence, timestampMillis, topic, contentType, informationDensity, inferenceMillis, "DETAIL");
    }

    public RecognitionResult(String category, float confidence, long timestampMillis,
                             String topic, String contentType, int informationDensity, long inferenceMillis,
                             String pageType) {
        this.category = category;
        this.confidence = confidence;
        this.timestampMillis = timestampMillis;
        this.topic = topic;
        this.contentType = contentType;
        this.informationDensity = informationDensity;
        this.inferenceMillis = inferenceMillis;
        this.pageType = pageType;
    }
}
