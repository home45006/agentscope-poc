# Data Models Codemap
_Updated: 2026-03-06_

## 指令 DTO

所有专家 Agent 的输出指令共享相同结构，各领域独立类型：

```java
// 统一结构（VoiceResponse / CarCommand / MusicCommand / NavCommand）
{
  String domain;   // "vehicle" | "music" | "nav" | "qa"（VoiceResponse 独有）
  String action;   // 领域内操作名称
  Map<String, Object> params;  // 操作参数
  String tts;      // TTS 文本，≤20字，口语，无标点
}
```

### CarCommand（车控指令）
`action` 合法值：`control_ac` / `control_window` / `control_seat` / `control_light` / `control_door`

| action | params 字段 |
|--------|-----------|
| control_ac | action(set_temperature/set_fan/turn_on/turn_off), temperature(16-30), fan_level(1-5) |
| control_window | position(front_left/front_right/rear_left/rear_right/all), action(open/close/set), percentage(0-100) |
| control_seat | target(driver/passenger/rear_left/rear_right), feature(heat/ventilate/position), action(on/off/level_1/2/3) |
| control_light | light_type(high_beam/low_beam/ambient/hazard), action(on/off) |
| control_door | action(lock/unlock) |

### MusicCommand（音乐指令）
`action` 合法值：`play_music` / `control_playback` / `adjust_volume` / `set_play_mode`

| action | params 字段 |
|--------|-----------|
| play_music | query, query_type(song/artist/genre/mood) |
| control_playback | action(pause/resume/next/previous) |
| adjust_volume | action(up/down/set), value(0-100) |
| set_play_mode | mode(shuffle/repeat_one/sequence) |

### NavCommand（导航指令）
详见 `NavTools.java`（通过 `@Tool` / `@ToolParam` 注解定义）

## 消息格式（AgentScope）

```java
Msg {
  MsgRole role;     // USER / ASSISTANT / SYSTEM / TOOL
  String content;   // 文本内容（getTextContent()/textContent()）
}
```

## 会话存储格式

`JsonSession` 将对话历史序列化为 JSON 文件：
```
./sessions/<sessionId>/<sessionId>.json
```

## 指令捕获格式（运行时）

RouterAgent 最终回复格式：
```
<自然语言回复>
[[JSON_START]]{"domain":"...","action":"...","params":{...},"tts":"..."}[[JSON_END]]
```

CLI 解析正则：`\[\[JSON_START\]\](.*?)\[\[JSON_END\]\]`（Pattern.DOTALL）
