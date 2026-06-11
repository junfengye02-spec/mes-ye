package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 制造BOM明细表实体
 */
@Data
@TableName("mes_manufacturing_bom_item")
public class ManufacturingBomItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** BOM主表ID */
    private Long bomId;

    /** 父级明细ID */
    private Long parentItemId;

    /** 层级 */
    private Integer level;

    /** 物料ID */
    private Long materialId;

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 物料规格 */
    private String materialSpec;

    /** 物料类型 */
    private String materialType;

    /** 用量 */
    private BigDecimal quantity;

    /** 损耗率(%) */
    private BigDecimal lossRate;

    /** 计量单位 */
    private String unit;

    /** 供应类型 */
    private String supplyType;

    /** 关联工艺路线步骤ID */
    private Long routeStepId;

    /** 关联工序ID（兼容字段） */
    private Long processId;

    /** 关联工序号 */
    private String processNo;

    /** 替代料标识 */
    private Integer isSubstitute;

    /** 替代料组 */
    private String substituteGroup;

    /** 是否关键件 */
    private Integer isKeyPart;

    /** 排序号 */
    private Integer sequenceNo;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 修改时间 */
    private LocalDateTime updatedTime;
}
