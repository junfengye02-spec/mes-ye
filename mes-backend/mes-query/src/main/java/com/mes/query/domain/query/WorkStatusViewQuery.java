package com.mes.query.domain.query;

import com.mes.common.core.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "工作状态查看查询参数")
public class WorkStatusViewQuery extends PageQuery {
    @Schema(description = "状态（六状态Tab过滤）") private String status;
    @Schema(description = "编号") private String workNo;
    @Schema(description = "名称") private String workName;
    @Schema(description = "工厂") private String factory;
    @Schema(description = "业务组织") private String businessOrg;
}
