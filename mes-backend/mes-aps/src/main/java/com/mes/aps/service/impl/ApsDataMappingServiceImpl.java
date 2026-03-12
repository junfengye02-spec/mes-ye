package com.mes.aps.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.aps.domain.dto.ApsDataMappingDTO;
import com.mes.aps.domain.entity.ApsDataMapping;
import com.mes.aps.domain.query.ApsDataMappingQuery;
import com.mes.aps.domain.vo.ApsDataMappingVO;
import com.mes.aps.mapper.ApsDataMappingMapper;
import com.mes.aps.service.IApsDataMappingService;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApsDataMappingServiceImpl extends ServiceImpl<ApsDataMappingMapper, ApsDataMapping>
        implements IApsDataMappingService {

    @Override
    public PageResult<ApsDataMappingVO> page(ApsDataMappingQuery query) {
        LambdaQueryWrapper<ApsDataMapping> wrapper = new LambdaQueryWrapper<ApsDataMapping>()
                .eq(StringUtils.hasText(query.getMappingType()),
                        ApsDataMapping::getMappingType, query.getMappingType())
                .like(StringUtils.hasText(query.getMesCode()),
                        ApsDataMapping::getMesCode, query.getMesCode())
                .like(StringUtils.hasText(query.getApsCode()),
                        ApsDataMapping::getApsCode, query.getApsCode())
                .eq(query.getEnabled() != null,
                        ApsDataMapping::getEnabled, query.getEnabled())
                .orderByAsc(ApsDataMapping::getMappingType)
                .orderByAsc(ApsDataMapping::getMesCode);

        Page<ApsDataMapping> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        List<ApsDataMappingVO> voList = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public ApsDataMappingVO getDetail(Long id) {
        ApsDataMapping entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ApsDataMappingDTO dto) {
        ApsDataMapping entity = new ApsDataMapping();
        BeanUtils.copyProperties(dto, entity);
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        save(entity);
        log.info("新增APS数据映射: type={}, mesCode={} -> apsCode={}",
                entity.getMappingType(), entity.getMesCode(), entity.getApsCode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ApsDataMappingDTO dto) {
        ApsDataMapping existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);
        log.info("修改APS数据映射: type={}, mesCode={} -> apsCode={}",
                existing.getMappingType(), existing.getMesCode(), existing.getApsCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ApsDataMapping entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        removeById(id);
        log.info("删除APS数据映射: type={}, mesCode={}", entity.getMappingType(), entity.getMesCode());
    }

    @Override
    public String getMesCode(String mappingType, String apsCode) {
        ApsDataMapping mapping = getOne(new LambdaQueryWrapper<ApsDataMapping>()
                .eq(ApsDataMapping::getMappingType, mappingType)
                .eq(ApsDataMapping::getApsCode, apsCode)
                .eq(ApsDataMapping::getEnabled, 1));
        return mapping != null ? mapping.getMesCode() : null;
    }

    @Override
    public String getApsCode(String mappingType, String mesCode) {
        ApsDataMapping mapping = getOne(new LambdaQueryWrapper<ApsDataMapping>()
                .eq(ApsDataMapping::getMappingType, mappingType)
                .eq(ApsDataMapping::getMesCode, mesCode)
                .eq(ApsDataMapping::getEnabled, 1));
        return mapping != null ? mapping.getApsCode() : null;
    }

    private ApsDataMappingVO toVO(ApsDataMapping entity) {
        ApsDataMappingVO vo = new ApsDataMappingVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
