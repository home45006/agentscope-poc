package io.agentscope.poc.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.poc.tool.VehicleTools;

/**
 * 车控专家 Agent 工厂类。
 *
 * <p>创建绑定 VehicleTools 和 vehicle_control Skill 的 ReActAgent 实例。
 */
public class VehicleAgent {

    private VehicleAgent() {
        // 工厂类，禁止实例化
    }

    /**
     * 构建车控专家 Agent。
     *
     * @param model    DashScope 聊天模型
     * @param skillRepo Skill 仓库
     * @return 配置好的 ReActAgent 实例
     */
    public static ReActAgent build(DashScopeChatModel model,
                                   ClasspathSkillRepository skillRepo) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new VehicleTools());

        SkillBox skillBox = new SkillBox(toolkit);
        AgentSkill skill = skillRepo.getSkill("vehicle_control");
        skillBox.registerSkill(skill);

        return ReActAgent.builder()
                .name("小安·车控")
                .sysPrompt("""
                        你是小安的车控模块，隶属于长安汽车。
                        负责将用户语音指令转换为标准车控 JSON 指令。
                        输出 tts 字段不超过20字，使用口语，不带标点。
                        """)
                .model(model)
                .toolkit(toolkit)
                .skillBox(skillBox)
                .memory(new InMemoryMemory())
                .build();
    }
}
