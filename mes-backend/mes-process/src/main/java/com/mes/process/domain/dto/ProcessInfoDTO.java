package com.mes.process.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 工序信息新增/编辑 DTO
 */
@Data
@Schema(description = "工序信息请求参数")
public class ProcessInfoDTO {

    @Schema(description = "工序号")
    private String processNo;

    @Schema(description = "工序名")
    private String processName;

    @Schema(description = "工艺编码")
    private String processCode;

    @Schema(description = "产品")
    private String product;

    @Schema(description = "G编码")
    private String gCode;

    @Schema(description = "产品类别")
    private String productCategory;

    @Schema(description = "机型")
    private String machineModel;

    @Schema(description = "产品类型")
    private String productType;

    @Schema(description = "加工图纸")
    private String processDrawing;

    @Schema(description = "工序过程表单")
    private String processForm;

    @Schema(description = "工序模板ID")
    private Long processTemplateId;

    @Schema(description = "工序类型")
    private String processType;

    @Schema(description = "工厂")
    private String factory;

    @Schema(description = "业务组织")
    private String businessOrg;

    @Schema(description = "工作中心ID")
    private Long workCenterId;

    @Schema(description = "工段/区域")
    private String workshopArea;

    @Schema(description = "班组ID")
    private Long teamId;

    @Schema(description = "是否剥离")
    private Integer needStrip;

    @Schema(description = "处理时间")
    private BigDecimal handleTime;

    @Schema(description = "拆卸时间")
    private BigDecimal disassembleTime;

    @Schema(description = "安装时间")
    private BigDecimal installTime;

    @Schema(description = "说明")
    private String remark;
}
