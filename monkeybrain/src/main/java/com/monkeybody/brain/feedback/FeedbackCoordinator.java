package com.monkeybody.brain.feedback;

import android.os.SystemClock;
import com.monkeybody.brain.api.*;
import com.monkeybody.brain.vlm.UserContextStore;
import com.monkeybody.brain.stats.BrainStatsRepository;

/** 将标准识别结果转换为限频的文案、声音和动画反馈。 */
public final class FeedbackCoordinator implements RecognitionResultReceiver {
    private static final long DEFAULT_MIN_INTERVAL_MS = 3_500L;
    private final TemplateTextGenerator generator = new TemplateTextGenerator();
    private final UserContextStore contextStore;
    private final BrainStatsRepository stats;
    private volatile long minIntervalMillis = DEFAULT_MIN_INTERVAL_MS;
    private long lastFeedbackAt;

    public FeedbackCoordinator(UserContextStore contextStore, BrainStatsRepository stats) {
        this.contextStore = contextStore; this.stats = stats;
    }
    public void setMinIntervalMillis(long value) { minIntervalMillis = Math.max(1_000L, value); }

    @Override public synchronized void onRecognitionResult(RecognitionResult result) {
        FeedbackOutput output = MonkeyBrainBridge.feedback();
        if (output == null) return;
        boolean high = "HIGH_VALUE_MATCH".equals(result.category);
        if (high) {
            // 高价值内容完全静默，猴子保持隐藏。
            output.hideMessage();
            output.setMonkeyVisible(false);
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastFeedbackAt < minIntervalMillis) return;
        lastFeedbackAt = now;
        output.setMonkeyVisible(true);
        output.showMessage(generator.generate(result, contextStore.get()));
        output.playAnimation("shake");
        output.playSound("monkey_roar");
        stats.record(new BrainStatsEvent(BrainStatsEvent.TYPE_REMINDER, System.currentTimeMillis(),
                result.category, result.contentType, result.topic, result.confidence));
    }

}
