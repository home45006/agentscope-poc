# 小安车载语音交互系统实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 基于 AgentScope Java SDK 构建小安·长安汽车车载语音交互系统，支持车控、音乐、导航、智能问答四大能力域。

**Architecture:** Router Agent（小安）通过 SubAgent-as-Tool 模式将 4 个领域专家 Agent 注册为工具，由 LLM ReAct 推理自动路由；各专家 Agent 通过 SkillBox 加载 Skill，工具渐进式披露；RouterAgent 使用 AutoContextMemory 管理全局多轮对话，各专家 Agent 通过 JsonSession 持久化分域上下文。

**Tech Stack:** AgentScope Java SDK 1.0.9、Java 17、Maven、DashScope（qwen-plus）、JUnit 5

---

## 准备工作

确认项目可正常构建：
```bash
cd agentscope-poc
mvn compile
```
期望：BUILD SUCCESS

---

### Task 1: 添加测试依赖 & 统一输出 DTO

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/io/agentscope/poc/model/VoiceResponse.java`
- Create: `src/main/java/io/agentscope/poc/model/CarCommand.java`
- Create: `src/main/java/io/agentscope/poc/model/MusicCommand.java`
- Create: `src/main/java/io/agentscope/poc/model/NavCommand.java`
- Create: `src/test/java/io/agentscope/poc/model/VoiceResponseTest.java`

**Step 1: 在 pom.xml 中添加 JUnit 5 依赖**

在 `<dependencies>` 块末尾添加：
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>
```

同时在 `<build><plugins>` 中添加 surefire 插件以支持 JUnit 5：
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
</plugin>
```

**Step 2: 先写失败的测试**

创建 `src/test/java/io/agentscope/poc/model/VoiceResponseTest.java`：
```java
package io.agentscope.poc.model;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class VoiceResponseTest {

    @Test
    void shouldCreateVoiceResponse() {
        VoiceResponse resp = new VoiceResponse();
        resp.domain = "vehicle";
        resp.action = "control_ac";
        resp.params = Map.of("temperature", 22);
        resp.tts = "已将空调调至22度";

        assertEquals("vehicle", resp.domain);
        assertEquals("control_ac", resp.action);
        assertEquals(22, resp.params.get("temperature"));
        assertEquals("已将空调调至22度", resp.tts);
    }
}
```

**Step 3: 运行测试，确认失败**

```bash
mvn test -pl . -Dtest=VoiceResponseTest
```
期望：FAIL（VoiceResponse 类不存在）

**Step 4: 创建 DTO 类**

创建 `src/main/java/io/agentscope/poc/model/VoiceResponse.java`：
```java
package io.agentscope.poc.model;

import java.util.Map;

public class VoiceResponse {
    public String domain;
    public String action;
    public Map<String, Object> params;
    public String tts;
}
```

创建 `src/main/java/io/agentscope/poc/model/CarCommand.java`：
```java
package io.agentscope.poc.model;

import java.util.Map;

public class CarCommand {
    public String action;
    public Map<String, Object> params;
    public String tts;
}
```

创建 `src/main/java/io/agentscope/poc/model/MusicCommand.java`：
```java
package io.agentscope.poc.model;

import java.util.Map;

public class MusicCommand {
    public String action;
    public Map<String, Object> params;
    public String tts;
}
```

创建 `src/main/java/io/agentscope/poc/model/NavCommand.java`：
```java
package io.agentscope.poc.model;

import java.util.Map;

public class NavCommand {
    public String action;
    public Map<String, Object> params;
    public String tts;
}
```

**Step 5: 运行测试，确认通过**

```bash
mvn test -Dtest=VoiceResponseTest
```
期望：BUILD SUCCESS, Tests run: 1, Failures: 0

**Step 6: 提交**

```bash
git add pom.xml src/main/java/io/agentscope/poc/model/ src/test/java/io/agentscope/poc/model/
git commit -m "feat: 添加测试依赖和统一输出 DTO 模型"
```

---

### Task 2: 车控工具（VehicleTools）

**Files:**
- Create: `src/main/java/io/agentscope/poc/tool/VehicleTools.java`
- Create: `src/test/java/io/agentscope/poc/tool/VehicleToolsTest.java`

**Step 1: 先写失败的测试**

创建 `src/test/java/io/agentscope/poc/tool/VehicleToolsTest.java`：
```java
package io.agentscope.poc.tool;

import io.agentscope.poc.model.CarCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VehicleToolsTest {

    private VehicleTools tools;

    @BeforeEach
    void setUp() {
        tools = new VehicleTools();
    }

    @Test
    void shouldControlAirConditioner() {
        CarCommand cmd = tools.controlAirConditioner("set_temperature", 22, null);
        assertEquals("control_ac", cmd.action);
        assertEquals(22, cmd.params.get("temperature"));
        assertNotNull(cmd.tts);
    }

    @Test
    void shouldControlWindow() {
        CarCommand cmd = tools.controlWindow("front_left", "open", 50);
        assertEquals("control_window", cmd.action);
        assertEquals("front_left", cmd.params.get("position"));
        assertEquals(50, cmd.params.get("percentage"));
    }

    @Test
    void shouldControlDoor() {
        CarCommand cmd = tools.controlDoor("lock");
        assertEquals("control_door", cmd.action);
        assertEquals("lock", cmd.params.get("action"));
    }
}
```

**Step 2: 运行测试，确认失败**

```bash
mvn test -Dtest=VehicleToolsTest
```
期望：FAIL（VehicleTools 类不存在）

**Step 3: 实现 VehicleTools**

创建 `src/main/java/io/agentscope/poc/tool/VehicleTools.java`：
```java
package io.agentscope.poc.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.poc.model.CarCommand;

