package com.mes.admin.service;

import com.mes.admin.domain.dto.SysRoleDTO;
import com.mes.admin.domain.query.SysRoleQuery;
import com.mes.admin.domain.vo.SysRoleVO;
import com.mes.common.core.PageResult;

import java.util.List;

public interface ISysRoleService {
    PageResult<SysRoleVO> page(SysRoleQuery query);
    List<SysRoleVO> listAll();
    SysRoleVO getDetail(Long id);
    Long create(SysRoleDTO dto);
    void update(Long id, SysRoleDTO dto);
    void delete(Long id);
    void assignMenus(Long roleId, List<Long> menuIds);
    List<Long> getRoleMenuIds(Long roleId);
}
