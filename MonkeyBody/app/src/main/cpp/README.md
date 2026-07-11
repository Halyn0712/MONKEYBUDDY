# llama.cpp JNI 接入状态

本目录预留 `monkey_vlm` JNI 库入口。当前工作机未安装 Android NDK，且工作区没有
llama.cpp 源码，因此没有提交会伪装成功的 stub `.so`。

接入真实后端时应固定 llama.cpp commit，并使用其 `libmtmd`：

1. 将官方 llama.cpp 放到 `third_party/llama.cpp`。
2. 用 Android NDK/CMake 构建 `llama`、`ggml`、`mtmd` 和 JNI wrapper，仅启用 arm64-v8a。
3. JNI 必须实现 `NativeVlmEngine` 的 `nativeCreate/nativeInfer/nativeDestroy`。
4. `nativeInfer` 从 Android Bitmap 读取 RGBA，交给 mtmd 图像输入，再执行严格 JSON 提示词。
5. 固定模型、mmproj 与 llama.cpp 版本组合并做真机基准测试，不能跨版本混用 projector。
