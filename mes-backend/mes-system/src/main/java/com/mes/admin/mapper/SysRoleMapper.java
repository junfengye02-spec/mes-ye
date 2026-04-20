package com.mes.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.admin.domain.entity.SysRole;
import com.mes.framework.tenant.TenantContextHolder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 按租户严格过滤的"用户所属角色列表"查询。
     */
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id AND ur.tenant_id = r.tenant_id " +
            "WHERE ur.user_id = #{userId} AND r.tenant_id = #{tenantId} AND r.deleted = 0")
    List<SysRole> selectRolesByUserIdScoped(@Param("userId") Long userId,
                                            @Param("tenantId") Long tenantId);

    /** 便捷方法，从 {@link TenantContextHolder} 读取当前租户。 */
    default List<SysRole> selectRolesByUserId(@Param("userId") Long userId) {
        return selectRolesByUserIdScoped(userId, TenantContextHolder.requireTenantId());
    }
}
