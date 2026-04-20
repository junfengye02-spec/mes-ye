package com.mes.admin.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SysMenuDTO {
    private Long parentId;
    @NotBlank(message = "菜单名称不能为空")
    private String menuName;
    private String path;
    private String component;
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;
    private String permission;
    private String icon;
    private Integer sortOrder;
    private Boolean visible;
}
