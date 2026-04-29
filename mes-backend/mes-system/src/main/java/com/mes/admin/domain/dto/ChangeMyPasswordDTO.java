package com.mes.admin.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 当前登录用户"自助修改密码"请求体（P0-06）。
 *
 * <p>使用场景：
 * <ul>
 *   <li>弱口令审计命中后（mustChangePwd=true），前端强制弹出的改密对话框；</li>
 *   <li>用户主动在个人中心修改密码。</li>
 * </ul>
 * </p>
 *
 * <p>校验约束（服务端还会做一次完整校验）：
 * <ul>
 *   <li>旧密码非空</li>
 *   <li>新密码非空且长度 &ge; 8</li>
 *   <li>服务端另外校验：新密码必须与旧密码不同、必须满足至少 3 类字符（大写/小写/数字/特殊）</li>
 * </ul>
 * </p>
 */
@Data
public class ChangeMyPasswordDTO {

    @Schema(description = "旧密码（明文）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码（明文），长度 8-64，建议含大小写/数字/特殊符号中至少 3 类",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 64, message = "新密码长度必须在 8-64 位之间")
    private String newPassword;
}
