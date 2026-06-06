package com.mes.ai.service;

import com.mes.ai.domain.dto.AiChatRequest;
import com.mes.ai.domain.model.AiIntent;
import com.mes.ai.domain.model.AiToolResult;

import java.util.List;

@FunctionalInterface
public interface AiToolRegistry {

    List<AiToolResult> collect(AiChatRequest request, AiIntent intent);
}
