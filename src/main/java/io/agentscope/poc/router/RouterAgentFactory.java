package io.agentscope.poc.router;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.session.JsonSession;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.subagent.SubAgentConfig;
import io.agentscope.poc.agent.MusicAgent;
import io.agentscope.poc.agent.NavAgent;
import io.agentscope.poc.agent.QAAgent;
import io.agentscope.poc.agent.VehicleAgent;
import io.agentscope.poc.hook.CommandCaptureHook;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * 小安路由 Agent 工厂类。
 *
 * <p>创建小安 RouterAgent，将 4 个领域专家 Agent 注册为 SubAgent Tool，
 * 使用 AutoContextMemory 管理全局多轮对话。
 */
public class RouterAgentFactory {

    private static final String SYS_PROMPT = """
            你是小安，长安汽车的专属智能车载助手，专业、亲切、有温度。
            你必须通过调用工具来完成任务，不能直接生成响应。
            当用户提出任何需求时，你必须调用对应的专家 Agent 工具：
            - 车辆控制（空调、车窗、座椅、车灯、车门）→ 必须调用 call_vehicle_agent
            - 音乐播放与控制 → 必须调用 call_music_agent
            - 导航与路线规划 → 必须调用 call_nav_agent
            - 知识问答 → 必须调用 call_qa_agent
            每次响应前必须先调用至少一个工具。
            """;

    private RouterAgentFactory() {
        // 工厂类，禁止实例化
    }

    /**
     * 构建小安 RouterAgent。
     *
     * @param model  DashScope 聊天模型
     * @param userId 用户标识，用于会话隔离
     * @return 配置好的 ReActAgent 实例
     */
    public static ReActAgent build(DashScopeChatModel model, String userId) {
        ClasspathSkillRepository skillRepo;
        try {
            skillRepo = new ClasspathSkillRepository("skills");
        } catch (java.io.IOException e) {
            throw new RuntimeException("初始化 Skill 仓库失败", e);
        }
        try {
            Toolkit toolkit = new Toolkit();

            registerExpert(toolkit, "call_vehicle_agent",
                    "控制车辆设备：空调、车窗、座椅、车灯、车门",
                    () -> VehicleAgent.build(model, skillRepo), userId, "vehicle");

            registerExpert(toolkit, "call_music_agent",
                    "播放和控制音乐、调节音量、切换播放模式",
                    () -> MusicAgent.build(model, skillRepo), userId, "music");

            registerExpert(toolkit, "call_nav_agent",
                    "导航路线规划、添加途经点、修改目的地、搜索途中POI",
                    () -> NavAgent.build(model, skillRepo), userId, "nav");

            registerExpert(toolkit, "call_qa_agent",
                    "长安汽车车型问答及通用知识问答",
                    () -> QAAgent.build(model, skillRepo), userId, "qa");

            AutoContextMemory memory = new AutoContextMemory(
                    AutoContextConfig.builder()
                            .msgThreshold(30)
                            .lastKeep(10)
                            .tokenRatio(0.3)
                            .build(),
                    model);

            return ReActAgent.builder()
                    .name("小安")
                    .sysPrompt(SYS_PROMPT)
                    .model(model)
                    .memory(memory)
                    .toolkit(toolkit)
                    .hooks(List.of(new CommandCaptureHook()))
                    .build();
        } catch (Exception e) {
            try { skillRepo.close(); } catch (Exception ignored) {}
            throw new RuntimeException("初始化小安 RouterAgent 失败", e);
        }
    }

    private static void registerExpert(Toolkit toolkit, String toolName,
            String description, Supplier<ReActAgent> factory,
            String userId, String domain) {
        SubAgentConfig config = SubAgentConfig.builder()
                .toolName(toolName)
                .description(description)
                .session(new JsonSession(Path.of("./sessions")))
                .build();
        // session_id 在实际调用时由框架按 userId_domain 命名
        toolkit.registration()
                .subAgent(factory::get, config)
                .apply();
    }
}