import java.util.HashMap;
import java.util.Map;

public class VehicleTools {

    @Tool(name = "control_air_conditioner", description = "控制车辆空调：温度、风速、开关")
    public CarCommand controlAirConditioner(
            @ToolParam(name = "action", description = "操作：set_temperature/set_fan/turn_on/turn_off") String action,
            @ToolParam(name = "temperature", description = "目标温度（16-30），仅 set_temperature 时有效") Integer temperature,
            @ToolParam(name = "fan_level", description = "风速级别（1-5），仅 set_fan 时有效") Integer fanLevel) {
        Map<String, Object> params = new HashMap<>();
        params.put("action", action);
        if (temperature != null) params.put("temperature", temperature);
        if (fanLevel != null) params.put("fan_level", fanLevel);

        CarCommand cmd = new CarCommand();
        cmd.action = "control_ac";
        cmd.params = params;
        cmd.tts = buildAcTts(action, temperature, fanLevel);
        return cmd;
    }

    @Tool(name = "control_window", description = "控制车窗：指定位置和开关幅度")
    public CarCommand controlWindow(
            @ToolParam(name = "position", description = "位置：front_left/front_right/rear_left/rear_right/all") String position,
            @ToolParam(name = "action", description = "操作：open/close/set") String action,
            @ToolParam(name = "percentage", description = "开启幅度（0-100），仅 set 时有效") Integer percentage) {
        Map<String, Object> params = new HashMap<>();
        params.put("position", position);
        params.put("action", action);
        if (percentage != null) params.put("percentage", percentage);

        CarCommand cmd = new CarCommand();
        cmd.action = "control_window";
        cmd.params = params;
        cmd.tts = "好的小安已" + ("open".equals(action) ? "打开" : "关闭") + "车窗";
        return cmd;
    }

    @Tool(name = "control_seat", description = "控制座椅：加热、通风、位置调节")
    public CarCommand controlSeat(
            @ToolParam(name = "target", description = "座椅：driver/passenger/rear_left/rear_right") String target,
            @ToolParam(name = "feature", description = "功能：heat/ventilate/position") String feature,
            @ToolParam(name = "action", description = "操作：on/off/level_1/level_2/level_3") String action) {
        Map<String, Object> params = new HashMap<>();
        params.put("target", target);
        params.put("feature", feature);
        params.put("action", action);

        CarCommand cmd = new CarCommand();
        cmd.action = "control_seat";
        cmd.params = params;
        cmd.tts = "好的小安已调节座椅" + feature;
        return cmd;
    }

    @Tool(name = "control_light", description = "控制车灯：远光/近光/氛围灯等")
    public CarCommand controlLight(
            @ToolParam(name = "light_type", description = "灯类型：high_beam/low_beam/ambient/hazard") String lightType,
            @ToolParam(name = "action", description = "操作：on/off") String action) {
        Map<String, Object> params = new HashMap<>();
        params.put("light_type", lightType);
        params.put("action", action);

        CarCommand cmd = new CarCommand();
        cmd.action = "control_light";
        cmd.params = params;
        cmd.tts = "好的小安已" + ("on".equals(action) ? "开启" : "关闭") + "车灯";
        return cmd;
    }

    @Tool(name = "control_door", description = "控制车门：锁定/解锁")
    public CarCommand controlDoor(
            @ToolParam(name = "action", description = "操作：lock/unlock") String action) {
        Map<String, Object> params = new HashMap<>();
        params.put("action", action);

        CarCommand cmd = new CarCommand();
        cmd.action = "control_door";
        cmd.params = params;
        cmd.tts = "lock".equals(action) ? "好的小安已锁车" : "好的小安已解锁车门";
        return cmd;
    }

    private String buildAcTts(String action, Integer temperature, Integer fanLevel) {
        return switch (action) {
            case "set_temperature" -> "好的小安已将空调调至" + temperature + "度";
            case "set_fan" -> "好的小安已将风速调至" + fanLevel + "档";
            case "turn_on" -> "好的小安已开启空调";
            case "turn_off" -> "好的小安已关闭空调";
            default -> "好的已完成操作";
        };
    }
}
```

**Step 4: 运行测试，确认通过**

```bash
mvn test -Dtest=VehicleToolsTest
```
期望：BUILD SUCCESS, Tests run: 3, Failures: 0

**Step 5: 提交**

```bash
git add src/main/java/io/agentscope/poc/tool/VehicleTools.java src/test/java/io/agentscope/poc/tool/VehicleToolsTest.java
git commit -m "feat: 实现车控工具 VehicleTools"
```

---

### Task 3: 音乐工具（MusicTools）

**Files:**
- Create: `src/main/java/io/agentscope/poc/tool/MusicTools.java`
- Create: `src/test/java/io/agentscope/poc/tool/MusicToolsTest.java`

**Step 1: 先写失败的测试**

创建 `src/test/java/io/agentscope/poc/tool/MusicToolsTest.java`：
```java
package io.agentscope.poc.tool;

