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

    /**
     * 平台模板标识：1=平台模板菜单（tenant_id=0 时有效，新租户克隆入库的基线）；
     * 0=普通租户菜单（从模板克隆而来或由租户自建）。
     *
     * <p>与 {@code SysRole#isTemplate} 语义一致，沿 DB 列 {@code sys_menu.is_template}
     * 自 V2.02 起存在；V2.06 将该列的 Flyway 语义正式化（NOT NULL + 回填 NULL=0）。</p>
     */
    private Integer isTemplate;
}
