package com.mes.admin.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SysUserDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Boolean enabled;
    private String factoryCode;
    /** ADMIN 或 STAFF，管理端创建用户时指定 */
    private String accountType;
    private List<Long> roleIds;
}