import io.agentscope.poc.model.MusicCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MusicToolsTest {

    private MusicTools tools;

    @BeforeEach
    void setUp() {
        tools = new MusicTools();
    }

    @Test
    void shouldPlayMusic() {
        MusicCommand cmd = tools.playMusic("周杰伦", "artist");
        assertEquals("play_music", cmd.action);
        assertEquals("周杰伦", cmd.params.get("query"));
        assertNotNull(cmd.tts);
    }

    @Test
    void shouldControlPlayback() {
        MusicCommand cmd = tools.controlPlayback("next");
        assertEquals("control_playback", cmd.action);
        assertEquals("next", cmd.params.get("action"));
    }

    @Test
    void shouldAdjustVolume() {
        MusicCommand cmd = tools.adjustVolume("set", 50);
        assertEquals("adjust_volume", cmd.action);
        assertEquals(50, cmd.params.get("value"));
    }
}
```

**Step 2: 运行测试，确认失败**

```bash
mvn test -Dtest=MusicToolsTest
```
期望：FAIL

**Step 3: 实现 MusicTools**

创建 `src/main/java/io/agentscope/poc/tool/MusicTools.java`：
```java
package io.agentscope.poc.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.poc.model.MusicCommand;

import java.util.HashMap;
import java.util.Map;

public class MusicTools {

    @Tool(name = "play_music", description = "播放音乐：歌曲名、歌手、风格、心情等")
    public MusicCommand playMusic(
            @ToolParam(name = "query", description = "搜索关键词：歌曲名/歌手/风格/心情") String query,
            @ToolParam(name = "query_type", description = "查询类型：song/artist/genre/mood") String queryType) {
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("query_type", queryType);

        MusicCommand cmd = new MusicCommand();
        cmd.action = "play_music";
        cmd.params = params;
        cmd.tts = "好的小安为您播放" + query;
        return cmd;
    }

    @Tool(name = "control_playback", description = "控制播放：暂停、继续、上一首、下一首")
    public MusicCommand controlPlayback(
            @ToolParam(name = "action", description = "操作：pause/resume/next/previous") String action) {
        Map<String, Object> params = new HashMap<>();
        params.put("action", action);

        MusicCommand cmd = new MusicCommand();
        cmd.action = "control_playback";
        cmd.params = params;
        cmd.tts = buildPlaybackTts(action);
        return cmd;
    }

    @Tool(name = "adjust_volume", description = "调节音量：增大、减小、设置为指定值")
    public MusicCommand adjustVolume(
            @ToolParam(name = "action", description = "操作：up/down/set") String action,
            @ToolParam(name = "value", description = "音量值（0-100），仅 set 时有效") Integer value) {
        Map<String, Object> params = new HashMap<>();
        params.put("action", action);
        if (value != null) params.put("value", value);

        MusicCommand cmd = new MusicCommand();
        cmd.action = "adjust_volume";
        cmd.params = params;
        cmd.tts = "set".equals(action) ? "好的已将音量调至" + value : ("up".equals(action) ? "好的已调高音量" : "好的已调低音量");
        return cmd;
    }

    @Tool(name = "set_play_mode", description = "切换播放模式：随机、单曲循环、顺序")
    public MusicCommand setPlayMode(
            @ToolParam(name = "mode", description = "模式：shuffle/repeat_one/sequence") String mode) {
        Map<String, Object> params = new HashMap<>();
        params.put("mode", mode);

        MusicCommand cmd = new MusicCommand();
        cmd.action = "set_play_mode";
        cmd.params = params;
        cmd.tts = "好的已切换播放模式";
        return cmd;
    }

    private String buildPlaybackTts(String action) {
        return switch (action) {
            case "pause" -> "好的已暂停播放";
            case "resume" -> "好的已继续播放";
            case "next" -> "好的播放下一首";
            case "previous" -> "好的播放上一首";
            default -> "好的已完成操作";
        };
    }
}
```

**Step 4: 运行测试，确认通过**

```bash
mvn test -Dtest=MusicToolsTest
```
期望：BUILD SUCCESS, Tests run: 3, Failures: 0

**Step 5: 提交**

```bash
git add src/main/java/io/agentscope/poc/tool/MusicTools.java src/test/java/io/agentscope/poc/tool/MusicToolsTest.java
git commit -m "feat: 实现音乐工具 MusicTools"
```

---

### Task 4: 导航工具（NavTools）

**Files:**
- Create: `src/main/java/io/agentscope/poc/tool/NavTools.java`
- Create: `src/test/java/io/agentscope/poc/tool/NavToolsTest.java`

**Step 1: 先写失败的测试**

创建 `src/test/java/io/agentscope/poc/tool/NavToolsTest.java`：
```java
package io.agentscope.poc.tool;

