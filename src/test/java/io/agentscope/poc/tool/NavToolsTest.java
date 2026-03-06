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
        assertEquals("fastest", cmd.params.get("preference"));
        assertNotNull(cmd.tts);
        assertTrue(cmd.tts.length() <= 20, "tts 不应超过20字");
    }

    @Test
    void shouldAddWaypoint() {
        NavCommand cmd = tools.addWaypoint("中石化加油站");
        assertEquals("add_waypoint", cmd.action);
        assertEquals("中石化加油站", cmd.params.get("waypoint"));
        assertNotNull(cmd.tts);
        assertTrue(cmd.tts.length() <= 20, "tts 不应超过20字");
    }

    @Test
    void shouldChangeDestination() {
        NavCommand cmd = tools.changeDestination("成都天府广场");
        assertEquals("change_destination", cmd.action);
        assertEquals("成都天府广场", cmd.params.get("destination"));
        assertNotNull(cmd.tts);
        assertTrue(cmd.tts.length() <= 20, "tts 不应超过20字");
    }

    @Test
    void shouldSearchAlongRoute() {
        NavCommand cmd = tools.searchAlongRoute("gas_station");
        assertEquals("search_along_route", cmd.action);
        assertEquals("gas_station", cmd.params.get("poi_type"));
        assertNotNull(cmd.tts);
        assertTrue(cmd.tts.length() <= 20, "tts 不应超过20字");
    }

    @Test
    void shouldCancelNavigation() {
        NavCommand cmd = tools.cancelNavigation();
        assertEquals("cancel_navigation", cmd.action);
        assertNotNull(cmd.tts);
        assertTrue(cmd.tts.length() <= 20, "tts 不应超过20字");
    }
}
