package com.shuagoumei.app;

import android.app.Application;

import com.monkeybody.brain.api.MonkeyFaceBridge;

/** Unified entry for MONKEYBUDDY: Face UI + Brain pipeline in one process. */
public final class MonkeyBuddyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BrainInitializer.init(this);
        MonkeyFaceBridge.register(new FaceBrainDataSink(this));
    }
}
