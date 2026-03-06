package io.agentscope.poc;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.poc.config.ModelConfig;
import io.agentscope.poc.router.RouterAgentFactory;
import reactor.core.scheduler.Schedulers;

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

        Schedulers.shutdownNow();
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
