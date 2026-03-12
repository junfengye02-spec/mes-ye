package com.mes.abnormal.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 异常联络单主表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_abnormal_contact")
public class AbnormalContact extends BaseEntity {

    /** 异常联络单号 */
    private String contactNo;

    /** 主题 */
    private String subject;

    /** 发生阶段 */
    private String occurStage;

    /** 事件分类 */
    private String eventCategory;

    /** 产品区分 */
    private String productDivision;

    /** 订单号 */
    private String orderNo;

    /** 客户/项目 */
    private String customerProject;

    /** 发起部门 */
    private String initiateDept;

    /** 产品型号 */
    private String productModel;

    /** 产品类型 */
    private String productType;

    /** 产品名称 */
    private String productName;

    /** 发起工序 */
    private String initiateProcess;

    /** 数量 */
    private BigDecimal qty;

    /** 实物存放点 */
    private String storageLocation;

    /** 发现日期 */
    private LocalDate discoveryDate;

    /** 异常描述 */
    private String abnormalDesc;

    /** 状态（DRAFT/SUBMITTED/PROCESSING/CLOSED） */
    private String status;

    /** 是否影响排程 */
    private Integer affectSchedule;

    /** 发布时间 */
    private LocalDateTime publishTime;
}
