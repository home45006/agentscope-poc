package io.agentscope.poc.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.poc.hook.CommandRegistry;
import io.agentscope.poc.model.CarCommand;

import java.util.HashMap;
import java.util.Map;

/**
 * 车控工具类。
 * 提供空调、车窗、座椅、车灯、车门五个控制方法。
 */
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

        // 注册指令到 CommandRegistry
        Map<String, Object> command = new HashMap<>();
        command.put("domain", "vehicle");
        command.put("action", "control_ac");
        command.put("params", params);
        command.put("tts", cmd.tts);
        CommandRegistry.register(command);

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
