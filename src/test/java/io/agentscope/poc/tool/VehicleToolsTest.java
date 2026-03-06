package io.agentscope.poc.tool;

import io.agentscope.poc.model.CarCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VehicleTools 单元测试。
 * 覆盖车控五个核心功能：空调、车窗、座椅、车灯、车门。
 */
class VehicleToolsTest {

    private VehicleTools tools;

    @BeforeEach
    void setUp() {
        tools = new VehicleTools();
    }

    // ==================== 空调测试 ====================

    @Test
    void shouldControlAirConditionerSetTemperature() {
        CarCommand cmd = tools.controlAirConditioner("set_temperature", 22, null);
        assertEquals("control_ac", cmd.action);
        assertEquals("set_temperature", cmd.params.get("action"));
        assertEquals(22, cmd.params.get("temperature"));
        assertNotNull(cmd.tts);
        assertTrue(cmd.tts.contains("22"));
    }

    @Test
    void shouldControlAirConditionerSetFan() {
        CarCommand cmd = tools.controlAirConditioner("set_fan", null, 3);
        assertEquals("control_ac", cmd.action);
        assertEquals("set_fan", cmd.params.get("action"));
        assertEquals(3, cmd.params.get("fan_level"));
        assertNotNull(cmd.tts);
    }

    @Test
    void shouldControlAirConditionerTurnOn() {
        CarCommand cmd = tools.controlAirConditioner("turn_on", null, null);
        assertEquals("control_ac", cmd.action);
        assertEquals("turn_on", cmd.params.get("action"));
        assertNotNull(cmd.tts);
    }

    @Test
    void shouldControlAirConditionerTurnOff() {
        CarCommand cmd = tools.controlAirConditioner("turn_off", null, null);
        assertEquals("control_ac", cmd.action);
        assertEquals("turn_off", cmd.params.get("action"));
        assertNotNull(cmd.tts);
    }

    // ==================== 车窗测试 ====================

    @Test
    void shouldControlWindowOpen() {
        CarCommand cmd = tools.controlWindow("front_left", "open", 50);
        assertEquals("control_window", cmd.action);
        assertEquals("front_left", cmd.params.get("position"));
        assertEquals("open", cmd.params.get("action"));
        assertEquals(50, cmd.params.get("percentage"));
        assertNotNull(cmd.tts);
    }

    @Test
    void shouldControlWindowClose() {
        CarCommand cmd = tools.controlWindow("all", "close", null);
        assertEquals("control_window", cmd.action);
        assertEquals("all", cmd.params.get("position"));
        assertEquals("close", cmd.params.get("action"));
        assertNull(cmd.params.get("percentage"));
    }

    // ==================== 座椅测试 ====================

    @Test
    void shouldControlSeatHeat() {
        CarCommand cmd = tools.controlSeat("driver", "heat", "level_2");
        assertEquals("control_seat", cmd.action);
        assertEquals("driver", cmd.params.get("target"));
        assertEquals("heat", cmd.params.get("feature"));
        assertEquals("level_2", cmd.params.get("action"));
        assertNotNull(cmd.tts);
    }

    @Test
    void shouldControlSeatVentilate() {
        CarCommand cmd = tools.controlSeat("passenger", "ventilate", "on");
        assertEquals("control_seat", cmd.action);
        assertEquals("passenger", cmd.params.get("target"));
        assertEquals("ventilate", cmd.params.get("feature"));
        assertEquals("on", cmd.params.get("action"));
    }

    // ==================== 车灯测试 ====================

    @Test
    void shouldControlLightOn() {
        CarCommand cmd = tools.controlLight("high_beam", "on");
        assertEquals("control_light", cmd.action);
        assertEquals("high_beam", cmd.params.get("light_type"));
        assertEquals("on", cmd.params.get("action"));
        assertNotNull(cmd.tts);
    }

    @Test
    void shouldControlLightOff() {
        CarCommand cmd = tools.controlLight("ambient", "off");
        assertEquals("control_light", cmd.action);
        assertEquals("ambient", cmd.params.get("light_type"));
        assertEquals("off", cmd.params.get("action"));
    }

    // ==================== 车门测试 ====================

    @Test
    void shouldControlDoorLock() {
        CarCommand cmd = tools.controlDoor("lock");
        assertEquals("control_door", cmd.action);
        assertEquals("lock", cmd.params.get("action"));
        assertNotNull(cmd.tts);
    }

    @Test
    void shouldControlDoorUnlock() {
        CarCommand cmd = tools.controlDoor("unlock");
        assertEquals("control_door", cmd.action);
        assertEquals("unlock", cmd.params.get("action"));
        assertNotNull(cmd.tts);
    }
}
