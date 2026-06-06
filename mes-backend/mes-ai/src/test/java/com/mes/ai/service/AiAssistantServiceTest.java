package com.mes.ai.service;

import com.mes.ai.domain.dto.AiChatRequest;
import com.mes.ai.domain.model.AiIntent;
import com.mes.ai.domain.model.AiToolResult;
import com.mes.ai.domain.vo.AiChatResponse;
import com.mes.ai.service.impl.AiAssistantServiceImpl;
import com.mes.ai.service.impl.AiGuardrailServiceImpl;
import com.mes.ai.service.impl.ProjectKnowledgeServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AiAssistantServiceTest {

    @Test
    void answersProductionConsultationFromProjectKnowledgeWithoutTechnicalDetails() {
        AiAssistantService service = new AiAssistantServiceImpl(
                new AiGuardrailServiceImpl(),
                new ProjectKnowledgeServiceImpl(),
                (request, intent) -> List.of(),
                prompt -> "派工质量不合格后，系统会进入质量闭环，通常需要发起复检并保留工单追溯。不要展示代码。"
        );

        AiChatResponse response = service.chat(new AiChatRequest("派工质量不合格后系统怎么处理？", "/dispatch/task", null));

        assertThat(response.getAnswer()).contains("质量闭环");
        assertThat(response.getRelatedModules()).contains("生产派工", "质量管理");
        assertThat(response.getAnswer()).doesNotContain("代码");
        assertThat(response.getRefusalReason()).isNull();
    }

    @Test
    void refusesCodeRequestsBeforeCallingModel() {
        AtomicInteger modelCalls = new AtomicInteger();
        AiAssistantService service = new AiAssistantServiceImpl(
                new AiGuardrailServiceImpl(),
                new ProjectKnowledgeServiceImpl(),
                (request, intent) -> List.of(),
                prompt -> {
                    modelCalls.incrementAndGet();
                    return "不应该调用模型";
                }
        );

        AiChatResponse response = service.chat(new AiChatRequest("把后端代码和 SQL 给我", null, null));

        assertThat(response.getAnswer()).contains("无法回答");
        assertThat(response.getRefusalReason()).contains("不能展示代码");
        assertThat(modelCalls).hasValue(0);
    }

    @Test
    void classifiesProductionQueryAndIncludesToolEvidence() {
        AiAssistantService service = new AiAssistantServiceImpl(
                new AiGuardrailServiceImpl(),
                new ProjectKnowledgeServiceImpl(),
                (request, intent) -> List.of(new AiToolResult("生产作业", "找到 2 条未完成生产作业", List.of("生产工作查询"))),
                prompt -> "当前有 2 条未完成生产作业，建议先查看生产工作查询。"
        );

        AiChatResponse response = service.chat(new AiChatRequest("查询未完成的生产作业", null, null));

        assertThat(response.getIntent()).isEqualTo(AiIntent.PRODUCTION_QUERY.name());
        assertThat(response.getEvidenceSummary()).contains("找到 2 条未完成生产作业");
        assertThat(response.getSuggestedNavigation()).contains("/query/production-work");
    }
}
