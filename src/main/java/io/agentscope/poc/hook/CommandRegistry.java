package io.agentscope.poc.hook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 指令注册中心 - 用于在工具执行时记录生成的结构化指令。
 * 使用 ConcurrentHashMap 确保多线程安全共享。
 */
public class CommandRegistry {

    // 全局指令存储，使用 sessionId 作为 key
    private static final Map<String, List<Map<String, Object>>> COMMANDS = new ConcurrentHashMap<>();

    // 当前会话 ID
    private static final ThreadLocal<String> CURRENT_SESSION = ThreadLocal.withInitial(() -> "default");

    /**
     * 设置当前会话 ID
     */
    public static void setSessionId(String sessionId) {
        CURRENT_SESSION.set(sessionId);
    }

    /**
     * 获取当前会话 ID
     */
    public static String getSessionId() {
        return CURRENT_SESSION.get();
    }

    /**
     * 注册一条指令
     */
    public static void register(Map<String, Object> command) {
        if (command != null) {
            String sessionId = CURRENT_SESSION.get();
            COMMANDS.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(command);
        }
    }

    /**
     * 获取当前会话的所有指令
     */
    public static List<Map<String, Object>> getCommands() {
        String sessionId = CURRENT_SESSION.get();
        List<Map<String, Object>> commands = COMMANDS.get(sessionId);
        return commands != null ? new ArrayList<>(commands) : new ArrayList<>();
    }

    /**
     * 清空当前会话的指令
     */
    public static void clear() {
        String sessionId = CURRENT_SESSION.get();
        COMMANDS.remove(sessionId);
    }

    /**
     * 格式化输出指令
     */
    public static String format() {
        List<Map<String, Object>> commands = getCommands();
        if (commands.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n┌─────────── 下发指令 ───────────┐\n");
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
        Object params = cmd.get("params");
        String paramsStr = params != null ? params.toString() : "";
        return domain + "." + action + (paramsStr.isEmpty() ? "" : " " + paramsStr);
    }
}
