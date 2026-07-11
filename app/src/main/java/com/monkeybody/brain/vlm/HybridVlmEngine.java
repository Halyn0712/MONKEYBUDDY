package com.monkeybody.brain.vlm;

import android.content.Context;
import android.graphics.Bitmap;

/** 云端模式显式启用时使用 SiliconFlow，否则保持原端侧 llama.cpp 路径。 */
public final class HybridVlmEngine implements VlmEngine {
    private final CloudVlmSettings settings;
    private final VlmEngine local;
    private final VlmEngine cloud;
    public HybridVlmEngine(Context context) {
        settings = new CloudVlmSettings(context); local = new NativeVlmEngine(context); cloud = new SiliconFlowVlmEngine(context);
    }
    private VlmEngine selected() { return settings.enabled() ? cloud : local; }
    @Override public boolean isReady() { return selected().isReady(); }
    @Override public String unavailableReason() { return selected().unavailableReason(); }
    @Override public String infer(Bitmap bitmap, String prompt) throws Exception { return selected().infer(bitmap, prompt); }
    @Override public void close() { local.close(); cloud.close(); }
}
