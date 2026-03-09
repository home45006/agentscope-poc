package io.agentscope.poc.util;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * 应用日志工具类。
 *
 * <p>使用 java.util.logging 将日志输出到 logs/xiaoan.log。
 * 日志级别通过 config.properties 中的 log.level 配置。
 * 可选值（从低到高）: ALL | FINE | INFO | WARNING | SEVERE | OFF
 */
public class AppLogger {

    private static final Logger LOGGER = Logger.getLogger("XiaoAn");
    private static final String LOG_DIR  = "logs";
    private static final String LOG_FILE = "logs/xiaoan.log";

    static {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));

            Level level = resolveLevel();

            // 清除 root logger 的默认 ConsoleHandler，避免控制台重复输出
            // 同时将 root logger 级别降至 ALL，避免父级 level 拦截 FINE 消息
            Logger rootLogger = Logger.getLogger("");
            for (Handler h : rootLogger.getHandlers()) {
                rootLogger.removeHandler(h);
            }
            rootLogger.setLevel(Level.ALL);

            FileHandler fileHandler = new FileHandler(LOG_FILE, /* append= */ true);
            fileHandler.setLevel(Level.ALL);   // handler 不过滤，统一由 LOGGER 层控制
            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("[%1$tF %1$tT.%1$tL] [%2$-7s] %3$s%n",
                            new Date(record.getMillis()),
                            record.getLevel().getName(),
                            record.getMessage());
                }
            });

            // 不继承 root logger 的 handler，防止级别被父 logger 覆盖
            LOGGER.setUseParentHandlers(false);
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(level);   // 仅此处控制过滤级别

            // 用第一条日志自证级别已生效
            LOGGER.log(level, String.format("[系统] 日志初始化完成，级别=%s，文件=%s", level.getName(), LOG_FILE));

        } catch (Exception e) {
            System.err.println("[AppLogger] 初始化失败，日志不可用: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private AppLogger() {}

    // ──────────────────────────────────────────────────

    /** 记录用户输入 */
    public static void logUserInput(String input) {
        LOGGER.info(String.format("[用户输入] %s", input));
    }

    /** 记录 Agent 输出 */
    public static void logAgentOutput(String agentName, String output) {
        LOGGER.info(String.format("[%s 输出] %s", agentName, output));
    }

    /** 记录工具调用请求（工具名 + 入参） */
    public static void logToolCall(String toolName, String input) {
        LOGGER.fine(String.format("[工具调用] %s << %s", toolName, input));
    }

    /** 记录工具调用结果 */
    public static void logToolResult(String toolName, String result) {
        LOGGER.fine(String.format("[工具结果] %s >> %s", toolName, result));
    }

    /** 记录拒识事件 */
    public static void logRejection(String input) {
        LOGGER.warning(String.format("[拒识] 无意义输入: %s", input));
    }

    /** 记录系统事件（启动、关闭等） */
    public static void logSystem(String message) {
        LOGGER.info(String.format("[系统] %s", message));
    }

    /** 记录错误 */
    public static void logError(String message, Throwable t) {
        LOGGER.log(Level.SEVERE, String.format("[错误] %s", message), t);
    }

    // ──────────────────────────────────────────────────

    /**
     * 从 config.properties 读取 log.level，读取失败则默认 INFO。
     * 打印到 stderr 便于问题排查。
     */
    private static Level resolveLevel() {
        File configFile = new File("config.properties");
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(in);
                String raw = props.getProperty("log.level", "INFO").trim();
                Level level = Level.parse(raw.toUpperCase());
                System.err.println("[AppLogger] 日志级别: " + level.getName()
                        + "（来自 config.properties）");
                return level;
            } catch (Exception e) {
                System.err.println("[AppLogger] 读取 log.level 失败，使用默认 INFO: " + e.getMessage());
            }
        } else {
            System.err.println("[AppLogger] config.properties 未找到，使用默认 INFO");
        }
        return Level.INFO;
    }
}