import io.agentscope.poc.model.NavCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NavToolsTest {

    private NavTools tools;

    @BeforeEach
    void setUp() {
        tools = new NavTools();
    }

    @Test
    void shouldStartNavigation() {
        NavCommand cmd = tools.startNavigation("重庆解放碑", "fastest");
        assertEquals("start_navigation", cmd.action);
        assertEquals("重庆解放碑", cmd.params.get("destination"));
        assertNotNull(cmd.tts);
    }

    @Test
    void shouldAddWaypoint() {
        NavCommand cmd = tools.addWaypoint("中石化加油站");
        assertEquals("add_waypoint", cmd.action);
        assertEquals("中石化加油站", cmd.params.get("waypoint"));
    }

    @Test
    void shouldCancelNavigation() {
        NavCommand cmd = tools.cancelNavigation();
        assertEquals("cancel_navigation", cmd.action);
        assertNotNull(cmd.tts);
    }
}
```

**Step 2: 运行测试，确认失败**

```bash
mvn test -Dtest=NavToolsTest
```
期望：FAIL

**Step 3: 实现 NavTools**

创建 `src/main/java/io/agentscope/poc/tool/NavTools.java`：
```java
package io.agentscope.poc.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.poc.model.NavCommand;

import java.util.HashMap;
import java.util.Map;

public class NavTools {

    @Tool(name = "start_navigation", description = "规划导航路线：目的地名称或地址")
    public NavCommand startNavigation(
            @ToolParam(name = "destination", description = "目的地名称或地址") String destination,
            @ToolParam(name = "preference", description = "路线偏好：fastest/shortest/avoid_toll/avoid_highway") String preference) {
        Map<String, Object> params = new HashMap<>();
        params.put("destination", destination);
        if (preference != null) params.put("preference", preference);

        NavCommand cmd = new NavCommand();
        cmd.action = "start_navigation";
        cmd.params = params;
        cmd.tts = "好的小安为您导航至" + destination;
        return cmd;
    }

    @Tool(name = "add_waypoint", description = "添加途经点")
    public NavCommand addWaypoint(
            @ToolParam(name = "waypoint", description = "途经点名称或地址") String waypoint) {
        Map<String, Object> params = new HashMap<>();
        params.put("waypoint", waypoint);

        NavCommand cmd = new NavCommand();
        cmd.action = "add_waypoint";
        cmd.params = params;
        cmd.tts = "好的已添加途经" + waypoint;
        return cmd;
    }

    @Tool(name = "change_destination", description = "修改目的地")
    public NavCommand changeDestination(
            @ToolParam(name = "new_destination", description = "新目的地名称或地址") String newDestination) {
        Map<String, Object> params = new HashMap<>();
        params.put("destination", newDestination);

        NavCommand cmd = new NavCommand();
        cmd.action = "change_destination";
        cmd.params = params;
        cmd.tts = "好的已修改目的地为" + newDestination;
        return cmd;
    }

    @Tool(name = "search_along_route", description = "查询途中信息：油站/充电站/停车场/服务区")
    public NavCommand searchAlongRoute(
            @ToolParam(name = "poi_type", description = "POI类型：gas_station/charging_station/parking/service_area") String poiType) {
        Map<String, Object> params = new HashMap<>();
        params.put("poi_type", poiType);

        NavCommand cmd = new NavCommand();
        cmd.action = "search_along_route";
        cmd.params = params;
        cmd.tts = "好的为您搜索途经" + translatePoiType(poiType);
        return cmd;
    }

    @Tool(name = "cancel_navigation", description = "取消导航")
    public NavCommand cancelNavigation() {
        NavCommand cmd = new NavCommand();
        cmd.action = "cancel_navigation";
        cmd.params = Map.of();
        cmd.tts = "好的已取消导航";
        return cmd;
    }

