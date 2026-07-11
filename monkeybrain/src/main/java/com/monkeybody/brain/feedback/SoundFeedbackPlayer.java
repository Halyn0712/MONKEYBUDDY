package com.monkeybody.brain.feedback;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/** 使用系统 ToneGenerator 合成短提示音，不增加音频资源和解码开销。 */
public final class SoundFeedbackPlayer {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Context context;
    private MediaPlayer monkeyPlayer;
    public SoundFeedbackPlayer(Context context) { this.context = context.getApplicationContext(); }
    public void play(String key) {
        if ("monkey_roar".equals(key)) {
            if (monkeyPlayer != null) { monkeyPlayer.release(); monkeyPlayer = null; }
            monkeyPlayer = MediaPlayer.create(context, com.monkeybody.brain.R.raw.monkey_call);
            if (monkeyPlayer != null) {
                monkeyPlayer.setOnCompletionListener(player -> { player.release(); if (monkeyPlayer == player) monkeyPlayer = null; });
                monkeyPlayer.start();
            }
        } else {
            ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 55);
            int kind = "warning_ad".equals(key) ? ToneGenerator.TONE_PROP_NACK
                    : "warning_video".equals(key) ? ToneGenerator.TONE_PROP_BEEP : ToneGenerator.TONE_PROP_PROMPT;
            tone.startTone(kind, 150);
            handler.postDelayed(tone::release, 220);
        }
    }
    public void release() { if (monkeyPlayer != null) { monkeyPlayer.release(); monkeyPlayer = null; } }
}
