package com.mes.process.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工序模板返回 VO（支持树形结构）
 */
@Data
@Schema(description = "工序模板信息")
public class ProcessTemplateVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "工序号")
    private String processNo;

    @Schema(description = "工序名")
    private String processName;

    @Schema(description = "父工序号")
    private String parentProcessNo;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "机型")
    private String machineModel;

    @Schema(description = "产品类型")
    private String productType;

    @Schema(description = "工序类型")
    private String processType;

    @Schema(description = "工序过程表单")
    private String processForm;

    @Schema(description = "加工图纸")
    private String processDrawing;

    @Schema(description = "工作中心ID")
    private Long workCenterId;

    @Schema(description = "处理时间")
    private BigDecimal handleTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "子工序列表")
    private List<ProcessTemplateVO> children;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改人")
    private String updatedBy;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;
}
