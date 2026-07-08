package com.shuagoumei.app;

import android.accessibilityservice.AccessibilityService;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Watches which app is in the foreground. When the user opens an app on the
 * monitored whitelist it shows a "make a deal with yourself" sheet, times the
 * session, and pops an alarm when the agreed time is up. It never force-closes
 * anything — at most it sends the user back to the home screen.
 *
 * The end-of-session alarm is scheduled through AlarmManager (not an in-process
 * Handler) so it still fires when aggressive ROMs freeze or kill our background
 * process while another app is in the foreground.
 */
public class AppWatchService extends AccessibilityService {

    static final int ALARM_REQUEST = 1001;
    private static final long SNOOZE_MS = 5 * 60 * 1000L;

    /** Live instance so AlarmReceiver can reach the running service. */
    static volatile AppWatchService INSTANCE;

    private OverlayManager overlay;

    private String prevPkg;
    private boolean interventionShowing;
    private Session session;

    private static class Session {
        String pkg;
        String label;
        String reason;
        int plannedMin;
        long plannedMs;
        long startTime;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        overlay = new OverlayManager(this);
        INSTANCE = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        CharSequence pc = event.getPackageName();
        if (pc == null) return;
        handleForeground(pc.toString());
    }

    private void handleForeground(String p) {
        if (isIgnored(p) || p.equals(prevPkg)) return;
        prevPkg = p;

        // Left the app we were timing -> finish and record it.
        if (session != null && !p.equals(session.pkg)) {
            endAndRecord();
        }
        // Left an app whose intervention sheet was still open, without deciding.
        if (interventionShowing) {
            overlay.removeIntervention();
            interventionShowing = false;
        }
        // Entered a monitored app while idle -> intervene.
        if (session == null && !interventionShowing && Prefs.isWhitelisted(this, p)) {
            showIntervention(p);
        }
    }

    private boolean isIgnored(String p) {
        if (p == null) return true;
        if (p.equals(getPackageName())) return true;
        if (p.equals("com.android.systemui") || p.equals("android")) return true;
        return p.toLowerCase(Locale.ROOT).contains("inputmethod");
    }

    private void showIntervention(final String pkg) {
        final String label = loadLabel(pkg);
        interventionShowing = true;
        overlay.showIntervention(label, new OverlayManager.InterventionCallback() {
            @Override public void onStart(int minutes, String reason) {
                interventionShowing = false;
                startSession(pkg, label, minutes, reason);
            }
            @Override public void onResist(String reason) {
                interventionShowing = false;
                recordResist(label, reason);
                goHome();
            }
        });
    }

    private void startSession(String pkg, String label, int minutes, String reason) {
        session = new Session();
        session.pkg = pkg;
        session.label = label;
        session.plannedMin = minutes;
        session.plannedMs = minutes * 60 * 1000L;
        session.reason = reason;
        session.startTime = System.currentTimeMillis();
        scheduleAlarm(session.plannedMs);
    }

    private void scheduleAlarm(long delayMs) {
        if (session == null) return;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long triggerAt = System.currentTimeMillis() + Math.max(0, delayMs);
        PendingIntent pi = buildAlarmIntent(this, session.pkg, session.label,
                session.plannedMin, session.reason, session.startTime);
        try {
            boolean canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    private void cancelAlarm() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(buildAlarmIntent(this, "", "", 0, "", 0));
    }

    static PendingIntent buildAlarmIntent(Context ctx, String pkg, String label,
                                          int plannedMin, String reason, long startTime) {
        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.setAction(AlarmReceiver.ACTION_FIRE);
        i.putExtra("pkg", pkg);
        i.putExtra("label", label);
        i.putExtra("plannedMin", plannedMin);
        i.putExtra("reason", reason);
        i.putExtra("startTime", startTime);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, ALARM_REQUEST, i, flags);
    }

    /** Called by AlarmReceiver when the agreed time is up and the service is alive. */
    void onAlarmFired(String pkg, String label, int plannedMin, String reason, long startTime) {
        if (session == null) {
            // Process may have been rebuilt; reconstruct enough to record correctly.
            session = new Session();
            session.pkg = pkg;
            session.label = label;
            session.plannedMin = plannedMin;
            session.plannedMs = plannedMin * 60 * 1000L;
            session.reason = reason;
            session.startTime = startTime > 0 ? startTime : System.currentTimeMillis();
        }
        if (overlay == null) overlay = new OverlayManager(this);
        overlay.showAlarm(session.label, session.plannedMin, new OverlayManager.AlarmCallback() {
            @Override public void onDone() {
                endAndRecord();
                goHome();
            }
            @Override public void onSnooze(String reason) {
                if (session == null) return;
                appendReason(reason);
                session.plannedMin += 5;
                session.plannedMs += SNOOZE_MS;
                scheduleAlarm(SNOOZE_MS);
            }
        });
    }

