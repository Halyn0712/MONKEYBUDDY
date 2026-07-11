package com.shuagoumei.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/** Shared storage for the monitored app whitelist and recorded sessions. */
final class Prefs {

    private static final String FILE = "sgm";
    private static final String KEY_WHITELIST = "whitelist";
    private static final String KEY_SESSIONS = "sessions";
    private static final String KEY_BRAIN_ENABLED = "brain_enabled";
    private static final String KEY_BRAIN_IDENTITY = "brain_identity";
    private static final String KEY_BRAIN_EVENTS = "brain_events";
    private static final String KEY_BRAIN_SNAPSHOT = "brain_snapshot";
    private static final int MAX_SESSIONS = 1000;

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static Set<String> getWhitelist(Context c) {
        // Copy so callers never mutate the stored set instance.
        return new LinkedHashSet<>(sp(c).getStringSet(KEY_WHITELIST, new HashSet<String>()));
    }

    static void setWhitelist(Context c, Set<String> packages) {
        sp(c).edit().putStringSet(KEY_WHITELIST, new HashSet<>(packages)).apply();
    }

    static boolean isWhitelisted(Context c, String pkg) {
        return pkg != null && sp(c).getStringSet(KEY_WHITELIST, new HashSet<String>()).contains(pkg);
    }

    static String getSessionsJson(Context c) {
        return sp(c).getString(KEY_SESSIONS, "[]");
    }

    static synchronized void addSession(Context c, JSONObject session) {
        JSONArray arr;
        try {
            arr = new JSONArray(getSessionsJson(c));
        } catch (Exception e) {
            arr = new JSONArray();
        }
        arr.put(session);
        // Keep only the most recent MAX_SESSIONS entries.
        if (arr.length() > MAX_SESSIONS) {
            JSONArray trimmed = new JSONArray();
            for (int i = arr.length() - MAX_SESSIONS; i < arr.length(); i++) {
                trimmed.put(arr.opt(i));
            }
            arr = trimmed;
        }
        sp(c).edit().putString(KEY_SESSIONS, arr.toString()).apply();
    }

    static boolean isBrainEnabled(Context c) {
        return sp(c).getBoolean(KEY_BRAIN_ENABLED, false);
    }

    static void setBrainEnabled(Context c, boolean enabled) {
        sp(c).edit().putBoolean(KEY_BRAIN_ENABLED, enabled).apply();
    }

    static String getIdentity(Context c) {
        return sp(c).getString(KEY_BRAIN_IDENTITY, "kaoyan");
    }

    static void setIdentity(Context c, String key) {
        sp(c).edit().putString(KEY_BRAIN_IDENTITY, key == null ? "kaoyan" : key).apply();
    }

    static synchronized void recordBrainEvent(Context c, com.monkeybody.brain.api.BrainStatsEvent event) {
        JSONArray arr;
        try {
            arr = new JSONArray(sp(c).getString(KEY_BRAIN_EVENTS, "[]"));
        } catch (Exception e) {
            arr = new JSONArray();
        }
        try {
            JSONObject o = new JSONObject();
            o.put("eventType", event.eventType);
            o.put("timestampMillis", event.timestampMillis);
            o.put("category", event.category);
            o.put("contentType", event.contentType);
            o.put("topic", event.topic);
            o.put("confidence", event.confidence);
            arr.put(o);
        } catch (Exception ignored) {}
        if (arr.length() > 200) {
            JSONArray trimmed = new JSONArray();
            for (int i = arr.length() - 200; i < arr.length(); i++) trimmed.put(arr.opt(i));
            arr = trimmed;
        }
        sp(c).edit().putString(KEY_BRAIN_EVENTS, arr.toString()).apply();
    }

    static void saveBrainSnapshot(Context c, com.monkeybody.brain.api.BrainStatsSnapshot snapshot) {
        try {
            JSONObject o = new JSONObject();
            o.put("recognitionCount", snapshot.recognitionCount);
            o.put("reminderCount", snapshot.reminderCount);
            o.put("lowValueCount", snapshot.lowValueCount);
            o.put("highValueCount", snapshot.highValueCount);
            sp(c).edit().putString(KEY_BRAIN_SNAPSHOT, o.toString()).apply();
        } catch (Exception ignored) {}
    }

    static String getBrainSnapshotJson(Context c) {
        return sp(c).getString(KEY_BRAIN_SNAPSHOT, "{}");
    }
}
