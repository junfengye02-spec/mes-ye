package com.mes.ai.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI助手问答请求")
public class AiChatRequest {

    @NotBlank(message = "问题不能为空")
    @Size(max = 1000, message = "问题最多1000字符")
    @Schema(description = "自然语言问题")
    private String question;

    @Size(max = 200, message = "页面上下文最多200字符")
    @Schema(description = "当前页面路径或业务上下文")
    private String pageContext;

    @Size(max = 64, message = "会话ID最多64字符")
    @Schema(description = "短会话ID，可为空")
    private String conversationId;
}
