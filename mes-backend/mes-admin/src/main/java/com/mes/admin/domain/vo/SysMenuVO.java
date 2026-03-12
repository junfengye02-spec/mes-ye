package com.mes.admin.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class SysMenuVO {
    private Long id;
    private Long parentId;
    private String menuName;
    private String path;
    private String component;
    private String menuType;
    private String permission;
    private String icon;
    private Integer sortOrder;
    private Boolean visible;
    private List<SysMenuVO> children;
}
