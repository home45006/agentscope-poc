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
