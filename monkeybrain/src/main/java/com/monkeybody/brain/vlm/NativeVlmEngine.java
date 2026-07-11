package com.monkeybody.brain.vlm;

import android.content.Context;
import android.graphics.Bitmap;
import java.io.File;

/** JNI 门面。只有模型和编译后的 llama.cpp Native 库同时存在时才允许推理。 */
public final class NativeVlmEngine implements VlmEngine {
    private static final boolean LIBRARY_LOADED;
    static {
        boolean loaded;
        try { System.loadLibrary("monkey_vlm"); loaded = true; }
        catch (UnsatisfiedLinkError error) { loaded = false; }
        LIBRARY_LOADED = loaded;
    }
    private long handle;
    private String reason = "MODEL_UNAVAILABLE: Native library or GGUF assets are missing";

    public NativeVlmEngine(Context context) {
        if (!LIBRARY_LOADED) return;
        try {
            File model = ModelAssets.materialize(context, "models/model-q4.gguf");
            File projector = ModelAssets.materialize(context, "models/mmproj.gguf");
            handle = nativeCreate(model.getAbsolutePath(), projector.getAbsolutePath(), 2048, 4);
            if (handle == 0L) reason = "MODEL_LOAD_FAILED";
        } catch (Exception error) { reason = "MODEL_UNAVAILABLE: " + error.getMessage(); }
    }
    @Override public boolean isReady() { return handle != 0L; }
    @Override public String unavailableReason() { return reason; }
    @Override public String infer(Bitmap bitmap, String prompt) {
        if (!isReady()) throw new IllegalStateException(reason);
        return nativeInfer(handle, bitmap, prompt, 160);
    }
    @Override public void close() { if (handle != 0L) { nativeDestroy(handle); handle = 0L; } }
    private static native long nativeCreate(String model, String mmproj, int contextSize, int threads);
    private static native String nativeInfer(long handle, Bitmap bitmap, String prompt, int maxTokens);
    private static native void nativeDestroy(long handle);
}
