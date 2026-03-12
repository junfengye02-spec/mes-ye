package com.mes.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.process.domain.dto.MachiningProgramDTO;
import com.mes.process.domain.entity.MachiningProgram;
import com.mes.process.domain.query.MachiningProgramQuery;
import com.mes.process.domain.vo.MachiningProgramVO;
import com.mes.process.mapper.MachiningProgramMapper;
import com.mes.process.service.IMachiningProgramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 机械加工程序 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MachiningProgramServiceImpl extends ServiceImpl<MachiningProgramMapper, MachiningProgram>
        implements IMachiningProgramService {

    @Override
    public PageResult<MachiningProgramVO> page(MachiningProgramQuery query) {
        LambdaQueryWrapper<MachiningProgram> wrapper = new LambdaQueryWrapper<MachiningProgram>()
                .like(StringUtils.hasText(query.getGCode()),
                        MachiningProgram::getGCode, query.getGCode())
                .like(StringUtils.hasText(query.getProductName()),
                        MachiningProgram::getProductName, query.getProductName())
                .orderByDesc(MachiningProgram::getCreatedTime);

        Page<MachiningProgram> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<MachiningProgramVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public MachiningProgramVO getDetail(Long id) {
        MachiningProgram entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MachiningProgramDTO dto) {
        checkGCodeUnique(dto.getGCode(), null);

        MachiningProgram entity = new MachiningProgram();
        BeanUtils.copyProperties(dto, entity);
        save(entity);

        log.info("新增机械加工程序: {}", entity.getGCode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MachiningProgramDTO dto) {
        MachiningProgram existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        checkGCodeUnique(dto.getGCode(), id);

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);

        log.info("修改机械加工程序: {}", existing.getGCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MachiningProgram entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        removeById(id);
        log.info("删除机械加工程序: {}", entity.getGCode());
    }

    // ==================== 私有方法 ====================

    private void checkGCodeUnique(String gCode, Long excludeId) {
        LambdaQueryWrapper<MachiningProgram> wrapper = new LambdaQueryWrapper<MachiningProgram>()
                .eq(MachiningProgram::getGCode, gCode)
                .ne(excludeId != null, MachiningProgram::getId, excludeId);
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "G-code已存在: " + gCode);
        }
    }

    private MachiningProgramVO toVO(MachiningProgram entity) {
        MachiningProgramVO vo = new MachiningProgramVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
