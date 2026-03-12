package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工序模板实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_process_template")
public class ProcessTemplate extends BaseEntity {

    /** 工序号 */
    private String processNo;

    /** 工序名 */
    private String processName;

    /** 父工序号 */
    private String parentProcessNo;

    /** 产品类别 */
    private String productCategory;

    /** 机型 */
    private String machineModel;

    /** 产品类型 */
    private String productType;

    /** 工序类型（生产工序/检验工序） */
    private String processType;

    /** 工序过程表单 */
    private String processForm;

    /** 加工图纸 */
    private String processDrawing;

    /** 工作中心ID */
    private Long workCenterId;

    /** 处理时间 */
    private BigDecimal handleTime;

    /** 备注 */
    private String remark;

    /** 说明 */
    private String description;
}
