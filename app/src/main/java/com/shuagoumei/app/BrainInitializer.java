package com.shuagoumei.app;

import android.content.Context;

import com.monkeybody.brain.api.MonkeyBrainBridge;
import com.monkeybody.brain.feedback.FeedbackCoordinator;
import com.monkeybody.brain.stats.BrainResultRouter;
import com.monkeybody.brain.stats.BrainStatsRepository;
import com.monkeybody.brain.vlm.VlmCoordinator;

/** Boots the Monkey Brain VLM queue and feedback pipeline inside the host app. */
final class BrainInitializer {

    private static VlmCoordinator vlm;
    private static BrainStatsRepository stats;

    private BrainInitializer() {}

    static synchronized void init(Context context) {
        if (vlm != null) return;
        Context app = context.getApplicationContext();
        vlm = new VlmCoordinator(app);
        stats = new BrainStatsRepository(app);
        FeedbackCoordinator feedback = new FeedbackCoordinator(vlm.contextStore(), stats);
        MonkeyBrainBridge.registerRecognitionReceiver(new BrainResultRouter(stats, feedback));
        vlm.start();
    }

    static VlmCoordinator vlm() {
        return vlm;
    }

    static BrainStatsRepository stats() {
        return stats;
    }
}
