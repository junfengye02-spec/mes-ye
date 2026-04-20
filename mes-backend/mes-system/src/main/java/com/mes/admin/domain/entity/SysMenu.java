package com.mes.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {
    private Long parentId;
    private String menuName;
    private String path;
    private String component;
    private String menuType;
    private String permission;
    private String icon;
    private Integer sortOrder;
    private Boolean visible;
}
