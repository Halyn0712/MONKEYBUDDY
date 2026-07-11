package com.monkeybody.brain.feedback;

import com.monkeybody.brain.api.RecognitionResult;
import java.util.*;

/** 纯内存规则模板引擎，记录近期模板编号以降低连续重复。 */
public final class TemplateTextGenerator {
    private static final String[] LOW = {
            "猴哥掐指一算：这条和「{interest}」八竿子打不着，手指可以下班了。",
            "信息密度 {density} 分，水分倒有九十九。别刷了，去看看真正的「{interest}」。",
            "这篇「{type}」像数字棉花糖：看着大，嚼完啥也没有。撤！",
            "你关注的是「{interest}」，不是给算法当免费饲养员。下一条也未必更香。",
            "主题「{topic}」已被猴脑鉴定为注意力小偷，建议当场抓获并退出。",
            "又一块碎片化香蕉皮。绕过去，别让它滑走你的时间。"
    };
    private static final String[] HIGH = {
            "这条有料！「{topic}」正中你的「{interest}」雷达，猴哥批准认真看。",
            "信息密度 {density} 分，不是电子零食，是知识香蕉。慢慢嚼！",
            "发现一条匹配的「{type}」，这次算法终于听懂你了。",
            "「{topic}」有新东西，值得收藏，但看完记得回来，别顺手漂流。",
            "猴哥点头：这条和「{interest}」对得上，今天不是白刷。",
            "高价值内容已捕获！允许多停留一会儿，注意力花得值。"
    };
    private final Random random = new Random();
    private int lastLow = -1, lastHigh = -1;

    public synchronized String generate(RecognitionResult result, String userContext) {
        boolean high = "HIGH_VALUE_MATCH".equals(result.category);
        String[] templates = high ? HIGH : LOW;
        int previous = high ? lastHigh : lastLow;
        int selected;
        do { selected = random.nextInt(templates.length); } while (templates.length > 1 && selected == previous);
        if (high) lastHigh = selected; else lastLow = selected;
        String interest = clean(userContext, "你的兴趣");
        return templates[selected]
                .replace("{interest}", interest)
                .replace("{topic}", clean(result.topic, interest))
                .replace("{type}", clean(result.contentType, "内容"))
                .replace("{density}", String.valueOf(result.informationDensity));
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        String trimmed = value.trim();
        return trimmed.length() > 24 ? trimmed.substring(0, 24) : trimmed;
    }
}
