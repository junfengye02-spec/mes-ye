package com.mes.quality.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 生产工单开工检查表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_order_start_check")
public class OrderStartCheck extends BaseEntity {

    /** 工单ID */
    private Long workOrderId;

    /** 工单号 */
    private String workOrderNo;

    /** 检查项目 */
    private String checkItem;

    /** 检查结果 */
    private String checkResult;

    /** 开工检查状态（PASSED/FAILED） */
    private String checkStatus;

    /** 开工检查备注 */
    private String checkRemark;

    /** 检查人 */
    private String checker;

    /** 检查时间 */
    private LocalDateTime checkTime;

    /** 备注 */
    private String remark;
}
