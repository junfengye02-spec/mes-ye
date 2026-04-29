package com.mes.admin.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录接口返回体。
 *
 * <p>P0-06 安全整改（mcp24）：新增 {@code mustChangePwd} 字段，当当前账号命中弱口令审计
 * （{@code sys_user.must_change_password = 1}）时，前端需在登录成功后立即弹出"强制改密"
 * 对话框并限制后续业务操作，直到调用 {@code POST /system/user/change-my-password} 改密成功。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "用户基础信息")
    private UserInfoVO userInfo;

    /**
     * 是否必须修改密码（P0-06）。
     *
     * <p>true 表示该账号被标记为弱口令，必须先完成改密才能使用；
     * false / null 可继续正常使用。</p>
     */
    @Schema(description = "是否必须修改密码：true=必须先改密；false/空=无需改密")
    private Boolean mustChangePwd;

    /**
     * 兼容旧的 3 参构造，默认 mustChangePwd=false。
     */
    public LoginVO(String accessToken, String refreshToken, UserInfoVO userInfo) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userInfo = userInfo;
        this.mustChangePwd = Boolean.FALSE;
    }
}
