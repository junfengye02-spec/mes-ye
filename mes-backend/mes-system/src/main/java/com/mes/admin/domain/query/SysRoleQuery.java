package com.mes.admin.domain.query;

import com.mes.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleQuery extends PageQuery {
    private String roleName;
    private String roleCode;
    private Boolean enabled;
}
