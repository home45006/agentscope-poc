package io.agentscope.poc.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.ChatModelBase;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.studio.StudioClient;
import io.agentscope.core.studio.StudioMessageHook;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.poc.tool.NavTools;

/**
 * 导航专家 Agent 工厂类。
 *
 * <p>创建绑定 NavTools 和 navigation_planning Skill 的 ReActAgent 实例。
 */
public class NavAgent {

    private NavAgent() {
        // 工厂类，禁止实例化
    }

    /**
     * 构建导航专家 Agent。
     *
     * @param model    聊天模型（DashScopeChatModel / OpenAIChatModel 均可）
     * @param skillRepo Skill 仓库
     * @return 配置好的 ReActAgent 实例
     */
    public static ReActAgent build(ChatModelBase model,
                                   ClasspathSkillRepository skillRepo,
                                   StudioClient studioClient) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new NavTools());

        SkillBox skillBox = new SkillBox(toolkit);
        AgentSkill skill = skillRepo.getSkill("navigation_planning");
        skillBox.registerSkill(skill);

        ReActAgent.Builder builder = ReActAgent.builder()
                .name("小安·导航")
                .sysPrompt("""
                        你是小安的导航模块，隶属于长安汽车。
                        负责路线规划、途经点添加和目的地修改等指令。
                        输出 tts 字段不超过20字，使用口语，不带标点。
                        """)
                .model(model)
                .toolkit(toolkit)
                .skillBox(skillBox)
                .memory(new InMemoryMemory());

        if (studioClient != null) {
            builder.hook(new StudioMessageHook(studioClient));
        }

        return builder.build();
    }
}
