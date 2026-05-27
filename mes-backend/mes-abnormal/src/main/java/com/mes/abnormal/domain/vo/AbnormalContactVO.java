package com.mes.abnormal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 异常联络单返回 VO
 */
@Data
@Schema(description = "异常联络单信息")
public class AbnormalContactVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "异常联络单号")
    private String contactNo;

    @Schema(description = "关联工单ID")
    private Long workOrderId;

    @Schema(description = "关联派工任务ID")
    private Long dispatchTaskId;

    @Schema(description = "主题")
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

    @Schema(description = "状态")
    private String status;

    @Schema(description = "是否影响排程")
    private Integer affectSchedule;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "附件列表")
    private List<AbnormalContactAttachmentVO> attachments;

    @Schema(description = "状态日志")
    private List<AbnormalContactLogVO> logs;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "修改人")
    private String updatedBy;

    @Schema(description = "修改时间")
    private LocalDateTime updatedTime;
}
