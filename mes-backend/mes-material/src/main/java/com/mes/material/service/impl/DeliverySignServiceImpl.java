package com.mes.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mes.common.core.PageResult;
import com.mes.common.utils.AssertUtil;
import com.mes.material.domain.dto.DeliverySignDTO;
import com.mes.material.domain.entity.DeliverySign;
import com.mes.material.domain.entity.StorageInventory;
import com.mes.material.domain.query.DeliverySignQuery;
import com.mes.material.domain.vo.DeliverySignVO;
import com.mes.material.mapper.DeliverySignMapper;
import com.mes.material.service.IDeliverySignService;
import com.mes.material.service.IStorageInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 发货签收 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliverySignServiceImpl implements IDeliverySignService {

    private final DeliverySignMapper mapper;
    private final IStorageInventoryService storageInventoryService;

    @Override
    public PageResult<DeliverySignVO> page(DeliverySignQuery query) {
        LambdaQueryWrapper<DeliverySign> wrapper = new LambdaQueryWrapper<DeliverySign>()
                .like(StringUtils.hasText(query.getWorkOrderNo()),
                        DeliverySign::getWorkOrderNo, query.getWorkOrderNo())
                .eq(query.getWorkOrderId() != null,
                        DeliverySign::getWorkOrderId, query.getWorkOrderId())
                .like(StringUtils.hasText(query.getMaterialCode()),
                        DeliverySign::getMaterialCode, query.getMaterialCode())
                .orderByDesc(DeliverySign::getCreatedTime);

        Page<DeliverySign> page = mapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<DeliverySignVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DeliverySignDTO dto) {
        DeliverySign entity = new DeliverySign();
        BeanUtils.copyProperties(dto, entity);
        entity.setCreatedTime(LocalDateTime.now());
        entity.setUpdatedTime(LocalDateTime.now());
        mapper.insert(entity);
        
        log.info("新增发货签收: id={}", entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        DeliverySign entity = mapper.selectById(id);
        AssertUtil.notNull(entity, "签收记录不存在");

        // 库存联动：签收确认后扣减库存（从仓库发出）
        if (entity.getMaterialId() != null && entity.getPendingSignQty() != null) {
            StorageInventory inventory = storageInventoryService.getOne(
                    new LambdaQueryWrapper<StorageInventory>()
                            .eq(StorageInventory::getMaterialId, entity.getMaterialId())
                            .last("LIMIT 1"));
            if (inventory != null) {
                storageInventoryService.deductStock(inventory.getId(), entity.getPendingSignQty());
                log.info("签收扣减库存: materialId={}, qty={}", entity.getMaterialId(), entity.getPendingSignQty());
            }
        }

        log.info("确认发货签收: id={}", id);
    }

    // ==================== 私有方法 ====================

    private DeliverySignVO toVO(DeliverySign entity) {
        DeliverySignVO vo = new DeliverySignVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
