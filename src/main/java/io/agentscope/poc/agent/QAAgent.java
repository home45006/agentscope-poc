package io.agentscope.poc.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.ChatModelBase;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.poc.tool.QATools;

/**
 * 问答专家 Agent 工厂类。
 *
 * <p>创建绑定 QATools 和 changan_knowledge/general_qa Skills 的 ReActAgent 实例。
 */
public class QAAgent {

    private QAAgent() {
        // 工厂类，禁止实例化
    }

    /**
     * 构建问答专家 Agent。
     *
     * @param model    聊天模型（DashScopeChatModel / OpenAIChatModel 均可）
     * @param skillRepo Skill 仓库
     * @return 配置好的 ReActAgent 实例
     */
    public static ReActAgent build(ChatModelBase model,
                                   ClasspathSkillRepository skillRepo) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new QATools());

        SkillBox skillBox = new SkillBox(toolkit);
        skillBox.registerSkill(skillRepo.getSkill("changan_knowledge"));
        skillBox.registerSkill(skillRepo.getSkill("general_qa"));

        return ReActAgent.builder()
                .name("小安·问答")
                .sysPrompt("""
                        你是小安的知识模块，隶属于长安汽车。
                        负责回答长安车型相关问题和通用知识问答。
                        回答简洁，适合语音播报，tts 不超过50字。
                        """)
                .model(model)
                .toolkit(toolkit)
                .skillBox(skillBox)
                .memory(new InMemoryMemory())
                .build();
    }
}
