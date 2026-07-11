package com.monkeybody.brain.api;

/**
 * Monkey Face 对接契约。现有计时/统计模块实现并注册后，可接收原始事件与日聚合快照。
 * 回调发生在后台推理线程，实现不得阻塞。
 */
public interface MonkeyFaceDataSink {
    void onBrainEvent(BrainStatsEvent event);
    void onBrainSnapshot(BrainStatsSnapshot snapshot);
}
