# MonkeyBody 产品需求文档（Monkey Face + Monkey Brain）

版本：1.0  
适用平台：Android 10 及以上  
文档状态：工程联调基线

## 1. 项目背景与产品定位

社交媒体的无限信息流容易造成无意识刷取。MonkeyBody 以“猴子搭子”为人格化交互载体，在不上传屏幕内容的前提下，帮助用户辨别当前内容是否值得继续投入注意力。

产品由两部分组成：

- **Monkey Face**：已有的使用时长预设、计时提醒和使用数据统计模块，承担目标设定、时间管理与数据看板。
- **Monkey Brain**：本工程实现的屏幕画面采集、端侧内容识别、价值判断和趣味反馈模块。

产品不是内容审查工具，不替用户决定观点是否正确；它依据用户主动配置的兴趣以及内容信息密度，给出注意力管理建议。默认本地模式不上传截图；用户也可在单独知情同意后启用硅基流动云端 VLM，此时截图会离开设备。

## 2. 核心功能总览

### 2.1 Monkey Face

- 设置目标社媒的可用时长与提醒节点。
- 记录每日使用时长、启动次数和目标完成情况。
- 展示时间趋势以及 Monkey Brain 提供的识别次数、提醒次数、高低价值比例和内容类型分布。
- 通过 `MonkeyFaceDataSink` 接收 Monkey Brain 原始事件和当日聚合快照。

> 当前工作区未包含 Monkey Face 源码。以上能力来自项目背景；真实字段映射需由 Monkey Face 工程实现 `MonkeyFaceDataSink` 后完成。

### 2.2 Monkey Brain

- 全局悬浮猴子：拖拽、贴边、迷你/展开状态、采集状态提示。
- 权限引导：悬浮窗、使用情况访问、MediaProjection 和通知权限。
- 周期采集：默认 2 秒/帧，仅在目标社媒前台时运行。
- 端侧 VLM：兴趣相关性、主题、内容类型和信息密度判断。
- 轻量反馈：规则模板、音效、动画、3 秒自动收起和 8 秒限频。
- 本地统计：识别事件、实际提醒事件及内容类型分布。

## 3. Monkey Brain 详细产品逻辑

### 3.1 首次配置流程

1. 用户进入首页，阅读悬浮窗用途和隐私说明。
2. 用户允许“显示在其他应用上层”。拒绝时停留在引导页，不启动服务。
3. 用户允许“使用情况访问”，仅用于判断当前前台包名。
4. 用户在 VLM 本地测试页填写关注领域和兴趣标签。配置写入应用私有 SharedPreferences。
5. 用户确认系统 MediaProjection 授权。授权令牌只在本次采集会话使用。
6. 悬浮猴子出现，采集服务进入等待状态；目标社媒进入前台后开始采样。

### 3.2 内容识别规则

输入为已裁掉系统栏并将最长边缩放至 768 px 的 Bitmap。提示词要求模型输出严格 JSON：

- `category`：`LOW_VALUE_IRRELEVANT` 或 `HIGH_VALUE_MATCH`
- `topic`：简短主题
- `contentType`：文章、短视频、广告、知识分享等
- `informationDensity`：0–100
- `confidence`：0–1

判断口径：

- **低质无关内容**：明显偏离用户兴趣，或信息密度低、重复、诱导停留、碎片化且无新增信息。
- **高价值匹配内容**：与用户关注领域相关，具有事实、方法、观点或知识增量。

模型输出不满足 JSON 协议、类别未知或字段越界时，该帧失败，不产生提醒和统计分类。

### 3.3 去重与推理队列

- 每帧计算 64 位 dHash；与上一推理帧汉明距离不超过 5 时跳过。
- 队列容量为 1。推理繁忙时，新帧替换尚未处理的旧帧，旧 Bitmap 立即回收。
- 识别帧只在内存传递；消费者返回后立即回收。
- Native 库或模型缺失时返回 `MODEL_UNAVAILABLE`，禁止生成模拟分类。

### 3.4 反馈触发机制

