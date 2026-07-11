package com.shuagoumei.app;

import android.app.Activity;
import android.os.Bundle;

/** Transparent helper that only requests the system screen-capture consent dialog. */
public final class BrainCaptureAuthActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BrainSessionCoordinator.requestCapturePermission(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        BrainSessionCoordinator.handleCaptureResult(this, requestCode, resultCode, data);
        finish();
    }
}
