package com.mes.ai.service.impl;

import com.mes.ai.domain.model.AiIntent;
import com.mes.ai.service.AiAuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingAiAuditService implements AiAuditService {

    @Override
    public void record(String question, AiIntent intent, String resultStatus, String refusalReason) {
        String summary = question == null ? "" : question.replaceAll("\\s+", " ");
        if (summary.length() > 80) {
            summary = summary.substring(0, 80);
        }
        log.info("[AI] intent={}, status={}, refusal={}, questionSummary={}",
                intent, resultStatus, refusalReason, summary);
    }
}
