package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 喷涂条件表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_spray_condition")
public class SprayCondition extends BaseEntity {

    /** 条件号 */
    private String conditionNo;

    /** 部长审批人 */
    private String ministerApprover;

    /** 部长审批时间 */
    private LocalDateTime ministerApproveTime;

    /** 工段审批人 */
    private String sectionApprover;

    /** 工段审批时间 */
    private LocalDateTime sectionApproveTime;

    /** 系长审批人 */
    private String leaderApprover;

    /** 系长审批时间 */
    private LocalDateTime leaderApproveTime;

    /** 送粉量(g/min) */
    private BigDecimal powderFeedRate;

    /** 喷涂距离(mm) */
    private BigDecimal sprayDistance;

    /** 喷枪型号 */
    private String sprayGunModel;

    /** FAI报告书 */
    private String faiReport;

    /** FAI要领书 */
    private String faiGuide;

    /** 送粉器 */
    private String powderFeeder;

    /** 送粉器转速(r/min) */
    private BigDecimal powderFeederSpeed;

    /** 氧气(SCFH) */
    private BigDecimal oxygenScfh;

    /** 煤油(GPH) */
    private BigDecimal keroseneGph;

    /** 燃烧压力(PSI) */
    private BigDecimal combustionPressure;

    /** 载气氮气 */
    private String carrierGas;

    /** 设备 */
    private String equipment;

    /** 对应粉末 */
    private String powderType;
}
