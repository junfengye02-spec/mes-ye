package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 指示书主表实体
 * <p>用于承载具体工单/批次执行过程中的随工单指示、流转卡与交付约束。</p>
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

    /** 扩展属性 JSON */
    private String extensionDataJson;

    /** 产品类别 */
    private String productCategory;

    /** 产品类型 */
    private String productType;

    /** 部件名称 */
    private String partName;

    /** 生产订单编号 */
    private String workOrderNo;

    /** 关联作业指导书ID（引用可复用SOP模板） */
    private Long workInstructionId;

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
