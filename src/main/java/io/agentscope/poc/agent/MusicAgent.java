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
import io.agentscope.poc.tool.MusicTools;

/**
 * 音乐专家 Agent 工厂类。
 *
 * <p>创建绑定 MusicTools 和 music_playback Skill 的 ReActAgent 实例。
 */
public class MusicAgent {

    private MusicAgent() {
        // 工厂类，禁止实例化
    }

    /**
     * 构建音乐专家 Agent。
     *
     * @param model    聊天模型（DashScopeChatModel / OpenAIChatModel 均可）
     * @param skillRepo Skill 仓库
     * @return 配置好的 ReActAgent 实例
     */
    public static ReActAgent build(ChatModelBase model,
                                   ClasspathSkillRepository skillRepo,
                                   StudioClient studioClient) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new MusicTools());

        SkillBox skillBox = new SkillBox(toolkit);
        AgentSkill skill = skillRepo.getSkill("music_playback");
        skillBox.registerSkill(skill);

        ReActAgent.Builder builder = ReActAgent.builder()
                .name("小安·音乐")
                .sysPrompt("""
                        你是小安的音乐模块，隶属于长安汽车。
                        负责处理音乐播放、控制和音量调节等指令。
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