    private String translatePoiType(String poiType) {
        return switch (poiType) {
            case "gas_station" -> "加油站";
            case "charging_station" -> "充电站";
            case "parking" -> "停车场";
            case "service_area" -> "服务区";
            default -> poiType;
        };
    }
}
```

**Step 4: 运行测试，确认通过**

```bash
mvn test -Dtest=NavToolsTest
```
期望：BUILD SUCCESS, Tests run: 3, Failures: 0

**Step 5: 提交**

```bash
git add src/main/java/io/agentscope/poc/tool/NavTools.java src/test/java/io/agentscope/poc/tool/NavToolsTest.java
git commit -m "feat: 实现导航工具 NavTools"
```

---

### Task 5: 问答工具（QATools）

**Files:**
- Create: `src/main/java/io/agentscope/poc/tool/QATools.java`
- Create: `src/test/java/io/agentscope/poc/tool/QAToolsTest.java`

**Step 1: 先写失败的测试**

创建 `src/test/java/io/agentscope/poc/tool/QAToolsTest.java`：
```java
package io.agentscope.poc.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QAToolsTest {

    private QATools tools;

    @BeforeEach
    void setUp() {
        tools = new QATools();
    }

    @Test
    void shouldReturnVehicleKnowledgeTemplate() {
        String result = tools.queryVehicleKnowledge("如何保养发动机");
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    void shouldReturnGeneralAnswerTemplate() {
        String result = tools.answerGeneralQuestion("今天天气怎么样");
        assertNotNull(result);
        assertFalse(result.isBlank());
    }
}
```

**Step 2: 运行测试，确认失败**

```bash
mvn test -Dtest=QAToolsTest
```
期望：FAIL

**Step 3: 实现 QATools**

创建 `src/main/java/io/agentscope/poc/tool/QATools.java`：
```java
package io.agentscope.poc.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

public class QATools {

    @Tool(name = "query_vehicle_knowledge",
          description = "查询长安汽车车型、功能、保养等专属知识")
    public String queryVehicleKnowledge(
            @ToolParam(name = "question", description = "关于长安汽车的问题") String question) {
        // 实际场景接入 RAG / 知识库 API
        // 此处返回占位符，供 Agent 基于 Skill 知识库补充回答
        return "【车型知识查询】问题：" + question + "。请参考 changan_knowledge Skill 中的参考资料作答。";
    }

    @Tool(name = "answer_general_question",
          description = "回答通用问题：天气、百科、计算等")
    public String answerGeneralQuestion(
            @ToolParam(name = "question", description = "通用问题") String question) {
        // 实际场景接入天气 API、搜索引擎等外部服务
        return "【通用问答】问题：" + question + "。请基于自身知识直接回答。";
    }
}
```

**Step 4: 运行测试，确认通过**

```bash
mvn test -Dtest=QAToolsTest
```
期望：BUILD SUCCESS, Tests run: 2, Failures: 0

**Step 5: 提交**

```bash
git add src/main/java/io/agentscope/poc/tool/QATools.java src/test/java/io/agentscope/poc/tool/QAToolsTest.java
git commit -m "feat: 实现问答工具 QATools"
```

---

### Task 6: Skill 资源文件

**Files:**
- Create: `src/main/resources/skills/vehicle_control/SKILL.md`
- Create: `src/main/resources/skills/vehicle_control/references/command-spec.md`
- Create: `src/main/resources/skills/vehicle_control/references/param-ranges.md`
- Create: `src/main/resources/skills/music_playback/SKILL.md`
- Create: `src/main/resources/skills/navigation_planning/SKILL.md`
- Create: `src/main/resources/skills/changan_knowledge/SKILL.md`
- Create: `src/main/resources/skills/changan_knowledge/references/faq.md`
- Create: `src/main/resources/skills/general_qa/SKILL.md`

**Step 1: 创建 vehicle_control Skill**

创建 `src/main/resources/skills/vehicle_control/SKILL.md`：
```markdown
---
name: vehicle_control
description: 当用户需要控制车辆设备时使用，包括空调、车窗、座椅、车灯、车门等操作
---
# 车控技能

## 职责
你是小安的车控模块，将用户语音指令转换为标准车控 JSON 指令。

## 输出规范
- tts 文本使用简短口语，不超过20字，避免标点符号
- 参数值必须在合法范围内（见 references/param-ranges.md）
- action 必须是合法值（见 references/command-spec.md）

## 可用资源
- references/command-spec.md：所有 action 的合法值
- references/param-ranges.md：各参数的取值范围
```

创建 `src/main/resources/skills/vehicle_control/references/command-spec.md`：
```markdown
# 车控指令规范

## control_air_conditioner
action 合法值：
- set_temperature：设置温度
- set_fan：设置风速
- turn_on：开启空调
- turn_off：关闭空调

## control_window
position 合法值：front_left / front_right / rear_left / rear_right / all
action 合法值：open / close / set

## control_seat
target 合法值：driver / passenger / rear_left / rear_right
feature 合法值：heat / ventilate / position
action 合法值：on / off / level_1 / level_2 / level_3

## control_light
light_type 合法值：high_beam / low_beam / ambient / hazard
action 合法值：on / off

## control_door
action 合法值：lock / unlock
```

创建 `src/main/resources/skills/vehicle_control/references/param-ranges.md`：
```markdown
# 参数取值范围

| 参数 | 范围 | 说明 |
|------|------|------|
| temperature | 16 ~ 30 | 摄氏度 |
| fan_level | 1 ~ 5 | 风速档位 |
| percentage | 0 ~ 100 | 车窗开启幅度 % |
```

**Step 2: 创建其余 Skill 文件**

创建 `src/main/resources/skills/music_playback/SKILL.md`：
```markdown
---
name: music_playback
description: 当用户需要播放音乐、控制播放、调节音量或切换播放模式时使用
---
# 音乐播放技能

## 职责
你是小安的音乐模块，处理一切音乐相关的用户指令。

## 输出规范
- tts 文本不超过20字，口语化
- 音量值范围：0 ~ 100
- 播放模式：shuffle（随机）/ repeat_one（单曲循环）/ sequence（顺序）
```

创建 `src/main/resources/skills/navigation_planning/SKILL.md`：
```markdown
---
name: navigation_planning
description: 当用户需要导航、规划路线、添加途经点、修改目的地或搜索途中 POI 时使用
---
# 导航技能

## 职责
你是小安的导航模块，处理一切导航相关的用户指令。

## 输出规范
- tts 文本不超过20字，口语化
- 路线偏好：fastest（最快）/ shortest（最短）/ avoid_toll（避收费）/ avoid_highway（避高速）
- POI 类型：gas_station / charging_station / parking / service_area
```

创建 `src/main/resources/skills/changan_knowledge/SKILL.md`：
```markdown
---
name: changan_knowledge
description: 当用户询问长安汽车相关问题时使用，包括车型介绍、功能说明、保养建议、故障排查等
---
# 长安车型知识技能

## 职责
你是小安的知识模块，回答用户关于长安汽车的专属问题。

## 回答要求
- 优先调用 query_vehicle_knowledge 工具检索知识库
- 如知识库无结果，基于长安汽车公开信息作答
- tts 回复简洁，详细内容可分段播报

## 可用资源
- references/faq.md：常见问题解答
```

创建 `src/main/resources/skills/changan_knowledge/references/faq.md`：
```markdown
# 长安汽车常见问题

## 保养
- 首保：5000km 或 6个月
- 常规保养：10000km 或 12个月
- 保养项目：机油、机滤、空调滤芯、空气滤芯

## 车型系列
- 长安 CS 系列：CS35、CS55、CS75、CS85（SUV）
- 长安 UNI 系列：UNI-T、UNI-V、UNI-K（高端系列）
- 长安 启源 系列：新能源车型
- 深蓝 系列：深蓝 SL03、深蓝 S7（新能源）

## 紧急情况
- 爆胎：保持方向盘稳定，缓慢减速靠边
- 刹车失灵：连续踩踏刹车，挂低挡，拉手刹
```

创建 `src/main/resources/skills/general_qa/SKILL.md`：
```markdown
---
name: general_qa
description: 当用户询问天气、百科知识、数学计算、时事等通用问题时使用
---
# 通用问答技能

## 职责
你是小安的通用知识模块，回答用户的日常问题。

## 回答要求
- 调用 answer_general_question 工具获取答案
- 回答简洁，适合语音播报
- 不确定的信息请说明不确定性
```

**Step 3: 确认文件结构**

```bash
find src/main/resources/skills -type f | sort
```

期望输出：
```
src/main/resources/skills/changan_knowledge/SKILL.md
src/main/resources/skills/changan_knowledge/references/faq.md
src/main/resources/skills/general_qa/SKILL.md
src/main/resources/skills/music_playback/SKILL.md
src/main/resources/skills/navigation_planning/SKILL.md
src/main/resources/skills/vehicle_control/SKILL.md
src/main/resources/skills/vehicle_control/references/command-spec.md
src/main/resources/skills/vehicle_control/references/param-ranges.md
```

**Step 4: 提交**

```bash
git add src/main/resources/skills/
git commit -m "feat: 添加五个领域 Skill 资源文件"
```

---

### Task 7: 各专家 Agent 工厂类

**Files:**
- Create: `src/main/java/io/agentscope/poc/agent/VehicleAgent.java`
- Create: `src/main/java/io/agentscope/poc/agent/MusicAgent.java`
- Create: `src/main/java/io/agentscope/poc/agent/NavAgent.java`
- Create: `src/main/java/io/agentscope/poc/agent/QAAgent.java`

> 注意：专家 Agent 依赖 AgentScope 框架，不做单元测试，在 Task 9 的集成测试中验证。

**Step 1: 创建 VehicleAgent.java**

创建 `src/main/java/io/agentscope/poc/agent/VehicleAgent.java`：
```java
package io.agentscope.poc.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.poc.tool.VehicleTools;

public class VehicleAgent {

    public static ReActAgent build(DashScopeChatModel model,
                                   ClasspathSkillRepository skillRepo) {
        Toolkit toolkit = new Toolkit();
        SkillBox skillBox = new SkillBox(toolkit);

        AgentSkill skill = skillRepo.getSkill("vehicle_control");
        skillBox.registration()
                .skill(skill)
                .tool(new AgentTool(new VehicleTools()))
                .apply();

        return ReActAgent.builder()
                .name("小安·车控")
                .sysPrompt("""
                        你是小安的车控模块，隶属于长安汽车。
                        负责将用户语音指令转换为标准车控 JSON 指令。
                        输出 tts 字段不超过20字，使用口语，不带标点。
                        """)
                .model(model)
                .skillBox(skillBox)
                .memory(new InMemoryMemory())
                .build();
    }
}
```

**Step 2: 创建 MusicAgent.java**

创建 `src/main/java/io/agentscope/poc/agent/MusicAgent.java`：
```java
package io.agentscope.poc.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.poc.tool.MusicTools;

public class MusicAgent {

    public static ReActAgent build(DashScopeChatModel model,
                                   ClasspathSkillRepository skillRepo) {
        Toolkit toolkit = new Toolkit();
        SkillBox skillBox = new SkillBox(toolkit);

        AgentSkill skill = skillRepo.getSkill("music_playback");
        skillBox.registration()
                .skill(skill)
                .tool(new AgentTool(new MusicTools()))
                .apply();

        return ReActAgent.builder()
                .name("小安·音乐")
                .sysPrompt("""
                        你是小安的音乐模块，隶属于长安汽车。
                        负责处理音乐播放、控制和音量调节等指令。
                        输出 tts 字段不超过20字，使用口语，不带标点。
                        """)
                .model(model)
                .skillBox(skillBox)
                .memory(new InMemoryMemory())
                .build();
    }
}
```

**Step 3: 创建 NavAgent.java**

创建 `src/main/java/io/agentscope/poc/agent/NavAgent.java`：
```java
package io.agentscope.poc.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.poc.tool.NavTools;

public class NavAgent {

    public static ReActAgent build(DashScopeChatModel model,
                                   ClasspathSkillRepository skillRepo) {
        Toolkit toolkit = new Toolkit();
        SkillBox skillBox = new SkillBox(toolkit);

        AgentSkill skill = skillRepo.getSkill("navigation_planning");
        skillBox.registration()
                .skill(skill)
                .tool(new AgentTool(new NavTools()))
                .apply();

        return ReActAgent.builder()
                .name("小安·导航")
                .sysPrompt("""
                        你是小安的导航模块，隶属于长安汽车。
                        负责路线规划、途经点添加和目的地修改等指令。
                        输出 tts 字段不超过20字，使用口语，不带标点。
                        """)
                .model(model)
                .skillBox(skillBox)
                .memory(new InMemoryMemory())
                .build();
    }
}
```

**Step 4: 创建 QAAgent.java**

创建 `src/main/java/io/agentscope/poc/agent/QAAgent.java`：
```java
package io.agentscope.poc.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.poc.tool.QATools;

public class QAAgent {

    public static ReActAgent build(DashScopeChatModel model,
                                   ClasspathSkillRepository skillRepo) {
        Toolkit toolkit = new Toolkit();
        SkillBox skillBox = new SkillBox(toolkit);

        QATools qaTools = new QATools();
        skillBox.registration()
                .skill(skillRepo.getSkill("changan_knowledge"))
                .tool(new AgentTool(qaTools))
                .apply();
        skillBox.registration()
                .skill(skillRepo.getSkill("general_qa"))
                .tool(new AgentTool(qaTools))
                .apply();

        return ReActAgent.builder()
                .name("小安·问答")
                .sysPrompt("""
                        你是小安的知识模块，隶属于长安汽车。
                        负责回答长安车型相关问题和通用知识问答。
                        回答简洁，适合语音播报，tts 不超过50字。
                        """)
                .model(model)
                .skillBox(skillBox)
                .memory(new InMemoryMemory())
                .build();
    }
}
```

**Step 5: 编译确认**

```bash
mvn compile
```
期望：BUILD SUCCESS

**Step 6: 提交**

```bash
git add src/main/java/io/agentscope/poc/agent/
git commit -m "feat: 实现四个领域专家 Agent 工厂类"
```

---

### Task 8: RouterAgentFactory（小安路由 Agent）

**Files:**
- Create: `src/main/java/io/agentscope/poc/router/RouterAgentFactory.java`
- Create: `src/main/java/io/agentscope/poc/config/ModelConfig.java`

**Step 1: 创建 ModelConfig.java**

创建 `src/main/java/io/agentscope/poc/config/ModelConfig.java`：
```java
package io.agentscope.poc.config;

import io.agentscope.core.model.DashScopeChatModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ModelConfig {

    public static DashScopeChatModel defaultModel() {
        return DashScopeChatModel.builder()
                .apiKey(loadApiKey())
                .modelName("qwen-plus")
                .build();
    }

    public static String loadApiKey() {
        File configFile = new File("config.properties");
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(in);
                String key = props.getProperty("dashscope.api.key");
                if (key != null && !key.isBlank()) return key;
            } catch (IOException e) {
                System.err.println("读取 config.properties 失败: " + e.getMessage());
            }
        }
        return System.getenv("DASHSCOPE_API_KEY");
    }
}
```

**Step 2: 创建 RouterAgentFactory.java**

创建 `src/main/java/io/agentscope/poc/router/RouterAgentFactory.java`：
```java
package io.agentscope.poc.router;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.session.JsonSession;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.tool.SubAgentConfig;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.poc.agent.MusicAgent;
import io.agentscope.poc.agent.NavAgent;
import io.agentscope.poc.agent.QAAgent;
import io.agentscope.poc.agent.VehicleAgent;

