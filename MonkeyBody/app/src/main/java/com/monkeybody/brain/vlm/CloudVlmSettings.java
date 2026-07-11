package com.monkeybody.brain.vlm;

import android.content.Context;
import android.content.SharedPreferences;

/** 云端 VLM 开关与知情同意状态；API Key 由 SecretStore 独立加密。 */
public final class CloudVlmSettings {
    public static final String MODEL = "Qwen/Qwen3-VL-32B-Thinking";
    private static final String PREFS = "cloud_vlm_settings";
    private final SharedPreferences preferences;
    public CloudVlmSettings(Context context) { preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public boolean enabled() { return preferences.getBoolean("enabled", false); }
    public long consentTimestamp() { return preferences.getLong("consent_at", 0L); }
    public void enableWithConsent() { preferences.edit().putBoolean("enabled", true).putLong("consent_at", System.currentTimeMillis()).apply(); }
    public void disable() { preferences.edit().putBoolean("enabled", false).apply(); }
}
