package com.monkeybody.brain.capture;

/** 采集服务与界面、悬浮控件之间的进程内广播契约。 */
public final class CaptureContract {
    public static final String ACTION_STATE_CHANGED = "com.monkeybody.brain.CAPTURE_STATE_CHANGED";
    public static final String EXTRA_STATE = "state";
    public static final int STATE_STOPPED = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_CAPTURING = 2;
    private CaptureContract() {}
}
