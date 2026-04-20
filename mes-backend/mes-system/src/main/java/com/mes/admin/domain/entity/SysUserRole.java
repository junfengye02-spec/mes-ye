package com.mes.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户角色关联。复合主键 (tenant_id, user_id, role_id)。
 *
 * <p>tenant_id 为冗余列（与 user / role 所属租户一致），主要作用：</p>
 * <ul>
 *   <li>防止"张三在租户 A，却被误绑到租户 B 的角色"；</li>
 *   <li>便于按租户批量清理 / 备份；</li>
 *   <li>配合唯一键阻止跨租户错挂。</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user_role")
public class SysUserRole {

    private Long tenantId;
    private Long userId;
    private Long roleId;
}
