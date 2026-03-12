package com.mes.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.common.utils.NumberGenerator;
import com.mes.material.domain.dto.RequisitionOrderDTO;
import com.mes.material.domain.entity.RequisitionOrder;
import com.mes.material.domain.query.RequisitionOrderQuery;
import com.mes.material.domain.vo.RequisitionOrderVO;
import com.mes.material.enums.RequisitionStatus;
import com.mes.material.mapper.RequisitionOrderMapper;
import com.mes.material.service.IRequisitionOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 生产领料单管理 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionOrderServiceImpl extends ServiceImpl<RequisitionOrderMapper, RequisitionOrder>
        implements IRequisitionOrderService {

    @Override
    public PageResult<RequisitionOrderVO> page(RequisitionOrderQuery query) {
        LambdaQueryWrapper<RequisitionOrder> wrapper = new LambdaQueryWrapper<RequisitionOrder>()
                .like(StringUtils.hasText(query.getWorkOrderNo()),
                        RequisitionOrder::getWorkOrderNo, query.getWorkOrderNo())
                .like(StringUtils.hasText(query.getMaterialCode()),
                        RequisitionOrder::getMaterialCode, query.getMaterialCode())
                .eq(StringUtils.hasText(query.getStatus()),
                        RequisitionOrder::getStatus, query.getStatus())
                .orderByDesc(RequisitionOrder::getCreatedTime);

        Page<RequisitionOrder> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<RequisitionOrderVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public RequisitionOrderVO getDetail(Long id) {
        RequisitionOrder entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RequisitionOrderDTO dto) {
        RequisitionOrder entity = new RequisitionOrder();
        BeanUtils.copyProperties(dto, entity);

        // 自动生成发货申请号（如未提供）
        if (!StringUtils.hasText(entity.getDeliveryRequestNo())) {
            entity.setDeliveryRequestNo(NumberGenerator.generate("RO"));
        }

        entity.setStatus(RequisitionStatus.CREATED.getCode());
        save(entity);

        log.info("新增领料单（按物料）: id={}, materialCode={}", entity.getId(), entity.getMaterialCode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RequisitionOrderDTO dto) {
        RequisitionOrder existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);

        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);

        log.info("修改领料单（按物料）: id={}, materialCode={}", id, existing.getMaterialCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        RequisitionOrder entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        removeById(id);

        log.info("删除领料单（按物料）: id={}, materialCode={}", id, entity.getMaterialCode());
    }

    // ==================== 私有方法 ====================

    private RequisitionOrderVO toVO(RequisitionOrder entity) {
        RequisitionOrderVO vo = new RequisitionOrderVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
