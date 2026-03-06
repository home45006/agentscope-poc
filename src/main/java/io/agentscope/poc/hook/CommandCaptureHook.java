package io.agentscope.poc.hook;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.message.TextBlock;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 捕获 Agent 执行工具后返回的结构化指令。
 * 用于在交互模式中展示下发到端侧的 JSON 指令。
 */
public class CommandCaptureHook implements Hook {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 使用 ThreadLocal 存储当前会话的指令列表
    private static final ThreadLocal<List<Map<String, Object>>> COMMANDS =
            ThreadLocal.withInitial(ArrayList::new);

    @Override
    @SuppressWarnings("unchecked")
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PostActingEvent e) {
            // 获取工具名称
            String toolName = e.getToolUse().getName();

            // 从工具返回结果中提取 JSON
            String resultText = e.getToolResult().getOutput().stream()
                    .filter(block -> block instanceof TextBlock)
                    .map(block -> ((TextBlock) block).getText())
                    .findFirst()
                    .orElse("");

            if (!resultText.isBlank()) {
                try {
                    // 尝试解析为 JSON
                    Map<String, Object> command = MAPPER.readValue(resultText, Map.class);
                    if (isValidCommand(command)) {
                        COMMANDS.get().add(command);
                    }
                } catch (Exception ex) {
                    // 不是 JSON 格式，忽略
                }
            }
        }
        return Mono.just(event);
    }

    /**
     * 判断是否为有效的指令结构
     */
    private boolean isValidCommand(Map<String, Object> command) {
        return command.containsKey("action") ||
               command.containsKey("domain") ||
               command.containsKey("tts");
    }

    /**
     * 获取当前会话捕获的所有指令
     */
    public static List<Map<String, Object>> getCapturedCommands() {
        return new ArrayList<>(COMMANDS.get());
    }

    /**
     * 清空当前会话的指令缓存
     */
    public static void clearCommands() {
        COMMANDS.get().clear();
    }

    /**
     * 格式化指令为可读字符串
     */
    public static String formatCommands() {
        List<Map<String, Object>> commands = getCapturedCommands();
        if (commands.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n┌─────────── 下发指令 ──────────┐\n");
        for (int i = 0; i < commands.size(); i++) {
            Map<String, Object> cmd = commands.get(i);
            sb.append("│ [").append(i + 1).append("] ");
            sb.append(formatCommand(cmd));
            sb.append("\n");
        }
        sb.append("└───────────────────────────────────┘");
        return sb.toString();
    }

    private static String formatCommand(Map<String, Object> cmd) {
        String domain = String.valueOf(cmd.getOrDefault("domain", "unknown"));
        String action = String.valueOf(cmd.getOrDefault("action", "unknown"));
        Map<String, Object> params = (Map<String, Object>) cmd.get("params");
        String paramsStr = params != null && !params.isEmpty() ? params.toString() : "";
        return domain + "." + action + (paramsStr.isEmpty() ? "" : " " + paramsStr);
    }
}
