# Backend Codemap
_Updated: 2026-03-06_

## 包结构

```
io.agentscope.poc
├── XiaoAnApplication          入口类，CLI 交互 & Demo 模式
├── config/
│   └── ModelConfig            加载 API Key（config.properties → 环境变量）
├── router/
│   └── RouterAgentFactory     构建顶层路由 ReActAgent，注册4个子Agent工具
├── agent/
│   ├── VehicleAgent           车控专家 Agent 工厂
│   ├── MusicAgent             音乐专家 Agent 工厂
│   ├── NavAgent               导航专家 Agent 工厂
│   └── QAAgent                问答专家 Agent 工厂
├── tool/
│   ├── VehicleTools           车控工具（@Tool 注解）→ 5个方法
│   ├── MusicTools             音乐工具（@Tool 注解）→ 4个方法
│   ├── NavTools               导航工具（@Tool 注解）
│   └── QATools                问答工具（@Tool 注解）
├── hook/
│   ├── CommandCaptureHook     Hook实现，捕获工具返回 JSON 指令（ThreadLocal）
│   └── CommandRegistry        会话级指令注册中心（ConcurrentHashMap + ThreadLocal）
└── model/
    ├── VoiceResponse          统一语音响应 DTO（domain/action/params/tts）
    ├── CarCommand             车控指令 DTO
    ├── MusicCommand           音乐指令 DTO
    └── NavCommand             导航指令 DTO
```

## Tool 方法清单

### VehicleTools
| 方法 | Tool名称 | 关键参数 |
|------|---------|---------|
| `controlAirConditioner` | `control_air_conditioner` | action, temperature(16-30), fan_level(1-5) |
| `controlWindow` | `control_window` | position, action, percentage(0-100) |
| `controlSeat` | `control_seat` | target, feature, action |
| `controlLight` | `control_light` | light_type, action |
| `controlDoor` | `control_door` | action(lock/unlock) |

### MusicTools
| 方法 | Tool名称 | 关键参数 |
|------|---------|---------|
| `playMusic` | `play_music` | query, query_type |
| `controlPlayback` | `control_playback` | action(pause/resume/next/previous) |
| `adjustVolume` | `adjust_volume` | action(up/down/set), value(0-100) |
| `setPlayMode` | `set_play_mode` | mode(shuffle/repeat_one/sequence) |

## 指令输出格式

所有 Tool 返回 JSON 字符串，结构统一：
```json
{
  "domain": "vehicle|music|nav|qa",
  "action": "control_ac|play_music|...",
  "params": { "key": "value" },
  "tts": "好的小安已..."
}
```

Router 将其包裹为：`[[JSON_START]]{...}[[JSON_END]]`

## Hook 机制

- `CommandCaptureHook` 实现 `Hook` 接口，监听 `PostActingEvent`
- 从工具返回的 `TextBlock` 中尝试解析 JSON
- 有效指令（含 action/domain/tts 字段）存入 `ThreadLocal<List<Map>>`
- `CommandRegistry` 提供按 sessionId 隔离的替代方案（ConcurrentHashMap）

## 配置加载优先级

```
config.properties (dashscope.api.key) > 环境变量 DASHSCOPE_API_KEY
```

## Skill 资源路径

```
src/main/resources/skills/
├── vehicle_control/
│   ├── SKILL.md               name: vehicle_control
│   └── references/
│       ├── command-spec.md
│       └── param-ranges.md
├── music_playback/            name: music_playback
│   └── SKILL.md
├── navigation_planning/       name: navigation_planning
│   └── SKILL.md
├── changan_knowledge/         name: changan_knowledge
│   ├── SKILL.md
│   └── references/faq.md
└── general_qa/                name: general_qa
    └── SKILL.md
```

`ClasspathSkillRepository("skills")` 扫描该目录，`skillRepo.getSkill("<name>")` 按 frontmatter `name` 字段查找。
