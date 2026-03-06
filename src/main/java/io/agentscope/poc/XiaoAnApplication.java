package io.agentscope.poc;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.ChatModelBase;
import io.agentscope.poc.config.ModelConfig;
import io.agentscope.poc.router.RouterAgentFactory;
import io.agentscope.poc.util.AppLogger;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
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

        String userId = "user_001";
        ChatModelBase model = ModelConfig.buildModel();
        ReActAgent xiaoAn = RouterAgentFactory.build(model, userId);

        AppLogger.logSystem("小安 RouterAgent 初始化完成");

        if (args.length > 0 && "--interactive".equals(args[0])) {
            runInteractiveMode(xiaoAn, userId);
        } else {
            runDemoMode(xiaoAn);
        }

        AppLogger.logSystem("小安会话结束");
        Schedulers.shutdownNow();
    }

    private static void runInteractiveMode(ReActAgent xiaoAn, String userId) {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║  小安 · 长安汽车专属智能助手              ║");
        System.out.println("║  ────────────────────────────────────────  ║");
        System.out.println("║  支持能力：车控 / 音乐 / 导航 / 问答       ║");
        System.out.println("║  输入 'quit' 或 'exit' 退出                ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("你: ");
            String userInput = scanner.nextLine().trim();

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
            String plainText = responseText;

            if (!commands.isEmpty()) {
                // 移除 JSON 部分，只显示自然语言回复
                plainText = responseText.replaceAll("\\n?\\[\\[JSON_START\\]\\].*?\\[\\[JSON_END\\]\\]\\n?", "");
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
