package io.agentscope.poc.model;

import java.util.Map;

/**
 * 音乐指令 DTO。
 * 用于音乐专家 Agent 输出标准化指令。
 */
public class MusicCommand {
    public String action;
    public Map<String, Object> params;
    public String tts;
}
