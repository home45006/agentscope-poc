# Architecture Codemap
_Updated: 2026-03-06_

## 项目定位

**小安** — 基于 AgentScope Java SDK (`io.agentscope:agentscope:1.0.9`) 的长安汽车车载智能助手 POC。纯 Java 后端，无前端，通过命令行交互。LLM 后端：DashScope `qwen-plus`。

## 整体架构

```
用户输入（CLI）
  └→ XiaoAnApplication          入口，解析 --interactive / demo 模式
       └→ RouterAgentFactory     构建顶层路由 Agent
            └→ ReActAgent（小安） 调用子 Agent 工具
                 ├→ VehicleAgent  车控专家（空调/车窗/座椅/车灯/车门）
                 ├→ MusicAgent    音乐专家（播放/音量/模式）
                 ├→ NavAgent      导航专家（规划/途经点/POI）
                 └→ QAAgent       问答专家（车型知识/通用问答）
```

## 数据流向图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           用户输入（CLI）                                │
│                     "把空调调到22度，导航去解放碑"                        │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │ Msg(role=USER, content=...)
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        XiaoAnApplication                                │
│  构建 Msg 对象，调用 xiaoAn.call(msg).block()                            │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │ Msg
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    RouterAgent（小安）ReAct 循环                         │
│                                                                         │
│  AutoContextMemory ◄──── 写入历史消息（满30条自动压缩，保留最近10条）       │
│                                                                         │
│  [Thought] 需要车控 + 导航，调用两个子 Agent                              │
│  [Action]  call_vehicle_agent / call_nav_agent  ◄── 可并发               │
└────────┬────────────────────────┬────────────────────────────────────────┘
         │ SubAgent 工具调用        │ SubAgent 工具调用
         ▼                        ▼
┌─────────────────┐    ┌──────────────────┐
│  VehicleAgent   │    │    NavAgent       │
│  ReAct 循环     │    │    ReAct 循环     │
│                 │    │                  │
│  SkillBox 加载  │    │  SkillBox 加载    │
│  vehicle_control│    │  navigation_plan  │
│  Skill（渐进披露）│   │  Skill（渐进披露） │
│                 │    │                  │
│  [Action]       │    │  [Action]         │
│  control_ac(22) │    │  start_nav(解放碑)│
└────────┬────────┘    └────────┬─────────┘
         │ @Tool 方法调用         │ @Tool 方法调用
         ▼                       ▼
  VehicleTools                NavTools
  构造 CarCommand              构造 NavCommand
  序列化为 JSON 字符串           序列化为 JSON 字符串
         │                       │
         ▼                       ▼
  {"domain":"vehicle",    {"domain":"navigation",
   "action":"control_ac", "action":"start_navigation",
   "params":{temp:22},    "params":{dest:"解放碑"},
   "tts":"已将空调调至22度"} "tts":"为您导航至解放碑"}
         │                       │
         └───────────┬───────────┘
                     │ JSON 字符串（Tool 返回值）
                     ▼
         ┌───────────────────────┐
         │  CommandCaptureHook   │  PostActingEvent 触发
         │  解析 JSON，校验字段   │  (含 action/domain/tts 之一)
         │  追加到 ThreadLocal   │
         └───────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    RouterAgent（汇总阶段）                                │
│                                                                         │
│  [Observation] 读取两个子 Agent 返回的 JSON 文本                          │
│  [Answer]      自然语言回复 + 附加 JSON 标记                              │
│                                                                         │
│  "好的，已调空调至22度，正在为您导航至解放碑。                             │
│   [[JSON_START]]{...control_ac...}[[JSON_END]]                           │
│   [[JSON_START]]{...start_navigation...}[[JSON_END]]"                    │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │ Msg(role=ASSISTANT)
                               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     XiaoAnApplication（展示层）                          │
│                                                                         │
│  正则提取 [[JSON_START]]...[[JSON_END]]（Pattern.DOTALL）                │
│  ├── 自然语言部分 → 直接打印到终端                                         │
│  └── JSON 指令列表 → 格式化输出「下发指令」面板                             │
│                                                                         │
│  ┌─────────── 下发指令 ──────────┐                                       │
│  │ [1] vehicle.control_ac {temperature:22}                               │
│  │ [2] navigation.start_navigation {destination:"解放碑"}                 │
│  └───────────────────────────────────┘                                  │
└─────────────────────────────────────────────────────────────────────────┘
                               │
                               ▼
                    端侧车机（消费 JSON 指令执行实际控制）
```

### 关键数据转换节点

| 节点 | 输入 | 输出 |
|------|------|------|
| `XiaoAnApplication` | 用户字符串 | `Msg(USER)` |
| `RouterAgent` | `Msg(USER)` | SubAgent 工具调用参数（字符串） |
| `XxxTools` 方法 | Java 类型参数 | JSON 字符串（含 domain/action/params/tts） |
| `CommandCaptureHook` | Tool 返回的 JSON 字符串 | `ThreadLocal<List<Map>>` 缓存 |
| `RouterAgent` 汇总 | 子 Agent 返回文本 | `[[JSON_START]]...[[JSON_END]]` 包裹的回复 |
| `XiaoAnApplication` 解析 | 含标记的回复字符串 | 自然语言文本 + 结构化指令列表 |

## 关键框架概念

| 概念 | AgentScope 类 | 用途 |
|------|--------------|------|
| 智能体 | `ReActAgent` | ReAct 循环推理+工具调用 |
| 记忆 | `AutoContextMemory` | Router 用，自动压缩（30条→保留10条） |
| 记忆 | `InMemoryMemory` | 专家 Agent 用，轻量短期记忆 |
| 工具 | `@Tool` / `@ToolParam` | 方法注解声明工具 |
| 技能 | `ClasspathSkillRepository` + `SkillBox` | 从 classpath 加载 SKILL.md |
| 子Agent | `SubAgentConfig` + `JsonSession` | 专家 Agent 作为工具注册到 Router |
| 钩子 | `Hook` / `PostActingEvent` | 工具调用后拦截结果 |
| 消息 | `Msg` / `MsgRole` | 统一消息格式 |

## 模块依赖关系

```
XiaoAnApplication
  ├→ RouterAgentFactory
  │    ├→ [VehicleAgent, MusicAgent, NavAgent, QAAgent]（工厂）
  │    ├→ ClasspathSkillRepository（共享，生命周期由 Router 管理）
  │    └→ CommandCaptureHook
  └→ ModelConfig（API Key + 模型配置）
```

## 会话持久化

专家 Agent 会话以 `JsonSession` 存储到 `./sessions/<sessionId>/` 目录，支持多轮对话跨请求保持上下文。
