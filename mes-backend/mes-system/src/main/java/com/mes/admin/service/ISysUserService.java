package com.mes.admin.service;

import com.mes.admin.domain.dto.SysUserDTO;
import com.mes.admin.domain.query.SysUserQuery;
import com.mes.admin.domain.vo.SysUserVO;
import com.mes.common.core.PageResult;

public interface ISysUserService {
    PageResult<SysUserVO> page(SysUserQuery query);
    SysUserVO getDetail(Long id);
    Long create(SysUserDTO dto);
    void update(Long id, SysUserDTO dto);
    void delete(Long id);
    void resetPassword(Long id, String newPassword);
}
