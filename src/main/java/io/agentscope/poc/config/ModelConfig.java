package io.agentscope.poc.config;

import io.agentscope.core.model.DashScopeChatModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 模型配置工具类。
 *
 * <p>提供 DashScope API Key 加载和默认模型配置。
 */
public class ModelConfig {

    private ModelConfig() {
        // 工具类，禁止实例化
    }

    /**
     * 创建默认的 DashScope 聊天模型。
     *
     * <p>使用 qwen-plus 模型。
     *
     * @return 配置好的 DashScopeChatModel 实例
     */
    public static DashScopeChatModel defaultModel() {
        return DashScopeChatModel.builder()
                .apiKey(loadApiKey())
                .modelName("qwen-plus")
                .build();
    }

    /**
     * 加载 DashScope API Key。
     *
     * <p>优先从 config.properties 读取，fallback 到环境变量 DASHSCOPE_API_KEY。
     *
     * @return API Key 字符串，如果未配置则返回 null
     */
    public static String loadApiKey() {
        File configFile = new File("config.properties");
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(in);
                String key = props.getProperty("dashscope.api.key");
                if (key != null && !key.isBlank()) {
                    return key;
                }
            } catch (IOException e) {
                System.err.println("读取 config.properties 失败: " + e.getMessage());
            }
        }
        return System.getenv("DASHSCOPE_API_KEY");
    }
}
