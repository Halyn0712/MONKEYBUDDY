# MONKEYBUDDY

针对小红书、抖音等易上瘾社媒的自律辅助 App，单 APK 集成：

- **Monkey Face**：打开社媒前约定时长与理由，到点提醒，统计今日报告/趋势/历史/理由。
- **Monkey Brain**：悬浮猴子搭子，识别低质无关内容并毒舌提醒（屏幕采集 + VLM 管线）。

## 工程结构

- `app/` — 主应用（WebView 主页 + Face 无障碍监控）
- `monkeybrain/` — Brain 库模块（悬浮猴、屏幕采集、VLM、反馈）

## 构建

需要 JDK 17+ 与 Android SDK 35。在 Android Studio 打开本目录，或使用：

```powershell
cd D:\CodexData\MONKEYBUDDY-main
.\gradlew.bat :app:assembleDebug
```

输出 APK：`app/build/outputs/apk/debug/app-debug.apk`

## 使用说明

1. 在主页开启无障碍与悬浮窗权限，管理监控名单。
2. 在 **Monkey Brain 搭子** 卡片选择身份，开启搭子模式。
3. 点击 **授权屏幕识别** 完成系统录屏授权（悬浮猴子图标保持 Brain 原版）。
4. 打开名单内社媒时，Face 会先让你选时长；Brain 在刷屏过程中识别内容并提醒。

已移除「手动记一笔 · 我要刷一会儿」功能，统计仅来自自动监控记录。
