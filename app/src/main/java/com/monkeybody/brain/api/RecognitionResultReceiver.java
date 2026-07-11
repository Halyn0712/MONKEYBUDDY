package com.monkeybody.brain.api;

/** 内容识别结果入口，可由 Monkey Face 或识别模块调用。 */
public interface RecognitionResultReceiver {
    void onRecognitionResult(RecognitionResult result);
}
