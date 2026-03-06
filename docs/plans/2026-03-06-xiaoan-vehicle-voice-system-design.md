# 小安·长安汽车车载语音交互系统设计文档

**日期**：2026-03-06
**项目**：agentscope-poc
**技术栈**：AgentScope Java SDK + DashScope + ReActAgent

---

## 一、系统概述

### 产品定位

小安是长安汽车的专属智能车载助手，通过语音交互为用户提供车控、音乐、导航、智能问答四大核心能力。

| 属性 | 内容 |
|------|------|
| 名称 | 小安 |
| 隶属 | 长安汽车 |
| 角色定位 | 长安汽车专属智能车载助手，专业、亲切、有温度 |
| 语言风格 | 简洁口语化，适合车载 TTS 播报（短句，不超过20字） |
| 自我介绍 | "您好，我是小安，您的长安专属智能助手，随时为您服务。" |

### 部署架构

- **云端**：意图理解 + 指令生成（本系统负责范围）
- **端侧**：执行云端下发的 JSON 指令（不在本系统范围内）

---

## 二、整体架构

### 选型：Router Agent + SubAgent-as-Tool（方案 A）

路由 Agent（小安）将 4 个领域专家 Agent 注册为工具，由 LLM ReAct 推理自动路由，支持跨域意图（如同时控制空调和播放音乐）。

```
                    ┌─────────────────────────────────────┐
  语音文本输入  ──▶  │       小安·RouterAgent (ReAct)       │
                    │  - AutoContextMemory（全局多轮记忆）  │
                    │  - 4 个 SubAgent 注册为 Tool         │
                    └──────────────┬──────────────────────┘
                                   │ LLM 推理决策，调用对应工具
              ┌────────────────────┼──────────────────────────┐
              ▼                    ▼                           ▼                  ▼
    ┌──────────────┐   ┌──────────────────┐   ┌───────────────┐   ┌──────────────────┐
    │ 小安·车控专家 │   │  小安·音乐专家   │   │ 小安·导航专家 │   │  小安·问答专家   │
    │  车控 Skill  │   │  音乐 Skill      │   │  导航 Skill   │   │ 知识/通用 Skill  │
    │  JsonSession │   │  JsonSession     │   │  JsonSession  │   │  JsonSession     │
    └──────────────┘   └──────────────────┘   └───────────────┘   └──────────────────┘
                                              │
                                   StructuredOutput（JSON）
                                              │
                              ┌───────────────▼───────────────┐
                              │  {                            │
                              │    "domain": "vehicle",       │
                              │    "action": "control_ac",   │
                              │    "params": {...},           │
                              │    "tts": "已将空调调至22度"  │
                              │  }                            │
                              └───────────────────────────────┘
                                              │
                                         下发至端侧执行
```

---

## 三、专家 Agent 工具设计

### 3.1 车控专家工具

```java
@Tool("控制车辆空调：温度、风速、开关")
CarCommand controlAirConditioner(String action, Integer temperature, Integer fanLevel)

@Tool("控制车窗：指定位置和开关幅度")
CarCommand controlWindow(String position, String action, Integer percentage)

@Tool("控制座椅：加热、通风、位置调节")
CarCommand controlSeat(String target, String feature, String action)

@Tool("控制车灯：远光/近光/氛围灯等")
CarCommand controlLight(String lightType, String action)

@Tool("控制车门：锁定/解锁")
CarCommand controlDoor(String action)
```

### 3.2 音乐专家工具

```java
@Tool("播放音乐：歌曲名、歌手、风格、心情等")
MusicCommand playMusic(String query, String queryType)

@Tool("控制播放：暂停、继续、上一首、下一首")
MusicCommand controlPlayback(String action)

@Tool("调节音量：增大、减小、设置为指定值")
MusicCommand adjustVolume(String action, Integer value)

@Tool("切换播放模式：随机、单曲循环、顺序")
MusicCommand setPlayMode(String mode)
```

### 3.3 导航专家工具

```java
@Tool("规划导航路线：目的地名称或地址")
NavCommand startNavigation(String destination, String preference)

@Tool("添加途经点")
NavCommand addWaypoint(String waypoint)

@Tool("修改目的地")
NavCommand changeDestination(String newDestination)

@Tool("查询途中信息：油站/充电站/停车场/服务区")
NavCommand searchAlongRoute(String poiType)

@Tool("取消导航")
NavCommand cancelNavigation()
```

### 3.4 问答专家工具

```java
@Tool("查询长安汽车车型、功能、保养等专属知识")
String queryVehicleKnowledge(String question)

@Tool("回答通用问题：天气、百科、计算等")
String answerGeneralQuestion(String question)
```

### 3.5 统一输出结构

