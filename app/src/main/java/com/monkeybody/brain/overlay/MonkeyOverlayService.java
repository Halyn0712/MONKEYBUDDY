package com.monkeybody.brain.overlay;

import android.animation.ValueAnimator;
import android.animation.ObjectAnimator;
import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import com.monkeybody.brain.MainActivity;
import com.monkeybody.brain.R;
import com.monkeybody.brain.api.FeedbackOutput;
import com.monkeybody.brain.api.MonkeyBrainBridge;
import com.monkeybody.brain.capture.CaptureContract;
import com.monkeybody.brain.feedback.SoundFeedbackPlayer;

/** 管理全局悬浮猴子的生命周期、拖拽、贴边及展开动画。 */
public class MonkeyOverlayService extends Service implements FeedbackOutput {
    private static final String CHANNEL_ID = "monkey_overlay";
    private static final String PREFS = "overlay_position";
    private WindowManager windowManager;
    private View overlay;
    private WindowManager.LayoutParams params;
    private TextView message;
    private View captureStatus;
    private boolean expanded;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private SoundFeedbackPlayer soundPlayer;
    private final Runnable autoHide = this::finishFeedback;
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            updateCaptureStyle(intent.getIntExtra(CaptureContract.EXTRA_STATE, CaptureContract.STATE_STOPPED));
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        soundPlayer = new SoundFeedbackPlayer(this);
        createChannel();
        startForeground(1001, buildNotification());
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
        showOverlay();
        IntentFilter filter = new IntentFilter(CaptureContract.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(stateReceiver, filter, RECEIVER_NOT_EXPORTED);
        else registerReceiver(stateReceiver, filter);
        MonkeyBrainBridge.registerFeedbackOutput(this);
    }

