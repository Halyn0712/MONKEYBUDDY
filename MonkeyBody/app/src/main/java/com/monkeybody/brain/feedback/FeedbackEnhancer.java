package com.monkeybody.brain.feedback;

import com.monkeybody.brain.api.RecognitionResult;
import java.util.concurrent.CompletionStage;

/**
 * 可选联网增强边界。默认工程不提供网络实现；实现方必须显式获得用户同意并异步返回。
 */
public interface FeedbackEnhancer {
    CompletionStage<String> enhance(String localText, RecognitionResult result, String userContext);
}
