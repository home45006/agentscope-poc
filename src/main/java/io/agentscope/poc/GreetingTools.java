package io.agentscope.poc;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * 示例工具集：提供问候与时间查询功能。
 *
 * <p>AgentScope 通过反射扫描标注了 {@link Tool} 注解的方法，将其暴露给 Agent 使用。
 * 每个工具方法的参数需用 {@link ToolParam} 注解描述，框架会将其转换为大模型可理解的函数签名（Function Calling Schema）。
 */
class GreetingTools {

    /**
     * 获取指定时区的当前时间。
     *
     * <p>当传入的时区字符串无效时，自动降级为 {@code Asia/Shanghai}（北京时间）。
     *
     * @param zone 时区标识符，遵循 IANA 时区数据库格式，例如 {@code Asia/Shanghai}、{@code America/New_York}
     * @return 格式为 {@code yyyy-MM-dd HH:mm:ss z} 的当前时间字符串
     */
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

    /**
     * 向指定用户打招呼。
     *
     * @param name 用户名字
     * @return 包含用户名的欢迎语
     */
    @Tool(name = "say_hello", description = "向用户打招呼")
    public String sayHello(
            @ToolParam(name = "name", description = "用户名字") String name) {
        return "Hello, " + name + "! 欢迎使用 AgentScope Java！";
    }
}
