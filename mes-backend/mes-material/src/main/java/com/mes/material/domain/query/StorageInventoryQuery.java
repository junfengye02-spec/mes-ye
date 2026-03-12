package com.mes.material.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "库存查询参数")
public class StorageInventoryQuery extends PageQuery {
    @Schema(description = "物料编码") private String materialCode;
    @Schema(description = "物料名称") private String materialName;
    @Schema(description = "仓库") private String warehouse;
    @Schema(description = "存储地点") private String storageLocation;
    @Schema(description = "工厂") private String factory;
}
