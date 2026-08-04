package com.mes.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.admin.domain.entity.SysMenu;
import com.mes.framework.tenant.TenantContextHolder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT DISTINCT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id AND rm.tenant_id = #{tenantId} " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id AND ur.tenant_id = #{tenantId} " +
            "WHERE ur.user_id = #{userId} " +
            "  AND m.deleted = 0 AND m.visible = 1 " +
            "  AND (m.tenant_id = #{tenantId} OR m.tenant_id = 0) " +
            "ORDER BY m.parent_id, m.sort_order, m.id")
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId,
                                      @Param("tenantId") Long tenantId);

    default List<SysMenu> selectMenusByUserId(@Param("userId") Long userId) {
        return selectMenusByUserId(userId, TenantContextHolder.requireTenantId());
    }

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId} AND tenant_id = #{tenantId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId,
                                     @Param("tenantId") Long tenantId);

    default List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId) {
        return selectMenuIdsByRoleId(roleId, TenantContextHolder.requireTenantId());
    }
}
