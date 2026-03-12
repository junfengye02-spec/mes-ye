package com.mes.quality.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 复检申请产品序列号 DTO
 */
@Data
@Schema(description = "复检产品序列号参数")
public class RecheckSerialDTO {

    @Schema(description = "序列号")
    private String serialNo;

    @Schema(description = "生产厂商")
    private String manufacturer;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "状态分类")
    private String statusCategory;

    @Schema(description = "数量")
    private BigDecimal qty;

    @Schema(description = "冻结")
    private Integer frozen;

    @Schema(description = "拆分完成")
    private Integer splitCompleted;

    @Schema(description = "计量单位")
    private String unit;

    @Schema(description = "条码号")
    private String barcode;
}
