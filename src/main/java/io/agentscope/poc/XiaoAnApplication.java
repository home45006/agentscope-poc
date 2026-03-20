package io.agentscope.poc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.ChatModelBase;
import io.agentscope.core.studio.StudioClient;
import io.agentscope.core.studio.StudioManager;
import io.agentscope.core.studio.StudioUserAgent;
import io.agentscope.poc.config.ModelConfig;
import io.agentscope.poc.router.RouterAgentFactory;
import io.agentscope.poc.util.AppLogger;
import reactor.core.scheduler.Schedulers;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.EndOfFileException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XiaoAnApplication {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern JSON_PATTERN = Pattern.compile(
            "\\[\\[JSON_START\\]\\](.*?)\\[\\[JSON_END\\]\\]",
            Pattern.DOTALL
    );

    public static void main(String[] args) {
        String apiKey = ModelConfig.loadApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "请配置 API Key：\n" +
                "  dashscope: config.properties 中设置 dashscope.api.key 或环境变量 DASHSCOPE_API_KEY\n" +
                "  glm:       config.properties 中设置 glm.api.key 或环境变量 GLM_API_KEY\n" +
                "  minimax:   config.properties 中设置 minimax.api.key 或环境变量 MINIMAX_API_KEY"
            );
        }

        String provider = ModelConfig.currentProvider();
        AppLogger.logSystem("启动小安，模型提供商: " + provider);

        // 初始化 Studio（如果配置了 studio.url）
        StudioClient studioClient = initStudio();

        try {
            String userId = "user_001";
            ChatModelBase model = ModelConfig.buildModel();
            ReActAgent xiaoAn = RouterAgentFactory.build(model, userId, studioClient);

            AppLogger.logSystem("小安 RouterAgent 初始化完成");

            if (args.length > 0 && "--studio".equals(args[0])) {
                runStudioMode(xiaoAn, studioClient);
            } else if (args.length > 0 && "--interactive".equals(args[0])) {
                runInteractiveMode(xiaoAn, userId);
            } else {
                runDemoMode(xiaoAn);
            }

            AppLogger.logSystem("小安会话结束");
        } finally {
            if (studioClient != null) {
                StudioManager.shutdown();
            }
            Schedulers.shutdownNow();
        }
    }

    private static StudioClient initStudio() {
        if (!ModelConfig.isStudioEnabled()) {
            return null;
        }
        String studioUrl = ModelConfig.loadStudioUrl();
        try {
            AppLogger.logSystem("连接 AgentScope Studio: " + studioUrl);
            // tracingUrl：OTLP/HTTP 端点，Studio 默认与 studioUrl 相同（localhost:3000）
            // 若 Studio 使用 gRPC 可改为 http://localhost:4317
            String tracingUrl = ModelConfig.loadTracingUrl(studioUrl);
            StudioManager.init()
                    .studioUrl(studioUrl)
                    .tracingUrl(tracingUrl)
                    .project("小安-POC")
                    .runName("xiao-an_" + System.currentTimeMillis())
                    .initialize()
                    .block();
            AppLogger.logSystem("Studio 连接成功，打开 " + studioUrl + " 查看 Trace");
            return StudioManager.getClient();
        } catch (Exception e) {
            AppLogger.logSystem("Studio 连接失败（已降级）: " + e.getMessage());
            return null;
        }
    }

    private static void runStudioMode(ReActAgent xiaoAn, StudioClient studioClient) {
        if (studioClient == null) {
            AppLogger.logSystem("Studio 未连接，降级为交互模式");
            runInteractiveMode(xiaoAn, "user_001");
            return;
        }

        StudioUserAgent user = StudioUserAgent.builder()
                .name("用户")
                .studioClient(StudioManager.getClient())
                .webSocketClient(StudioManager.getWebSocketClient())
                .build();

        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║  小安 · Studio 模式已启动                 ║");
        System.out.println("║  请打开 Studio Web UI 与小安对话           ║");
        System.out.println("║  输入 exit 退出                            ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println();

        Msg msg = null;
        while (true) {
            msg = user.call(msg).block();
            if (msg == null || "exit".equalsIgnoreCase(msg.getTextContent())) {
                System.out.println("\n小安: 再见，随时为您服务！");
                break;
            }
            AppLogger.logUserInput(msg.getTextContent());
            // 用户消息来自 WebSocket，需显式推送到 Studio DataView
            studioClient.pushMessage(msg).block();

            msg = xiaoAn.call(msg).block();
            // 代理回复由 StudioMessageHook（PostCallEvent）自动推送，无需重复 push
            if (msg != null) {
                AppLogger.logAgentOutput("小安", msg.getTextContent());
            }
        }
    }

    private static void runInteractiveMode(ReActAgent xiaoAn, String userId) {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║  小安 · 长安汽车专属智能助手              ║");
        System.out.println("║  ────────────────────────────────────────  ║");
        System.out.println("║  支持能力：车控 / 音乐 / 导航 / 问答       ║");
        System.out.println("║  输入 'quit' 或 'exit' 退出                ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println();

        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            LineReader lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            while (true) {
                String userInput;
                try {
                    userInput = lineReader.readLine("你: ").trim();
                } catch (UserInterruptException e) {
                    // Ctrl+C
                    break;
                } catch (EndOfFileException e) {
                    // Ctrl+D
                    break;
                }

                if (userInput.isEmpty()) {
                    continue;
                }

                if ("quit".equalsIgnoreCase(userInput) || "exit".equalsIgnoreCase(userInput)) {
                    System.out.println("\n小安: 再见，随时为您服务！");
                    AppLogger.logSystem("用户主动退出");
                    break;
                }

                AppLogger.logUserInput(userInput);

                Msg msg = Msg.builder()
                        .role(MsgRole.USER)
                        .textContent(userInput)
                        .build();

                System.out.print("小安: ");
                Msg response = xiaoAn.call(msg).block();
                String responseText = response.getTextContent();

                AppLogger.logAgentOutput("小安", responseText);

                // 提取 JSON 指令
                List<Map<String, Object>> commands = extractCommands(responseText);

                if (!commands.isEmpty()) {
                    // 移除 JSON 部分，只显示自然语言回复
                    String plainText = responseText.replaceAll("\\n?\\[\\[JSON_START\\]\\].*?\\[\\[JSON_END\\]\\]\\n?", "");
                    System.out.println(plainText.trim());

                    // 显示结构化指令
                    System.out.println();
                    System.out.println("┌─────────── 下发指令 ──────────┐");
                    for (int i = 0; i < commands.size(); i++) {
                        try {
                            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(commands.get(i));
                            System.out.println("│ [" + (i + 1) + "] " + json.replace("\n", "\n│   ").trim());
                        } catch (Exception e) {
                            System.out.println("│ [" + (i + 1) + "] " + commands.get(i));
                        }
                    }
                    System.out.println("└───────────────────────────────────┘");
                } else {
                    System.out.println(responseText);
                }
                System.out.println();
            }
        } catch (IOException e) {
            AppLogger.logSystem("终端初始化失败，降级使用标准输入: " + e.getMessage());
            runInteractiveModeStdin(xiaoAn);
        }
    }

    private static void runInteractiveModeStdin(ReActAgent xiaoAn) {
        java.util.Scanner scanner = new java.util.Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);
        while (true) {
            System.out.print("你: ");
            if (!scanner.hasNextLine()) break;
            String userInput = scanner.nextLine().trim();
            if (userInput.isEmpty()) continue;
            if ("quit".equalsIgnoreCase(userInput) || "exit".equalsIgnoreCase(userInput)) {
                System.out.println("\n小安: 再见，随时为您服务！");
                break;
            }
            AppLogger.logUserInput(userInput);
            Msg msg = Msg.builder().role(MsgRole.USER).textContent(userInput).build();
            System.out.print("小安: ");
            Msg response = xiaoAn.call(msg).block();
            String responseText = response.getTextContent();
            AppLogger.logAgentOutput("小安", responseText);
            List<Map<String, Object>> commands = extractCommands(responseText);
            if (!commands.isEmpty()) {
                String plainText = responseText.replaceAll("\\n?\\[\\[JSON_START\\]\\].*?\\[\\[JSON_END\\]\\]\\n?", "");
                System.out.println(plainText.trim());
                System.out.println();
                System.out.println("┌─────────── 下发指令 ──────────┐");
                for (int i = 0; i < commands.size(); i++) {
                    try {
                        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(commands.get(i));
                        System.out.println("│ [" + (i + 1) + "] " + json.replace("\n", "\n│   ").trim());
                    } catch (Exception e) {
                        System.out.println("│ [" + (i + 1) + "] " + commands.get(i));
                    }
                }
                System.out.println("└───────────────────────────────────┘");
            } else {
                System.out.println(responseText);
            }
            System.out.println();
        }
        scanner.close();
    }

    private static List<Map<String, Object>> extractCommands(String text) {
        List<Map<String, Object>> commands = new ArrayList<>();
        Matcher matcher = JSON_PATTERN.matcher(text);

        while (matcher.find()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> cmd = MAPPER.readValue(matcher.group(1), Map.class);
                commands.add(cmd);
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        return commands;
    }

    private static void runDemoMode(ReActAgent xiaoAn) {
        System.out.println("════════════════════════════════════════════");
        System.out.println("  小安演示模式（使用 --interactive 进入交互模式）");
        System.out.println("════════════════════════════════════════════");

        sendAndPrint(xiaoAn, "把空调调到22度");
        sendAndPrint(xiaoAn, "再低2度");
        sendAndPrint(xiaoAn, "播放一首周杰伦的歌");
        sendAndPrint(xiaoAn, "同时打开车窗，并且导航去重庆解放碑");
        sendAndPrint(xiaoAn, "长安CS75多久保养一次");
    }

    private static void sendAndPrint(ReActAgent agent, String userInput) {
        System.out.println("\n用户: " + userInput);
        AppLogger.logUserInput(userInput);

        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent(userInput)
                .build();
        Msg response = agent.call(msg).block();
        String responseText = response.getTextContent();

        AppLogger.logAgentOutput("小安", responseText);

        List<Map<String, Object>> commands = extractCommands(responseText);
        String plainText = responseText;

        if (!commands.isEmpty()) {
            plainText = responseText.replaceAll("\\n?\\[\\[JSON_START\\]\\].*?\\[\\[JSON_END\\]\\]\\n?", "");
            System.out.println("小安: " + plainText.trim());
            System.out.println("下发指令: " + commands);
        } else {
            System.out.println("小安: " + responseText);
        }
    }
}
