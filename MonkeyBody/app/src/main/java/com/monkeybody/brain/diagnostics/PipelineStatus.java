package com.monkeybody.brain.diagnostics;

import android.app.*;
import android.content.*;
import android.os.Build;
import com.monkeybody.brain.MainActivity;
import com.monkeybody.brain.R;

/** 保存并广播最近一条全链路状态，同时用同一条通知向用户暴露静默错误。 */
public final class PipelineStatus {
    public static final String ACTION_CHANGED = "com.monkeybody.brain.PIPELINE_STATUS_CHANGED";
    public static final String EXTRA_MESSAGE = "message";
    private static final String PREFS = "pipeline_status";
    private static final String CHANNEL = "pipeline_diagnostics";
    private static final int NOTIFICATION_ID = 1003;

    public static void report(Context context, String message, boolean important) {
        Context app = context.getApplicationContext();
        String value = android.text.format.DateFormat.format("HH:mm:ss", System.currentTimeMillis()) + "  " + message;
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("last", value).apply();
        app.sendBroadcast(new Intent(ACTION_CHANGED).setPackage(app.getPackageName()).putExtra(EXTRA_MESSAGE, value));
        if (important) notify(app, message);
    }

    public static String last(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("last", "尚未启动搭子模式");
    }

    private static void notify(Context context, String message) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL, "搭子链路诊断", NotificationManager.IMPORTANCE_DEFAULT));
        }
        PendingIntent pending = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL) : new Notification.Builder(context);
        manager.notify(NOTIFICATION_ID, builder.setSmallIcon(R.drawable.ic_monkey)
                .setContentTitle("Monkey Body 运行提示").setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message)).setContentIntent(pending).setAutoCancel(true).build());
    }

    private PipelineStatus() {}
}
