package com.mes.aps.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mes.aps.domain.dto.ApsSyncConfigDTO;
import com.mes.aps.domain.entity.ApsSyncConfig;
import com.mes.aps.domain.query.ApsSyncConfigQuery;
import com.mes.aps.domain.vo.ApsSyncConfigVO;
import com.mes.common.core.PageResult;

import java.util.List;

public interface IApsSyncConfigService extends IService<ApsSyncConfig> {
    PageResult<ApsSyncConfigVO> page(ApsSyncConfigQuery query);
    List<ApsSyncConfigVO> listAll();
    ApsSyncConfigVO getDetail(Long id);
    String getConfigValue(String key, String defaultValue);
    boolean getBooleanConfig(String key, boolean defaultValue);
    int getIntConfig(String key, int defaultValue);
    Long create(ApsSyncConfigDTO dto);
    void update(Long id, ApsSyncConfigDTO dto);
    void delete(Long id);
}
