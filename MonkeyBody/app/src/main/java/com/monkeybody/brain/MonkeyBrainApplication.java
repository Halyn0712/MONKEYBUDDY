package com.monkeybody.brain;

import android.app.Application;
import com.monkeybody.brain.vlm.VlmCoordinator;
import com.monkeybody.brain.feedback.FeedbackCoordinator;
import com.monkeybody.brain.api.MonkeyBrainBridge;
import com.monkeybody.brain.stats.BrainStatsRepository;
import com.monkeybody.brain.stats.BrainResultRouter;

/** 初始化进程级 VLM 队列；不会在此处联网或自动下载模型。 */
public class MonkeyBrainApplication extends Application {
    private VlmCoordinator vlm;
    private FeedbackCoordinator feedback;
    private BrainStatsRepository stats;
    @Override public void onCreate() {
        super.onCreate();
        vlm = new VlmCoordinator(this);
        stats = new BrainStatsRepository(this);
        feedback = new FeedbackCoordinator(vlm.contextStore(), stats);
        MonkeyBrainBridge.registerRecognitionReceiver(new BrainResultRouter(stats, feedback));
        vlm.start();
    }
    public VlmCoordinator vlm() { return vlm; }
    public BrainStatsRepository stats() { return stats; }
}
