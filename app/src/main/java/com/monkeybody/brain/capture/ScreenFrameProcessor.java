package com.monkeybody.brain.capture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

/** 裁掉系统栏并按最长边限制等比缩放；所有中间 Bitmap 均由调用方及时回收。 */
final class ScreenFrameProcessor {
    private final Context context;
    private final int maxLongEdge;

    ScreenFrameProcessor(Context context, int maxLongEdge) {
        this.context = context.getApplicationContext();
        this.maxLongEdge = maxLongEdge;
    }

    Bitmap process(Bitmap source) {
        Rect crop = contentRect(source.getWidth(), source.getHeight());
        Bitmap cropped = Bitmap.createBitmap(source, crop.left, crop.top, crop.width(), crop.height());
        int longEdge = Math.max(cropped.getWidth(), cropped.getHeight());
        if (longEdge <= maxLongEdge) return cropped;
        float scale = maxLongEdge / (float) longEdge;
        int width = Math.max(1, Math.round(cropped.getWidth() * scale));
        int height = Math.max(1, Math.round(cropped.getHeight() * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(cropped, width, height, true);
        if (scaled != cropped) cropped.recycle();
        return scaled;
    }

    private Rect contentRect(int width, int height) {
        int status = systemDimension("status_bar_height");
        int navigation = systemDimension("navigation_bar_height");
        // 横屏时导航栏常位于右侧；竖屏时通常位于底部。
        int right = width > height ? Math.max(1, width - navigation) : width;
        int bottom = width > height ? height : Math.max(status + 1, height - navigation);
        return new Rect(0, Math.min(status, height - 1), right, bottom);
    }

    private int systemDimension(String name) {
        int id = context.getResources().getIdentifier(name, "dimen", "android");
        return id == 0 ? 0 : context.getResources().getDimensionPixelSize(id);
    }
}
