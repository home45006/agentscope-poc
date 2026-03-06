package io.agentscope.poc.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MusicTools 单元测试。
 *
 * 注意：Tool 方法返回 JSON 字符串（用于 CommandCaptureHook 捕获），
 * 测试需解析 JSON 进行断言。
 */
class MusicToolsTest {

    private MusicTools tools;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        tools = new MusicTools();
        mapper = new ObjectMapper();
    }

    @Test
    void shouldPlayMusic() throws Exception {
        String json = tools.playMusic("周杰伦", "artist");
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("music", cmd.get("domain"));
        assertEquals("play_music", cmd.get("action"));
        assertEquals("周杰伦", ((Map<?, ?>) cmd.get("params")).get("query"));
        assertEquals("artist", ((Map<?, ?>) cmd.get("params")).get("query_type"));
        assertNotNull(cmd.get("tts"));
        assertTrue(cmd.get("tts").toString().length() <= 20);
    }

    @Test
    void shouldControlPlayback() throws Exception {
        String json = tools.controlPlayback("next");
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("control_playback", cmd.get("action"));
        assertEquals("next", ((Map<?, ?>) cmd.get("params")).get("action"));
        assertNotNull(cmd.get("tts"));
        assertTrue(cmd.get("tts").toString().length() <= 20);
    }

    @Test
    void shouldAdjustVolume() throws Exception {
        String json = tools.adjustVolume("set", 50);
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("adjust_volume", cmd.get("action"));
        assertEquals("set", ((Map<?, ?>) cmd.get("params")).get("action"));
        assertEquals(50, ((Map<?, ?>) cmd.get("params")).get("value"));
        assertNotNull(cmd.get("tts"));
        assertTrue(cmd.get("tts").toString().length() <= 20);
    }

    @Test
    void shouldSetPlayMode() throws Exception {
        String json = tools.setPlayMode("shuffle");
        Map<String, Object> cmd = mapper.readValue(json, Map.class);

        assertEquals("set_play_mode", cmd.get("action"));
        assertEquals("shuffle", ((Map<?, ?>) cmd.get("params")).get("mode"));
        assertNotNull(cmd.get("tts"));
        assertTrue(cmd.get("tts").toString().length() <= 20);
    }
}