    private void showOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlay = LayoutInflater.from(this).inflate(R.layout.view_monkey_overlay, null);
        message = overlay.findViewById(R.id.messageText);
        captureStatus = overlay.findViewById(R.id.captureStatus);
        updateCaptureStyle(CaptureContract.STATE_STOPPED);
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, dp(132), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        // 首次使用新全身形象时重置到左侧半探出位置，形成“挂在屏幕边缘”的视觉。
        if (!prefs.getBoolean("hanging_asset_v1", false)) {
            params.x = -dp(18);
            prefs.edit().putBoolean("hanging_asset_v1", true).putInt("x", params.x).apply();
        } else params.x = prefs.getInt("x", -dp(18));
        params.y = prefs.getInt("y", dp(120));
        overlay.setOnTouchListener(new DragTouchListener());
        windowManager.addView(overlay, params);
        setMonkeyVisibleInternal(false);
    }

    private final class DragTouchListener implements View.OnTouchListener {
        private float downRawX, downRawY;
        private int downX, downY;
        private boolean dragged;
        @Override public boolean onTouch(View view, android.view.MotionEvent event) {
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    downRawX = event.getRawX(); downRawY = event.getRawY();
                    downX = params.x; downY = params.y; dragged = false; return true;
                case android.view.MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - downRawX, dy = event.getRawY() - downRawY;
                    if (Math.hypot(dx, dy) > dp(6)) dragged = true;
                    params.x = downX + Math.round(dx);
                    int maxY = Math.max(0, getResources().getDisplayMetrics().heightPixels - overlay.getHeight());
                    params.y = Math.max(0, Math.min(maxY, downY + Math.round(dy)));
                    windowManager.updateViewLayout(overlay, params); return true;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    if (dragged) snapToEdge(); else toggleExpanded();
                    return true;
                default: return false;
            }
        }
    }

    private void toggleExpanded() {
        expanded = !expanded;
        message.setVisibility(expanded ? View.VISIBLE : View.GONE);
        overlay.setAlpha(0.75f);
        overlay.setScaleX(0.94f); overlay.setScaleY(0.94f);
        overlay.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start();
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        windowManager.updateViewLayout(overlay, params);
        // 在右侧贴边时，展开后等待重新测量，再把整个控件移回可见区域。
        overlay.post(() -> {
            int maxX = Math.max(0, getResources().getDisplayMetrics().widthPixels - overlay.getWidth());
            if (params.x > maxX) {
                params.x = maxX;
                windowManager.updateViewLayout(overlay, params);
            }
        });
    }

    private void snapToEdge() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int viewWidth = overlay.getWidth();
        int peek = dp(18);
        int target = params.x + viewWidth / 2 < screenWidth / 2 ? -peek : Math.max(0, screenWidth - viewWidth + peek);
        ValueAnimator animator = ValueAnimator.ofInt(params.x, target);
        animator.setDuration(220); animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            params.x = (int) a.getAnimatedValue();
            if (overlay != null) windowManager.updateViewLayout(overlay, params);
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt("x", params.x).putInt("y", params.y).apply();
            }
        });
        animator.start();
    }

    @Override public void showMessage(String text) {
        if (overlay == null) return;
        uiHandler.post(() -> {
            setMonkeyVisibleInternal(true);
            message.setText(text);
            if (!expanded) toggleExpanded();
            uiHandler.removeCallbacks(autoHide);
            uiHandler.postDelayed(autoHide, 3_000L);
        });
    }
    @Override public void hideMessage() {
        if (overlay != null) uiHandler.post(() -> {
            uiHandler.removeCallbacks(autoHide);
            if (expanded) toggleExpanded();
            message.setText("");
        });
    }
    @Override public void playSound(String soundKey) { uiHandler.post(() -> soundPlayer.play(soundKey)); }
    @Override public void playAnimation(String animationKey) {
        uiHandler.post(() -> {
            if (overlay == null) return;
            ObjectAnimator animator = "bounce".equals(animationKey)
                    ? ObjectAnimator.ofFloat(overlay, View.TRANSLATION_Y, 0f, -dp(18), 0f, -dp(8), 0f)
                    : ObjectAnimator.ofFloat(overlay, View.TRANSLATION_X, 0f, -dp(9), dp(9), -dp(6), dp(6), 0f);
            animator.setDuration("bounce".equals(animationKey) ? 520L : 380L);
            animator.start();
        });
    }
    @Override public void setMonkeyVisible(boolean visible) {
        uiHandler.post(() -> setMonkeyVisibleInternal(visible));
    }

    private void finishFeedback() {
        if (expanded) toggleExpanded();
        message.setText("");
        setMonkeyVisibleInternal(false);
    }

    private void setMonkeyVisibleInternal(boolean visible) {
        if (overlay == null || windowManager == null || params == null) return;
        overlay.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        else params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        windowManager.updateViewLayout(overlay, params);
    }

    @Override public void onDestroy() {
        uiHandler.removeCallbacksAndMessages(null);
        if (soundPlayer != null) soundPlayer.release();
        try { unregisterReceiver(stateReceiver); } catch (IllegalArgumentException ignored) {}
        if (overlay != null && windowManager != null) windowManager.removeView(overlay);
        MonkeyBrainBridge.clearFeedbackOutput(this);
        super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent intent) { return null; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void updateCaptureStyle(int state) {
        if (captureStatus == null || overlay == null) return;
        int color = state == CaptureContract.STATE_CAPTURING ? Color.rgb(46, 160, 67)
                : state == CaptureContract.STATE_PAUSED ? Color.rgb(232, 153, 28) : Color.rgb(130, 130, 130);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(color);
        captureStatus.setBackground(dot);
        overlay.animate().alpha(state == CaptureContract.STATE_CAPTURING ? 0.82f : 0.68f).setDuration(180).start();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_monkey)
                .setContentTitle(getString(R.string.app_name)).setContentText(getString(R.string.notification_text))
                .setContentIntent(pending).setOngoing(true).build();
    }
}
