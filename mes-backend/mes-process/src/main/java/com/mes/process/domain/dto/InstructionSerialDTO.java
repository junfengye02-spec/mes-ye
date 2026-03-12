package com.mes.process.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 指示书序列号 DTO
 */
@Data
@Schema(description = "指示书序列号请求参数")
public class InstructionSerialDTO {

    @Schema(description = "产品类型")
    private String productType;

    @Schema(description = "数量")
    private Integer qty;

    @Schema(description = "接收K/G编码")
    private String receiveKgCode;

    @Schema(description = "发送时G编码")
    private String sendGCode;

    @Schema(description = "定检时间")
    private LocalDateTime scheduledCheckTime;

    @Schema(description = "接收时间")
    private LocalDateTime receiveTime;

    @Schema(description = "备注")
    private String remark;
}
