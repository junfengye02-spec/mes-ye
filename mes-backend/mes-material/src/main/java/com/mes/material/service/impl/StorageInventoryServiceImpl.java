package com.mes.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.material.domain.dto.StorageInventoryDTO;
import com.mes.material.domain.entity.StorageInventory;
import com.mes.material.domain.query.StorageInventoryQuery;
import com.mes.material.domain.vo.StorageInventoryVO;
import com.mes.material.mapper.StorageInventoryMapper;
import com.mes.material.service.IStorageInventoryService;
import com.mes.common.event.ApsSyncEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 存储地点库存 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageInventoryServiceImpl extends ServiceImpl<StorageInventoryMapper, StorageInventory>
        implements IStorageInventoryService {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResult<StorageInventoryVO> page(StorageInventoryQuery query) {
        LambdaQueryWrapper<StorageInventory> wrapper = new LambdaQueryWrapper<StorageInventory>()
                .like(StringUtils.hasText(query.getMaterialCode()),
                        StorageInventory::getMaterialCode, query.getMaterialCode())
                .like(StringUtils.hasText(query.getMaterialName()),
                        StorageInventory::getMaterialName, query.getMaterialName())
                .eq(StringUtils.hasText(query.getWarehouse()),
                        StorageInventory::getWarehouse, query.getWarehouse())
                .eq(StringUtils.hasText(query.getStorageLocation()),
                        StorageInventory::getStorageLocation, query.getStorageLocation())
                .eq(StringUtils.hasText(query.getFactory()),
                        StorageInventory::getFactory, query.getFactory())
                .orderByDesc(StorageInventory::getCreatedTime);

        Page<StorageInventory> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<StorageInventoryVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public StorageInventoryVO getDetail(Long id) {
        StorageInventory entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(StorageInventoryDTO dto) {
        StorageInventory entity = new StorageInventory();
        BeanUtils.copyProperties(dto, entity);
        
        // 初始化库存为0如果为null
        if (entity.getUnrestrictedStock() == null) {
            entity.setUnrestrictedStock(BigDecimal.ZERO);
        }
        if (entity.getQualityStock() == null) {
            entity.setQualityStock(BigDecimal.ZERO);
        }
        if (entity.getFrozenStock() == null) {
            entity.setFrozenStock(BigDecimal.ZERO);
        }
        
        save(entity);
        log.info("新增存储地点库存: {} - {}", entity.getMaterialCode(), entity.getMaterialName());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, StorageInventoryDTO dto) {
        StorageInventory existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
        
        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);
        
        log.info("修改存储地点库存: {} - {}", existing.getMaterialCode(), existing.getMaterialName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Long inventoryId, BigDecimal qty) {
        int result = baseMapper.deductStock(inventoryId, qty);
        AssertUtil.isTrue(result > 0, "库存不足");
        log.info("扣减库存: inventoryId={}, qty={}", inventoryId, qty);

        // 发布APS同步事件：库存变动
        publishInventorySyncEvent(inventoryId, "DEDUCT", qty);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStock(Long inventoryId, BigDecimal qty) {
        baseMapper.addStock(inventoryId, qty);
        log.info("增加库存: inventoryId={}, qty={}", inventoryId, qty);

        // 发布APS同步事件：库存变动
        publishInventorySyncEvent(inventoryId, "ADD", qty);
    }

    // ==================== 私有方法 ====================

    private StorageInventoryVO toVO(StorageInventory entity) {
        StorageInventoryVO vo = new StorageInventoryVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private void publishInventorySyncEvent(Long inventoryId, String action, BigDecimal qty) {
        try {
            StorageInventory inv = getById(inventoryId);
            String materialCode = inv != null ? inv.getMaterialCode() : "";
            String payload = String.format(
                    "{\"inventoryId\":%d,\"materialCode\":\"%s\",\"action\":\"%s\",\"qty\":%s}",
                    inventoryId, materialCode, action, qty.toPlainString());
            eventPublisher.publishEvent(new ApsSyncEvent(
                    this, "INVENTORY", "STOCK_CHANGE",
                    inventoryId, materialCode, 5, payload));
        } catch (Exception e) {
            log.warn("发布库存APS同步事件失败（不影响业务）: {}", e.getMessage());
        }
    }
}
