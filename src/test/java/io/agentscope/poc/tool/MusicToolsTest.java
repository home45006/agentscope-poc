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
        assertEquals("artist", cmd.params.get("query_type"));
        assertNotNull(cmd.tts);
        assertTrue(cmd.tts.length() <= 20, "tts文本不超过20字");
    }

    @Test
    void shouldControlPlayback() {
        MusicCommand cmd = tools.controlPlayback("next");
        assertEquals("control_playback", cmd.action);
        assertEquals("next", cmd.params.get("action"));
        assertNotNull(cmd.tts);
        assertTrue(cmd.tts.length() <= 20, "tts文本不超过20字");
    }

    @Test
    void shouldAdjustVolume() {
        MusicCommand cmd = tools.adjustVolume("set", 50);
        assertEquals("adjust_volume", cmd.action);
        assertEquals("set", cmd.params.get("action"));
        assertEquals(50, cmd.params.get("value"));
        assertNotNull(cmd.tts);
        assertTrue(cmd.tts.length() <= 20, "tts文本不超过20字");
    }

    @Test
    void shouldSetPlayMode() {
        MusicCommand cmd = tools.setPlayMode("shuffle");
        assertEquals("set_play_mode", cmd.action);
        assertEquals("shuffle", cmd.params.get("mode"));
        assertNotNull(cmd.tts);
        assertTrue(cmd.tts.length() <= 20, "tts文本不超过20字");
    }
}
