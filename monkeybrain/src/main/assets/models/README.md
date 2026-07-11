# 模型放置说明

默认部署目标为 llama.cpp 官方支持的 `SmolVLM-500M-Instruct-GGUF` INT4 版本。

请将经许可获得的两个文件放在本目录并保持以下名称：

- `model-q4.gguf`：语言模型 INT4 GGUF
- `mmproj.gguf`：对应的多模态视觉 projector

模型权重体积较大且受各自许可证约束，本仓库不放置伪权重。也可切换到
`Qwen2.5-VL-3B-Instruct-GGUF`，但 3B 模型对内存和延迟要求显著更高。
