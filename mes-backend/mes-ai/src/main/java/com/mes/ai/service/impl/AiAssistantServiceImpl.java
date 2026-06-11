package com.mes.ai.service.impl;

import com.mes.ai.domain.dto.AiChatRequest;
import com.mes.ai.domain.model.AiIntent;
import com.mes.ai.domain.model.AiKnowledgeContext;
import com.mes.ai.domain.model.AiModelPrompt;
import com.mes.ai.domain.model.AiToolResult;
import com.mes.ai.domain.vo.AiChatResponse;
import com.mes.ai.service.AiAssistantService;
import com.mes.ai.service.AiAuditService;
import com.mes.ai.service.AiGuardrailService;
import com.mes.ai.service.AiKnowledgeService;
import com.mes.ai.service.AiModelClient;
import com.mes.ai.service.AiToolRegistry;
import com.mes.ai.service.GuardrailDecision;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    private final AiGuardrailService guardrailService;
    private final AiKnowledgeService knowledgeService;
    private final AiToolRegistry toolRegistry;
    private final AiModelClient modelClient;
    private final AiAuditService auditService;

    @Autowired
    public AiAssistantServiceImpl(AiGuardrailService guardrailService,
                                  AiKnowledgeService knowledgeService,
                                  AiToolRegistry toolRegistry,
                                  AiModelClient modelClient,
                                  AiAuditService auditService) {
        this.guardrailService = guardrailService;
        this.knowledgeService = knowledgeService;
        this.toolRegistry = toolRegistry;
        this.modelClient = modelClient;
        this.auditService = auditService;
    }

    public AiAssistantServiceImpl(AiGuardrailService guardrailService,
                                  AiKnowledgeService knowledgeService,
                                  AiToolRegistry toolRegistry,
                                  AiModelClient modelClient) {
        this(guardrailService, knowledgeService, toolRegistry, modelClient, AiAuditService.noop());
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        GuardrailDecision decision = guardrailService.inspectQuestion(request.getQuestion());
        if (!decision.allowed()) {
            AiChatResponse response = AiChatResponse.builder()
                    .answer("无法回答该问题。AI助手只能提供当前权限内的生产业务咨询和只读查询。")
                    .intent(AiIntent.UNSUPPORTED.name())
                    .refusalReason(decision.reason())
                    .modelConfigured(false)
                    .build();
            auditService.record(request.getQuestion(), AiIntent.UNSUPPORTED, "REFUSED", decision.reason());
            return response;
        }

        AiIntent intent = inferIntent(request);
        if (intent == AiIntent.ASSISTANT_IDENTITY) {
            AiChatResponse response = buildAssistantIdentityResponse();
            auditService.record(request.getQuestion(), intent, "OK", null);
            return response;
        }

        AiKnowledgeContext knowledge = knowledgeService.findKnowledge(request, intent);
        List<AiToolResult> toolResults = toolRegistry.collect(request, intent);
        List<String> toolEvidence = toolResults.stream().map(AiToolResult::summary).toList();
        String modelAnswer;
        try {
            modelAnswer = modelClient.complete(new AiModelPrompt(
                    request.getQuestion(), intent, knowledge.context(), toolEvidence));
        } catch (AiModelUnavailableException e) {
            AiChatResponse response = buildModelUnavailableResponse(intent, knowledge, toolResults, toolEvidence, e);
            auditService.record(request.getQuestion(), intent, "MODEL_UNAVAILABLE", e.getMessage());
            return response;
        }
        String answer = guardrailService.sanitizeAnswer(
                StringUtils.hasText(modelAnswer) ? modelAnswer : fallbackAnswer(knowledge, toolEvidence));

        Set<String> modules = new LinkedHashSet<>(knowledge.relatedModules());
        toolResults.forEach(result -> modules.addAll(result.relatedModules()));

        List<String> evidence = new ArrayList<>(knowledge.evidenceSummary());
        evidence.addAll(toolEvidence);

        Set<String> navigation = new LinkedHashSet<>(knowledge.suggestedNavigation());
        if (intent == AiIntent.PRODUCTION_QUERY) {
            navigation.add("/query/production-work");
        }

        AiChatResponse response = AiChatResponse.builder()
                .answer(answer)
                .intent(intent.name())
                .relatedModules(new ArrayList<>(modules))
                .evidenceSummary(evidence)
                .suggestedNavigation(new ArrayList<>(navigation))
                .modelConfigured(modelClient.isConfigured())
                .build();
        auditService.record(request.getQuestion(), intent, "OK", null);
        return response;
    }

    private AiChatResponse buildAssistantIdentityResponse() {
        List<String> modules = List.of("MES生产执行");
        String answer = "我是MES AI助手，可以帮你在当前权限范围内查询和解释工单、派工、质量、物料、异常、APS等生产执行问题。";
        return AiChatResponse.builder()
                .answer(answer)
                .intent(AiIntent.ASSISTANT_IDENTITY.name())
                .relatedModules(modules)
                .evidenceSummary(List.of("身份说明：AI助手仅提供当前权限内的生产业务咨询和只读查询。"))
                .suggestedNavigation(List.of())
                .modelConfigured(modelClient.isConfigured())
                .build();
    }

    private AiChatResponse buildModelUnavailableResponse(AiIntent intent,
                                                         AiKnowledgeContext knowledge,
                                                         List<AiToolResult> toolResults,
                                                         List<String> toolEvidence,
                                                         AiModelUnavailableException exception) {
        Set<String> modules = new LinkedHashSet<>(knowledge.relatedModules());
        toolResults.forEach(result -> modules.addAll(result.relatedModules()));

        List<String> evidence = new ArrayList<>(knowledge.evidenceSummary());
        evidence.addAll(toolEvidence);

        Set<String> navigation = new LinkedHashSet<>(knowledge.suggestedNavigation());
        if (intent == AiIntent.PRODUCTION_QUERY) {
            navigation.add("/query/production-work");
        }

        return AiChatResponse.builder()
                .answer("AI助手模型调用失败，请检查模型服务配置或稍后重试。")
                .intent(intent.name())
                .relatedModules(new ArrayList<>(modules))
                .evidenceSummary(evidence)
                .suggestedNavigation(new ArrayList<>(navigation))
                .refusalReason(exception.getMessage())
                .modelConfigured(modelClient.isConfigured())
                .build();
    }

    private AiIntent inferIntent(AiChatRequest request) {
        String q = (request.getQuestion() + " " + request.getPageContext()).toLowerCase(Locale.ROOT);
        if (isAssistantIdentityQuestion(q)) {
            return AiIntent.ASSISTANT_IDENTITY;
        }
        if (containsAny(q, "查询", "未完成", "有多少", "哪些", "生产作业", "生产工作", "工作状态")) {
            return AiIntent.PRODUCTION_QUERY;
        }
        if (containsAny(q, "派工", "任务分配")) return AiIntent.DISPATCH_STATUS;
        if (containsAny(q, "质量", "复检", "检验", "不合格")) return AiIntent.QUALITY_STATUS;
        if (containsAny(q, "工单")) return AiIntent.WORK_ORDER_STATUS;
        if (containsAny(q, "异常")) return AiIntent.ABNORMAL_ISSUE;
        if (containsAny(q, "物料", "库存", "领料", "退料")) return AiIntent.MATERIAL_STATUS;
        if (containsAny(q, "aps", "排程", "同步")) return AiIntent.APS_STATUS;
        if (containsAny(q, "工艺", "bom", "指导书")) return AiIntent.PROCESS_GUIDANCE;
        return AiIntent.MES_CONSULTATION;
    }

    private boolean isAssistantIdentityQuestion(String value) {
        String compact = value.replaceAll("\\s+", "");
        return containsAny(compact,
                "你好", "您好", "hello", "hi",
                "你是谁", "你是什么", "介绍一下你", "自我介绍", "你能做什么", "你可以做什么");
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) return true;
        }
        return false;
    }

    private String fallbackAnswer(AiKnowledgeContext knowledge, List<String> toolEvidence) {
        if (!toolEvidence.isEmpty()) {
            return "根据当前系统查询结果：" + String.join("；", toolEvidence);
        }
        return "根据项目业务说明：" + knowledge.context();
    }
}
