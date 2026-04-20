package com.mes.admin.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 登录端：ADMIN=管理端入口，USER=现场端入口（与账号 accountType 校验）
     */
    private String loginClient;

    /**
     * 租户编码（可选）。
     * <p>生产环境强烈建议始终传：当账号按 (tenant_id, username) 唯一时，
     * 只靠 username 不足以唯一定位用户。</p>
     * <p>若留空，则后端会按 "全局仅剩一个该 username" 的宽松模式兜底，
     * 命中多个租户同名账号时直接报 "请指定租户"。</p>
     */
    private String tenantCode;
}
