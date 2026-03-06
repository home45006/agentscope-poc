# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 语言规范
- 所有对话和文档都使用中文
- 文档使用 markdown 格式

## 项目概述

**小安** —— 基于 AgentScope Java SDK（`io.agentscope:agentscope:1.0.9`）的长安汽车车载智能助手 POC。使用 DashScope（通义千问 qwen-plus）作为 LLM 后端，采用多 Agent 路由架构处理车控、音乐、导航、问答四类场景。

## 构建与运行

```bash
# 编译
mvn compile

# 运行测试
mvn test

# 运行单个测试类
mvn test -Dtest=VehicleToolsTest

# Demo 模式运行（预设问题）
mvn exec:java

# 交互模式运行（命令行对话）
mvn exec:java -Dexec.args="--interactive"
```

### 配置 API Key

运行前必须配置 DashScope API Key（二选一）：
1. 复制 `config.properties.example` 为 `config.properties`，填入 `dashscope.api.key`
2. 或设置环境变量 `DASHSCOPE_API_KEY`

## 代码架构

### 多 Agent 路由架构

```
用户输入
  └→ XiaoAnApplication（入口）
       └→ RouterAgentFactory.build() → 小安 RouterAgent（ReActAgent）
            ├→ call_vehicle_agent → VehicleAgent（车控）
            ├→ call_music_agent   → MusicAgent（音乐）
            ├→ call_nav_agent     → NavAgent（导航）
            └→ call_qa_agent      → QAAgent（问答）
```

- **RouterAgent**：顶层路由，根据意图调用对应专家 Agent；使用 `AutoContextMemory` 管理多轮对话（30条消息阈值，保留最近10条）
- **专家 Agent**：各自绑定领域 Tools（`VehicleTools`/`MusicTools`/`NavTools`/`QATools`）和对应 Skill
- **SubAgentConfig + JsonSession**：专家 Agent 会话持久化到 `./sessions/` 目录
- **CommandCaptureHook**：挂载在 RouterAgent 上，通过 `PostActingEvent` 捕获工具调用返回的 JSON 指令，用于调试展示

### Skill 系统

Skill 定义在 `src/main/resources/skills/` 下，每个 Skill 一个目录：
- `SKILL.md`：Skill 名称、描述、职责说明（frontmatter 中的 `name` 字段对应 `skillRepo.getSkill("xxx")` 的参数）
- `references/`：辅助参考文档（命令规范、参数范围等）

当前 Skill：`vehicle_control`、`music_playback`、`navigation_planning`、`changan_knowledge`、`general_qa`

### 结构化指令输出

专家 Agent 输出 JSON 指令，RouterAgent 将其以 `[[JSON_START]]...[[JSON_END]]` 格式附加在自然语言回复后。`XiaoAnApplication` 解析并在交互界面中分离展示。

### 关键包路径
- `io.agentscope.poc.agent` — 各专家 Agent 工厂类
- `io.agentscope.poc.tool` — AgentScope Tool 实现（工具调用逻辑）
- `io.agentscope.poc.model` — JSON 指令数据模型（`CarCommand`、`MusicCommand` 等）
- `io.agentscope.poc.hook` — Hook 实现（`CommandCaptureHook`）
- `io.agentscope.poc.router` — RouterAgent 工厂
- `io.agentscope.poc.config` — 配置加载（`ModelConfig`）

## 注意事项

- Maven 仓库必须使用 Maven Central（`repo1.maven.org`），**不能用阿里云镜像**（aliyun 无 agentscope 包）
- Java 版本要求：17+
- 新增专家 Agent 需同时：① 创建 `xxxAgent.java` 工厂类，② 添加对应 Tool 类，③ 在 `resources/skills/` 添加 Skill 目录，④ 在 `RouterAgentFactory` 注册
