 package com.shuagoumei.app;
 
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Locale;
 import java.util.Map;
 import java.util.Random;
 
 /**
  * Monkey Brain: keyword-based content quality evaluator.
  * Given a user identity (focus area) and scraped screen text, determines
  * whether the user is consuming distracting low-quality content and picks
  * a roast message (≤10 characters).
  */
 final class ContentJudge {
 
     /** A preset identity the user can choose for Monkey Brain filtering. */
     static class Identity {
         final String key;
         final String label;
         final String emoji;
         /** Keywords that indicate content aligned with the user's goals. */
         final List<String> focusWords;
 
         Identity(String key, String label, String emoji, List<String> focusWords) {
             this.key = key;
             this.label = label;
             this.emoji = emoji;
             this.focusWords = Collections.unmodifiableList(focusWords);
         }
     }
 
     /** Result of a single content judgment. */
     static class Judgment {
         final boolean shouldRoast;      // true → disrupt the scroll
         final String roast;             // 10-char max roast line
         final String category;          // which distraction category triggered it
 
         static final Judgment PASS = new Judgment(false, null, null);
 
         Judgment(boolean shouldRoast, String roast, String category) {
             this.shouldRoast = shouldRoast;
             this.roast = roast;
             this.category = category;
         }
     }
 
     // ── Identity presets ──────────────────────────────────────────
 
     static final List<Identity> IDENTITIES = Collections.unmodifiableList(Arrays.asList(
         new Identity("kaoyan",   "考研党", "📚",
             Arrays.asList("考研", "英语", "政治", "数学", "专业课", "复习", "笔记",
                 "学习方法", "真题", "上岸", "知识点", "记忆", "背书", "错题",
                 "复试", "调剂", "院校", "专业", "备考", "进度", "计划")),
         new Identity("kaogong",  "考公党", "🏛️",
             Arrays.asList("考公", "行测", "申论", "面试", "事业单位", "公务员",
                 "真题", "时政", "常识", "资料分析", "数量关系", "体制",
                 "备考", "上岸", "选调", "国考", "省考")),
         new Identity("worker",   "工作党", "💼",
             Arrays.asList("职场", "效率", "工具", "行业", "技能", "复盘",
                 "管理", "项目", "产品", "运营", "代码", "设计", "写作",
                 "投资", "副业", "搞钱", "成长", "思维", "方法论")),
         new Identity("student",  "学生", "🎓",
             Arrays.asList("学习", "考试", "作业", "课程", "笔记", "读书",
                 "论文", "实验", "绩点", "四六级", "证书", "实习",
                 "时间管理", "方法", "规划")),
         new Identity("creator",  "创作者", "🎨",
             Arrays.asList("创作", "灵感", "选题", "拍摄", "剪辑", "文案",
                 "涨粉", "算法", "运营", "变现", "接广", "内容",
                 "审美", "风格", "工具", "教程")),
         new Identity("health",   "健康管理", "🏃",
             Arrays.asList("健身", "跑步", "饮食", "减脂", "增肌", "睡眠",
                 "冥想", "拉伸", "食谱", "卡路里", "瑜伽", "体检",
                 "养生", "中医", "心理", "情绪")),
         new Identity("custom",   "自定义", "✏️", Collections.<String>emptyList())
     ));
 
     // ── Distraction categories + keywords ────────────────────────
 
     private static class DistractionCat {
         final String key;
         final String label;
         /** Every keyword in the category, lowercased. */
         final List<String> words;
         /** Roast variants, each ≤10 characters. */
         final List<String> roasts;
 
         DistractionCat(String key, String label, List<String> words, List<String> roasts) {
             this.key = key;
             this.label = label;
             // Store lowercased for fast matching.
             List<String> lower = new ArrayList<>(words.size());
             for (String w : words) lower.add(w.toLowerCase(Locale.ROOT));
             this.words = Collections.unmodifiableList(lower);
             this.roasts = Collections.unmodifiableList(roasts);
         }
     }
 
     private static final List<DistractionCat> DISTRACTIONS = Collections.unmodifiableList(Arrays.asList(
         new DistractionCat("gossip", "吃瓜八卦",
             Arrays.asList("八卦", "吃瓜", "塌房", "翻车", "吵架", "撕逼",
                 "曝光", "恋情", "分手", "出轨", "离婚", "黑料",
                 "爆料", "热搜", "震惊", "居然", "没想到", "不敢相信"),
             Arrays.asList("跟你有关吗", "别吃瓜了", "关你啥事", "看人脸红吗")),
         new DistractionCat("entertainment", "追剧综艺",
             Arrays.asList("追剧", "综艺", "电视剧", "网剧", "剧情", "大结局",
                 "花絮", "路透", "名场面", "笑死", "搞笑", "段子",
                 "整活", "搞笑视频", "哈哈哈哈", "xswl"),
             Arrays.asList("笑完该学了", "别笑了干活", "剧能当饭吃", "看完能加分")),
         new DistractionCat("shopping", "种草购物",
             Arrays.asList("种草", "好物", "测评", "开箱", "必买", "入手",
                 "平替", "性价比", "推荐", "穿搭", "OOTD", "美妆",
                 "护肤品", "划算", "剁手", "618", "双十一"),
             Arrays.asList("钱够花吗", "买得起吗你", "看了也买不起", "省省吧")),
         new DistractionCat("drama", "家长里短",
             Arrays.asList("婆婆", "老公", "男朋友", "女朋友", "相亲", "彩礼",
                 "闺蜜", "原生家庭", "重男轻女", "催婚", "丁克",
                 "月子", "带娃", "二胎", "三胎"),
             Arrays.asList("先管好自己", "想太远了", "跟你无关", "过好你日子")),
         new DistractionCat("doomscroll", "无目的刷",
             Arrays.asList("又刷到了", "推给我干嘛", "大数据", "猜你喜欢",
                 "太真实了", "世另我", "同款", "谁懂啊", "dddd"),
             Arrays.asList("找什么呢", "手不累吗", "还刷？", "够了够了", "醒醒")),
         new DistractionCat("meme", "表情包段子",
             Arrays.asList("表情包", "斗图", "梗图", "meme", "神评论",
                 "评论区", "太有才了", "笑不活了", "绷不住", "破防",
                 "绝了", "抽象", "典", "蚌", "乐"),
             Arrays.asList("收藏≠学完", "图能当饭吃", "别存了没用")),
         new DistractionCat("fan", "明星饭圈",
             Arrays.asList("爱豆", "打榜", "应援", "周边", "演唱会", "签售",
                 "粉丝", "站姐", "神颜", "麦外敷", "哥哥", "姐姐",
                 "老公", "老婆", "本命", "墙头", "爬墙", "塌房"),
             Arrays.asList("他又不认得你", "追星能涨薪", "花那钱干嘛"))
     ));
 
     /** Minimum cooldown between roasts, in milliseconds. */
     static final long ROAST_COOLDOWN_MS = 45 * 1000L;
 
     private static final Random RNG = new Random();
 
     // ── Public API ───────────────────────────────────────────────
 
     /**
      * @param screenText concatenated visible text from the screen
      * @param identityKey the user's chosen identity key (or null for "custom")
      * @return a Judgment — roast or pass
      */
     static Judgment judge(String screenText, String identityKey) {
         if (screenText == null || screenText.isEmpty()) return Judgment.PASS;
         Identity id = findIdentity(identityKey);
         String lower = screenText.toLowerCase(Locale.ROOT);
 
         // 1. If the screen shows enough "focus" content, let it pass.
         if (id.focusWords.size() > 0 && focusRatio(lower, id.focusWords) >= 0.08f) {
             return Judgment.PASS;
         }
 
         // 2. Find the strongest distraction signal.
         DistractionCat best = null;
         int bestHits = 2; // require at least 2 keyword hits to trigger
         for (DistractionCat cat : DISTRACTIONS) {
             int hits = countHits(lower, cat.words);
             if (hits > bestHits) {
                 bestHits = hits;
                 best = cat;
             }
         }
 
         if (best != null) {
             return new Judgment(true, pickRoast(best), best.key);
         }
         return Judgment.PASS;
     }
 
     static Identity findIdentity(String key) {
         for (Identity id : IDENTITIES) {
             if (id.key.equals(key)) return id;
         }
         return IDENTITIES.get(IDENTITIES.size() - 1); // "custom"
     }
 
     static Map<String, Identity> identityMap() {
         Map<String, Identity> m = new LinkedHashMap<>();
         for (Identity id : IDENTITIES) m.put(id.key, id);
         return m;
     }
 
     // ── Internals ────────────────────────────────────────────────
 
     /** Fraction of characters that fall within focus keywords (rough proxy). */
     private static float focusRatio(String lower, List<String> focusWords) {
         int chars = 0;
         for (String w : focusWords) {
             int idx = 0;
             while ((idx = lower.indexOf(w, idx)) >= 0) {
                 chars += w.length();
                 idx += w.length();
             }
         }
         return lower.length() > 0 ? (float) chars / lower.length() : 0f;
     }
 
     private static int countHits(String lower, List<String> words) {
         int hits = 0;
         for (String w : words) {
             if (lower.contains(w)) hits++;
         }
         return hits;
     }
 
     private static String pickRoast(DistractionCat cat) {
         return cat.roasts.get(RNG.nextInt(cat.roasts.size()));
     }
 
     private ContentJudge() {}
 }
