package com.mes.ai.service;

import com.mes.ai.domain.model.AiIntent;

public interface AiAuditService {

    void record(String question, AiIntent intent, String resultStatus, String refusalReason);

    static AiAuditService noop() {
        return (question, intent, resultStatus, refusalReason) -> { };
    }
}