import java.nio.file.Path;
import java.util.function.Supplier;

public class RouterAgentFactory {

    private static final String SYS_PROMPT = """
            你是小安，长安汽车的专属智能车载助手，专业、亲切、有温度。
            你负责理解用户的语音指令，调用对应专家能力完成任务。
            - 车辆控制（空调、车窗、座椅、车灯、车门）→ call_vehicle_agent
            - 音乐播放与控制 → call_music_agent
            - 导航与路线规划 → call_nav_agent
            - 知识问答 → call_qa_agent
            支持同时处理多个领域的指令。
            最终 tts 回复使用简短口语，不超过20字，避免复杂标点。
            """;

    public static ReActAgent build(DashScopeChatModel model, String userId) {
        try (ClasspathSkillRepository skillRepo = new ClasspathSkillRepository("skills")) {
            Toolkit toolkit = new Toolkit();

            registerExpert(toolkit, "call_vehicle_agent",
                    "控制车辆设备：空调、车窗、座椅、车灯、车门",
                    () -> VehicleAgent.build(model, skillRepo), userId, "vehicle");

            registerExpert(toolkit, "call_music_agent",
                    "播放和控制音乐、调节音量、切换播放模式",
                    () -> MusicAgent.build(model, skillRepo), userId, "music");

            registerExpert(toolkit, "call_nav_agent",
                    "导航路线规划、添加途经点、修改目的地、搜索途中POI",
                    () -> NavAgent.build(model, skillRepo), userId, "nav");

            registerExpert(toolkit, "call_qa_agent",
                    "长安汽车车型问答及通用知识问答",
                    () -> QAAgent.build(model, skillRepo), userId, "qa");

            AutoContextMemory memory = new AutoContextMemory(
                    AutoContextConfig.builder()
                            .msgThreshold(30)
                            .lastKeep(10)
                            .tokenRatio(0.3)
                            .build(),
                    model);

            return ReActAgent.builder()
                    .name("小安")
                    .sysPrompt(SYS_PROMPT)
                    .model(model)
                    .memory(memory)
                    .toolkit(toolkit)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("初始化小安 RouterAgent 失败", e);
        }
    }

