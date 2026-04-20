package com.mes.admin.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SysUserVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private Boolean enabled;
    private String factoryCode;
    private Long tenantId;
    private String accountType;
    private List<SysRoleVO> roles;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
