# Monkey Face 接入约定

`MonkeyBrainBridge` 是当前进程内的最小对接门面：

- `ScreenCaptureController`：由 `ScreenCaptureService` 实现启停、采样周期和目标包名配置。MediaProjection 授权由可见 Activity 发起。
- `ScreenFrameConsumer`：接收裁剪缩放后的内存 Bitmap；回调返回后 Bitmap 立即回收。
- `RecognitionResultReceiver`：由业务层实现，用于接收识别类别、置信度与时间戳。
- `FeedbackOutput`：悬浮服务已实现文案展示；音效由拥有资源的 Monkey Face 模块实现或扩展。

同一 APK/进程集成时，在自定义 `Application.onCreate()` 中调用 `MonkeyBrainBridge.register(...)`。若 Monkey Face 是另一个 APK，静态门面不能跨进程，应在保持三个接口语义不变的前提下增加显式 Binder/AIDL 适配层。

## VLM 管线

`VlmCoordinator` 已注册为 `ScreenFrameConsumer`，负责 dHash 去重、容量 1 的最新帧队列、严格 JSON 解析和 `RecognitionResultReceiver` 输出。兴趣配置存于应用私有 SharedPreferences。Native 库或 GGUF 缺失时状态为 `MODEL_UNAVAILABLE`，不会生成模拟分类。

`HybridVlmEngine` 默认选择本地 `NativeVlmEngine`。用户在独立页面完成截图上传知情同意并配置 Keystore 加密 API Key 后，才切换到 `SiliconFlowVlmEngine`，模型固定为 `Qwen/Qwen3-VL-32B-Thinking`。Key 不得由宿主硬编码或提交到版本库。

## 反馈闭环

`FeedbackCoordinator` 在 Application 中注册为识别结果接收器，完成模板变量替换、8 秒限频、音效与动画选择，再调用悬浮服务的 `FeedbackOutput`。面板 3 秒后自动收起。联网文案增强仅保留 `FeedbackEnhancer` 接口，默认构建不包含任何网络实现。
