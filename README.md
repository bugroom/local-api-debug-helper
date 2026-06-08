# 本地 API 调试助手

> 代码全由 AI 修改生成。

本地 API 调试助手是一个 Android 测试工具，用于验证 OpenAI 兼容本地 API 服务是否可用。它可以帮助开发者在手机端直接测试健康检查、模型列表、非流式聊天补全、SSE 流式聊天补全和请求日志，适合作为本地 API 服务开发、联调和排障工具。

目标服务项目是 [google-ai-edge-gallery-local-api](https://github.com/bugroom/google-ai-edge-gallery-local-api)。

这个仓库只维护 API 调试助手本身，适合独立构建、独立发布 APK、独立记录问题。

## 目录

- [功能](#功能)
- [目标服务](#目标服务)
- [快速使用](#快速使用)
- [构建说明](#构建说明)
- [GitHub Actions](#github-actions)
- [项目结构](#项目结构)
- [故障排查](#故障排查)
- [License](#license)

## 功能

- 配置 API 服务地址、端口和 API Key。
- 调用 `GET /health` 检查服务状态。
- 调用 `GET /v1/models` 获取模型列表。
- 调用 `POST /v1/chat/completions` 发送聊天补全请求。
- 支持非流式响应。
- 支持 SSE 流式响应和 `data: [DONE]` 结束事件。
- 提供请求历史记录。
- 提供应用内日志页面，支持复制日志，便于排查问题。

## 目标服务

推荐搭配以下服务端项目使用：

```text
https://github.com/bugroom/google-ai-edge-gallery-local-api
```

目标服务支持的主要端点：

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/health` | 健康检查 |
| GET | `/v1/models` | 模型列表 |
| POST | `/v1/chat/completions` | 聊天补全 |

## 快速使用

1. 在 Google AI Edge Gallery 本地 API 版中下载 LLM 模型。
2. 从 Gallery 侧栏进入 `API Server`。
3. 设置默认模型、采样参数和推理后端。
4. 本机测试可使用 `127.0.0.1`，局域网测试将 Gallery Host 设置为 `0.0.0.0`。
5. 按需启用 API Key。
6. 启动 API Server。
7. 打开 Local API Debug Helper。
8. 输入 Host、Port 和 API Key。
9. 依次测试健康检查、模型列表和聊天补全。

局域网测试时，调试助手中填写的是运行 Gallery 服务端设备的局域网 IP。

## 构建说明

### 环境要求

- Android SDK 34
- JDK 17 或更新版本
- Gradle wrapper 已包含在仓库中

### 本地构建

```bash
# Build release APK
./gradlew :app:assembleRelease
```

Release APK 输出路径：

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

Debug APK 构建：

```bash
# Build debug APK
./gradlew :app:assembleDebug
```

Debug APK 输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

工作流文件：

```text
.github/workflows/build_android.yaml
```

当前工作流会在以下场景触发：

- 手动运行 `workflow_dispatch`。
- 推送到 `main` 且修改 Android 项目文件。
- 针对 `main` 的 Pull Request 且修改 Android 项目文件。

构建命令：

```bash
./gradlew :app:assembleRelease
```

上传 artifact：

```text
api-debug-helper-release-unsigned
```

artifact 对应文件：

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

下载方式：

1. 打开 GitHub 仓库的 `Actions` 页面。
2. 进入一次 `Build API Debug Helper APK` workflow run。
3. 在 `Artifacts` 区域下载 `api-debug-helper-release-unsigned`。

## 项目结构

```text
app/src/main/java/com/api/debug/helper/
```

| 目录 | 说明 |
|------|------|
| `api/` | OkHttp API 调用封装 |
| `data/` | 请求、响应、配置和历史记录模型 |
| `ui/` | Compose UI 和 ViewModel |
| `ui/theme/` | Material3 主题配置 |
| `util/` | 应用内日志工具 |

## 故障排查

### 连接失败

- 确认 Gallery API Server 已启动。
- 本机测试使用 `127.0.0.1`。
- 局域网测试使用 Gallery 设备的局域网 IP。
- 局域网测试时 Gallery Host 应设置为 `0.0.0.0`。
- 启用 API Key 时，确认调试助手中填写了相同 Key。

### 模型列表为空

- 确认 Gallery 中已下载 LLM 模型。
- 重新点击模型列表加载按钮。
- 检查 Gallery 端日志中的 `LOCAL_API event=models_list`。

### 聊天请求耗时较长

- 首次请求可能触发模型初始化。
- 大模型在移动设备上推理耗时更长。
- 可尝试降低 `max_tokens` 或切换推理后端。

### 流式响应没有结束

- 确认服务端发送 `data: [DONE]`。
- 查看调试助手日志页中的 `Chunks` 和 `ParseErrors`。
- 确认服务端没有在推理过程中被系统回收。

## License

Apache License 2.0
