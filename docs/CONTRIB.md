# 贡献指南

_最后更新：2026-03-06_

## 环境准备

### 必须条件

| 工具 | 版本要求 |
|------|---------|
| Java | 17+ |
| Maven | 3.6+ |
| DashScope API Key | 通义千问账号申请 |

### 配置 API Key

```bash
# 方式 1：config.properties（推荐本地开发）
cp config.properties.example config.properties
# 编辑 config.properties，填入真实 Key：
# dashscope.api.key=sk-xxxxxxxxxxxxxxxx

# 方式 2：环境变量
export DASHSCOPE_API_KEY=sk-xxxxxxxxxxxxxxxx
```

> `config.properties` 已在 `.gitignore` 中，不会提交到仓库。

### Maven 仓库注意事项

**必须使用 Maven Central**，不能配置阿里云镜像（aliyun 无 `io.agentscope:agentscope` 包）。如果本地 `~/.m2/settings.xml` 配置了 mirror，请临时移除或在项目 `pom.xml` 中已指定 `repo1.maven.org` 直连。

---

## Maven 命令参考

| 命令 | 说明 |
|------|------|
| `mvn compile` | 编译源码 |
| `mvn test` | 运行所有单元测试 |
| `mvn test -Dtest=VehicleToolsTest` | 运行单个测试类 |
| `mvn exec:java` | Demo 模式运行（预设5条测试问题） |
| `mvn exec:java -Dexec.args="--interactive"` | 交互模式运行（命令行对话） |
| `mvn package` | 打包为 JAR |
| `mvn clean` | 清理构建产物 |

---

## 开发工作流

### 1. 添加新的专家 Agent

每个专家 Agent 由四个部分组成，必须同时创建：

```
① src/main/java/io/agentscope/poc/tool/XxxTools.java     # Tool 实现
② src/main/java/io/agentscope/poc/agent/XxxAgent.java    # Agent 工厂类
③ src/main/resources/skills/xxx_skill/SKILL.md           # Skill 定义
④ 在 RouterAgentFactory 中注册                            # 接入路由
```

**Tool 编写规范：**
- 方法加 `@Tool(name, description)` 注解
- 参数加 `@ToolParam(name, description)` 注解
- 返回 JSON 字符串（`MAPPER.writeValueAsString(result)`），结构为 `{domain, action, params, tts}`
- `tts` 字段不超过 20 字，口语化，无标点

**SKILL.md frontmatter 格式：**
```markdown
---
name: skill_name          # 对应 skillRepo.getSkill("skill_name")
description: 技能描述
---
```

### 2. TDD 开发流程

```bash
# Step 1: 写测试（先失败）
# 在 src/test/java/io/agentscope/poc/tool/ 创建 XxxToolsTest.java

# Step 2: 确认失败
mvn test -Dtest=XxxToolsTest

# Step 3: 实现代码

# Step 4: 确认通过
mvn test -Dtest=XxxToolsTest

# Step 5: 运行全量测试
mvn test
```

### 3. 代码结构规范

- 所有 Agent 工厂类使用静态工厂方法 `build(model, skillRepo)`，私有构造器
- Tool 类不持有状态（无实例字段）
- 数据模型（`model/` 包）用公共字段，无需 getter/setter（Jackson 直接序列化）

---

## 测试说明

### 单元测试（无需 API Key）

测试位于 `src/test/java/`，覆盖所有 Tool 类：

| 测试类 | 覆盖范围 |
|--------|---------|
| `VehicleToolsTest` | 车控5个 Tool 方法 |
| `MusicToolsTest` | 音乐4个 Tool 方法 |
| `NavToolsTest` | 导航5个 Tool 方法 |
| `QAToolsTest` | 问答2个 Tool 方法 |
| `VoiceResponseTest` | VoiceResponse DTO |

```bash
mvn test   # 全量运行，无需 API Key
```

### 集成测试（需要 API Key）

```bash
# Demo 模式：5条预设问题端到端验证
mvn exec:java

# 交互模式：手动输入对话
mvn exec:java -Dexec.args="--interactive"
```

---

## 项目结构速览

```
src/main/java/io/agentscope/poc/
├── XiaoAnApplication.java    # 入口
├── agent/                    # 专家 Agent 工厂（4个）
├── tool/                     # AgentScope Tool 实现（4个）
├── model/                    # 指令 DTO（4个）
├── hook/                     # PostActingEvent Hook
├── router/                   # RouterAgent 工厂
└── config/                   # ModelConfig

src/main/resources/skills/    # 5个 Skill 目录
src/test/java/                # 单元测试（5个测试类）
```
