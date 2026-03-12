package com.mes.process.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BOM替代料表实体
 */
@Data
@TableName("mes_bom_substitute")
public class BomSubstitute implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** BOM明细ID */
    private Long bomItemId;

    /** 主物料ID */
    private Long mainMaterialId;

    /** 主物料编码 */
    private String mainMaterialCode;

    /** 替代物料ID */
    private Long substituteMaterialId;

    /** 替代物料编码 */
    private String substituteMaterialCode;

    /** 替代物料名称 */
    private String substituteMaterialName;

    /** 替代优先级 */
    private Integer priority;

    /** 替代比例 */
    private BigDecimal substituteRatio;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 失效日期 */
    private LocalDate expiryDate;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 修改时间 */
    private LocalDateTime updatedTime;
}
