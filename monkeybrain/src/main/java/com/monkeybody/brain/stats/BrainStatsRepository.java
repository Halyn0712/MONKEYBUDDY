package com.monkeybody.brain.stats;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import com.monkeybody.brain.api.*;
import java.util.*;

/** 应用私有 SQLite 统计库，不保存截图、识别原文或用户兴趣。 */
public final class BrainStatsRepository extends SQLiteOpenHelper {
    private static final String DB = "monkey_brain_stats.db";
    public BrainStatsRepository(Context context) { super(context, DB, null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE brain_event(id INTEGER PRIMARY KEY AUTOINCREMENT,event_type TEXT NOT NULL,"
                + "timestamp_ms INTEGER NOT NULL,category TEXT,content_type TEXT,topic TEXT,confidence REAL)");
        db.execSQL("CREATE INDEX idx_brain_event_time ON brain_event(timestamp_ms)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public synchronized void record(BrainStatsEvent event) {
        ContentValues values = new ContentValues();
        values.put("event_type", event.eventType); values.put("timestamp_ms", event.timestampMillis);
        values.put("category", safe(event.category)); values.put("content_type", safe(event.contentType));
        values.put("topic", safe(event.topic)); values.put("confidence", event.confidence);
        getWritableDatabase().insertOrThrow("brain_event", null, values);
        MonkeyFaceDataSink sink = MonkeyFaceBridge.sink();
        if (sink != null) {
            try { sink.onBrainEvent(event); sink.onBrainSnapshot(today()); }
            catch (RuntimeException ignored) { /* Monkey Face 异常不得中断本地识别与反馈链路。 */ }
        }
    }

    public synchronized BrainStatsSnapshot today() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0);
        return snapshot(calendar.getTimeInMillis(), System.currentTimeMillis() + 1L);
    }

    public synchronized BrainStatsSnapshot snapshot(long start, long end) {
        int recognition = 0, reminder = 0, low = 0, high = 0;
        Map<String, Integer> distribution = new LinkedHashMap<>();
        try (Cursor cursor = getReadableDatabase().query("brain_event",
                new String[]{"event_type","category","content_type"}, "timestamp_ms>=? AND timestamp_ms<?",
                new String[]{String.valueOf(start),String.valueOf(end)}, null,null,null)) {
            while (cursor.moveToNext()) {
                String event = cursor.getString(0), category = cursor.getString(1), type = cursor.getString(2);
                if (BrainStatsEvent.TYPE_RECOGNITION.equals(event)) {
                    recognition++;
                    if ("LOW_VALUE_IRRELEVANT".equals(category)) low++;
                    if ("HIGH_VALUE_MATCH".equals(category)) high++;
                    type = type == null || type.isEmpty() ? "未分类" : type;
                    distribution.put(type, distribution.containsKey(type) ? distribution.get(type) + 1 : 1);
                } else if (BrainStatsEvent.TYPE_REMINDER.equals(event)) reminder++;
            }
        }
        return new BrainStatsSnapshot(start, recognition, reminder, low, high, distribution);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
