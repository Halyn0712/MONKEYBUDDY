package com.monkeybody.brain.capture;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.view.Display;
import android.view.WindowManager;
import com.monkeybody.brain.MainActivity;
import com.monkeybody.brain.R;
import com.monkeybody.brain.api.*;
import java.nio.ByteBuffer;
import java.util.*;

/** 周期性内存屏幕采集服务：不创建文件、不执行网络请求。 */
public class ScreenCaptureService extends Service implements ScreenCaptureController, DisplayManager.DisplayListener {
    public static final String EXTRA_RESULT_CODE = "resultCode";
    public static final String EXTRA_RESULT_DATA = "resultData";
    public static final String EXTRA_TARGET_PACKAGES = "targetPackages";
    private static final String CHANNEL_ID = "screen_capture";
    /** 搭子模式默认每 2.5 秒取一帧，处于用户要求的 2~3 秒范围内。 */
    private static final long DEFAULT_INTERVAL_MS = 2_500L;
    private static final int MAX_LONG_EDGE = 768;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final HandlerThread imageThread = new HandlerThread("screen-frame-reader");
    private final Set<String> targetPackages = Collections.synchronizedSet(new HashSet<>(
            Collections.singletonList("com.xingin.xhs")));
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private DisplayManager displayManager;
    private ForegroundAppDetector appDetector;
    private ScreenFrameProcessor processor;
    private volatile long intervalMillis = DEFAULT_INTERVAL_MS;
    private volatile long lastFrameAt;
    private volatile boolean manuallyPaused;
    private volatile boolean targetInForeground;

    private final Runnable appCheck = new Runnable() {
        @Override public void run() {
            String current = appDetector.currentPackage();
            boolean shouldCapture = !manuallyPaused && current != null && targetPackages.contains(current);
            if (targetInForeground != shouldCapture) {
                targetInForeground = shouldCapture;
                publishState(shouldCapture ? CaptureContract.STATE_CAPTURING : CaptureContract.STATE_PAUSED);
            }
            mainHandler.postDelayed(this, 1_000L);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(1002, buildNotification());
        imageThread.start();
        displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        displayManager.registerDisplayListener(this, mainHandler);
        appDetector = new ForegroundAppDetector(this);
        processor = new ScreenFrameProcessor(this, MAX_LONG_EDGE);
        MonkeyBrainBridge.registerCaptureController(this);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (projection != null) return START_NOT_STICKY;
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }
        ArrayList<String> requestedTargets = intent.getStringArrayListExtra(EXTRA_TARGET_PACKAGES);
        if (requestedTargets != null && !requestedTargets.isEmpty()) setTargetPackages(new HashSet<>(requestedTargets));
        Intent data = Build.VERSION.SDK_INT >= 33
                ? intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class)
                : intent.getParcelableExtra(EXTRA_RESULT_DATA);
        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED);
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        projection = manager.getMediaProjection(resultCode, data);
        if (projection == null) { stopSelf(); return START_NOT_STICKY; }
        projection.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() { mainHandler.post(() -> stopSelf()); }
        }, mainHandler);
        createDisplay();
        mainHandler.post(appCheck);
        publishState(CaptureContract.STATE_PAUSED);
        return START_NOT_STICKY;
    }

    private void createDisplay() {
        Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        display.getRealMetrics(metrics);
        replaceImageReader(metrics.widthPixels, metrics.heightPixels);
        virtualDisplay = projection.createVirtualDisplay("MonkeyBodyCapture", metrics.widthPixels, metrics.heightPixels,
                metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, mainHandler);
    }

    private void replaceImageReader(int width, int height) {
        ImageReader old = imageReader;
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(this::onImageAvailable, new Handler(imageThread.getLooper()));
        if (virtualDisplay != null) {
            virtualDisplay.resize(width, height, getResources().getDisplayMetrics().densityDpi);
            virtualDisplay.setSurface(imageReader.getSurface());
        }
        if (old != null) old.close();
    }

    private void onImageAvailable(ImageReader reader) {
        try (Image image = reader.acquireLatestImage()) {
            long now = SystemClock.elapsedRealtime();
            if (image == null || !targetInForeground || now - lastFrameAt < intervalMillis) return;
            lastFrameAt = now;
            Bitmap raw = imageToBitmap(image);
            Bitmap prepared = null;
            try {
                prepared = processor.process(raw);
                ScreenFrameConsumer consumer = MonkeyBrainBridge.frameConsumer();
                if (consumer != null) consumer.onFrame(prepared, System.currentTimeMillis());
            } finally {
                if (prepared != null && !prepared.isRecycled()) prepared.recycle();
                if (!raw.isRecycled()) raw.recycle();
            }
        } catch (IllegalStateException ignored) {
            // 旋转替换 ImageReader 或系统回收 Projection 时可能与帧回调并发。
        }
    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int paddedWidth = image.getWidth() + (rowStride - pixelStride * image.getWidth()) / pixelStride;
        Bitmap padded = Bitmap.createBitmap(paddedWidth, image.getHeight(), Bitmap.Config.ARGB_8888);
        padded.copyPixelsFromBuffer(buffer);
        Bitmap exact = Bitmap.createBitmap(padded, 0, 0, image.getWidth(), image.getHeight());
        if (exact != padded) padded.recycle();
        return exact;
    }

    @Override public void onDisplayChanged(int displayId) {
        if (displayId != Display.DEFAULT_DISPLAY || virtualDisplay == null) return;
        Display display = displayManager.getDisplay(displayId);
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        display.getRealMetrics(metrics);
        if (imageReader.getWidth() != metrics.widthPixels || imageReader.getHeight() != metrics.heightPixels) {
            replaceImageReader(metrics.widthPixels, metrics.heightPixels);
        }
    }
    @Override public void onDisplayAdded(int id) {}
    @Override public void onDisplayRemoved(int id) {}

    @Override public void startCapture() { manuallyPaused = false; }
    @Override public void pauseCapture() { manuallyPaused = true; targetInForeground = false; publishState(CaptureContract.STATE_PAUSED); }
    @Override public void stopCapture() { stopSelf(); }
    @Override public boolean isCapturing() { return targetInForeground; }
    @Override public void setCaptureIntervalMillis(long value) { intervalMillis = Math.max(250L, value); }
    @Override public void setTargetPackages(Set<String> values) {
        targetPackages.clear();
        if (values != null) targetPackages.addAll(values);
    }

    private void publishState(int state) {
        Intent update = new Intent(CaptureContract.ACTION_STATE_CHANGED).setPackage(getPackageName());
        update.putExtra(CaptureContract.EXTRA_STATE, state);
        sendBroadcast(update);
    }

    @Override public void onDestroy() {
        mainHandler.removeCallbacks(appCheck);
        publishState(CaptureContract.STATE_STOPPED);
        if (displayManager != null) displayManager.unregisterDisplayListener(this);
        if (virtualDisplay != null) virtualDisplay.release();
        if (imageReader != null) imageReader.close();
        if (projection != null) projection.stop();
        MonkeyBrainBridge.clearCaptureController(this);
        imageThread.quitSafely();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.capture_channel_name), NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    private Notification buildNotification() {
        PendingIntent pending = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_monkey)
                .setContentTitle(getString(R.string.capture_channel_name))
                .setContentText(getString(R.string.capture_notification_text)).setContentIntent(pending).setOngoing(true).build();
    }
}
