# 对接接口可能由 Monkey Face 或后续动态模块通过反射加载，保留公开契约。
-keep public interface com.monkeybody.brain.api.** { *; }
-keep public class com.monkeybody.brain.api.** { *; }
