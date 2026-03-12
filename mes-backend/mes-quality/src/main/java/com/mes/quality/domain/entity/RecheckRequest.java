package com.mes.quality.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 复检申请主表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_recheck_request")
public class RecheckRequest extends BaseEntity {

    /** 项目编码 */
    private String projectCode;

    /** 项目名称 */
    private String projectName;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 生产订单 */
    private String productionOrderNo;

    /** 复检需求 */
    private String recheckRequirement;

    /** 复检原因 */
    private String recheckReason;

    /** 复检提出人 */
    private String recheckProposer;

    /** 复检提出时间 */
    private LocalDateTime recheckProposeTime;

    /** 需求交货时间 */
    private LocalDateTime requiredDeliveryTime;

    /** 是否合理 */
    private Integer isReasonable;

    /** 审核人员 */
    private String reviewer;

    /** 审核日期 */
    private LocalDate reviewDate;

    /** 状态 */
    private String status;
}
