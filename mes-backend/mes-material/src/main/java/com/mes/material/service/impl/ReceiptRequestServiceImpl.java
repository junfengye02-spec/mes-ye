package com.mes.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.common.utils.NumberGenerator;
import com.mes.material.domain.dto.ReceiptRequestDTO;
import com.mes.material.domain.entity.FinishedGoodsReceiptRequest;
import com.mes.material.domain.query.ReceiptRequestQuery;
import com.mes.material.domain.vo.ReceiptRequestVO;
import com.mes.material.enums.ReceiptStatus;
import com.mes.material.mapper.FinishedGoodsReceiptRequestMapper;
import com.mes.material.service.IReceiptRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 完工入库申请 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptRequestServiceImpl extends ServiceImpl<FinishedGoodsReceiptRequestMapper, FinishedGoodsReceiptRequest>
        implements IReceiptRequestService {

    @Override
    public PageResult<ReceiptRequestVO> page(ReceiptRequestQuery query) {
        LambdaQueryWrapper<FinishedGoodsReceiptRequest> wrapper = new LambdaQueryWrapper<FinishedGoodsReceiptRequest>()
                .like(StringUtils.hasText(query.getRequestNo()),
                        FinishedGoodsReceiptRequest::getRequestNo, query.getRequestNo())
                .eq(StringUtils.hasText(query.getReceiptType()),
                        FinishedGoodsReceiptRequest::getReceiptType, query.getReceiptType())
                .eq(query.getWorkOrderId() != null,
                        FinishedGoodsReceiptRequest::getWorkOrderId, query.getWorkOrderId())
                .eq(StringUtils.hasText(query.getStatus()),
                        FinishedGoodsReceiptRequest::getStatus, query.getStatus())
                .orderByDesc(FinishedGoodsReceiptRequest::getCreatedTime);

        Page<FinishedGoodsReceiptRequest> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<ReceiptRequestVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public ReceiptRequestVO getDetail(Long id) {
        FinishedGoodsReceiptRequest entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ReceiptRequestDTO dto) {
        FinishedGoodsReceiptRequest entity = new FinishedGoodsReceiptRequest();
        BeanUtils.copyProperties(dto, entity);
        
        // 自动生成申请单号
        if (!StringUtils.hasText(entity.getRequestNo())) {
            entity.setRequestNo(NumberGenerator.generate("RK-SQ"));
        }
        
        entity.setStatus(ReceiptStatus.CREATED.getCode());
        save(entity);
        
        log.info("新增完工入库申请: {}", entity.getRequestNo());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ReceiptRequestDTO dto) {
        FinishedGoodsReceiptRequest existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
        
        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);
        
        log.info("修改完工入库申请: {}", existing.getRequestNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FinishedGoodsReceiptRequest entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        
        removeById(id);
        
        log.info("删除完工入库申请: {}", entity.getRequestNo());
    }

    // ==================== 私有方法 ====================

    private ReceiptRequestVO toVO(FinishedGoodsReceiptRequest entity) {
        ReceiptRequestVO vo = new ReceiptRequestVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
