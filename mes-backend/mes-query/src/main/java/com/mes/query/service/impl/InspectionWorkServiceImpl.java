package com.mes.query.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.event.DispatchTaskCompletedEvent;
import com.mes.common.event.RecheckCompletedEvent;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.query.domain.entity.InspectionWork;
import com.mes.query.domain.query.InspectionWorkQuery;
import com.mes.query.domain.vo.InspectionWorkVO;
import com.mes.query.mapper.InspectionWorkMapper;
import com.mes.query.service.IInspectionWorkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class InspectionWorkServiceImpl extends ServiceImpl<InspectionWorkMapper, InspectionWork>
        implements IInspectionWorkService {

    @Override
    public PageResult<InspectionWorkVO> page(InspectionWorkQuery query) {
        LambdaQueryWrapper<InspectionWork> wrapper = new LambdaQueryWrapper<InspectionWork>()
                .like(StringUtils.hasText(query.getWorkNo()), InspectionWork::getWorkNo, query.getWorkNo())
                .like(StringUtils.hasText(query.getWorkName()), InspectionWork::getWorkName, query.getWorkName())
                .eq(StringUtils.hasText(query.getWorkStatus()), InspectionWork::getWorkStatus, query.getWorkStatus())
                .eq(query.getWorkOrderId() != null, InspectionWork::getWorkOrderId, query.getWorkOrderId())
                .eq(StringUtils.hasText(query.getInspectCategory()), InspectionWork::getInspectCategory, query.getInspectCategory())
                .orderByDesc(InspectionWork::getCreatedTime);

        Page<InspectionWork> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<InspectionWorkVO> voList = page.getRecords().stream().map(e -> {
            InspectionWorkVO vo = new InspectionWorkVO();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).toList();
        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public InspectionWorkVO getDetail(Long id) {
        InspectionWork entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        InspectionWorkVO vo = new InspectionWorkVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void projectDispatchCompletion(DispatchTaskCompletedEvent event) {
        BigDecimal inspectQty = defaultQty(event.getActualQty());
        InspectionWork projection = new InspectionWork();
        projection.setWorkNo("IW-DISPATCH-" + event.getDispatchTaskId());
        projection.setWorkName(resolveWorkName(event.getWorkName(), event.getWorkOrderNo(), "完工检验"));
        projection.setPlanInspectQty(inspectQty);
        projection.setInspectedQty(inspectQty);
        projection.setQualifiedQty(isPass(event.getQualityResult()) ? inspectQty : BigDecimal.ZERO);
        projection.setUnqualifiedQty(isFail(event.getQualityResult()) ? inspectQty : BigDecimal.ZERO);
        projection.setJudgment(resolveJudgment(event.getQualityResult()));
        projection.setIsCheckPoint(1);
        projection.setDispatchStatus("COMPLETED");
        projection.setWorkStatus("COMPLETED");
        projection.setInspectType("过程检验");
        projection.setInspectCategory("完工检验");
        projection.setQcOrg("质量部");
        projection.setActualEndTime(event.getActualEndTime());
        projection.setIsReportPoint(1);
        projection.setWorkOrderId(event.getWorkOrderId());
        projection.setWorkOrderNo(event.getWorkOrderNo());
        projection.setDescription(buildDispatchDescription(event));
        upsertProjection(projection);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void projectRecheckCompletion(RecheckCompletedEvent event) {
        InspectionWork projection = new InspectionWork();
        projection.setWorkNo("IW-RECHECK-" + event.getRecheckId());
        projection.setWorkName(resolveWorkName(event.getMaterialName(), event.getWorkOrderNo(), "复检"));
        projection.setPlanInspectQty(BigDecimal.ZERO);
        projection.setInspectedQty(BigDecimal.ZERO);
        projection.setQualifiedQty(BigDecimal.ZERO);
        projection.setUnqualifiedQty(BigDecimal.ZERO);
        projection.setDispatchStatus(event.getDispatchTaskId() == null ? null : "COMPLETED");
        projection.setWorkStatus("COMPLETED");
        projection.setInspectType("复检");
        projection.setInspectCategory("复检");
        projection.setQcOrg("质量部");
        projection.setActualEndTime(event.getCompletedTime());
        projection.setIsReportPoint(1);
        projection.setWorkOrderId(event.getWorkOrderId());
        projection.setWorkOrderNo(event.getWorkOrderNo());
        projection.setDescription("复检申请 " + event.getRecheckId() + " 已完结");
        upsertProjection(projection);
    }

    private void upsertProjection(InspectionWork projection) {
        InspectionWork existing = baseMapper.selectOne(new LambdaQueryWrapper<InspectionWork>()
                .eq(InspectionWork::getWorkNo, projection.getWorkNo()));
        if (existing == null) {
            save(projection);
            return;
        }

        projection.setId(existing.getId());
        updateById(projection);
    }

    private BigDecimal defaultQty(BigDecimal qty) {
        return qty == null ? BigDecimal.ZERO : qty;
    }

    private boolean isPass(String qualityResult) {
        return "PASS".equalsIgnoreCase(qualityResult);
    }

    private boolean isFail(String qualityResult) {
        return "FAIL".equalsIgnoreCase(qualityResult);
    }

    private String resolveJudgment(String qualityResult) {
        if (isPass(qualityResult)) {
            return "合格";
        }
        if (isFail(qualityResult)) {
            return "不合格";
        }
        return StringUtils.hasText(qualityResult) ? qualityResult : null;
    }

    private String resolveWorkName(String preferredName, String fallbackName, String suffix) {
        String baseName = StringUtils.hasText(preferredName) ? preferredName : fallbackName;
        if (!StringUtils.hasText(baseName)) {
            return suffix;
        }
        return baseName + suffix;
    }

    private String buildDispatchDescription(DispatchTaskCompletedEvent event) {
        StringBuilder description = new StringBuilder("派工任务完工后自动生成检验作业");
        if (StringUtils.hasText(event.getWorkNo())) {
            description.append("；工序=").append(event.getWorkNo());
        }
        if (StringUtils.hasText(event.getQualityResult())) {
            description.append("；质量结果=").append(event.getQualityResult());
        }
        if (StringUtils.hasText(event.getRemark())) {
            description.append("；备注=").append(event.getRemark());
        }
        return description.toString();
    }
}
