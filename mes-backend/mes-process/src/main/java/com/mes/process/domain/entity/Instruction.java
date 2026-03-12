package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 指示书主表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_instruction")
public class Instruction extends BaseEntity {

    /** 指示书号 */
    private String instructionNo;

    /** 版本 */
    private String version;

    /** 状态 */
    private String status;

    /** 来源版本ID */
    private Long upgradeFromId;

    /** 项目编号 */
    private String projectNo;

    /** WBS */
    private String wbs;

    /** 新制维修类型 */
    private String newOrRepairType;

    /** 类型（如主机） */
    private String mainType;

    /** G/T类型 */
    private String gtType;

    /** 产品类别 */
    private String productCategory;

    /** 产品类型 */
    private String productType;

    /** 部件名称 */
    private String partName;

    /** 生产订单编号 */
    private String workOrderNo;

    /** 生产完工日期 */
    private LocalDate finishDate;

    /** 数量 */
    private Integer qty;

    /** 发行日期 */
    private LocalDate issueDate;

    /** 产品最终交货期 */
    private LocalDate finalDeliveryDate;

    /** 检查提交日期 */
    private LocalDate checkSubmitDate;

    /** 项目·图纸号 */
    private String drawingNo;

    /** 维修指导图 */
    private String repairGuideDrawing;

    /** 担当 */
    private String assignee;

    /** 加工状态 */
    private String processingStatus;

    /** 原材料到货期 */
    private LocalDate rawMaterialArrivalDate;

    /** 原材料采购名义 */
    private String rawMaterialPurchaseName;

    /** 采购申请单号 */
    private String purchaseRequestNo;

    /** 接收时间 */
    private LocalDateTime receiveTime;

    /** 备注 */
    private String remark;
}
