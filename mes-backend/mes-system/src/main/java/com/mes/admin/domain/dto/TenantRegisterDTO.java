package com.mes.admin.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 租户注册入参。tenantCode 将成为登录子域，必须为纯小写字母数字短横线。
 */
@Data
public class TenantRegisterDTO {

    @NotBlank(message = "租户编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9-]{1,63}$",
             message = "租户编码须小写字母开头，仅允许字母数字短横线，长度 2~64")
    private String tenantCode;

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 100)
    private String tenantName;

    @Size(max = 100)
    private String contactName;

    @Email(message = "联系邮箱格式不合法")
    @Size(max = 128)
    private String contactEmail;

    /** 可选：首次管理员账号（留空默认 admin） */
    @Size(max = 50)
    private String initialAdminUsername = "admin";

    /** 可选：首次管理员密码（强制首登修改） */
    @Size(min = 8, max = 128, message = "初始密码至少 8 位")
    private String initialAdminPassword;
}
