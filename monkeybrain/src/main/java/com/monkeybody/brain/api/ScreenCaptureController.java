package com.monkeybody.brain.api;

/** 屏幕采集控制契约。实际 MediaProjection 授权与实现由后续模块注入。 */
public interface ScreenCaptureController {
    void startCapture();
    void pauseCapture();
    void stopCapture();
    boolean isCapturing();
    void setCaptureIntervalMillis(long intervalMillis);
    void setTargetPackages(java.util.Set<String> packageNames);
}