    private static void registerExpert(Toolkit toolkit, String toolName,
            String description, Supplier<ReActAgent> factory,
            String userId, String domain) {
        SubAgentConfig config = SubAgentConfig.builder()
                .toolName(toolName)
                .description(description)
                .session(new JsonSession(Path.of("./sessions")))
                .build();
        // session_id 在实际调用时由框架按 userId_domain 命名
        toolkit.registration()
                .subAgent(factory::get, config)
                .apply();
    }
}
```

**Step 3: 编译确认**

```bash
mvn compile
```
期望：BUILD SUCCESS

**Step 4: 提交**

```bash
git add src/main/java/io/agentscope/poc/router/ src/main/java/io/agentscope/poc/config/
git commit -m "feat: 实现小安 RouterAgentFactory 和 ModelConfig"
```

---

### Task 9: 主入口 & 端到端验证

**Files:**
- Create: `src/main/java/io/agentscope/poc/XiaoAnApplication.java`
- Modify: `pom.xml`（更新 mainClass）

**Step 1: 创建主入口**

创建 `src/main/java/io/agentscope/poc/XiaoAnApplication.java`：
```java
package io.agentscope.poc;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.poc.config.ModelConfig;
import io.agentscope.poc.router.RouterAgentFactory;
import reactor.core.scheduler.Schedulers;

