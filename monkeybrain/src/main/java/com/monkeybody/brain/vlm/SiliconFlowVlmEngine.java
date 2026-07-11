package com.monkeybody.brain.vlm;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** 可选硅基流动云端 VLM。仅在用户明确启用后上传预处理截图。 */
public final class SiliconFlowVlmEngine implements VlmEngine {
    private static final String ENDPOINT = "https://api.siliconflow.cn/v1/chat/completions";
    private final CloudVlmSettings settings;
    private final SecretStore secrets;
    public SiliconFlowVlmEngine(Context context) { settings = new CloudVlmSettings(context); secrets = new SecretStore(context); }
    @Override public boolean isReady() { return settings.enabled() && settings.consentTimestamp() > 0L && secrets.configured(); }
    @Override public String unavailableReason() { return settings.enabled() ? "CLOUD_KEY_NOT_CONFIGURED" : "CLOUD_MODE_DISABLED"; }

    @Override public String infer(Bitmap bitmap, String prompt) throws Exception {
        if (!isReady()) throw new IllegalStateException(unavailableReason());
        String apiKey = secrets.read();
        if (apiKey == null || apiKey.trim().isEmpty()) throw new IllegalStateException("CLOUD_KEY_NOT_CONFIGURED");
        ByteArrayOutputStream image = new ByteArrayOutputStream();
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 78, image)) throw new IOException("JPEG encode failed");
        String dataUrl = "data:image/jpeg;base64," + Base64.encodeToString(image.toByteArray(), Base64.NO_WRAP);
        JSONObject request = request(prompt, dataUrl);
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        try {
            connection.setRequestMethod("POST"); connection.setDoOutput(true);
            connection.setConnectTimeout(30_000); connection.setReadTimeout(90_000);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            byte[] body = request.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) { output.write(body); }
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String response = readAll(stream);
            if (code < 200 || code >= 300) throw new IOException("SiliconFlow HTTP " + code + ": " + safeError(response));
            return new JSONObject(response).getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content");
        } finally { connection.disconnect(); }
    }

    private static JSONObject request(String prompt, String dataUrl) throws JSONException {
        JSONArray content = new JSONArray()
                .put(new JSONObject().put("type", "text").put("text", prompt))
                .put(new JSONObject().put("type", "image_url").put("image_url", new JSONObject().put("url", dataUrl)));
        return new JSONObject().put("model", CloudVlmSettings.MODEL)
                .put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", content)))
                .put("stream", false).put("temperature", 0.2).put("max_tokens", 512)
                .put("response_format", new JSONObject().put("type", "json_object"));
    }
    private static String readAll(InputStream input) throws IOException {
        if (input == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder value = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) value.append(line);
            return value.toString();
        }
    }
    private static String safeError(String response) {
        try { return new JSONObject(response).optString("message", "request failed"); }
        catch (JSONException ignored) { return "request failed"; }
    }
    @Override public void close() {}
}
