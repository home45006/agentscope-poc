package io.agentscope.poc.model;

import java.util.Map;

/**
 * 统一语音响应输出结构。
 * 用于将各领域 Agent 的处理结果标准化为 JSON + TTS 格式下发到端侧。
 */
public class VoiceResponse {
    public String domain;
    public String action;
    public Map<String, Object> params;
    public String tts;
}