- 每个有效识别结果都计入识别次数。
- 默认最短提醒间隔为 8 秒；被频控抑制的结果不计入提醒次数。
- 低质内容：毒舌模板、抖动动画、按内容类型区分的警示音。
- 高价值内容：正向模板、弹跳动画、双段提示音。
- 模板变量包括兴趣、主题、内容类型和信息密度；连续两次不使用同一模板。
- 面板显示 3 秒后自动收起，不抢占输入焦点。
- `FeedbackEnhancer` 为可选异步增强接口；默认构建不包含网络实现。

## 4. 技术架构设计

数据流：

`MediaProjection → ImageReader → 预处理 → dHash/队列 → llama.cpp/libmtmd → RecognitionResult → 统计路由 → 模板反馈 → 悬浮窗`

### 4.1 悬浮交互控件

- `MonkeyOverlayService` 使用 `TYPE_APPLICATION_OVERLAY`。
- 前台服务类型为 `specialUse`，窗口设置 `FLAG_NOT_FOCUSABLE`。
- SharedPreferences 保存位置；拖动结束后按屏幕中线贴边。
- 绿色圆点表示采集中，橙色表示暂停，灰色表示停止。

### 4.2 本地计时与统计

- Monkey Face 负责原有计时、时长目标和主数据看板。
- Monkey Brain 使用 SQLite `brain_event` 表保存识别与提醒事件。
- 不保存截图、识别原文或用户兴趣；仅保存类别、主题短标签、内容类型、置信度和时间戳。
- `BrainStatsSnapshot` 提供日识别数、提醒数、高低价值数和类型分布。
- Monkey Face 实现并注册 `MonkeyFaceDataSink` 后可同步事件与快照；其异常不会阻断 Brain 链路。

### 4.3 屏幕画面采集

- `ScreenCaptureService` 使用 `mediaProjection` 前台服务类型。
- `ImageReader` 获取 RGBA 帧，默认 2 秒采样一次，配置下限 250 ms。
- UsageStatsManager 每秒判断前台应用；离开目标应用自动暂停。
- 旋转时调用 `VirtualDisplay.resize/setSurface`，不重复使用一次性授权令牌。
- Projection 被系统回收后停止服务并广播停止状态。

### 4.4 端侧 VLM 推理

- 默认目标：SmolVLM-500M-Instruct GGUF INT4；可切换 Qwen2.5-VL-3B INT4。
- llama.cpp `libmtmd` 负责视觉输入，JNI 暴露 create/infer/destroy。
- GGUF 与匹配的 mmproj 放入 assets，首次启动复制到 noBackupFilesDir。
- 当前源码已提供 Java/JNI 契约；发布构建必须固定 llama.cpp commit、加入 NDK 编译产物和真实权重。

### 4.4.1 可选云端 VLM

- 模型：`Qwen/Qwen3-VL-32B-Thinking`。
- 接口：`POST https://api.siliconflow.cn/v1/chat/completions`。
- 用户必须在独立设置页勾选截图上传知情同意，云端模式才可启用。
- API Key 使用 Android Keystore AES-GCM 加密，不进入源码、日志或统计数据库。
- 上传内容为预处理后的 JPEG 截图与分类提示词；不保存服务端 reasoning_content，只解析最终 JSON content。
- 网络失败、401、429、503 等错误不生成分类结果，不自动降级为伪结果。
- Base64 data URL 视觉输入需在发布前用新 Key 做兼容性实测。

### 4.5 文案生成

- `TemplateTextGenerator` 为同步纯内存规则引擎。
- `FeedbackCoordinator` 负责频控、音效和动画策略。
- ToneGenerator 合成短提示音，不增加音频素材体积。
- 联网增强必须由上层显式实现、征得同意且不得上传截图。

## 5. Monkey Face 数据对接规范

Monkey Face 在其 Application 初始化代码中注册：

```java
MonkeyFaceBridge.register(new MonkeyFaceDataSink() {
    @Override public void onBrainEvent(BrainStatsEvent event) {
        // 映射到已有统计事件表或 Repository。
    }

    @Override public void onBrainSnapshot(BrainStatsSnapshot snapshot) {
        // 更新数据看板：识别数、提醒数、高低价值比例、内容类型分布。
    }
});
```

要求：回调在后台线程发生，不执行长任务；如需更新 UI，应切换到主线程。若 Monkey Face 与 Brain 位于不同 APK/进程，需以相同字段定义增加 Binder/AIDL 或 ContentProvider 适配，不能直接使用静态 Bridge。

