package com.shuagoumei.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Fires when an agreed session time is up. AlarmManager wakes us here even if
 * our process was frozen or killed by the ROM, so the reminder is reliable.
 */
public class AlarmReceiver extends BroadcastReceiver {

    static final String ACTION_FIRE = "com.shuagoumei.app.ALARM_FIRE";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null) return;
        String pkg = intent.getStringExtra("pkg");
        String label = intent.getStringExtra("label");
        int plannedMin = intent.getIntExtra("plannedMin", 0);
        String reason = intent.getStringExtra("reason");
        long startTime = intent.getLongExtra("startTime", System.currentTimeMillis());

        AppWatchService svc = AppWatchService.INSTANCE;
        if (svc != null) {
            svc.onAlarmFired(pkg, label, plannedMin, reason, startTime);
        } else {
            AppWatchService.showAlarmFallback(ctx.getApplicationContext(),
                    pkg, label, plannedMin, reason, startTime);
        }
    }
}