    /** Merge a "keep scrolling" reason into the running session so it gets recorded. */
    private void appendReason(String extra) {
        if (session == null || extra == null || extra.trim().isEmpty()) return;
        String e = extra.trim();
        if (session.reason == null || session.reason.isEmpty()) {
            session.reason = e;
        } else if (!session.reason.contains(e)) {
            session.reason = session.reason + " · 续:" + e;
        }
    }

    private void endAndRecord() {
        if (session == null) return;
        cancelAlarm();
        overlay.removeAll();
        long now = System.currentTimeMillis();
        long actualSec = Math.max(0, (now - session.startTime) / 1000);
        writeBrowse(this, session, now, actualSec);
        session = null;
    }

    private static void writeBrowse(Context ctx, Session s, long endTime, long actualSec) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", s.startTime);
            o.put("date", dateStr(s.startTime));
            o.put("type", "browse");
            o.put("platform", s.label);
            o.put("plannedMin", s.plannedMin);
            o.put("reason", s.reason == null ? "" : s.reason);
            o.put("actualSec", actualSec);
            o.put("mood", "");
            o.put("startTime", s.startTime);
            o.put("endTime", endTime);
            o.put("source", "auto");
            Prefs.addSession(ctx, o);
        } catch (Exception ignored) {}
    }

    private void recordResist(String label, String reason) {
        long now = System.currentTimeMillis();
        try {
            JSONObject o = new JSONObject();
            o.put("id", now);
            o.put("date", dateStr(now));
            o.put("type", "resist");
            o.put("platform", label);
            o.put("plannedMin", 0);
            o.put("reason", reason == null || reason.isEmpty() ? "临时点开" : reason);
            o.put("actualSec", 0);
            o.put("mood", "💪 忍住了");
            o.put("startTime", now);
            o.put("endTime", now);
            o.put("source", "auto");
            Prefs.addSession(this, o);
        } catch (Exception ignored) {}
    }

    private void goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME);
    }

    private String loadLabel(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    private static String dateStr(long ms) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(ms));
    }

    /**
     * Best-effort path for when the alarm fires but the service was killed (not
     * just frozen). Shows the reminder from a fresh context and records the
     * session; going Home isn't possible without the accessibility connection.
     */
    static void showAlarmFallback(final Context ctx, final String pkg, final String label,
                                  final int plannedMin, final String reason, final long startTime) {
        final OverlayManager om = new OverlayManager(ctx);
        try {
            om.showAlarm(label, plannedMin, new OverlayManager.AlarmCallback() {
                @Override public void onDone() {
                    Session s = new Session();
                    s.pkg = pkg;
                    s.label = label;
                    s.plannedMin = plannedMin;
                    s.reason = reason;
                    s.startTime = startTime > 0 ? startTime : System.currentTimeMillis();
                    long now = System.currentTimeMillis();
                    writeBrowse(ctx, s, now, Math.max(0, (now - s.startTime) / 1000));
                    om.removeAll();
                }
                @Override public void onSnooze(String reasonMore) {
                    AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                    if (am != null) {
                        String combined = reason;
                        if (reasonMore != null && !reasonMore.trim().isEmpty()) {
                            String e = reasonMore.trim();
                            combined = (reason == null || reason.isEmpty())
                                    ? e
                                    : (reason.contains(e) ? reason : reason + " · 续:" + e);
                        }
                        long triggerAt = System.currentTimeMillis() + SNOOZE_MS;
                        PendingIntent pi = buildAlarmIntent(ctx, pkg, label, plannedMin + 5, combined, startTime);
                        try {
                            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                        } catch (Exception ignored) {}
                    }
                    om.removeAll();
                }
            });
        } catch (Exception ignored) {}
    }

    @Override
    public void onInterrupt() {}

    @Override
    public boolean onUnbind(Intent intent) {
        cleanup();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    private void cleanup() {
        cancelAlarm();
        if (overlay != null) overlay.removeAll();
        interventionShowing = false;
        session = null;
        if (INSTANCE == this) INSTANCE = null;
    }
}
