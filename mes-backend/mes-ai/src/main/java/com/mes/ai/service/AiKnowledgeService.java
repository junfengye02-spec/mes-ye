package com.mes.ai.service;

import com.mes.ai.domain.dto.AiChatRequest;
import com.mes.ai.domain.model.AiIntent;
import com.mes.ai.domain.model.AiKnowledgeContext;

public interface AiKnowledgeService {

    AiKnowledgeContext findKnowledge(AiChatRequest request, AiIntent intent);
}
