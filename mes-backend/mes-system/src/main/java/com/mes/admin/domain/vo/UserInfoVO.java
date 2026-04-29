package com.mes.admin.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 当前登录用户信息 VO。
 *
 * <p>P0-06（mcp24）：新增 {@code mustChangePwd} 字段，供 /auth/user-info 回显当前账号是否
 * 仍处于"必须改密"状态。此字段与 {@link LoginVO#getMustChangePwd()} 语义一致，
 * 任一为 true 时前端都应阻断业务页面并弹出修改密码对话框。</p>
 */
@Data
public class UserInfoVO {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String factoryCode;
    private Long tenantId;
    private String tenantCode;
    /** ADMIN 或 STAFF */
    private String accountType;
    private List<String> roles;
    private Set<String> permissions;

    /**
     * 是否必须修改密码（P0-06 弱口令审计结果）。
     *
     * <p>true=当前账号被审计为弱口令，登录后必须先通过 /system/user/change-my-password 完成改密</p>
     */
    @Schema(description = "是否必须修改密码：true=必须先改密")
    private Boolean mustChangePwd;
}
