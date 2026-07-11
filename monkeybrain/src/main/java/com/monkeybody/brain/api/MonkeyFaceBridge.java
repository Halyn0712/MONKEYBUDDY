package com.monkeybody.brain.api;

/** Monkey Face 可在 Application 初始化时注册真实统计模块。未注册时仅保留本地统计。 */
public final class MonkeyFaceBridge {
    private static volatile MonkeyFaceDataSink sink;
    public static void register(MonkeyFaceDataSink value) { sink = value; }
    public static void unregister(MonkeyFaceDataSink value) { if (sink == value) sink = null; }
    public static MonkeyFaceDataSink sink() { return sink; }
    private MonkeyFaceBridge() {}
}
