package io.agentscope.poc.model;

import java.util.Map;

/**
 * 导航指令 DTO。
 * 用于导航专家 Agent 输出标准化指令。
 */
public class NavCommand {
    public String action;
    public Map<String, Object> params;
    public String tts;
}
