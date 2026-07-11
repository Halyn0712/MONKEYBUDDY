package com.monkeybody.brain.vlm;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;

/** 云端模式必须由用户查看上传说明、勾选同意并输入自己的 Key 后才能启用。 */
public final class CloudVlmSettingsActivity extends Activity {
    private CloudVlmSettings settings;
    private SecretStore secrets;
    private Switch enabled;
    private CheckBox consent;
    private EditText key;
    private TextView status;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); settings = new CloudVlmSettings(this); secrets = new SecretStore(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,48,32,32);
        TextView warning = new TextView(this);
        warning.setText("云端模式会将预处理后的屏幕截图上传到硅基流动，用于 " + CloudVlmSettings.MODEL
                + " 内容分类。截图将离开本机，可能产生 API 费用。请勿使用已公开或泄露的 API Key。");
        enabled = new Switch(this); enabled.setText("启用硅基流动云端 VLM"); enabled.setChecked(settings.enabled());
        consent = new CheckBox(this); consent.setText("我理解截图会上传到硅基流动，并同意启用云端识别");
        key = new EditText(this); key.setHint(secrets.configured() ? "已配置 Key；留空表示不更换" : "输入新 API Key");
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Button save = new Button(this); save.setText("保存设置");
        Button clear = new Button(this); clear.setText("删除本机 API Key 并关闭云端模式");
        status = new TextView(this); status.setPadding(0,20,0,0);
        root.addView(warning); root.addView(enabled); root.addView(consent); root.addView(key); root.addView(save); root.addView(clear); root.addView(status);
        setContentView(root); refresh();
        save.setOnClickListener(v -> save());
        clear.setOnClickListener(v -> { secrets.clear(); settings.disable(); enabled.setChecked(false); refresh(); });
    }
    private void save() {
        try {
            String value = key.getText().toString().trim();
            if (!value.isEmpty()) secrets.save(value);
            if (enabled.isChecked()) {
                if (!consent.isChecked()) { status.setText("必须勾选截图上传知情同意"); return; }
                if (!secrets.configured()) { status.setText("请先输入 API Key"); return; }
                settings.enableWithConsent();
            } else settings.disable();
            key.setText(""); refresh();
        } catch (Exception error) { status.setText("保存失败：" + error.getClass().getSimpleName()); }
    }
    private void refresh() {
        status.setText("模式：" + (settings.enabled() ? "云端" : "本地") + "\nAPI Key：" + (secrets.configured() ? "已加密保存" : "未配置"));
    }
}
