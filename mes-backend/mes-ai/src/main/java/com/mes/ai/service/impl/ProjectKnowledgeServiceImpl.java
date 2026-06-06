package com.mes.ai.service.impl;

import com.mes.ai.domain.dto.AiChatRequest;
import com.mes.ai.domain.model.AiIntent;
import com.mes.ai.domain.model.AiKnowledgeContext;
import com.mes.ai.service.AiKnowledgeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ProjectKnowledgeServiceImpl implements AiKnowledgeService {

    @Override
    public AiKnowledgeContext findKnowledge(AiChatRequest request, AiIntent intent) {
        String question = normalize(request.getQuestion() + " " + request.getPageContext());
        Set<String> modules = new LinkedHashSet<>();
        Set<String> navigation = new LinkedHashSet<>();
        List<String> evidence = new ArrayList<>();

        if (question.contains("派工") || intent == AiIntent.DISPATCH_STATUS) {
            modules.add("生产派工");
            navigation.add("/dispatch/task");
            evidence.add("生产派工模块负责人员、设备、班组维度的任务分配、完工和撤销。");
        }
        if (question.contains("质量") || question.contains("复检") || question.contains("不合格")
                || intent == AiIntent.QUALITY_STATUS) {
            modules.add("质量管理");
            navigation.add("/quality/recheck");
            evidence.add("质量管理覆盖复检申请、开工检查、订单开工检查和交接班记录。");
        }
        if (question.contains("生产作业") || question.contains("生产工作") || question.contains("工作状态")
                || intent == AiIntent.PRODUCTION_QUERY) {
            modules.add("工作查询");
            navigation.add("/query/production-work");
            evidence.add("工作查询模块提供生产作业、检验工作、工作状态、工单和派工维度查询。");
        }
        if (question.contains("工单") || intent == AiIntent.WORK_ORDER_STATUS) {
            modules.add("生产工单");
            navigation.add("/workorder/list");
            evidence.add("生产工单模块管理创建、下发、开工、完工和强制完工生命周期。");
        }
        if (question.contains("异常") || intent == AiIntent.ABNORMAL_ISSUE) {
            modules.add("异常管理");
            navigation.add("/abnormal/contact");
            evidence.add("异常联络单支持生产异常上报、处理和关闭闭环。");
        }
        if (question.contains("物料") || question.contains("库存") || intent == AiIntent.MATERIAL_STATUS) {
            modules.add("物料管理");
            navigation.add("/material-mgmt/inventory");
            evidence.add("物料管理覆盖库存、领料、退料、入库和配送签收。");
        }
        if (question.contains("aps") || question.contains("排程") || intent == AiIntent.APS_STATUS) {
            modules.add("APS 集成");
            navigation.add("/aps/sync-log");
            evidence.add("APS集成负责同步配置、同步日志、数据映射和排程反馈。");
        }
        if (modules.isEmpty()) {
            modules.add("MES生产执行");
            evidence.add("本系统覆盖订单计划、生产计划、工单、派工、质量、物料、异常和APS集成。");
        }
        return new AiKnowledgeContext(
                String.join("\n", evidence),
                new ArrayList<>(modules),
                evidence,
                new ArrayList<>(navigation)
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
