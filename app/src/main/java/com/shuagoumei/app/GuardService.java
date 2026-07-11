package com.shuagoumei.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * A lightweight foreground service whose only job is to keep our process alive
 * and honest. Aggressive ROMs (ColorOS/OriginOS) freeze background processes to
 * save battery; when that happens while a full-screen overlay is up the whole
 * screen becomes unresponsive. Holding an ongoing foreground notification makes
 * the system far less likely to freeze us, so the overlay stays interactive.
 *
 * It also polls whether the accessibility service is still enabled. ROMs love to
 * silently switch it off, which leaves the app "running" but blind. When that
 * happens we flip the notification to a tap-to-reopen prompt.
 */
public class GuardService extends Service {

    private static final String CHANNEL_ID = "monkeyface_guard";
    private static final int NOTIF_ID = 4671;
    private static final long CHECK_INTERVAL_MS = 15 * 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean lastOnline;
    private boolean looping;

    private final Runnable checker = new Runnable() {
        @Override public void run() {
            updateNotification(isAccessibilityEnabled());
            handler.postDelayed(this, CHECK_INTERVAL_MS);
        }
    };

    /** Best-effort start; safe to call repeatedly. Must be called from foreground on API 31+. */
    static void start(Context ctx) {
        Context app = ctx.getApplicationContext();
        Intent i = new Intent(app, GuardService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                app.startForegroundService(i);
            } else {
                app.startService(i);
            }
        } catch (Exception ignored) {}
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        lastOnline = isAccessibilityEnabled();
        Notification n = buildNotification(lastOnline);
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIF_ID, n);
            }
        } catch (Exception ignored) {}
        if (!looping) {
            looping = true;
            handler.postDelayed(checker, CHECK_INTERVAL_MS);
        }
        return START_STICKY;
    }

    private void updateNotification(boolean online) {
        if (online == lastOnline) return;
        lastOnline = online;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            try { nm.notify(NOTIF_ID, buildNotification(online)); } catch (Exception ignored) {}
        }
    }

    private Notification buildNotification(boolean online) {
        String title, text;
        PendingIntent pi;
        if (online) {
            title = "\uD83D\uDC35 Monkey Face 正在守护";
            text = "监控运行中，帮你管住刷手机的时间";
            Intent open = new Intent(this, MainActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pi = PendingIntent.getActivity(this, 0, open, piFlags());
        } else {
            title = "\u26A0\uFE0F 监控已掉线，点我重新开启";
            text = "系统关闭了无障碍权限，点这里去打开";
            Intent acc = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            acc.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            pi = PendingIntent.getActivity(this, 1, acc, piFlags());
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        b.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(pi);
        return b.build();
    }

    private int piFlags() {
        int f = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) f |= PendingIntent.FLAG_IMMUTABLE;
        return f;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    "守护状态", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("显示监控是否在运行");
            ch.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private boolean isAccessibilityEnabled() {
        try {
            String enabled = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (TextUtils.isEmpty(enabled)) return false;
            ComponentName me = new ComponentName(this, AppWatchService.class);
            String flat = me.flattenToString();
            String shortFlat = me.flattenToShortString();
            for (String part : enabled.split(":")) {
                if (part.equalsIgnoreCase(flat) || part.equalsIgnoreCase(shortFlat)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(checker);
        looping = false;
        super.onDestroy();
    }
}
