package io.agentscope.poc;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.poc.config.ModelConfig;
import io.agentscope.poc.router.RouterAgentFactory;
import reactor.core.scheduler.Schedulers;

import java.util.Scanner;

public class XiaoAnApplication {

    public static void main(String[] args) {
        String apiKey = ModelConfig.loadApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "请配置 DASHSCOPE_API_KEY：\n" +
                "  1. 在 config.properties 中设置 dashscope.api.key\n" +
                "  2. 或设置环境变量 DASHSCOPE_API_KEY"
            );
        }

        String userId = "user_001";
        ReActAgent xiaoAn = RouterAgentFactory.build(ModelConfig.defaultModel(), userId);

        // 检查命令行参数
        if (args.length > 0 && "--interactive".equals(args[0])) {
            runInteractiveMode(xiaoAn);
        } else {
            runDemoMode(xiaoAn);
        }

        Schedulers.shutdownNow();
    }

    /**
     * 交互模式：用户通过命令行与小安实时对话
     */
    private static void runInteractiveMode(ReActAgent xiaoAn) {
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
                break;
            }

            Msg msg = Msg.builder()
                    .role(MsgRole.USER)
                    .textContent(userInput)
                    .build();

            System.out.print("小安: ");
            Msg response = xiaoAn.call(msg).block();
            System.out.println(response.getTextContent());
            System.out.println();
        }

        scanner.close();
    }

    /**
     * 演示模式：运行预设的测试场景
     */
    private static void runDemoMode(ReActAgent xiaoAn) {
        System.out.println("════════════════════════════════════════════");
        System.out.println("  小安演示模式（使用 --interactive 进入交互模式）");
        System.out.println("════════════════════════════════════════════");

        // 测试场景 1：车控
        sendAndPrint(xiaoAn, "把空调调到22度");

        // 测试场景 2：域内追问
        sendAndPrint(xiaoAn, "再低2度");

        // 测试场景 3：音乐
        sendAndPrint(xiaoAn, "播放一首周杰伦的歌");

        // 测试场景 4：跨域
        sendAndPrint(xiaoAn, "同时打开车窗，并且导航去重庆解放碑");

        // 测试场景 5：知识问答
        sendAndPrint(xiaoAn, "长安CS75多久保养一次");
    }

    private static void sendAndPrint(ReActAgent agent, String userInput) {
        System.out.println("\n用户: " + userInput);
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent(userInput)
                .build();
        Msg response = agent.call(msg).block();
        System.out.println("小安: " + response.getTextContent());
    }
}
