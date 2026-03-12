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
    private List<Long> roleIds;
}
