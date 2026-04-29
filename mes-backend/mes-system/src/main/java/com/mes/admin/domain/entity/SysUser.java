package com.mes.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mes.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private Boolean enabled;
    private String factoryCode;
    private Long tenantId;
    /** ADMIN=管理端+现场端；STAFF=仅现场端 */
    private String accountType;

    /**
     * 是否需要首次登录强制修改密码（P0-06）。
     *
     * <p>取值：0=正常；1=必须改密</p>
     *
     * <p>触发来源：
     * <ul>
     *   <li>启动时 {@code WeakPasswordAuditor} 巡检到 BCrypt 密文命中已知弱口令常量；</li>
     *   <li>管理员重置密码后可主动设为 1（后续扩展）；</li>
     *   <li>用户通过 {@code /system/user/change-my-password} 改密成功后复位为 0。</li>
     * </ul>
     * </p>
     */
    private Integer mustChangePassword;
}
