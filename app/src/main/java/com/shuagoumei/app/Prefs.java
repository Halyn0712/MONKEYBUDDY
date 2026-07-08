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
}
