package com.mes.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.process.domain.dto.SprayConditionDTO;
import com.mes.process.domain.entity.SprayCondition;
import com.mes.process.domain.query.SprayConditionQuery;
import com.mes.process.domain.vo.SprayConditionVO;
import com.mes.process.mapper.SprayConditionMapper;
import com.mes.process.service.ISprayConditionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 喷涂条件 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SprayConditionServiceImpl extends ServiceImpl<SprayConditionMapper, SprayCondition>
        implements ISprayConditionService {

    @Override
    public PageResult<SprayConditionVO> page(SprayConditionQuery query) {
        LambdaQueryWrapper<SprayCondition> wrapper = new LambdaQueryWrapper<SprayCondition>()
                .like(StringUtils.hasText(query.getConditionNo()),
                        SprayCondition::getConditionNo, query.getConditionNo())
                .like(StringUtils.hasText(query.getSprayGunModel()),
                        SprayCondition::getSprayGunModel, query.getSprayGunModel())
                .like(StringUtils.hasText(query.getEquipment()),
                        SprayCondition::getEquipment, query.getEquipment())
                .orderByDesc(SprayCondition::getCreatedTime);

        Page<SprayCondition> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<SprayConditionVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public SprayConditionVO getDetail(Long id) {
        SprayCondition entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SprayConditionDTO dto) {
        checkConditionNoUnique(dto.getConditionNo(), null);

        SprayCondition entity = new SprayCondition();
        BeanUtils.copyProperties(dto, entity);
        save(entity);

        log.info("新增喷涂条件: {}", entity.getConditionNo());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SprayConditionDTO dto) {
        SprayCondition existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        checkConditionNoUnique(dto.getConditionNo(), id);

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);

        log.info("修改喷涂条件: {}", existing.getConditionNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SprayCondition entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        removeById(id);
        log.info("删除喷涂条件: {}", entity.getConditionNo());
    }

    // ==================== 私有方法 ====================

    private void checkConditionNoUnique(String conditionNo, Long excludeId) {
        LambdaQueryWrapper<SprayCondition> wrapper = new LambdaQueryWrapper<SprayCondition>()
                .eq(SprayCondition::getConditionNo, conditionNo)
                .ne(excludeId != null, SprayCondition::getId, excludeId);
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "条件号已存在: " + conditionNo);
        }
    }

    private SprayConditionVO toVO(SprayCondition entity) {
        SprayConditionVO vo = new SprayConditionVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
