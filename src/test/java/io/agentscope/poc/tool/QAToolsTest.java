package io.agentscope.poc.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QAToolsTest {

    private QATools tools;

    @BeforeEach
    void setUp() {
        tools = new QATools();
    }

    @Test
    void shouldReturnVehicleKnowledgeTemplate() {
        String result = tools.queryVehicleKnowledge("如何保养发动机");
        assertNotNull(result);
        assertFalse(result.isBlank());
        assertTrue(result.contains("保养发动机"), "结果应包含原始问题");
    }

    @Test
    void shouldReturnGeneralAnswerTemplate() {
        String result = tools.answerGeneralQuestion("今天天气怎么样");
        assertNotNull(result);
        assertFalse(result.isBlank());
        assertTrue(result.contains("天气"), "结果应包含原始问题关键词");
    }
}
