package com.monkeybody.brain.vlm;

import com.monkeybody.brain.api.RecognitionResult;
import org.json.JSONObject;

/** 只接受约定 JSON，拒绝将自由文本猜测为分类结果。 */
final class VlmResultParser {
    static RecognitionResult parse(String text, long startedAt, long timestamp) throws Exception {
        int first = text.indexOf('{'), last = text.lastIndexOf('}');
        if (first < 0 || last <= first) throw new IllegalArgumentException("VLM response is not JSON");
        JSONObject json = new JSONObject(text.substring(first, last + 1));
        String category = json.getString("category");
        if (!"LOW_VALUE_IRRELEVANT".equals(category) && !"HIGH_VALUE_MATCH".equals(category))
            throw new IllegalArgumentException("Unknown category: " + category);
        int density = Math.max(0, Math.min(100, json.getInt("informationDensity")));
        float confidence = (float) Math.max(0d, Math.min(1d, json.getDouble("confidence")));
        return new RecognitionResult(category, confidence, timestamp, json.optString("topic"),
                json.optString("contentType"), density, android.os.SystemClock.elapsedRealtime() - startedAt,
                json.optString("pageType", "OTHER"));
    }
    private VlmResultParser() {}
}
