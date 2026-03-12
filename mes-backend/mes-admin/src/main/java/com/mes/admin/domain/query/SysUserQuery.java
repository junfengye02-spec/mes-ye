package com.mes.admin.domain.query;

import com.mes.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserQuery extends PageQuery {
    private String username;
    private String realName;
    private Boolean enabled;
}