```java
public class VoiceResponse {
    public String domain;              // vehicle / music / nav / qa
    public String action;              // control_ac / play / start_nav 等
    public Map<String, Object> params; // 指令参数
    public String tts;                 // 播报文本，不超过20字
}
```

**示例：**
```json
{
  "domain": "vehicle",
  "action": "control_ac",
  "params": { "action": "set_temperature", "value": 22 },
  "tts": "好的小安已将空调调至22度"
}
```

---

## 四、Agent Skill 设计

### 4.1 Skill 的作用

AgentScope Skill 采用**渐进式披露**机制：初始只加载元数据（~100 tokens），判断相关后才加载完整指令（<5k tokens），工具在此时才激活。优势：

- 减少 token 消耗：专家工具只在被路由时激活
- 知识模块化：参考资料按需加载
- 集中管理：存入 Git 仓库，支持 OTA 更新

### 4.2 Skill 分配

| Agent | Skill 名称 | 触发描述 | 绑定工具 | 参考资料 |
|---|---|---|---|---|
| 车控专家 | `vehicle_control` | 用户需要控制车辆设备时 | 5 个车控工具 | 指令枚举表、参数范围 |
| 音乐专家 | `music_playback` | 用户需要播放/控制音乐时 | 4 个音乐工具 | 音乐服务 API 说明 |
| 导航专家 | `navigation_planning` | 用户需要导航或查询路线时 | 5 个导航工具 | POI 类型表、路线偏好 |
| 问答专家 | `changan_knowledge` | 用户询问长安车型/功能/保养 | queryVehicleKnowledge | 车型手册摘要、FAQ |
| 问答专家 | `general_qa` | 用户询问通用知识/天气/百科 | answerGeneralQuestion | 无 |

### 4.3 Skill 文件结构

```
src/main/resources/skills/
├── vehicle_control/
│   ├── SKILL.md
│   └── references/
│       ├── command-spec.md      # 指令枚举：action 合法值列表
│       └── param-ranges.md      # 参数范围：温度16~30、音量0~100等
├── music_playback/
│   └── SKILL.md
├── navigation_planning/
│   └── SKILL.md
├── changan_knowledge/
│   ├── SKILL.md
│   └── references/
│       └── faq.md               # 长安车型 FAQ
└── general_qa/
    └── SKILL.md
```

**SKILL.md 格式示例（vehicle_control）：**

```yaml
---
name: vehicle_control
description: 当用户需要控制车辆设备时使用，包括空调、车窗、座椅、车灯、车门等操作
---
# 车控技能

## 职责
你是小安的车控模块，负责将用户语音转换为标准车控 JSON 指令。

## 输出规范
- domain 固定为 "vehicle"
- tts 文本使用简短口语，不超过20字
- 参数值必须在合法范围内（见 references/param-ranges.md）

## 可用资源
- references/command-spec.md：所有 action 的合法值
- references/param-ranges.md：各参数的取值范围
```

### 4.4 Skill OTA 更新

```java
// 从 Git 仓库加载，支持自动同步
GitSkillRepository skillRepo = new GitSkillRepository(
    "https://github.com/changan-auto/xiaoan-skills.git"
);
skillRepo.sync(); // OTA 触发时调用
```

---

## 五、记忆与会话管理

### 5.1 记忆分层

```
RouterAgent（小安）
  └── AutoContextMemory（全局多轮，msgThreshold=30, lastKeep=10）
      解决：跨域上下文感知，如"刚才说的目的地"

各专家 Agent
  └── InMemoryMemory + JsonSession（分域持久化）
      解决：域内多轮追问，如"再低2度"
```

### 5.2 Session ID 规则

```
格式：{userId}_{domain}

示例：
  user_001_vehicle   ← 用户001的车控上下文
  user_001_music     ← 用户001的音乐上下文
  user_001_nav       ← 用户001的导航上下文
  user_001_qa        ← 用户001的问答上下文
```

### 5.3 多轮对话示例

```
用户：把空调调到22度
小安：[call_vehicle_agent] → 好的小安已将空调温度调至22度

用户：再低2度                         ← 域内追问，车控专家通过 session_id 感知上下文
小安：[call_vehicle_agent, session=user_001_vehicle] → 已调至20度

用户：导航去最近的加油站
小安：[call_nav_agent] → 为您规划路线，前方2.3公里有中石化加油站

用户：空调也关掉吧                    ← 跨域，RouterAgent AutoContextMemory 感知
小安：[call_vehicle_agent, session=user_001_vehicle] → 已关闭空调
```

---

## 六、项目结构

