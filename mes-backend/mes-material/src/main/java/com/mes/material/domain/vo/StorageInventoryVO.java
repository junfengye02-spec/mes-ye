package com.mes.material.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "库存信息")
public class StorageInventoryVO {
    @Schema(description = "ID") private Long id;
    @Schema(description = "工厂") private String factory;
    @Schema(description = "存货组织") private String inventoryOrg;
    @Schema(description = "仓库") private String warehouse;
    @Schema(description = "存储地点") private String storageLocation;
    @Schema(description = "物料ID") private Long materialId;
    @Schema(description = "物料编码") private String materialCode;
    @Schema(description = "物料名称") private String materialName;
    @Schema(description = "非限制库存") private BigDecimal unrestrictedStock;
    @Schema(description = "质检库存") private BigDecimal qualityStock;
    @Schema(description = "冻结库存") private BigDecimal frozenStock;
    @Schema(description = "计量单位") private String unit;
    @Schema(description = "班组ID") private Long teamId;
    @Schema(description = "更新时间") private LocalDateTime updatedTime;
}
