package com.shuagoumei.app;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Draws the intervention and alarm sheets on top of other apps via WindowManager. */
class OverlayManager {

    interface InterventionCallback {
        void onStart(int minutes, String reason);
        void onResist(String reason);
        /** No decision was made (back key / safety timeout): just leave, record nothing. */
        void onCancel();
    }

    interface AlarmCallback {
        void onDone();
        void onSnooze(String reason);
    }

    /** If a sheet is left untouched this long, auto-dismiss it so the screen can never lock up. */
    private static final long SAFETY_MS = 5 * 60 * 1000L;

    private final Context ctx;
    private final WindowManager wm;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private View interveneView;
    private View alarmView;
    private Runnable interveneTimeout;
    private Runnable alarmTimeout;

    OverlayManager(Context ctx) {
        this.ctx = ctx;
        this.wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
    }

    private WindowManager.LayoutParams params() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                // Focusable (no FLAG_NOT_FOCUSABLE) so the reason EditText can receive input.
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        return lp;
    }

    boolean isShowing() {
        return interveneView != null || alarmView != null;
    }

    void showIntervention(String appLabel, final InterventionCallback cb) {
        removeIntervention();
        final View v = LayoutInflater.from(ctx).inflate(R.layout.overlay_intervene, null);
        ((TextView) v.findViewById(R.id.iv_sub))
                .setText("打开「" + appLabel + "」之前，先跟自己约定一下");

        final EditText reason = v.findViewById(R.id.iv_reason);
        final TextView go = v.findViewById(R.id.iv_go);
        final LinearLayout chips = v.findViewById(R.id.iv_chips);
        final int[] selected = {-1};

        final Runnable refresh = new Runnable() {
            @Override public void run() {
                boolean ok = selected[0] > 0 && reason.getText().toString().trim().length() > 0;
                go.setAlpha(ok ? 1f : 0.45f);
                armIntervene();
            }
        };

        for (int i = 0; i < chips.getChildCount(); i++) {
            final TextView chip = (TextView) chips.getChildAt(i);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    for (int j = 0; j < chips.getChildCount(); j++) {
                        chips.getChildAt(j).setSelected(false);
                    }
                    chip.setSelected(true);
                    try {
                        selected[0] = Integer.parseInt(String.valueOf(chip.getTag()));
                    } catch (Exception e) {
                        selected[0] = -1;
                    }
                    refresh.run();
                }
            });
        }

        reason.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { refresh.run(); }
        });

        // Quick-reason chips: tapping fills the reason box (still editable by hand).
        final LinearLayout reasonChips = v.findViewById(R.id.iv_reason_chips);
        if (reasonChips != null) {
            for (int r = 0; r < reasonChips.getChildCount(); r++) {
                if (!(reasonChips.getChildAt(r) instanceof LinearLayout)) continue;
                LinearLayout row = (LinearLayout) reasonChips.getChildAt(r);
                for (int k = 0; k < row.getChildCount(); k++) {
                    if (!(row.getChildAt(k) instanceof TextView)) continue;
                    final TextView rc = (TextView) row.getChildAt(k);
                    rc.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            for (int r2 = 0; r2 < reasonChips.getChildCount(); r2++) {
                                if (!(reasonChips.getChildAt(r2) instanceof LinearLayout)) continue;
                                LinearLayout g = (LinearLayout) reasonChips.getChildAt(r2);
                                for (int k2 = 0; k2 < g.getChildCount(); k2++) {
                                    g.getChildAt(k2).setSelected(false);
                                }
                            }
                            reason.setText(rc.getText());
                            reason.setSelection(reason.getText().length());
                            rc.setSelected(true);
                            refresh.run();
                        }
                    });
                }
            }
        }

        go.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                String r = reason.getText().toString().trim();
                if (selected[0] <= 0 || r.length() == 0) return;
                removeIntervention();
                cb.onStart(selected[0], r);
            }
        });

        v.findViewById(R.id.iv_resist).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                String r = reason.getText().toString().trim();
                removeIntervention();
                cb.onResist(r);
            }
        });

        interveneTimeout = new Runnable() {
            @Override public void run() {
                removeIntervention();
                cb.onCancel();
            }
        };
        attachEscape(v, new Runnable() {
            @Override public void run() {
                removeIntervention();
                cb.onCancel();
            }
        }, new Runnable() {
            @Override public void run() { armIntervene(); }
        });

        interveneView = v;
        wm.addView(v, params());
        armIntervene();
    }

    private void armIntervene() {
        if (interveneTimeout == null) return;
        ui.removeCallbacks(interveneTimeout);
        ui.postDelayed(interveneTimeout, SAFETY_MS);
    }

    private void armAlarm() {
        if (alarmTimeout == null) return;
        ui.removeCallbacks(alarmTimeout);
        ui.postDelayed(alarmTimeout, SAFETY_MS);
    }

    /**
     * Makes a full-screen overlay impossible to get stuck behind: the back key
     * dismisses it, and any touch keeps the safety timeout from firing while the
     * user is actually interacting.
     */
    private void attachEscape(View v, final Runnable onBack, final Runnable onTouch) {
        v.setFocusableInTouchMode(true);
        v.requestFocus();
        v.setOnKeyListener(new View.OnKeyListener() {
            @Override public boolean onKey(View view, int keyCode, KeyEvent e) {
                if (keyCode == KeyEvent.KEYCODE_BACK && e.getAction() == KeyEvent.ACTION_UP) {
                    onBack.run();
                    return true;
                }
                return false;
            }
        });
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View view, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) onTouch.run();
                return false;
            }
        });
    }

    void showAlarm(String appLabel, int plannedMin, final AlarmCallback cb) {
        removeAlarm();
        final View v = LayoutInflater.from(ctx).inflate(R.layout.overlay_alarm, null);
        ((TextView) v.findViewById(R.id.al_msg)).setText(
                "你说好刷「" + appLabel + "」" + plannedMin + " 分钟，到点了。\n要不要继续刷？");

        final EditText reason = v.findViewById(R.id.al_reason);
        final TextView snooze = v.findViewById(R.id.al_snooze);

        final Runnable refresh = new Runnable() {
            @Override public void run() {
                boolean ok = reason.getText().toString().trim().length() > 0;
                snooze.setAlpha(ok ? 1f : 0.45f);
                armAlarm();
            }
        };

        // Quick-reason chips: tapping fills the reason box (still editable by hand).
        final LinearLayout reasonChips = v.findViewById(R.id.al_reason_chips);
        if (reasonChips != null) {
            for (int r = 0; r < reasonChips.getChildCount(); r++) {
                if (!(reasonChips.getChildAt(r) instanceof LinearLayout)) continue;
                LinearLayout row = (LinearLayout) reasonChips.getChildAt(r);
                for (int k = 0; k < row.getChildCount(); k++) {
                    if (!(row.getChildAt(k) instanceof TextView)) continue;
                    final TextView rc = (TextView) row.getChildAt(k);
                    rc.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View view) {
                            for (int r2 = 0; r2 < reasonChips.getChildCount(); r2++) {
                                if (!(reasonChips.getChildAt(r2) instanceof LinearLayout)) continue;
                                LinearLayout g = (LinearLayout) reasonChips.getChildAt(r2);
                                for (int k2 = 0; k2 < g.getChildCount(); k2++) {
                                    g.getChildAt(k2).setSelected(false);
                                }
                            }
                            reason.setText(rc.getText());
                            reason.setSelection(reason.getText().length());
                            rc.setSelected(true);
                            refresh.run();
                        }
                    });
                }
            }
        }

        reason.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { refresh.run(); }
        });
        refresh.run();

        v.findViewById(R.id.al_done).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                removeAlarm();
                cb.onDone();
            }
        });
        snooze.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                String r = reason.getText().toString().trim();
                if (r.length() == 0) return;
                removeAlarm();
                cb.onSnooze(r);
            }
        });

        alarmTimeout = new Runnable() {
            @Override public void run() {
                removeAlarm();
                cb.onDone();
            }
        };
        attachEscape(v, new Runnable() {
            @Override public void run() {
                removeAlarm();
                cb.onDone();
            }
        }, new Runnable() {
            @Override public void run() { armAlarm(); }
        });

        alarmView = v;
        wm.addView(v, params());
        armAlarm();
    }

    void removeIntervention() {
        if (interveneTimeout != null) {
            ui.removeCallbacks(interveneTimeout);
            interveneTimeout = null;
        }
        if (interveneView != null) {
            try { wm.removeView(interveneView); } catch (Exception ignored) {}
            interveneView = null;
        }
    }

    void removeAlarm() {
        if (alarmTimeout != null) {
            ui.removeCallbacks(alarmTimeout);
            alarmTimeout = null;
        }
        if (alarmView != null) {
            try { wm.removeView(alarmView); } catch (Exception ignored) {}
            alarmView = null;
        }
    }

    void removeAll() {
        removeIntervention();
        removeAlarm();
    }
}
