package io.agentscope.poc.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * 问答工具集：提供长安汽车专属知识查询和通用问答功能。
 *
 * <p>用于小安·问答模块，支持：
 * <ul>
 *   <li>长安汽车车型、功能、保养等专属知识查询</li>
 *   <li>天气、百科、计算等通用问题回答</li>
 * </ul>
 */
public class QATools {

    /**
     * 查询长安汽车专属知识。
     *
     * <p>实际场景应接入 RAG 或知识库 API。
     * 此处返回占位符模板，供 Agent 基于 Skill 知识库补充回答。
     *
     * @param question 关于长安汽车的问题
     * @return 包含问题的模板回复
     */
    @Tool(name = "query_vehicle_knowledge",
          description = "查询长安汽车车型、功能、保养等专属知识")
    public String queryVehicleKnowledge(
            @ToolParam(name = "question", description = "关于长安汽车的问题") String question) {
        return "【车型知识查询】问题：" + question + "。请参考 changan_knowledge Skill 中的参考资料作答。";
    }

    /**
     * 回答通用问题。
     *
     * <p>实际场景应接入天气 API、搜索引擎等外部服务。
     * 此处返回占位符模板，供 Agent 基于自身知识直接回答。
     *
     * @param question 通用问题
     * @return 包含问题的模板回复
     */
    @Tool(name = "answer_general_question",
          description = "回答通用问题：天气、百科、计算等")
    public String answerGeneralQuestion(
            @ToolParam(name = "question", description = "通用问题") String question) {
        return "【通用问答】问题：" + question + "。请基于自身知识直接回答。";
    }
}
