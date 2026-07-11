package com.shuagoumei.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.monkeybody.brain.capture.ScreenCaptureService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/** Holds the latest MediaProjection grant and restarts capture when possible. */
final class BrainProjectionStore {

    private static int resultCode = Activity.RESULT_CANCELED;
    private static Intent resultData;
    private static Set<String> targetPackages = Collections.emptySet();

    private BrainProjectionStore() {}

    static void saveGrant(int code, Intent data) {
        resultCode = code;
        resultData = data;
    }

    static boolean hasGrant() {
        return resultCode == Activity.RESULT_OK && resultData != null;
    }

    static void setTargetPackages(Set<String> packages) {
        targetPackages = packages == null ? Collections.<String>emptySet() : packages;
    }

    static void tryStartCapture(Context context) {
        if (!Prefs.isBrainEnabled(context) || !hasGrant() || targetPackages.isEmpty()) return;
        Context app = context.getApplicationContext();
        Intent capture = new Intent(app, ScreenCaptureService.class)
                .putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                .putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, resultData)
                .putStringArrayListExtra(
                        ScreenCaptureService.EXTRA_TARGET_PACKAGES,
                        new ArrayList<>(targetPackages));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(capture);
        } else {
            app.startService(capture);
        }
    }
}
