package com.monkeybody.brain.vlm;

import android.content.Context;
import android.graphics.Bitmap;
import com.monkeybody.brain.api.*;
import java.util.concurrent.*;

/**
 * 容量为 1 的“保留最新帧”推理队列。新帧到来时会释放尚未推理的旧帧，避免积压和 OOM。
 */
public final class VlmCoordinator implements ScreenFrameConsumer {
    public interface Observer { void onStatus(String text); }
    private final Object lock = new Object();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "local-vlm"));
    private final UserContextStore contextStore;
    private final CloudVlmSettings cloudSettings;
    private final VlmEngine engine;
    private Bitmap pending;
    private long pendingTimestamp;
    private boolean draining;
    private PerceptualHash.Value lastHash;
    private boolean hasHash;
    private volatile Observer observer;

    public VlmCoordinator(Context context) {
        contextStore = new UserContextStore(context);
        cloudSettings = new CloudVlmSettings(context);
        engine = new HybridVlmEngine(context);
    }
    public void start() { MonkeyBrainBridge.registerFrameConsumer(this); }
    public boolean isReady() { return engine.isReady(); }
    public String status() {
        String mode = cloudSettings.enabled() ? "硅基流动云端" : "端侧本地";
        return mode + " / " + (engine.isReady() ? "已就绪" : engine.unavailableReason());
    }
    public UserContextStore contextStore() { return contextStore; }
    public void setObserver(Observer value) { observer = value; }

    @Override public void onFrame(Bitmap source, long timestampMillis) {
        if (!engine.isReady()) { notifyStatus(engine.unavailableReason()); return; }
        PerceptualHash.Value hash = PerceptualHash.dHash(source);
        synchronized (lock) {
            if (hasHash && PerceptualHash.similar(lastHash, hash)) { notifyStatus("DEDUPLICATED"); return; }
            lastHash = hash; hasHash = true;
            Bitmap copy = source.copy(Bitmap.Config.ARGB_8888, false);
            if (pending != null) pending.recycle();
            pending = copy; pendingTimestamp = timestampMillis;
            if (!draining) { draining = true; worker.execute(this::drain); }
        }
    }

    private void drain() {
        while (true) {
            Bitmap frame; long timestamp;
            synchronized (lock) {
                frame = pending; timestamp = pendingTimestamp; pending = null;
                if (frame == null) { draining = false; return; }
            }
            long started = android.os.SystemClock.elapsedRealtime();
            try {
                notifyStatus("正在调用 VLM 分析截图…");
                String output = engine.infer(frame, buildPrompt(contextStore.get()));
                RecognitionResult result = VlmResultParser.parse(output, started, timestamp);
                if (!"DETAIL".equals(result.pageType)) {
                    FeedbackOutput feedback = MonkeyBrainBridge.feedback();
                    if (feedback != null) feedback.setMonkeyVisible(false);
                    notifyStatus("当前不是单条详情页，已跳过反馈");
                    continue;
                }
                RecognitionResultReceiver receiver = MonkeyBrainBridge.receiver();
                if (receiver != null) receiver.onRecognitionResult(result);
                notifyStatus(result.category + " | " + result.inferenceMillis + " ms");
            } catch (Exception error) { notifyStatus("分析失败：" + error.getMessage()); }
            finally { frame.recycle(); }
        }
    }

    private static String buildPrompt(String interests) {
        return "分析小红书截图。先判断页面类型：单条图文或视频详情页为DETAIL，信息流列表为LIST，其他为OTHER。"
                + "仅当页面类型为DETAIL时评估内容价值。用户兴趣：" + interests + "。识别主题、类型、信息密度，并判断是否符合兴趣且有信息增量。"
                + "只输出JSON：{\"category\":\"LOW_VALUE_IRRELEVANT或HIGH_VALUE_MATCH\","
                + "\"pageType\":\"DETAIL或LIST或OTHER\",\"topic\":\"简短主题\","
                + "\"contentType\":\"类型\",\"informationDensity\":0到100,\"confidence\":0到1}";
    }
    private void notifyStatus(String value) { Observer valueObserver = observer; if (valueObserver != null) valueObserver.onStatus(value); }
}
