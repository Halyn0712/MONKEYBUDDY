package com.monkeybody.brain.vlm;

import android.content.Context;
import java.io.*;

/** 将只读 assets GGUF 顺序复制到 noBackupFilesDir；不涉及屏幕帧。 */
final class ModelAssets {
    static File materialize(Context context, String assetPath) throws IOException {
        File directory = new File(context.getNoBackupFilesDir(), "models");
        if (!directory.exists() && !directory.mkdirs()) throw new IOException("Cannot create model directory");
        File output = new File(directory, new File(assetPath).getName());
        if (output.length() > 1024 * 1024) return output;
        File temporary = new File(output.getPath() + ".tmp");
        try (InputStream input = context.getAssets().open(assetPath); OutputStream stream = new FileOutputStream(temporary)) {
            byte[] buffer = new byte[1024 * 1024]; int count;
            while ((count = input.read(buffer)) != -1) stream.write(buffer, 0, count);
        }
        if (!temporary.renameTo(output)) throw new IOException("Cannot install model asset");
        return output;
    }
    private ModelAssets() {}
}
