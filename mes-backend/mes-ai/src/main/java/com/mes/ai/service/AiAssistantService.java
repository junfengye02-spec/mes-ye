package com.mes.ai.service;

import com.mes.ai.domain.dto.AiChatRequest;
import com.mes.ai.domain.vo.AiChatResponse;

public interface AiAssistantService {

    AiChatResponse chat(AiChatRequest request);
}
