package com.monkeybody.brain;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.media.projection.MediaProjectionManager;
import android.widget.Button;
import android.widget.Toast;
import com.monkeybody.brain.overlay.MonkeyOverlayService;
import com.monkeybody.brain.capture.ScreenCaptureService;
import com.monkeybody.brain.vlm.VlmDemoActivity;
import com.monkeybody.brain.diagnostics.DiagnosticsActivity;

/** 权限引导页：系统授权返回后由用户再次点击启动，避免厂商 ROM 回调不一致。 */
public class MainActivity extends Activity {
    private static final int REQUEST_CAPTURE = 21;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        Button start = findViewById(R.id.startButton);
        Button stop = findViewById(R.id.stopButton);
        Button demo = findViewById(R.id.demoButton);
        Button diagnostics = findViewById(R.id.diagnosticsButton);
        start.setOnClickListener(v -> startOrRequestPermission());
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, ScreenCaptureService.class));
            stopService(new Intent(this, MonkeyOverlayService.class));
        });
        demo.setOnClickListener(v -> startActivity(new Intent(this, VlmDemoActivity.class)));
        diagnostics.setOnClickListener(v -> startActivity(new Intent(this, DiagnosticsActivity.class)));
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 12);
    }

    private void startOrRequestPermission() {
        if (!Settings.canDrawOverlays(this)) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {
                // 个别 ROM 不接受 package URI，退回悬浮窗应用列表。
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
            }
            Toast.makeText(this, R.string.permission_hint, Toast.LENGTH_LONG).show();
            return;
        }
        if (!hasUsageAccess()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            Toast.makeText(this, R.string.usage_permission_hint, Toast.LENGTH_LONG).show();
            return;
        }
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CAPTURE) return;
        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, R.string.projection_permission_denied, Toast.LENGTH_SHORT).show();
            return;
        }
        startForegroundService(new Intent(this, MonkeyOverlayService.class));
        Intent capture = new Intent(this, ScreenCaptureService.class)
                .putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                .putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
                .putStringArrayListExtra(ScreenCaptureService.EXTRA_TARGET_PACKAGES,
                        new java.util.ArrayList<>(java.util.Collections.singletonList("com.xingin.xhs")));
        startForegroundService(capture);
    }

    private boolean hasUsageAccess() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}
