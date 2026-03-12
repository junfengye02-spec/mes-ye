package com.mes.common.core;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询参数基类
 */
@Data
@Schema(description = "分页查询参数")
public class PageQuery implements Serializable {

    @Schema(description = "页码（从1开始）", example = "1")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数", example = "20")
    @Min(value = 1, message = "每页最少1条")
    @Max(value = 500, message = "每页最多500条")
    private Integer pageSize = 20;

    @Schema(description = "排序字段")
    private String orderBy;

    @Schema(description = "排序方向（asc/desc）")
    private String orderDir;
}
