package com.shuagoumei.app;

import android.content.Context;

import com.monkeybody.brain.api.BrainStatsEvent;
import com.monkeybody.brain.api.BrainStatsSnapshot;
import com.monkeybody.brain.api.MonkeyFaceDataSink;

/** Routes Brain stats into Face storage so the WebView dashboard can read them. */
final class FaceBrainDataSink implements MonkeyFaceDataSink {

    private final Context context;

    FaceBrainDataSink(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void onBrainEvent(BrainStatsEvent event) {
        Prefs.recordBrainEvent(context, event);
    }

    @Override
    public void onBrainSnapshot(BrainStatsSnapshot snapshot) {
        Prefs.saveBrainSnapshot(context, snapshot);
    }
}
