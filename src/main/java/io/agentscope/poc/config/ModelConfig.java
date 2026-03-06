package io.agentscope.poc.config;

import io.agentscope.core.model.ChatModelBase;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.OpenAIChatModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 模型配置工具类。
 *
 * <p>支持三种模型提供商，通过 config.properties 中的 model.provider 切换：
 * <ul>
 *   <li>{@code dashscope} — 阿里云 DashScope（千问/Qwen 系列）</li>
 *   <li>{@code glm}       — 智谱 AI（GLM 系列），使用 OpenAI 兼容接口</li>
 *   <li>{@code minimax}   — MiniMax，使用 OpenAI 兼容接口</li>
 * </ul>
 */
public class ModelConfig {

    private static final String GLM_BASE_URL     = "https://open.bigmodel.cn/api/paas/v4";
    private static final String MINIMAX_BASE_URL = "https://api.minimax.chat/v1";

    private static Properties cachedProps;

    private ModelConfig() {
        // 工具类，禁止实例化
    }

    // ──────────────────────────────────────────────────
    // 公共入口
    // ──────────────────────────────────────────────────

    /**
     * 根据配置文件中的 {@code model.provider} 构建对应的聊天模型。
     *
     * @return ChatModelBase 实例（DashScopeChatModel 或 OpenAIChatModel）
     * @throws IllegalStateException 如果 provider 未知或必要参数缺失
     */
    public static ChatModelBase buildModel() {
        Properties props = loadProperties();
        String provider = props.getProperty("model.provider", "dashscope").trim().toLowerCase();

        return switch (provider) {
            case "dashscope" -> buildDashScopeModel(props);
            case "glm"       -> buildGlmModel(props);
            case "minimax"   -> buildMinimaxModel(props);
            default -> throw new IllegalStateException(
                "未知的模型提供商: " + provider + "，可选值: dashscope | glm | minimax");
        };
    }

    /**
     * 加载当前 provider 对应的 API Key（供启动校验使用）。
     *
     * @return API Key 字符串，未配置则返回 null
     */
    public static String loadApiKey() {
        Properties props = loadProperties();
        String provider = props.getProperty("model.provider", "dashscope").trim().toLowerCase();

        String key = switch (provider) {
            case "dashscope" -> coalesce(
                    props.getProperty("dashscope.api.key"),
                    System.getenv("DASHSCOPE_API_KEY"));
            case "glm"       -> coalesce(
                    props.getProperty("glm.api.key"),
                    System.getenv("GLM_API_KEY"));
            case "minimax"   -> coalesce(
                    props.getProperty("minimax.api.key"),
                    System.getenv("MINIMAX_API_KEY"));
            default          -> null;
        };

        return (key != null && !key.isBlank()) ? key : null;
    }

    /** 返回当前配置的模型提供商名称，用于日志展示 */
    public static String currentProvider() {
        return loadProperties().getProperty("model.provider", "dashscope").trim().toLowerCase();
    }

    // ──────────────────────────────────────────────────
    // 各 Provider 构建逻辑
    // ──────────────────────────────────────────────────

    private static DashScopeChatModel buildDashScopeModel(Properties props) {
        String apiKey = coalesce(
                props.getProperty("dashscope.api.key"),
                System.getenv("DASHSCOPE_API_KEY"));
        requireNonBlank(apiKey, "dashscope.api.key / DASHSCOPE_API_KEY");

        String modelName = props.getProperty("dashscope.model.name", "qwen-plus");
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    private static OpenAIChatModel buildGlmModel(Properties props) {
        String apiKey = coalesce(
                props.getProperty("glm.api.key"),
                System.getenv("GLM_API_KEY"));
        requireNonBlank(apiKey, "glm.api.key / GLM_API_KEY");

        String modelName = props.getProperty("glm.model.name", "glm-4-flash");
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(GLM_BASE_URL)
                .build();
    }

    private static OpenAIChatModel buildMinimaxModel(Properties props) {
        String apiKey = coalesce(
                props.getProperty("minimax.api.key"),
                System.getenv("MINIMAX_API_KEY"));
        requireNonBlank(apiKey, "minimax.api.key / MINIMAX_API_KEY");

        String modelName = props.getProperty("minimax.model.name", "abab6.5s-chat");
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(MINIMAX_BASE_URL)
                .build();
    }

    // ──────────────────────────────────────────────────
    // 内部工具方法
    // ──────────────────────────────────────────────────

    static Properties loadProperties() {
        if (cachedProps != null) {
            return cachedProps;
        }
        Properties props = new Properties();
        File configFile = new File("config.properties");
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("读取 config.properties 失败: " + e.getMessage());
            }
        }
        cachedProps = props;
        return props;
    }

    private static String coalesce(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        return null;
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("必须配置 " + fieldName);
        }
    }
}
