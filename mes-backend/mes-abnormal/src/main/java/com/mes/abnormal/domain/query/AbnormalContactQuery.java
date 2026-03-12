package com.mes.abnormal.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 异常联络单查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "异常联络单查询参数")
public class AbnormalContactQuery extends PageQuery {

    @Schema(description = "联络单号")
    private String contactNo;

    @Schema(description = "主题")
    private String subject;

    @Schema(description = "事件分类")
    private String eventCategory;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "发起部门")
    private String initiateDept;
}
