package com.monkeybody.brain.vlm;

import android.content.Context;
import android.content.SharedPreferences;

/** 用户兴趣配置仅保存在应用私有 SharedPreferences。 */
public final class UserContextStore {
    private static final String PREFS = "vlm_user_context";
    private static final String KEY = "interests";
    private final SharedPreferences preferences;
    public UserContextStore(Context context) { preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public String get() { return preferences.getString(KEY, ""); }
    public void set(String value) { preferences.edit().putString(KEY, value == null ? "" : value.trim()).apply(); }
}
