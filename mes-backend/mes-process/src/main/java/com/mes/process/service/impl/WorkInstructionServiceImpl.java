package com.mes.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.process.domain.dto.WorkInstructionDTO;
import com.mes.process.domain.dto.WorkInstructionPersonDTO;
import com.mes.process.domain.entity.WorkInstruction;
import com.mes.process.domain.entity.WorkInstructionPerson;
import com.mes.process.domain.query.WorkInstructionQuery;
import com.mes.process.domain.vo.WorkInstructionPersonVO;
import com.mes.process.domain.vo.WorkInstructionVO;
import com.mes.process.mapper.WorkInstructionMapper;
import com.mes.process.mapper.WorkInstructionPersonMapper;
import com.mes.process.service.IWorkInstructionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 指导书 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkInstructionServiceImpl extends ServiceImpl<WorkInstructionMapper, WorkInstruction>
        implements IWorkInstructionService {

    private final WorkInstructionPersonMapper personMapper;

    @Override
    public PageResult<WorkInstructionVO> page(WorkInstructionQuery query) {
        LambdaQueryWrapper<WorkInstruction> wrapper = new LambdaQueryWrapper<WorkInstruction>()
                .like(StringUtils.hasText(query.getInstructionCode()),
                        WorkInstruction::getInstructionCode, query.getInstructionCode())
                .eq(StringUtils.hasText(query.getLevel()),
                        WorkInstruction::getLevel, query.getLevel())
                .eq(StringUtils.hasText(query.getStatus()),
                        WorkInstruction::getStatus, query.getStatus())
                .orderByDesc(WorkInstruction::getCreatedTime);

        Page<WorkInstruction> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<WorkInstructionVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public WorkInstructionVO getDetail(Long id) {
        WorkInstruction entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        WorkInstructionVO vo = toVO(entity);
        // 查询关联人员列表
        List<WorkInstructionPerson> persons = personMapper.selectList(
                new LambdaQueryWrapper<WorkInstructionPerson>()
                        .eq(WorkInstructionPerson::getInstructionId, id)
        );
        vo.setPersons(persons.stream().map(this::toPersonVO).toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WorkInstructionDTO dto) {
        checkCodeUnique(dto.getInstructionCode(), null);

        WorkInstruction entity = new WorkInstruction();
        BeanUtils.copyProperties(dto, entity);
        save(entity);

        // 保存人员子表
        savePersons(entity.getId(), dto.getPersons());

        log.info("新增指导书: {}", entity.getInstructionCode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, WorkInstructionDTO dto) {
        WorkInstruction existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        checkCodeUnique(dto.getInstructionCode(), id);

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);

        // 先删后插更新人员子表
        personMapper.delete(new LambdaQueryWrapper<WorkInstructionPerson>()
                .eq(WorkInstructionPerson::getInstructionId, id));
        savePersons(id, dto.getPersons());

        log.info("修改指导书: {}", existing.getInstructionCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        WorkInstruction entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        // 级联删除人员
        personMapper.delete(new LambdaQueryWrapper<WorkInstructionPerson>()
                .eq(WorkInstructionPerson::getInstructionId, id));
        removeById(id);

        log.info("删除指导书: {}", entity.getInstructionCode());
    }

    // ==================== 私有方法 ====================

    private void checkCodeUnique(String instructionCode, Long excludeId) {
        LambdaQueryWrapper<WorkInstruction> wrapper = new LambdaQueryWrapper<WorkInstruction>()
                .eq(WorkInstruction::getInstructionCode, instructionCode)
                .ne(excludeId != null, WorkInstruction::getId, excludeId);
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "指导书编号已存在: " + instructionCode);
        }
    }

    private void savePersons(Long instructionId, List<WorkInstructionPersonDTO> personDTOs) {
        if (CollectionUtils.isEmpty(personDTOs)) {
            return;
        }
        for (WorkInstructionPersonDTO personDTO : personDTOs) {
            WorkInstructionPerson person = new WorkInstructionPerson();
            BeanUtils.copyProperties(personDTO, person);
            person.setInstructionId(instructionId);
            personMapper.insert(person);
        }
    }

    private WorkInstructionVO toVO(WorkInstruction entity) {
        WorkInstructionVO vo = new WorkInstructionVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private WorkInstructionPersonVO toPersonVO(WorkInstructionPerson entity) {
        WorkInstructionPersonVO vo = new WorkInstructionPersonVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
