# 小安 · 长安汽车智能助手

基于 [AgentScope Java SDK](https://agentscope.io/) 构建的车载智能助手 POC，支持车控、音乐、导航、问答四大能力域。

## 功能特性

| 能力域 | 功能 |
|--------|------|
| 🚗 车控 | 空调、车窗、座椅、车灯、车门 |
| 🎵 音乐 | 播放控制、音量调节、播放模式 |
| 🧭 导航 | 路线规划、途经点、目的地修改、POI 搜索 |
| 💬 问答 | 长安汽车知识问答、通用问答 |

## 技术栈

- **Agent Framework**: AgentScope Java SDK 1.0.9
- **LLM**: 阿里云 DashScope（通义千问 qwen-plus）
- **Build**: Maven 3.6+ / Java 17+
- **Test**: JUnit 5

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/home45006/agentscope-poc.git
cd agentscope-poc
```

### 2. 配置 API Key

```bash
cp config.properties.example config.properties
# 编辑 config.properties，填入 dashscope.api.key
```

或设置环境变量：
```bash
export DASHSCOPE_API_KEY=sk-xxxxxxxx
```

### 3. 构建与运行

```bash
# 编译
mvn compile

# 运行测试
mvn test

# Demo 模式（预设5条测试问题）
mvn exec:java

# 交互模式（命令行对话）
mvn exec:java -Dexec.args="--interactive"
```

## 项目结构

```
src/main/java/io/agentscope/poc/
├── XiaoAnApplication.java    # 入口
├── agent/                   # 专家 Agent 工厂
│   ├── VehicleAgent.java    # 车控专家
│   ├── MusicAgent.java      # 音乐专家
│   ├── NavAgent.java        # 导航专家
│   └── QAAgent.java         # 问答专家
├── tool/                    # Tool 实现
│   ├── VehicleTools.java
│   ├── MusicTools.java
│   ├── NavTools.java
│   └── QATools.java
├── model/                   # 指令 DTO
├── hook/                    # Hook 实现
├── router/                  # RouterAgent 工厂
└── config/                  # 配置加载

src/main/resources/skills/   # Skill 资源
src/test/                    # 单元测试
```

## 架构说明

采用 **多 Agent 路由架构**：

```
用户输入 → RouterAgent（小安）
           ├→ VehicleAgent（车控）
           ├→ MusicAgent（音乐）
           ├→ NavAgent（导航）
           └→ QAAgent（问答）
```

- RouterAgent 负责意图分发，使用 AutoContextMemory 管理多轮对话
- 专家 Agent 通过 SkillBox 加载 Skill，工具渐进式披露
- Tool 返回 JSON 指令，用于车机端侧执行

## 文档

- [CLAUDE.md](./.claude/CLAUDE.md) — 开发指南
- [docs/CONTRIB.md](./docs/CONTRIB.md) — 贡献指南
- [docs/RUNBOOK.md](./docs/RUNBOOK.md) — 运行手册
- [codemaps/](./codemaps/) — 架构文档

## License

MIT
