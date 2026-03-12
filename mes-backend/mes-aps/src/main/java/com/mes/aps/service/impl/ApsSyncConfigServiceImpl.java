package com.mes.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.aps.domain.dto.ApsSyncConfigDTO;
import com.mes.aps.domain.entity.ApsSyncConfig;
import com.mes.aps.domain.query.ApsSyncConfigQuery;
import com.mes.aps.domain.vo.ApsSyncConfigVO;
import com.mes.aps.mapper.ApsSyncConfigMapper;
import com.mes.aps.service.IApsSyncConfigService;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApsSyncConfigServiceImpl extends ServiceImpl<ApsSyncConfigMapper, ApsSyncConfig>
        implements IApsSyncConfigService {

    @Override
    public PageResult<ApsSyncConfigVO> page(ApsSyncConfigQuery query) {
        LambdaQueryWrapper<ApsSyncConfig> wrapper = new LambdaQueryWrapper<ApsSyncConfig>()
                .like(StringUtils.hasText(query.getConfigKey()),
                        ApsSyncConfig::getConfigKey, query.getConfigKey())
                .eq(query.getEnabled() != null,
                        ApsSyncConfig::getEnabled, query.getEnabled())
                .orderByAsc(ApsSyncConfig::getConfigKey);

        Page<ApsSyncConfig> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        List<ApsSyncConfigVO> voList = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public List<ApsSyncConfigVO> listAll() {
        return list(new LambdaQueryWrapper<ApsSyncConfig>()
                .orderByAsc(ApsSyncConfig::getConfigKey))
                .stream().map(this::toVO).toList();
    }

    @Override
    public ApsSyncConfigVO getDetail(Long id) {
        ApsSyncConfig entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    public String getConfigValue(String key, String defaultValue) {
        ApsSyncConfig config = getOne(new LambdaQueryWrapper<ApsSyncConfig>()
                .eq(ApsSyncConfig::getConfigKey, key)
                .eq(ApsSyncConfig::getEnabled, 1));
        return config != null ? config.getConfigValue() : defaultValue;
    }

    @Override
    public boolean getBooleanConfig(String key, boolean defaultValue) {
        String value = getConfigValue(key, String.valueOf(defaultValue));
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    @Override
    public int getIntConfig(String key, int defaultValue) {
        String value = getConfigValue(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ApsSyncConfigDTO dto) {
        ApsSyncConfig entity = new ApsSyncConfig();
        BeanUtils.copyProperties(dto, entity);
        entity.setCreatedTime(LocalDateTime.now());
        entity.setUpdatedTime(LocalDateTime.now());
        save(entity);
        log.info("新增APS同步配置: key={}", entity.getConfigKey());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ApsSyncConfigDTO dto) {
        ApsSyncConfig existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        existing.setUpdatedTime(LocalDateTime.now());
        updateById(existing);
        log.info("修改APS同步配置: key={}", existing.getConfigKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ApsSyncConfig entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        removeById(id);
        log.info("删除APS同步配置: key={}", entity.getConfigKey());
    }

    private ApsSyncConfigVO toVO(ApsSyncConfig entity) {
        ApsSyncConfigVO vo = new ApsSyncConfigVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
