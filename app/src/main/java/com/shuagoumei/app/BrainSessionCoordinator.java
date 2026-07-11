package com.shuagoumei.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;

import com.monkeybody.brain.overlay.MonkeyOverlayService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Starts/stops the floating monkey and optional screen capture for a Face session. */
final class BrainSessionCoordinator {

    private static final int REQUEST_CAPTURE = 71;

    private BrainSessionCoordinator() {}

    static void onSessionStarted(Context context, String targetPackage) {
        if (!Prefs.isBrainEnabled(context)) return;
        Context app = context.getApplicationContext();
        syncIdentityToBrain(app);
        Intent overlay = new Intent(app, MonkeyOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            app.startForegroundService(overlay);
        } else {
            app.startService(overlay);
        }
        Set<String> targets = new HashSet<>(Prefs.getWhitelist(app));
        if (targetPackage != null && !targetPackage.isEmpty()) {
            targets.add(targetPackage);
        }
        if (targets.isEmpty() && targetPackage != null) {
            targets = Collections.singleton(targetPackage);
        }
        BrainProjectionStore.setTargetPackages(targets);
        BrainProjectionStore.tryStartCapture(app);
    }

    static void onSessionEnded(Context context) {
        Context app = context.getApplicationContext();
        app.stopService(new Intent(app, com.monkeybody.brain.capture.ScreenCaptureService.class));
        app.stopService(new Intent(app, MonkeyOverlayService.class));
    }

    static void requestCapturePermission(Activity activity) {
        MediaProjectionManager manager =
                (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        activity.startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }

    static boolean handleCaptureResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CAPTURE) return false;
        if (resultCode != Activity.RESULT_OK || data == null) return true;
        BrainProjectionStore.saveGrant(resultCode, data);
        BrainProjectionStore.tryStartCapture(activity.getApplicationContext());
        return true;
    }

    private static void syncIdentityToBrain(Context app) {
        if (BrainInitializer.vlm() == null) return;
        String identity = Prefs.getIdentity(app);
        if (identity == null || identity.isEmpty()) return;
        ContentJudge.Identity preset = ContentJudge.findIdentity(identity);
        BrainInitializer.vlm().contextStore().set(
                preset.label + " " + String.join(" ", preset.focusWords));
    }
}
