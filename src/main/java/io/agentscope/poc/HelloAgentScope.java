package io.agentscope.poc;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;

/**
 * AgentScope Java SDK 入门示例。
 *
 * <p>本示例演示了如何：
 * <ol>
 *   <li>通过 {@link DashScopeChatModel} 接入阿里云 DashScope 大模型服务</li>
 *   <li>使用 {@link Toolkit} 注册自定义工具，供 Agent 在推理过程中调用</li>
 *   <li>构建基于 ReAct（Reasoning + Acting）范式的 {@link ReActAgent}</li>
 *   <li>向 Agent 发送消息并阻塞等待响应</li>
 * </ol>
 *
 * <p><b>运行前提：</b>需要配置有效的 DashScope API Key，支持以下两种方式：
 * <ul>
 *   <li>在项目根目录的 {@code config.properties} 中设置 {@code dashscope.api.key=<your-key>}</li>
 *   <li>设置环境变量 {@code DASHSCOPE_API_KEY=<your-key>}</li>
 * </ul>
 */
public class HelloAgentScope {

    public static void main(String[] args) {
        // 加载 API Key，优先读取配置文件，回退到环境变量
        String apiKey = loadApiKey();

        // API Key 为空时快速失败，避免后续网络请求报错信息不直观
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "请配置 DASHSCOPE_API_KEY：\n" +
                "  1. 在 config.properties 中设置 dashscope.api.key\n" +
                "  2. 或设置环境变量 DASHSCOPE_API_KEY"
            );
        }

        // ---------- 1. 注册工具 ----------
        // Toolkit 是工具的容器，Agent 会在需要时自动发现并调用其中的方法。
        // registerTool() 会扫描传入对象上所有标注了 @Tool 的方法并完成注册。
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new GreetingTools());

        // ---------- 2. 构建 Agent ----------
        // ReActAgent 实现了 ReAct（Reasoning + Acting）推理框架：
        //   - Reasoning：Agent 分析用户意图，决定是否需要调用工具
        //   - Acting：调用工具获取外部信息，再将结果纳入下一轮推理
        // 该过程循环进行，直至 Agent 得出最终答案。
        ReActAgent agent = ReActAgent.builder()
                .name("Jarvis")                                   // Agent 标识名，用于日志和追踪
                .sysPrompt("你是一个名叫 Jarvis 的助手，请用中文回答。") // 系统提示词，定义 Agent 角色与行为
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("qwen-plus")                   // 使用通义千问 Plus 模型
                        .build())
                .toolkit(toolkit)                                 // 注入工具集
                .build();

        // ---------- 3. 发送消息并获取响应 ----------
        // Msg 是 AgentScope 中统一的消息载体，支持文本、图片等多种内容类型。
        Msg msg = Msg.builder()
                .textContent("你好，请问现在几点了？")
                .build();

        // agent.call() 返回 Mono<Msg>（响应式流），调用 block() 阻塞当前线程等待结果。
        // 生产环境中建议保持响应式链路，避免使用 block()。
        Msg response = agent.call(msg).block();
        System.out.println("Jarvis: " + response.getTextContent());

        // 关闭 Reactor 全局调度器，释放后台线程，确保 JVM 正常退出
        reactor.core.scheduler.Schedulers.shutdownNow();
    }

    /**
     * 按优先级加载 DashScope API Key。
     *
     * <p>加载顺序：
     * <ol>
     *   <li>优先读取项目根目录下的 {@code config.properties}（便于本地开发，不提交到版本控制）</li>
     *   <li>若文件不存在或未配置，则回退到环境变量 {@code DASHSCOPE_API_KEY}（适合 CI/CD 及生产环境）</li>
     * </ol>
     *
     * @return API Key 字符串，若均未配置则返回 {@code null}
     */
    private static String loadApiKey() {
        // 1. 优先读 config.properties（项目根目录）
        java.io.File configFile = new java.io.File("config.properties");
        if (configFile.exists()) {
            try (java.io.InputStream in = new java.io.FileInputStream(configFile)) {
                java.util.Properties props = new java.util.Properties();
                props.load(in);
                String key = props.getProperty("dashscope.api.key");
                if (key != null && !key.isBlank()) {
                    return key;
                }
            } catch (java.io.IOException e) {
                System.err.println("读取 config.properties 失败: " + e.getMessage());
            }
        }
        // 2. 回退到环境变量（适合容器化部署和 CI/CD 环境）
        return System.getenv("DASHSCOPE_API_KEY");
    }
}
