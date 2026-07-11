package com.monkeybody.brain.vlm;

import android.graphics.Bitmap;

/** 17x16 灰度差值指纹；比旧 64 位哈希更容易识别信息流内容已经切换。 */
final class PerceptualHash {
    static final class Value {
        final long[] words;
        Value(long[] words) { this.words = words; }
    }
    static Value dHash(Bitmap source) {
        // ImageDecoder 默认可能返回 HARDWARE Bitmap；先复制到软件内存，CPU 才能读取像素。
        Bitmap readable = source.getConfig() == Bitmap.Config.HARDWARE
                ? source.copy(Bitmap.Config.ARGB_8888, false) : source;
        if (readable == null) throw new IllegalArgumentException("Unable to create readable bitmap");
        Bitmap tiny = Bitmap.createScaledBitmap(readable, 17, 16, true);
        long[] hash = new long[4];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int left = tiny.getPixel(x, y), right = tiny.getPixel(x + 1, y);
                int l = (android.graphics.Color.red(left) * 30 + android.graphics.Color.green(left) * 59 + android.graphics.Color.blue(left) * 11) / 100;
                int r = (android.graphics.Color.red(right) * 30 + android.graphics.Color.green(right) * 59 + android.graphics.Color.blue(right) * 11) / 100;
                int bit = y * 16 + x;
                if (l > r) hash[bit / 64] |= 1L << (bit % 64);
            }
        }
        if (tiny != readable) tiny.recycle();
        if (readable != source) readable.recycle();
        return new Value(hash);
    }
    static boolean similar(Value a, Value b) {
        int distance = 0;
        for (int i = 0; i < a.words.length; i++) distance += Long.bitCount(a.words[i] ^ b.words[i]);
        return distance <= 10;
    }
    private PerceptualHash() {}
}