public class XiaoAnApplication {

    public static void main(String[] args) {
        String apiKey = ModelConfig.loadApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "请配置 DASHSCOPE_API_KEY：\n" +
                "  1. 在 config.properties 中设置 dashscope.api.key\n" +
                "  2. 或设置环境变量 DASHSCOPE_API_KEY"
            );
        }

        String userId = "user_001";
        ReActAgent xiaoAn = RouterAgentFactory.build(ModelConfig.defaultModel(), userId);

        // 测试场景 1：车控
        sendAndPrint(xiaoAn, "把空调调到22度");

        // 测试场景 2：域内追问
        sendAndPrint(xiaoAn, "再低2度");

        // 测试场景 3：音乐
        sendAndPrint(xiaoAn, "播放一首周杰伦的歌");

        // 测试场景 4：跨域
        sendAndPrint(xiaoAn, "同时打开车窗，并且导航去重庆解放碑");

        // 测试场景 5：知识问答
        sendAndPrint(xiaoAn, "长安CS75多久保养一次");

        Schedulers.shutdownNow();
    }

    private static void sendAndPrint(ReActAgent agent, String userInput) {
        System.out.println("\n用户: " + userInput);
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent(userInput)
                .build();
        Msg response = agent.call(msg).block();
        System.out.println("小安: " + response.getTextContent());
    }
}
```

**Step 2: 更新 pom.xml 的 mainClass**

修改 `pom.xml` 中 exec-maven-plugin 的 mainClass：
```xml
<mainClass>io.agentscope.poc.XiaoAnApplication</mainClass>
```

**Step 3: 编译**

```bash
mvn compile
```
期望：BUILD SUCCESS

**Step 4: 运行所有单元测试**

```bash
mvn test
```
期望：BUILD SUCCESS，所有 Tool 测试通过（VehicleToolsTest、MusicToolsTest、NavToolsTest、QAToolsTest、VoiceResponseTest）

**Step 5: 端到端运行（需要有效的 API Key）**

```bash
mvn exec:java
```
期望：小安依次响应 5 条测试指令，输出合理的中文回复

**Step 6: 提交**

```bash
git add src/main/java/io/agentscope/poc/XiaoAnApplication.java pom.xml
git commit -m "feat: 添加小安主入口，完成端到端集成"
```

---

## 完成标准

- [ ] `mvn test` 全部通过（5 个测试类，12+ 个测试用例）
- [ ] `mvn compile` 无报错
- [ ] `mvn exec:java` 能正常启动并响应车控/音乐/导航/问答四类指令
- [ ] 所有专家 Agent 均通过 Skill 加载工具（渐进式披露）
- [ ] RouterAgent 持有 AutoContextMemory，支持跨域多轮对话
- [ ] sessions/ 目录下生成各专家 Agent 的 JSON 会话文件
