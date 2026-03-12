package com.mes.basic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.basic.domain.dto.WorkCenterDTO;
import com.mes.basic.domain.entity.WorkCenter;
import com.mes.basic.domain.query.WorkCenterQuery;
import com.mes.basic.domain.vo.WorkCenterVO;
import com.mes.basic.mapper.WorkCenterMapper;
import com.mes.basic.service.IWorkCenterService;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工作中心 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkCenterServiceImpl extends ServiceImpl<WorkCenterMapper, WorkCenter>
        implements IWorkCenterService {

    @Override
    public PageResult<WorkCenterVO> page(WorkCenterQuery query) {
        // 构建查询条件
        LambdaQueryWrapper<WorkCenter> wrapper = new LambdaQueryWrapper<WorkCenter>()
                .like(StringUtils.hasText(query.getWorkCenterCode()),
                        WorkCenter::getWorkCenterCode, query.getWorkCenterCode())
                .like(StringUtils.hasText(query.getWorkCenterName()),
                        WorkCenter::getWorkCenterName, query.getWorkCenterName())
                .eq(StringUtils.hasText(query.getWorkCenterCategory()),
                        WorkCenter::getWorkCenterCategory, query.getWorkCenterCategory())
                .eq(StringUtils.hasText(query.getBusinessUnit()),
                        WorkCenter::getBusinessUnit, query.getBusinessUnit())
                .orderByDesc(WorkCenter::getCreatedTime);

        // 分页查询
        Page<WorkCenter> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        // 转换为 VO
        List<WorkCenterVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public WorkCenterVO getDetail(Long id) {
        WorkCenter workCenter = getById(id);
        AssertUtil.notNull(workCenter, ResultCode.DATA_NOT_EXIST);
        return toVO(workCenter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(WorkCenterDTO dto) {
        // 校验编码唯一性
        checkCodeUnique(dto.getWorkCenterCode(), null);

        WorkCenter workCenter = new WorkCenter();
        BeanUtils.copyProperties(dto, workCenter);
        save(workCenter);

        log.info("新增工作中心: {} - {}", workCenter.getWorkCenterCode(), workCenter.getWorkCenterName());
        return workCenter.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, WorkCenterDTO dto) {
        WorkCenter existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        // 校验编码唯一性（排除自身）
        checkCodeUnique(dto.getWorkCenterCode(), id);

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);

        log.info("修改工作中心: {} - {}", existing.getWorkCenterCode(), existing.getWorkCenterName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        WorkCenter workCenter = getById(id);
        AssertUtil.notNull(workCenter, ResultCode.DATA_NOT_EXIST);

        // TODO: 检查是否被工序/工单引用，若引用则不允许删除

        removeById(id);
        log.info("删除工作中心: {} - {}", workCenter.getWorkCenterCode(), workCenter.getWorkCenterName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(List<WorkCenterDTO> dtoList, List<Long> ids) {
        AssertUtil.isTrue(dtoList.size() == ids.size(), "ID列表与数据列表数量不一致");
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            WorkCenterDTO dto = dtoList.get(i);
            WorkCenter existing = getById(id);
            AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
            checkCodeUnique(dto.getWorkCenterCode(), id);
            BeanUtils.copyProperties(dto, existing);
            existing.setId(id);
            updateById(existing);
        }
        log.info("批量修改工作中心: {} 条", ids.size());
    }

    // ==================== 私有方法 ====================

    /**
     * 校验工作中心编码唯一性
     */
    private void checkCodeUnique(String workCenterCode, Long excludeId) {
        LambdaQueryWrapper<WorkCenter> wrapper = new LambdaQueryWrapper<WorkCenter>()
                .eq(WorkCenter::getWorkCenterCode, workCenterCode)
                .ne(excludeId != null, WorkCenter::getId, excludeId);
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "工作中心编码已存在: " + workCenterCode);
        }
    }

    /**
     * Entity → VO 转换
     */
    private WorkCenterVO toVO(WorkCenter entity) {
        WorkCenterVO vo = new WorkCenterVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
