package com.monkeybody.brain.diagnostics;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import com.monkeybody.brain.*;
import com.monkeybody.brain.api.*;

/** 手工联调页：不依赖模型即可注入标准结果，验证统计与反馈后半链路。 */
public final class DiagnosticsActivity extends Activity {
    private TextView status;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,48,32,32);
        status = new TextView(this);
        Button refresh = button("刷新诊断状态");
        Button low = button("注入低质内容测试");
        Button high = button("注入高价值内容测试");
        root.addView(status); root.addView(refresh); root.addView(low); root.addView(high); setContentView(root);
        refresh.setOnClickListener(v -> refresh());
        low.setOnClickListener(v -> inject("LOW_VALUE_IRRELEVANT", "娱乐八卦", "短视频", 8));
        high.setOnClickListener(v -> inject("HIGH_VALUE_MATCH", "端侧人工智能", "技术文章", 90));
        refresh();
    }
    private Button button(String text) { Button button = new Button(this); button.setText(text); return button; }
    private void inject(String category, String topic, String type, int density) {
        RecognitionResultReceiver receiver = MonkeyBrainBridge.receiver();
        if (receiver == null) { Toast.makeText(this, "识别结果接收器未注册", Toast.LENGTH_SHORT).show(); return; }
        receiver.onRecognitionResult(new RecognitionResult(category, .95f, System.currentTimeMillis(), topic, type, density, 120));
        Toast.makeText(this, "测试结果已注入；8 秒限频可能抑制连续提醒", Toast.LENGTH_SHORT).show();
        refresh();
    }
    private void refresh() {
        MonkeyBrainApplication app = (MonkeyBrainApplication) getApplication();
        BrainStatsSnapshot data = app.stats().today();
        status.setText("悬浮窗权限：" + yes(Settings.canDrawOverlays(this))
                + "\n使用情况权限：" + yes(hasUsageAccess())
                + "\nVLM 后端：" + app.vlm().status()
                + "\n悬浮反馈输出：" + yes(MonkeyBrainBridge.feedback() != null)
                + "\n今日识别：" + data.recognitionCount
                + "\n今日提醒：" + data.reminderCount
                + "\n低价值/高价值：" + data.lowValueCount + "/" + data.highValueCount
                + "\n内容类型：" + data.contentTypeDistribution);
    }
    private boolean hasUsageAccess() {
        AppOpsManager ops = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }
    private static String yes(boolean value) { return value ? "正常" : "未就绪"; }
}
