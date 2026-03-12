package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工序信息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_process_info")
public class ProcessInfo extends BaseEntity {

    /** 工序号 */
    private String processNo;

    /** 工序名 */
    private String processName;

    /** 工艺编码 */
    private String processCode;

    /** 产品 */
    private String product;

    /** G编码 */
    private String gCode;

    /** 产品类别 */
    private String productCategory;

    /** 机型 */
    private String machineModel;

    /** 产品类型 */
    private String productType;

    /** 加工图纸 */
    private String processDrawing;

    /** 工序过程表单 */
    private String processForm;

    /** 工序模板ID */
    private Long processTemplateId;

    /** 工序类型 */
    private String processType;

    /** 工厂 */
    private String factory;

    /** 业务组织 */
    private String businessOrg;

    /** 工作中心ID */
    private Long workCenterId;

    /** 工段/区域 */
    private String workshopArea;

    /** 班组ID */
    private Long teamId;

    /** 是否剥离 */
    private Integer needStrip;

    /** 处理时间 */
    private BigDecimal handleTime;

    /** 拆卸时间 */
    private BigDecimal disassembleTime;

    /** 安装时间 */
    private BigDecimal installTime;

    /** 说明 */
    private String remark;
}
