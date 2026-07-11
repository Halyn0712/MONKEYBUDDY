package com.monkeybody.brain.vlm;

import android.graphics.Bitmap;

/** Native VLM 的可替换边界，便于真机后端与测试实现隔离。 */
public interface VlmEngine extends AutoCloseable {
    boolean isReady();
    String unavailableReason();
    String infer(Bitmap bitmap, String prompt) throws Exception;
    @Override void close();
}
