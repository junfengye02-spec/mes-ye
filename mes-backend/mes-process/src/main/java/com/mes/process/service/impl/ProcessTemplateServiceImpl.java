package com.mes.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.exception.BusinessException;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.process.domain.dto.ProcessTemplateDTO;
import com.mes.process.domain.entity.ProcessTemplate;
import com.mes.process.domain.query.ProcessTemplateQuery;
import com.mes.process.domain.vo.ProcessTemplateVO;
import com.mes.process.mapper.ProcessTemplateMapper;
import com.mes.process.service.IProcessTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工序模板 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessTemplateServiceImpl extends ServiceImpl<ProcessTemplateMapper, ProcessTemplate>
        implements IProcessTemplateService {

    @Override
    public PageResult<ProcessTemplateVO> page(ProcessTemplateQuery query) {
        LambdaQueryWrapper<ProcessTemplate> wrapper = new LambdaQueryWrapper<ProcessTemplate>()
                .like(StringUtils.hasText(query.getProcessNo()),
                        ProcessTemplate::getProcessNo, query.getProcessNo())
                .like(StringUtils.hasText(query.getProcessName()),
                        ProcessTemplate::getProcessName, query.getProcessName())
                .eq(StringUtils.hasText(query.getProductCategory()),
                        ProcessTemplate::getProductCategory, query.getProductCategory())
                .eq(StringUtils.hasText(query.getProcessType()),
                        ProcessTemplate::getProcessType, query.getProcessType())
                .orderByDesc(ProcessTemplate::getCreatedTime);

        Page<ProcessTemplate> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<ProcessTemplateVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public List<ProcessTemplateVO> tree() {
        // 查全量数据
        List<ProcessTemplate> allTemplates = list(
                new LambdaQueryWrapper<ProcessTemplate>()
                        .orderByAsc(ProcessTemplate::getProcessNo)
        );

        List<ProcessTemplateVO> allVOs = allTemplates.stream()
                .map(this::toVO)
                .toList();

        // 按 parentProcessNo 分组
        Map<String, List<ProcessTemplateVO>> childrenMap = allVOs.stream()
                .filter(vo -> StringUtils.hasText(vo.getParentProcessNo()))
                .collect(Collectors.groupingBy(ProcessTemplateVO::getParentProcessNo));

        // 构建树：根节点为 parentProcessNo 为空的节点
        List<ProcessTemplateVO> roots = new ArrayList<>();
        for (ProcessTemplateVO vo : allVOs) {
            vo.setChildren(childrenMap.getOrDefault(vo.getProcessNo(), new ArrayList<>()));
            if (!StringUtils.hasText(vo.getParentProcessNo())) {
                roots.add(vo);
            }
        }

        return roots;
    }

    @Override
    public ProcessTemplateVO getDetail(Long id) {
        ProcessTemplate entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProcessTemplateDTO dto) {
        checkProcessNoUnique(dto.getProcessNo(), null);

        // 校验父工序存在性
        if (StringUtils.hasText(dto.getParentProcessNo())) {
            long parentCount = count(new LambdaQueryWrapper<ProcessTemplate>()
                    .eq(ProcessTemplate::getProcessNo, dto.getParentProcessNo()));
            AssertUtil.isTrue(parentCount > 0, "父工序不存在: " + dto.getParentProcessNo());
        }

        ProcessTemplate entity = new ProcessTemplate();
        BeanUtils.copyProperties(dto, entity);
        save(entity);

        log.info("新增工序模板: {} - {}", entity.getProcessNo(), entity.getProcessName());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProcessTemplateDTO dto) {
        ProcessTemplate existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        checkProcessNoUnique(dto.getProcessNo(), id);

        // 校验父工序存在性
        if (StringUtils.hasText(dto.getParentProcessNo())) {
            // 防止自引用
            AssertUtil.isFalse(dto.getParentProcessNo().equals(dto.getProcessNo()),
                    "父工序不能为自身");
            long parentCount = count(new LambdaQueryWrapper<ProcessTemplate>()
                    .eq(ProcessTemplate::getProcessNo, dto.getParentProcessNo()));
            AssertUtil.isTrue(parentCount > 0, "父工序不存在: " + dto.getParentProcessNo());
        }

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);

        log.info("修改工序模板: {} - {}", existing.getProcessNo(), existing.getProcessName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProcessTemplate entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        // 检查是否有子工序引用
        long childCount = count(new LambdaQueryWrapper<ProcessTemplate>()
                .eq(ProcessTemplate::getParentProcessNo, entity.getProcessNo()));
        AssertUtil.isFalse(childCount > 0, "该工序存在子工序，无法删除");

        removeById(id);
        log.info("删除工序模板: {} - {}", entity.getProcessNo(), entity.getProcessName());
    }

    // ==================== 私有方法 ====================

    private void checkProcessNoUnique(String processNo, Long excludeId) {
        LambdaQueryWrapper<ProcessTemplate> wrapper = new LambdaQueryWrapper<ProcessTemplate>()
                .eq(ProcessTemplate::getProcessNo, processNo)
                .ne(excludeId != null, ProcessTemplate::getId, excludeId);
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXIST, "工序号已存在: " + processNo);
        }
    }

    private ProcessTemplateVO toVO(ProcessTemplate entity) {
        ProcessTemplateVO vo = new ProcessTemplateVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
