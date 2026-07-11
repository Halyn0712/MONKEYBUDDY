# MonkeyBody / Monkey Brain

Android 10+ 原生悬浮猴子工程。已实现悬浮交互、端侧周期性屏幕采集、VLM 调度管线与对接契约。GGUF 权重和 llama.cpp Native 库需按 `assets/models/README.md` 与 `cpp/README.md` 配置。

> 产品逻辑、架构、隐私和验收边界见 [PRD.md](PRD.md)，全流程用例见 [TEST_PLAN.md](TEST_PLAN.md)。

## 运行

1. 使用 Android Studio 打开本目录，等待 Gradle 同步。
2. 连接 Android 10+ 设备并运行 `app`。
3. 按提示依次允许悬浮窗、使用情况访问和系统屏幕采集授权。
4. 目标社媒进入前台后自动按 2 秒/帧采集，离开后暂停。绿点表示采集中，橙点表示暂停，灰点表示停止。
5. 拖动猴子后松手会贴边；点击可展开/收起文案。

当前测试版首页提供“启动小红书搭子模式”按钮，仅在包名 `com.xingin.xhs` 位于前台时每 2.5 秒取一帧。相册测试和周期采集都兼容 HARDWARE Bitmap。低质内容显示毒舌锐评；高价值内容播放 `res/raw/monkey_call.m4a` 的真实猴叫，两类反馈互斥。

悬浮形象与 APP 图标使用用户提供的透明全身猴子 PNG。迷你态约 92×128dp，吸附时向屏幕外偏移 18dp，呈现手抓边缘、身体探出的“挂边”效果；锐评气泡在猴子旁边展开。

无需单独安装 Gradle。Android Studio 会使用工程配置的 Gradle/AGP；本机 SDK 路径由 `local.properties` 提供。首次同步需要联网下载缺失依赖。

## 发布构建前置条件

- Android Studio、Android SDK 35、JDK 17 或 21。
- 若启用真实 VLM：Android NDK/CMake、固定版本 llama.cpp、INT4 GGUF 与匹配 mmproj。
- Release APK 需要配置自己的签名。当前 `release` 构建没有内置任何生产密钥。

没有 Native 库/模型时，普通 Android 工程仍可编译和运行权限、悬浮、采集、统计及诊断页面，但 VLM 会明确显示 `MODEL_UNAVAILABLE`。

也可在“VLM 本地测试 → 云端 VLM 与 API Key 设置”中显式启用硅基流动 `Qwen/Qwen3-VL-32B-Thinking`。云端模式会上传预处理截图并产生 API 调用费用；Key 通过 Android Keystore 加密，绝不写入源码。公开过的 Key 必须先在平台撤销并换新。

## Monkey Face 对接

通过 `MonkeyBrainBridge` 获取 `ScreenCaptureController`，或注入 `RecognitionResultReceiver`、`FeedbackOutput`、`ScreenFrameConsumer`。屏幕捕获由 Activity 发起 MediaProjection 授权；本工程不会绕过系统授权。

`ScreenFrameConsumer.onFrame()` 收到裁剪、缩放后的 Bitmap，只能在回调期间使用，返回后会立即回收。采集模块没有存储与网络代码。

VLM 默认部署目标为 SmolVLM-500M INT4；可切换到 Qwen2.5-VL-3B INT4。点击主页“VLM 本地测试”可保存兴趣配置、选择截图并查看模型状态/分类耗时。模型或 Native 库缺失时会明确显示 `MODEL_UNAVAILABLE`，不会返回模拟结果。

识别结果由 `FeedbackCoordinator` 转换为本地模板文案。低价值内容触发抖动与警示音，高价值内容触发弹跳与双段提示音；反馈默认限频 8 秒，面板展示 3 秒自动收起。`FeedbackEnhancer` 仅为可选异步增强接口，默认没有联网实现。

## Monkey Face 数据对接

现有 Monkey Face 模块实现并注册 `MonkeyFaceDataSink`，即可接收 `BrainStatsEvent` 与 `BrainStatsSnapshot`。当前工作区未包含 Monkey Face 源码，因此没有猜测它的数据库或 Repository 类型；映射示例见 PRD 第 5 章。

## 工程目录

```text
app/src/main/
├─ assets/models/       GGUF 配置与权重放置说明
├─ cpp/                 llama.cpp JNI 接入说明
├─ java/.../api/        Face/Brain 稳定接口与数据模型
├─ java/.../capture/    MediaProjection、前台应用检测、帧预处理
├─ java/.../vlm/        去重、队列、JNI 门面、结果解析与 Demo
├─ java/.../feedback/   模板、频控、音效和反馈编排
├─ java/.../stats/      SQLite 统计与 Monkey Face 路由
├─ java/.../overlay/    全局悬浮猴子
└─ java/.../diagnostics/权限、模型、统计与后半链路诊断
```

## 厂商 ROM

标准入口为 `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`。MIUI、ColorOS、OriginOS、HarmonyOS 等系统还可能要求在“应用管理/权限管理”中开启悬浮窗，并允许前台服务后台运行；入口名称和位置随 ROM 版本变化，因此不硬编码非公开设置页面。
