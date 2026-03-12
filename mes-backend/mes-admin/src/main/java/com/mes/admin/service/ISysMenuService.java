package com.mes.admin.service;

import com.mes.admin.domain.dto.SysMenuDTO;
import com.mes.admin.domain.vo.SysMenuVO;

import java.util.List;

public interface ISysMenuService {
    List<SysMenuVO> getTree();
    List<SysMenuVO> getUserTree(Long userId);
    SysMenuVO getDetail(Long id);
    Long create(SysMenuDTO dto);
    void update(Long id, SysMenuDTO dto);
    void delete(Long id);
}
