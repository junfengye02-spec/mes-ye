package com.mes.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {
    private String roleName;
    private String roleCode;
    private String description;
    private Boolean enabled;

    /** 1=平台模板（tenant_id=0 时有效），用于新租户克隆；0=普通租户角色 */
    private Integer isTemplate;
}
