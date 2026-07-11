package com.monkeybody.brain.stats;

import com.monkeybody.brain.api.*;
import com.monkeybody.brain.feedback.FeedbackCoordinator;

/** 单一识别出口：先落统计，再进入限频反馈，保证识别次数不受提醒频控影响。 */
public final class BrainResultRouter implements RecognitionResultReceiver {
    private final BrainStatsRepository repository;
    private final FeedbackCoordinator feedback;
    public BrainResultRouter(BrainStatsRepository repository, FeedbackCoordinator feedback) {
        this.repository = repository; this.feedback = feedback;
    }
    @Override public void onRecognitionResult(RecognitionResult result) {
        repository.record(new BrainStatsEvent(BrainStatsEvent.TYPE_RECOGNITION, result.timestampMillis,
                result.category, result.contentType, result.topic, result.confidence));
        feedback.onRecognitionResult(result);
    }
}
