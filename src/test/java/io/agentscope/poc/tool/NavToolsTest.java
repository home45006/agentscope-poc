package io.agentscope.poc.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NavTools 单元测试。
 *
 * 注意：Tool 方法返回 JSON 字符串（用于 CommandCaptureHook 捕获），
 * 测试需解析 JSON 进行断言。
 */
class NavToolsTest {

    private NavTools tools;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        tools = new NavTools();
        mapper = new ObjectMapper();
    }

    @Test
    void shouldStartNavigation() throws Exception {
        String json = tools.startNavigation("重庆解放碑", "fastest");
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("navigation", cmd.get("domain"));
        assertEquals("start_navigation", cmd.get("action"));
        assertEquals("重庆解放碑", ((Map<?, ?>) cmd.get("params")).get("destination"));
        assertEquals("fastest", ((Map<?, ?>) cmd.get("params")).get("preference"));
        assertNotNull(cmd.get("tts"));
        assertTrue(cmd.get("tts").toString().length() <= 20);
    }

    @Test
    void shouldAddWaypoint() throws Exception {
        String json = tools.addWaypoint("中石化加油站");
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("add_waypoint", cmd.get("action"));
        assertEquals("中石化加油站", ((Map<?, ?>) cmd.get("params")).get("waypoint"));
        assertNotNull(cmd.get("tts"));
        assertTrue(cmd.get("tts").toString().length() <= 20);
    }

    @Test
    void shouldChangeDestination() throws Exception {
        String json = tools.changeDestination("成都天府广场");
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("change_destination", cmd.get("action"));
        assertEquals("成都天府广场", ((Map<?, ?>) cmd.get("params")).get("destination"));
        assertNotNull(cmd.get("tts"));
        assertTrue(cmd.get("tts").toString().length() <= 20);
    }

    @Test
    void shouldSearchAlongRoute() throws Exception {
        String json = tools.searchAlongRoute("gas_station");
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("search_along_route", cmd.get("action"));
        assertEquals("gas_station", ((Map<?, ?>) cmd.get("params")).get("poi_type"));
        assertNotNull(cmd.get("tts"));
        assertTrue(cmd.get("tts").toString().length() <= 20);
    }

    @Test
    void shouldCancelNavigation() throws Exception {
        String json = tools.cancelNavigation();
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("cancel_navigation", cmd.get("action"));
        assertNotNull(cmd.get("tts"));
        assertTrue(cmd.get("tts").toString().length() <= 20);
    }
}