## 6. 交互设计规范

- 悬浮球默认位置：屏幕左侧、顶部下方约 120 dp。
- 迷你尺寸：64 dp；触摸移动超过 6 dp 判定为拖拽，否则为点击。
- 展开文案最多两行，宽度约 220 dp；展示 3 秒。
- 动画时长：展开 180 ms、贴边 220 ms、抖动 380 ms、弹跳 520 ms。
- 悬浮窗不获取输入焦点，不阻挡第三方应用操作。
- 通知常驻说明采集仅用于本地分析。

## 7. 边界场景处理

| 场景 | 处理策略 |
|---|---|
| 拒绝悬浮窗权限 | 不启动悬浮服务，保留引导入口 |
| 拒绝使用情况权限 | 不启动采集，跳转系统设置并解释用途 |
| 拒绝 MediaProjection | 提示授权失败，不创建 VirtualDisplay |
| Android 13+ 拒绝通知 | 功能可能受系统前台服务展示策略影响，继续保留设置入口 |
| 用户切换到非目标应用 | 一秒内暂停帧消费，悬浮状态变橙 |
| 屏幕旋转 | resize VirtualDisplay 并替换 ImageReader Surface |
| Projection 被回收 | 释放 ImageReader/VirtualDisplay，状态变灰，需用户重新授权 |
| 系统杀死采集服务 | `START_NOT_STICKY`，不复用失效令牌，回到应用重新授权 |
| 推理慢于采样 | 只保留最新一帧，释放旧帧 |
| 连续相同画面 | dHash 去重，不重复推理 |
| VLM 输出非法 | 丢弃结果并报告诊断状态，不触发提醒 |
| 模型/Native 缺失 | 显示 `MODEL_UNAVAILABLE`，不伪造结果 |
| Monkey Face 回调异常 | 捕获异常，本地统计与反馈继续运行 |
| 悬浮窗被关闭 | 识别仍可统计，但反馈不计为已提醒 |

## 8. 性能指标

- 屏幕采样默认频率：0.5 FPS，可动态调整。
- 预处理输出：最长边 768 px，系统栏区域裁剪。
- 推理队列：最多 1 个待处理帧。
- Java 侧单帧额外常驻 Bitmap：不超过 1 个待处理副本。
- 文案生成：目标小于 10 ms。
- 反馈展示：识别结果到悬浮窗调用目标小于 100 ms（不含 VLM 推理）。
- VLM 真机验收：需分别记录 P50/P95 推理耗时、峰值 RSS、温升和 10 分钟耗电；未实测前不得承诺具体毫秒值。
- 稳定性：连续运行 30 分钟无 OOM、ANR、截图文件残留。

## 9. 隐私合规

- MediaProjection 必须由用户通过系统对话框主动授权。
- 截图只在内存中流转，不写文件、不进入 SQLite、不上传云端。
- 默认云端开关关闭。工程声明 INTERNET 权限仅供用户主动启用的 SiliconFlow 后端使用。
- 本地保存的信息仅用于数据看板，可随应用数据一并删除。
- 通知和权限页明确说明采集目的、运行状态和关闭方式。
- 云端 VLM 会上传截图，启用前必须单独告知并征得用户同意；可选文案增强不得额外接收截图。

## 10. 验收标准与已知交付边界

### 软件验收

- Java 源码通过 Android 35 API 编译检查。
- XML/JSON 资源可解析。
- 诊断页可注入高低价值结果，验证统计、模板、限频、声音、动画和自动收起。
- Monkey Face sink 可接收事件与聚合快照。

### 发布前必须补齐

- 将真实 Monkey Face 源码或接口实现接入 `MonkeyFaceDataSink`。
- 安装 Android NDK/CMake并固定 llama.cpp 版本。
- 放入合法的 INT4 GGUF 和匹配 mmproj，编译 `libmonkey_vlm.so`。
- 在至少一台 Android 10 中端机和一台 Android 14/15 设备完成测试矩阵。
- 配置正式 release 签名后生成可安装 APK。

当前缺少 Monkey Face 源码、NDK/llama.cpp Native 库和 GGUF 权重，因此不能把现状描述为“完整真实 VLM APK 已验收”。
