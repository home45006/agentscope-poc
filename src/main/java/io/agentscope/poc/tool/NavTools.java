package io.agentscope.poc.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.poc.model.NavCommand;

import java.util.HashMap;
import java.util.Map;

/**
 * 导航工具类。
 * 返回 JSON 字符串格式以便捕获。
 */
public class NavTools {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Tool(name = "start_navigation", description = "规划导航路线：目的地名称或地址")
    public String startNavigation(
            @ToolParam(name = "destination", description = "目的地名称或地址") String destination,
            @ToolParam(name = "preference", description = "路线偏好：fastest/shortest/avoid_toll/avoid_highway") String preference) {
        Map<String, Object> params = new HashMap<>();
        params.put("destination", destination);
        if (preference != null) {
            params.put("preference", preference);
        }

        NavCommand cmd = new NavCommand();
        cmd.action = "start_navigation";
        cmd.params = params;
        cmd.tts = "为您导航至" + destination;

        return toJson("navigation", cmd);
    }

    @Tool(name = "add_waypoint", description = "添加途经点")
    public String addWaypoint(
            @ToolParam(name = "waypoint", description = "途经点名称或地址") String waypoint) {
        Map<String, Object> params = new HashMap<>();
        params.put("waypoint", waypoint);

        NavCommand cmd = new NavCommand();
        cmd.action = "add_waypoint";
        cmd.params = params;
        cmd.tts = "已添加途经" + waypoint;

        return toJson("navigation", cmd);
    }

    @Tool(name = "change_destination", description = "修改目的地")
    public String changeDestination(
            @ToolParam(name = "new_destination", description = "新目的地名称或地址") String newDestination) {
        Map<String, Object> params = new HashMap<>();
        params.put("destination", newDestination);

        NavCommand cmd = new NavCommand();
        cmd.action = "change_destination";
        cmd.params = params;
        cmd.tts = "已改去" + newDestination;

        return toJson("navigation", cmd);
    }

    @Tool(name = "search_along_route", description = "查询途中信息：油站/充电站/停车场/服务区")
    public String searchAlongRoute(
            @ToolParam(name = "poi_type", description = "POI类型：gas_station/charging_station/parking/service_area") String poiType) {
        Map<String, Object> params = new HashMap<>();
        params.put("poi_type", poiType);

        NavCommand cmd = new NavCommand();
        cmd.action = "search_along_route";
        cmd.params = params;
        cmd.tts = "为您搜索途中" + translatePoiType(poiType);

        return toJson("navigation", cmd);
    }

    @Tool(name = "cancel_navigation", description = "取消导航")
    public String cancelNavigation() {
        NavCommand cmd = new NavCommand();
        cmd.action = "cancel_navigation";
        cmd.params = Map.of();
        cmd.tts = "已取消导航";

        return toJson("navigation", cmd);
    }

    private String toJson(String domain, NavCommand cmd) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("domain", domain);
            result.put("action", cmd.action);
            result.put("params", cmd.params);
            result.put("tts", cmd.tts);
            return MAPPER.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
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
