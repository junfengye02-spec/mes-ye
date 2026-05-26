package com.mes.process.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 工艺路线请求参数。
 */
@Data
@Schema(description = "工艺路线请求参数")
public class RouteDTO {

    @NotBlank(message = "路线编码不能为空")
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

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期")
    private LocalDate expiryDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "路线步骤")
    private List<RouteStepDTO> steps;
}
