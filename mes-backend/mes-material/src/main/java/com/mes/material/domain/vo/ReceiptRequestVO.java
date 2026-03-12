package com.mes.material.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "完工入库申请信息")
public class ReceiptRequestVO {
    @Schema(description = "ID") private Long id;
    @Schema(description = "申请单号") private String requestNo;
    @Schema(description = "入库类型") private String receiptType;
    @Schema(description = "工单ID") private Long workOrderId;
    @Schema(description = "工单号") private String workOrderNo;
    @Schema(description = "项目") private String projectName;
    @Schema(description = "物料编码") private String materialCode;
    @Schema(description = "物料名称") private String materialName;
    @Schema(description = "序列号") private String serialNo;
    @Schema(description = "数量") private BigDecimal qty;
    @Schema(description = "合格数量") private BigDecimal qualifiedQty;
    @Schema(description = "不合格数量") private BigDecimal unqualifiedQty;
    @Schema(description = "待收料数量") private BigDecimal pendingReceiptQty;
    @Schema(description = "状态") private String status;
    @Schema(description = "创建人") private String createdBy;
    @Schema(description = "创建时间") private LocalDateTime createdTime;
}
