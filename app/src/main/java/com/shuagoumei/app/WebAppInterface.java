package com.shuagoumei.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONObject;

/** Exposed to the WebView UI as the global "Android" object. */
public class WebAppInterface {

    private final Activity activity;

    WebAppInterface(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public String getWhitelist() {
        PackageManager pm = activity.getPackageManager();
        JSONArray arr = new JSONArray();
        for (String pkg : Prefs.getWhitelist(activity)) {
            String label = pkg;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                label = pm.getApplicationLabel(ai).toString();
            } catch (Exception ignored) {}
            try {
                JSONObject o = new JSONObject();
                o.put("pkg", pkg);
                o.put("label", label);
                arr.put(o);
            } catch (Exception ignored) {}
        }
        return arr.toString();
    }

    @JavascriptInterface
    public String getSessions() {
        return Prefs.getSessionsJson(activity);
    }

    @JavascriptInterface
    public void openAppPicker() {
        activity.runOnUiThread(new Runnable() {
            @Override public void run() {
                activity.startActivity(new Intent(activity, AppPickerActivity.class));
            }
        });
    }

    @JavascriptInterface
    public boolean hasAccessibility() {
        String enabled = Settings.Secure.getString(
                activity.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        ComponentName expected = new ComponentName(activity, AppWatchService.class);
        String flat = expected.flattenToString();
        String flatShort = expected.flattenToShortString();
        for (String part : enabled.split(":")) {
            if (part.equalsIgnoreCase(flat) || part.equalsIgnoreCase(flatShort)) {
                return true;
            }
        }
        return false;
    }

    @JavascriptInterface
    public boolean hasOverlay() {
        return Settings.canDrawOverlays(activity);
    }

    @JavascriptInterface
    public void openAccessibilitySettings() {
        activity.runOnUiThread(new Runnable() {
            @Override public void run() {
                Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    activity.startActivity(i);
                } catch (Exception ignored) {}
            }
        });
    }

    @JavascriptInterface
    public void openOverlaySettings() {
        activity.runOnUiThread(new Runnable() {
            @Override public void run() {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + activity.getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    activity.startActivity(i);
                } catch (Exception e) {
                    try {
                        activity.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                    } catch (Exception ignored) {}
                }
            }
        });
    }
     // 鈹€鈹€ Monkey Brain settings 鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€鈹€
 
     @JavascriptInterface
     public String getBrainIdentity() {
         return Prefs.getIdentity(activity);
     }
 
     @JavascriptInterface
     public void setBrainIdentity(String key) {
         Prefs.setIdentity(activity, key);
     }
 
     @JavascriptInterface
     public boolean getBrainEnabled() {
         return Prefs.isBrainEnabled(activity);
     }
 
     @JavascriptInterface
     public void setBrainEnabled(boolean enabled) {
         Prefs.setBrainEnabled(activity, enabled);
     }
 
    @JavascriptInterface
    public String getIdentityOptions() {
        JSONArray arr = new JSONArray();
        for (ContentJudge.Identity id : ContentJudge.IDENTITIES) {
            try {
                JSONObject o = new JSONObject();
                o.put("key", id.key);
                o.put("label", id.label);
                o.put("emoji", id.emoji);
                arr.put(o);
            } catch (Exception ignored) {}
        }
        return arr.toString();
    }

    @JavascriptInterface
    public String getBrainSnapshot() {
        return Prefs.getBrainSnapshotJson(activity);
    }

    @JavascriptInterface
    public void openBrainDemo() {
        activity.runOnUiThread(new Runnable() {
            @Override public void run() {
                activity.startActivity(new Intent(activity,
                        com.monkeybody.brain.vlm.VlmDemoActivity.class));
            }
        });
    }

    @JavascriptInterface
    public void openBrainCaptureAuth() {
        activity.runOnUiThread(new Runnable() {
            @Override public void run() {
                activity.startActivity(new Intent(activity, BrainCaptureAuthActivity.class));
            }
        });
    }

}
