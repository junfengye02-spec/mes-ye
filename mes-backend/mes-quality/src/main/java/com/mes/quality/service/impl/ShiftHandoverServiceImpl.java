package com.mes.quality.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.quality.domain.dto.ShiftHandoverDTO;
import com.mes.quality.domain.entity.ShiftHandover;
import com.mes.quality.domain.query.ShiftHandoverQuery;
import com.mes.quality.domain.vo.ShiftHandoverVO;
import com.mes.quality.enums.HandoverStatus;
import com.mes.quality.mapper.ShiftHandoverMapper;
import com.mes.quality.service.IShiftHandoverService;
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
public class ShiftHandoverServiceImpl extends ServiceImpl<ShiftHandoverMapper, ShiftHandover>
        implements IShiftHandoverService {

    @Override
    public PageResult<ShiftHandoverVO> page(ShiftHandoverQuery query) {
        LambdaQueryWrapper<ShiftHandover> wrapper = new LambdaQueryWrapper<ShiftHandover>()
                .like(StringUtils.hasText(query.getProjectName()), ShiftHandover::getProjectName, query.getProjectName())
                .eq(query.getHandoverDate() != null, ShiftHandover::getHandoverDate, query.getHandoverDate())
                .like(StringUtils.hasText(query.getHandoverTeamName()), ShiftHandover::getHandoverTeamName, query.getHandoverTeamName())
                .eq(StringUtils.hasText(query.getStatus()), ShiftHandover::getStatus, query.getStatus())
                .orderByDesc(ShiftHandover::getCreatedTime);

        Page<ShiftHandover> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<ShiftHandoverVO> voList = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public ShiftHandoverVO getDetail(Long id) {
        ShiftHandover entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ShiftHandoverDTO dto) {
        ShiftHandover entity = new ShiftHandover();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(HandoverStatus.PENDING.getCode());
        entity.setCreatedBy("system"); // TODO: SecurityContext
        entity.setCreatedTime(LocalDateTime.now());
        entity.setUpdatedTime(LocalDateTime.now());
        baseMapper.insert(entity);

        log.info("新增交班记录: id={}", entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ShiftHandoverDTO dto) {
        ShiftHandover existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(HandoverStatus.PENDING.getCode().equals(existing.getStatus()),
                "仅待接收状态的交班记录可以编辑");

        String status = existing.getStatus();
        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        existing.setStatus(status);
        existing.setUpdatedTime(LocalDateTime.now());
        baseMapper.updateById(existing);

        log.info("修改交班记录: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long id) {
        ShiftHandover entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        AssertUtil.isTrue(HandoverStatus.PENDING.getCode().equals(entity.getStatus()),
                "仅待接收状态的交班记录可以接收");

        entity.setStatus(HandoverStatus.RECEIVED.getCode());
        entity.setUpdatedTime(LocalDateTime.now());
        baseMapper.updateById(entity);

        log.info("交班记录接收: id={}", id);
    }

    private ShiftHandoverVO toVO(ShiftHandover entity) {
        ShiftHandoverVO vo = new ShiftHandoverVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
