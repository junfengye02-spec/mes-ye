package com.mes.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.admin.domain.entity.SysTenant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysTenantMapper extends BaseMapper<SysTenant> {

    /**
     * 按租户编码精确查询。此查询只命中 sys_tenant 表，该表已列入
     * MyBatis-Plus 租户拦截器的忽略名单，因此不会自动拼 where tenant_id，
     * 可用于登录流程中"未知租户下根据 tenant_code 定位租户"的场景。
     */
    @Select("SELECT * FROM sys_tenant WHERE tenant_code = #{tenantCode} AND deleted = 0 LIMIT 1")
    SysTenant selectByTenantCode(@Param("tenantCode") String tenantCode);
}
