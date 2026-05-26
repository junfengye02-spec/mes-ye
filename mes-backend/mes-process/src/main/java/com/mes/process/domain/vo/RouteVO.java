package com.mes.process.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工艺路线返回值。
 */
@Data
@Schema(description = "工艺路线")
public class RouteVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "路线编码")
    private String routeCode;

    @Schema(description = "路线名称")
    private String routeName;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "机型")
    private String machineModel;

    @Schema(description = "产品类型")
    private String productType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期")
    private LocalDate expiryDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "路线步骤")
    private List<RouteStepVO> steps;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改人")
    private String updatedBy;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;
}
