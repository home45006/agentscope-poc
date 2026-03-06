# 运行手册

_最后更新：2026-03-06_

## 运行方式

### Demo 模式

预设 5 条测试问题，自动依次执行，验证四个能力域：

```bash
mvn exec:java
```

预设测试问题：
1. `把空调调到22度` → 车控域
2. `再低2度` → 车控域（多轮追问）
3. `播放一首周杰伦的歌` → 音乐域
4. `同时打开车窗，并且导航去重庆解放碑` → 跨域
5. `长安CS75多久保养一次` → 问答域

### 交互模式

命令行实时对话，输入 `quit` 或 `exit` 退出：

```bash
mvn exec:java -Dexec.args="--interactive"
```

交互模式会在 AI 回复后显示结构化指令面板：

```
小安: 好的，已为您将空调调至22度。

┌─────────── 下发指令 ──────────┐
│ [1] {
│     "domain": "vehicle",
│     "action": "control_ac",
│     "params": { "action": "set_temperature", "temperature": 22 },
│     "tts": "好的小安已将空调调至22度"
│   }
└───────────────────────────────────┘
```

---

## 环境变量 / 配置

| 配置项 | 来源 | 必填 | 说明 |
|--------|------|------|------|
| `dashscope.api.key` | `config.properties` | 二选一 | DashScope API Key |
| `DASHSCOPE_API_KEY` | 环境变量 | 二选一 | 同上，优先级低于配置文件 |

**加载优先级：** `config.properties` > 环境变量

**默认模型：** `qwen-plus`（在 `ModelConfig.defaultModel()` 中配置）

---

## 会话文件

专家 Agent 会话持久化到 `./sessions/` 目录（程序运行后自动创建）：

```
sessions/
└── <sessionId>/
    └── <sessionId>.json    # JSON 格式对话历史
```

清理会话（重置上下文）：

```bash
rm -rf ./sessions/
```

---

## 常见问题

### 启动失败：请配置 DASHSCOPE_API_KEY

**原因：** 未配置 API Key。

**解决：**
```bash
cp config.properties.example config.properties
# 编辑 config.properties，填入 dashscope.api.key
```

---

### 构建失败：Could not find artifact io.agentscope:agentscope

**原因：** Maven 使用了阿里云镜像，该镜像无 agentscope 包。

**解决：**
1. 检查 `~/.m2/settings.xml` 是否配置了 `<mirror>`
2. 临时移除 mirror 配置，或注释掉
3. 确保网络可访问 `repo1.maven.org`

---

### 运行超时 / 无响应

**原因：** DashScope API 网络问题或 Key 无效。

**排查：**
1. 验证 API Key 有效（在 DashScope 控制台检查）
2. 检查网络代理设置
3. 查看控制台是否有异常栈

---

### 专家 Agent 总是调用错误的工具

**原因：** RouterAgent 路由 Prompt 或专家 Agent Tool description 不够清晰。

**排查：**
1. 检查 `RouterAgentFactory.SYS_PROMPT` 中的路由规则
2. 检查对应 Tool 的 `@Tool(description)` 是否准确描述了使用场景
3. 检查 Skill 文件（`SKILL.md`）的职责描述

---

### 指令 JSON 未在交互界面显示

**原因：** 专家 Agent 未返回符合格式的 JSON，或 `[[JSON_START]]...[[JSON_END]]` 标记缺失。

**排查：**
1. 检查专家 Agent Tool 返回值是否为合法 JSON 字符串
2. 检查 RouterAgent SYS_PROMPT 是否包含 `[[JSON_START]]` 格式要求
3. `CommandCaptureHook` 仅捕获含 `action`/`domain`/`tts` 字段之一的 JSON

---

## 回滚 / 降级

### 切换 LLM 模型

在 `ModelConfig.defaultModel()` 中修改 `modelName`：

```java
// 可选模型：qwen-plus / qwen-turbo / qwen-max
DashScopeChatModel.builder()
    .apiKey(loadApiKey())
    .modelName("qwen-turbo")   // 改为更快/更便宜的模型
    .build();
```

### 禁用 CommandCaptureHook

在 `RouterAgentFactory.build()` 中移除 Hook 注册：

```java
// 注释或删除以下行
.hooks(List.of(new CommandCaptureHook()))
```

### 调整 AutoContextMemory 阈值

在 `RouterAgentFactory.build()` 中修改参数：

```java
AutoContextConfig.builder()
    .msgThreshold(50)   // 触发压缩的消息数（默认 30）
    .lastKeep(15)       // 保留最近 N 条（默认 10）
    .tokenRatio(0.3)    // token 压缩比
    .build()
```
