package com.mes.process.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 喷涂条件返回 VO
 */
@Data
@Schema(description = "喷涂条件信息")
public class SprayConditionVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "条件号")
    private String conditionNo;

    @Schema(description = "部长审批人")
    private String ministerApprover;

    @Schema(description = "部长审批时间")
    private LocalDateTime ministerApproveTime;

    @Schema(description = "工段审批人")
    private String sectionApprover;

    @Schema(description = "工段审批时间")
    private LocalDateTime sectionApproveTime;

    @Schema(description = "系长审批人")
    private String leaderApprover;

    @Schema(description = "系长审批时间")
    private LocalDateTime leaderApproveTime;

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

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改人")
    private String updatedBy;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;
}
