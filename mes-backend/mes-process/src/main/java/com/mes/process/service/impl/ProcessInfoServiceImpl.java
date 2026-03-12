package com.mes.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.process.domain.dto.ProcessInfoDTO;
import com.mes.process.domain.entity.ProcessInfo;
import com.mes.process.domain.query.ProcessInfoQuery;
import com.mes.process.domain.vo.ProcessInfoVO;
import com.mes.process.mapper.ProcessInfoMapper;
import com.mes.process.service.IProcessInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工序信息 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessInfoServiceImpl extends ServiceImpl<ProcessInfoMapper, ProcessInfo>
        implements IProcessInfoService {

    @Override
    public PageResult<ProcessInfoVO> page(ProcessInfoQuery query) {
        LambdaQueryWrapper<ProcessInfo> wrapper = new LambdaQueryWrapper<ProcessInfo>()
                .like(StringUtils.hasText(query.getProcessNo()),
                        ProcessInfo::getProcessNo, query.getProcessNo())
                .like(StringUtils.hasText(query.getProcessName()),
                        ProcessInfo::getProcessName, query.getProcessName())
                .eq(StringUtils.hasText(query.getProductCategory()),
                        ProcessInfo::getProductCategory, query.getProductCategory())
                .eq(StringUtils.hasText(query.getProcessType()),
                        ProcessInfo::getProcessType, query.getProcessType())
                .eq(query.getWorkCenterId() != null,
                        ProcessInfo::getWorkCenterId, query.getWorkCenterId())
                .orderByDesc(ProcessInfo::getCreatedTime);

        Page<ProcessInfo> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<ProcessInfoVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public ProcessInfoVO getDetail(Long id) {
        ProcessInfo entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProcessInfoDTO dto) {
        ProcessInfo entity = new ProcessInfo();
        BeanUtils.copyProperties(dto, entity);
        save(entity);

        log.info("新增工序信息: {} - {}", entity.getProcessNo(), entity.getProcessName());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProcessInfoDTO dto) {
        ProcessInfo existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);

        log.info("修改工序信息: {} - {}", existing.getProcessNo(), existing.getProcessName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProcessInfo entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        removeById(id);
        log.info("删除工序信息: {} - {}", entity.getProcessNo(), entity.getProcessName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(List<Long> ids, List<ProcessInfoDTO> dtoList) {
        AssertUtil.isTrue(ids.size() == dtoList.size(), "ID列表与数据列表数量不一致");
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            ProcessInfoDTO dto = dtoList.get(i);
            ProcessInfo existing = getById(id);
            AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
            BeanUtils.copyProperties(dto, existing);
            existing.setId(id);
            updateById(existing);
        }
        log.info("批量修改工序信息: {} 条", ids.size());
    }

    // ==================== 私有方法 ====================

    private ProcessInfoVO toVO(ProcessInfo entity) {
        ProcessInfoVO vo = new ProcessInfoVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
