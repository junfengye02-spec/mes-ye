package com.mes.process.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 喷涂条件新增/编辑 DTO
 */
@Data
@Schema(description = "喷涂条件请求参数")
public class SprayConditionDTO {

    @NotBlank(message = "条件号不能为空")
    @Schema(description = "条件号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String conditionNo;

    @Schema(description = "部长审批人")
    private String ministerApprover;

    @Schema(description = "工段审批人")
    private String sectionApprover;

    @Schema(description = "系长审批人")
    private String leaderApprover;

    @Schema(description = "送粉量(g/min)")
    private BigDecimal powderFeedRate;

    @Schema(description = "喷涂距离(mm)")
    private BigDecimal sprayDistance;

    @Schema(description = "喷枪型号")
    private String sprayGunModel;

    @Schema(description = "FAI报告书")
    private String faiReport;

    @Schema(description = "FAI要领书")
    private String faiGuide;

    @Schema(description = "送粉器")
    private String powderFeeder;

    @Schema(description = "送粉器转速(r/min)")
    private BigDecimal powderFeederSpeed;

    @Schema(description = "氧气(SCFH)")
    private BigDecimal oxygenScfh;

    @Schema(description = "煤油(GPH)")
    private BigDecimal keroseneGph;

    @Schema(description = "燃烧压力(PSI)")
    private BigDecimal combustionPressure;

    @Schema(description = "载气氮气")
    private String carrierGas;

    @Schema(description = "设备")
    private String equipment;

    @Schema(description = "对应粉末")
    private String powderType;
}
