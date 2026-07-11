package com.monkeybody.brain.api;

import android.graphics.Bitmap;

/**
 * 预处理后屏幕帧的内存消费者。
 * Bitmap 仅在回调执行期间有效；回调返回后采集服务会立即 recycle，禁止缓存引用。
 */
public interface ScreenFrameConsumer {
    void onFrame(Bitmap bitmap, long timestampMillis);
}
