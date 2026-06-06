package com.mes.ai.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.mes.ai.domain.dto.AiChatRequest;
import com.mes.ai.domain.vo.AiChatResponse;
import com.mes.ai.service.AiAssistantService;
import com.mes.common.result.R;
import com.mes.framework.sentinel.MesRateLimit;
import com.mes.framework.sentinel.SentinelBlockHandlers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI助手", description = "生产问题自然语言问答")
@RestController
@RequestMapping("/ai/assistant")
@RequiredArgsConstructor
public class AiAssistantController {

    private static final String RESOURCE = "ai:assistant:chat";

    private final AiAssistantService aiAssistantService;

    @Operation(summary = "AI助手问答", description = "仅回答当前权限内的只读生产业务问题，不展示代码、SQL或内部配置")
    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('ai:assistant:chat')")
    @SentinelResource(value = RESOURCE, blockHandler = "handleR", blockHandlerClass = SentinelBlockHandlers.class)
    @MesRateLimit(resource = RESOURCE, key = MesRateLimit.Key.TENANT, count = 5)
    public R<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return R.ok(aiAssistantService.chat(request));
    }
}
