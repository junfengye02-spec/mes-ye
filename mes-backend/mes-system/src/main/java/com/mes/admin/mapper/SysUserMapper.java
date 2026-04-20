package com.mes.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.admin.domain.entity.SysUser;
import com.mes.framework.tenant.TenantContextHolder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 按租户过滤的角色编码查询。
     */
    @Select("SELECT r.role_code FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id AND ur.tenant_id = r.tenant_id " +
            "WHERE ur.user_id = #{userId} AND r.tenant_id = #{tenantId} " +
            "  AND r.deleted = 0 AND r.enabled = 1")
    List<String> selectRoleCodesByUserIdScoped(@Param("userId") Long userId,
                                               @Param("tenantId") Long tenantId);

    /**
     * 按租户过滤的权限码查询。菜单同时支持租户自身与平台模板（tenant_id=0），
     * 用于租户尚未完成模板克隆的场景。
     */
    @Select("SELECT DISTINCT m.permission FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id AND rm.tenant_id = #{tenantId} " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id AND ur.tenant_id = #{tenantId} " +
            "WHERE ur.user_id = #{userId} " +
            "  AND m.deleted = 0 AND m.permission IS NOT NULL AND m.permission != '' " +
            "  AND (m.tenant_id = #{tenantId} OR m.tenant_id = 0)")
    List<String> selectPermissionsByUserIdScoped(@Param("userId") Long userId,
                                                 @Param("tenantId") Long tenantId);

    /**
     * 便捷方法：从 {@link TenantContextHolder} 读取当前租户，转发到
     * {@link #selectRoleCodesByUserIdScoped(Long, Long)}。
     * 用于已经处在租户请求上下文中的业务代码。
     */
    default List<String> selectRoleCodesByUserId(@Param("userId") Long userId) {
        return selectRoleCodesByUserIdScoped(userId, TenantContextHolder.requireTenantId());
    }

    /**
     * 便捷方法：从 {@link TenantContextHolder} 读取当前租户，转发到
     * {@link #selectPermissionsByUserIdScoped(Long, Long)}。
     */
    default List<String> selectPermissionsByUserId(@Param("userId") Long userId) {
        return selectPermissionsByUserIdScoped(userId, TenantContextHolder.requireTenantId());
    }
}
