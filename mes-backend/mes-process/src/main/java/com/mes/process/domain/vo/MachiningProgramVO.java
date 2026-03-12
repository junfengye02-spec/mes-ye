package com.mes.process.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机械加工程序返回 VO
 */
@Data
@Schema(description = "机械加工程序信息")
public class MachiningProgramVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "G-code")
    private String gCode;

    @Schema(description = "程序表")
    private String programTable;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改人")
    private String updatedBy;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;
}
