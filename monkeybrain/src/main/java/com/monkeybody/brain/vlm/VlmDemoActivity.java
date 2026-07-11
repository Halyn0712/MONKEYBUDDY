package com.monkeybody.brain.vlm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import com.monkeybody.brain.MonkeyBrainApplication;

/** 可独立选择一张本地图像，观察模型可用性、分类与单帧耗时。 */
public final class VlmDemoActivity extends Activity implements VlmCoordinator.Observer {
    private static final int PICK_IMAGE = 31;
    private EditText interests;
    private TextView output;
    private VlmCoordinator coordinator;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        coordinator = ((MonkeyBrainApplication) getApplication()).vlm();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32, 48, 32, 32);
        interests = new EditText(this); interests.setHint("关注领域、兴趣标签"); interests.setText(coordinator.contextStore().get());
        Button save = new Button(this); save.setText("保存兴趣配置");
        Button pick = new Button(this); pick.setText("选择截图并测试");
        Button cloud = new Button(this); cloud.setText("云端 VLM 与 API Key 设置");
        output = new TextView(this); output.setText("后端状态：" + coordinator.status()); output.setPadding(0, 24, 0, 0);
        root.addView(interests); root.addView(save); root.addView(pick); root.addView(cloud); root.addView(output); setContentView(root);
        save.setOnClickListener(v -> { coordinator.contextStore().set(interests.getText().toString()); output.setText("兴趣配置已保存到本地"); });
        pick.setOnClickListener(v -> startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE), PICK_IMAGE));
        cloud.setOnClickListener(v -> startActivity(new Intent(this, CloudVlmSettingsActivity.class)));
    }
    @Override protected void onResume() {
        super.onResume(); coordinator.setObserver(this);
        output.setText("后端状态：" + coordinator.status());
    }
    @Override protected void onPause() { coordinator.setObserver(null); super.onPause(); }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_IMAGE || resultCode != RESULT_OK || data == null) return;
        try {
            Uri uri = data.getData();
            Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri),
                    (decoder, info, source) -> decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE));
            coordinator.onFrame(bitmap, System.currentTimeMillis()); bitmap.recycle();
            output.setText("截图已进入推理队列，正在分析…");
        } catch (Exception error) { output.setText("读取失败：" + error.getMessage()); }
    }
    @Override public void onStatus(String text) { runOnUiThread(() -> output.setText(text)); }
}
