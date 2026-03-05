package io.agentscope.poc;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;

public class HelloAgentScope {

    public static void main(String[] args) {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请设置环境变量 DASHSCOPE_API_KEY");
        }

        // 注册工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new GreetingTools());

        // 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("Jarvis")
                .sysPrompt("你是一个名叫 Jarvis 的助手，请用中文回答。")
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("qwen-plus")
                        .build())
                .toolkit(toolkit)
                .build();

        // 发送消息
        Msg msg = Msg.builder()
                .textContent("你好，请问现在几点了？")
                .build();

        Msg response = agent.call(msg).block();
        System.out.println("Jarvis: " + response.getTextContent());
    }
}

class GreetingTools {

    @Tool(name = "get_current_time", description = "获取当前时间")
    public String getCurrentTime(
            @ToolParam(name = "zone", description = "时区，例如 Asia/Shanghai") String zone) {
        java.time.ZoneId zoneId;
        try {
            zoneId = java.time.ZoneId.of(zone);
        } catch (Exception e) {
            zoneId = java.time.ZoneId.of("Asia/Shanghai");
        }
        return java.time.ZonedDateTime.now(zoneId)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
    }

    @Tool(name = "say_hello", description = "向用户打招呼")
    public String sayHello(
            @ToolParam(name = "name", description = "用户名字") String name) {
        return "Hello, " + name + "! 欢迎使用 AgentScope Java！";
    }
}
