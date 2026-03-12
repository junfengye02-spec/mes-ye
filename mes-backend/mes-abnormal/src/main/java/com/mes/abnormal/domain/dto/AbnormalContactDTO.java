package com.mes.abnormal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 异常联络单新增/编辑 DTO
 */
@Data
@Schema(description = "异常联络单请求参数")
public class AbnormalContactDTO {

    @Schema(description = "异常联络单号（可手工录入，留空则自动生成）")
    private String contactNo;

    @NotBlank(message = "主题不能为空")
    @Schema(description = "主题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String subject;

    @Schema(description = "发生阶段")
    private String occurStage;

    @Schema(description = "事件分类")
    private String eventCategory;

    @Schema(description = "产品区分")
    private String productDivision;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "客户/项目")
    private String customerProject;

    @Schema(description = "发起部门")
    private String initiateDept;

    @Schema(description = "产品型号")
    private String productModel;

    @Schema(description = "产品类型")
    private String productType;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "发起工序")
    private String initiateProcess;

    @Schema(description = "数量")
    private BigDecimal qty;

    @Schema(description = "实物存放点")
    private String storageLocation;

    @Schema(description = "发现日期")
    private LocalDate discoveryDate;

    @Schema(description = "异常描述")
    private String abnormalDesc;

    @Schema(description = "是否影响排程")
    private Integer affectSchedule;
}
