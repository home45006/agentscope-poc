package io.agentscope.poc.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VehicleTools 单元测试。
 * 覆盖车控五个核心功能：空调、车窗、座椅、车灯、车门。
 *
 * 注意：Tool 方法返回 JSON 字符串（用于 CommandCaptureHook 捕获），
 * 测试需解析 JSON 进行断言。
 */
class VehicleToolsTest {

    private VehicleTools tools;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        tools = new VehicleTools();
        mapper = new ObjectMapper();
    }

    // ==================== 空调测试 ====================

    @Test
    void shouldControlAirConditionerSetTemperature() throws Exception {
        String json = tools.controlAirConditioner("set_temperature", 22, null);
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("vehicle", cmd.get("domain"));
        assertEquals("control_ac", cmd.get("action"));
        assertEquals("set_temperature", ((Map<?, ?>) cmd.get("params")).get("action"));
        assertEquals(22, ((Map<?, ?>) cmd.get("params")).get("temperature"));
        assertNotNull(cmd.get("tts"));
        assertTrue(cmd.get("tts").toString().contains("22"));
    }

    @Test
    void shouldControlAirConditionerSetFan() throws Exception {
        String json = tools.controlAirConditioner("set_fan", null, 3);
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("control_ac", cmd.get("action"));
        assertEquals("set_fan", ((Map<?, ?>) cmd.get("params")).get("action"));
        assertEquals(3, ((Map<?, ?>) cmd.get("params")).get("fan_level"));
        assertNotNull(cmd.get("tts"));
    }

    @Test
    void shouldControlAirConditionerTurnOn() throws Exception {
        String json = tools.controlAirConditioner("turn_on", null, null);
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("control_ac", cmd.get("action"));
        assertEquals("turn_on", ((Map<?, ?>) cmd.get("params")).get("action"));
        assertNotNull(cmd.get("tts"));
    }

    @Test
    void shouldControlAirConditionerTurnOff() throws Exception {
        String json = tools.controlAirConditioner("turn_off", null, null);
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("control_ac", cmd.get("action"));
        assertEquals("turn_off", ((Map<?, ?>) cmd.get("params")).get("action"));
        assertNotNull(cmd.get("tts"));
    }

    // ==================== 车窗测试 ====================

    @Test
    void shouldControlWindowOpen() throws Exception {
        String json = tools.controlWindow("front_left", "open", 50);
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("control_window", cmd.get("action"));
        assertEquals("front_left", ((Map<?, ?>) cmd.get("params")).get("position"));
        assertEquals("open", ((Map<?, ?>) cmd.get("params")).get("action"));
        assertEquals(50, ((Map<?, ?>) cmd.get("params")).get("percentage"));
        assertNotNull(cmd.get("tts"));
    }

    @Test
    void shouldControlWindowClose() throws Exception {
        String json = tools.controlWindow("all", "close", null);
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("control_window", cmd.get("action"));
        assertEquals("all", ((Map<?, ?>) cmd.get("params")).get("position"));
        assertEquals("close", ((Map<?, ?>) cmd.get("params")).get("action"));
        assertNull(((Map<?, ?>) cmd.get("params")).get("percentage"));
    }

    // ==================== 座椅测试 ====================

    @Test
    void shouldControlSeatHeat() throws Exception {
        String json = tools.controlSeat("driver", "heat", "level_2");
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("control_seat", cmd.get("action"));
        assertEquals("driver", ((Map<?, ?>) cmd.get("params")).get("target"));
        assertEquals("heat", ((Map<?, ?>) cmd.get("params")).get("feature"));
        assertEquals("level_2", ((Map<?, ?>) cmd.get("params")).get("action"));
        assertNotNull(cmd.get("tts"));
    }

    @Test
    void shouldControlSeatVentilate() throws Exception {
        String json = tools.controlSeat("passenger", "ventilate", "on");
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("control_seat", cmd.get("action"));
        assertEquals("passenger", ((Map<?, ?>) cmd.get("params")).get("target"));
        assertEquals("ventilate", ((Map<?, ?>) cmd.get("params")).get("feature"));
        assertEquals("on", ((Map<?, ?>) cmd.get("params")).get("action"));
    }

    // ==================== 车灯测试 ====================

    @Test
    void shouldControlLightOn() throws Exception {
        String json = tools.controlLight("high_beam", "on");
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("control_light", cmd.get("action"));
        assertEquals("high_beam", ((Map<?, ?>) cmd.get("params")).get("light_type"));
        assertEquals("on", ((Map<?, ?>) cmd.get("params")).get("action"));
        assertNotNull(cmd.get("tts"));
    }

    @Test
    void shouldControlLightOff() throws Exception {
        String json = tools.controlLight("ambient", "off");
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("control_light", cmd.get("action"));
        assertEquals("ambient", ((Map<?, ?>) cmd.get("params")).get("light_type"));
        assertEquals("off", ((Map<?, ?>) cmd.get("params")).get("action"));
    }

    // ==================== 车门测试 ====================

    @Test
    void shouldControlDoorLock() throws Exception {
        String json = tools.controlDoor("lock");
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("control_door", cmd.get("action"));
        assertEquals("lock", ((Map<?, ?>) cmd.get("params")).get("action"));
        assertNotNull(cmd.get("tts"));
    }

    @Test
    void shouldControlDoorUnlock() throws Exception {
        String json = tools.controlDoor("unlock");
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("control_door", cmd.get("action"));
        assertEquals("unlock", ((Map<?, ?>) cmd.get("params")).get("action"));
        assertNotNull(cmd.get("tts"));
    }
}