```
agentscope-poc/
├── src/main/java/io/agentscope/poc/
│   ├── XiaoAnApplication.java          # 入口，组装所有 Agent
│   ├── router/
│   │   └── RouterAgentFactory.java     # 小安路由 Agent 构建工厂
│   ├── agent/
│   │   ├── VehicleAgent.java           # 车控专家 Agent
│   │   ├── MusicAgent.java             # 音乐专家 Agent
│   │   ├── NavAgent.java               # 导航专家 Agent
│   │   └── QAAgent.java                # 问答专家 Agent
│   ├── tool/
│   │   ├── VehicleTools.java           # @Tool 车控工具方法
│   │   ├── MusicTools.java             # @Tool 音乐工具方法
│   │   ├── NavTools.java               # @Tool 导航工具方法
│   │   └── QATools.java                # @Tool 问答工具方法
│   ├── model/
│   │   ├── CarCommand.java             # 车控指令 DTO
│   │   ├── MusicCommand.java           # 音乐指令 DTO
│   │   ├── NavCommand.java             # 导航指令 DTO
│   │   └── VoiceResponse.java          # 统一输出：JSON + TTS
│   └── config/
│       └── ModelConfig.java            # DashScope 模型配置
├── src/main/resources/
│   └── skills/                         # Skill 文件（Classpath 加载）
│       ├── vehicle_control/
│       ├── music_playback/
│       ├── navigation_planning/
│       ├── changan_knowledge/
│       └── general_qa/
└── docs/
    └── plans/
        └── 2026-03-06-xiaoan-vehicle-voice-system-design.md
```

---

## 七、核心代码骨架

### RouterAgentFactory.java

```java
public class RouterAgentFactory {

    public static ReActAgent build(DashScopeChatModel model, String userId) {
        ClasspathSkillRepository skillRepo = new ClasspathSkillRepository("skills");
        Toolkit toolkit = new Toolkit();

        registerExpert(toolkit, "call_vehicle_agent", "控制车辆设备：空调、车窗、座椅、车灯、车门",
                () -> VehicleAgent.build(model, skillRepo, userId));
        registerExpert(toolkit, "call_music_agent", "播放和控制音乐、调节音量",
                () -> MusicAgent.build(model, skillRepo, userId));
        registerExpert(toolkit, "call_nav_agent", "导航路线规划、途经点、目的地修改",
                () -> NavAgent.build(model, skillRepo, userId));
        registerExpert(toolkit, "call_qa_agent", "长安车型问答及通用知识问答",
                () -> QAAgent.build(model, skillRepo, userId));

        AutoContextMemory memory = new AutoContextMemory(
                AutoContextConfig.builder()
                        .msgThreshold(30).lastKeep(10).tokenRatio(0.3).build(),
                model);

        return ReActAgent.builder()
                .name("小安")
                .sysPrompt("""
                        你是小安，长安汽车的专属智能车载助手。
                        你负责理解用户的语音指令，调用对应专家能力完成任务。
                        回答时语气亲切自然，TTS 文本请使用简短口语，不超过20字，避免复杂标点。
                        支持同时处理多个领域的指令，如同时控制空调和播放音乐。
                        """)
                .model(model)
                .memory(memory)
                .toolkit(toolkit)
                .build();
    }

    private static void registerExpert(Toolkit toolkit, String toolName,
            String description, Supplier<ReActAgent> factory) {
        SubAgentConfig config = SubAgentConfig.builder()
                .toolName(toolName)
                .description(description)
                .session(new JsonSession(Path.of("./sessions")))
                .build();
        toolkit.registration().subAgent(factory::get, config).apply();
    }
}
```

### VehicleAgent.java（专家 Agent 示例）

```java
public class VehicleAgent {

    public static ReActAgent build(DashScopeChatModel model,
            ClasspathSkillRepository skillRepo, String userId) {

        SkillBox skillBox = new SkillBox(new Toolkit());
        skillBox.registration()
                .skill(skillRepo.getSkill("vehicle_control"))
                .tool(new AgentTool(new VehicleTools()))
                .apply();

        return ReActAgent.builder()
                .name("小安·车控")
                .sysPrompt("""
                        你是小安的车控模块，负责将用户指令转换为标准车控 JSON 指令。
                        输出必须是合法的 VoiceResponse JSON，tts 字段不超过20字。
                        """)
                .model(model)
                .skillBox(skillBox)
                .memory(new InMemoryMemory())
                .build();
    }
}
```

---

## 八、关键设计决策

| 决策点 | 选择 | 原因 |
|--------|------|------|
| 路由方式 | SubAgent-as-Tool | AgentScope 原生支持，LLM 推理路由，支持跨域指令 |
| 全局记忆 | AutoContextMemory | 自动压缩，避免 token 无限增长 |
| 分域记忆 | InMemoryMemory + JsonSession | 各域独立，重启可恢复 |
| Skill 存储 | ClasspathSkillRepository（开发） / GitSkillRepository（生产） | 开发简单，生产支持 OTA |
| 输出格式 | VoiceResponse JSON + tts 字段 | 端侧直接执行，无需二次解析 |
| 模型 | qwen-plus | 成本与能力平衡，适合车载场景 |
