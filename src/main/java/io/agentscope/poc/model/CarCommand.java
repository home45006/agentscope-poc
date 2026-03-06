package io.agentscope.poc.model;

import java.util.Map;

/**
 * 车控指令 DTO。
 * 用于车控专家 Agent 输出标准化指令。
 */
public class CarCommand {
    public String action;
    public Map<String, Object> params;
    public String tts;
}
