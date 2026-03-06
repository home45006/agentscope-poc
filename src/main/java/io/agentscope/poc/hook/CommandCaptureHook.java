package io.agentscope.poc.hook;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.message.TextBlock;
import io.agentscope.poc.model.CarCommand;
import io.agentscope.poc.model.MusicCommand;
import io.agentscope.poc.model.NavCommand;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
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

            // 从工具返回结果中提取结构化指令
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
                        // 添加领域信息
                        addDomainInfo(command, toolName);
                        COMMANDS.get().add(command);
                    }
                } catch (Exception ex) {
                    // 不是 JSON 格式，尝试解析为简单文本
                    if (resultText.contains("action=") || resultText.contains("tts=")) {
                        Map<String, Object> simpleCmd = parseSimpleFormat(resultText, toolName);
                        if (simpleCmd != null) {
                            COMMANDS.get().add(simpleCmd);
                        }
                    }
                }
            }
        }
        return Mono.just(event);
    }

    /**
     * 根据工具名称添加领域信息
     */
    private void addDomainInfo(Map<String, Object> command, String toolName) {
        if (command != null && !command.containsKey("domain")) {
            if (toolName != null) {
                if (toolName.contains("vehicle")) {
                    command.put("domain", "vehicle");
                } else if (toolName.contains("music")) {
                    command.put("domain", "music");
                } else if (toolName.contains("nav")) {
                    command.put("domain", "navigation");
                } else if (toolName.contains("qa")) {
                    command.put("domain", "qa");
                }
            }
        }
    }

    /**
     * 解析简单格式的输出（如 toString() 格式）
     */
    private Map<String, Object> parseSimpleFormat(String text, String toolName) {
        Map<String, Object> cmd = new HashMap<>();
        addDomainInfo(cmd, toolName);

        // 尝试从文本中提取关键信息
        if (text.contains("空调") || text.contains("温度")) {
            cmd.put("action", "control_ac");
            cmd.put("domain", "vehicle");
        } else if (text.contains("播放") || text.contains("音乐")) {
            cmd.put("action", "play_music");
            cmd.put("domain", "music");
        } else if (text.contains("导航")) {
            cmd.put("action", "start_navigation");
            cmd.put("domain", "navigation");
        }

        cmd.put("raw_output", text);
        return cmd.isEmpty() ? null : cmd;
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
