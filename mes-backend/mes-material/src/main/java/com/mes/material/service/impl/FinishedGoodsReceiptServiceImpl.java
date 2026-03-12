package com.mes.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.common.utils.NumberGenerator;
import com.mes.material.domain.dto.FinishedGoodsReceiptDTO;
import com.mes.material.domain.dto.FinishedGoodsReceiptItemDTO;
import com.mes.material.domain.entity.FinishedGoodsReceipt;
import com.mes.material.domain.entity.FinishedGoodsReceiptItem;
import com.mes.material.domain.query.FinishedGoodsReceiptQuery;
import com.mes.material.domain.vo.FinishedGoodsReceiptVO;
import com.mes.material.domain.vo.FinishedGoodsReceiptItemVO;
import com.mes.material.enums.ReceiptStatus;
import com.mes.material.mapper.FinishedGoodsReceiptMapper;
import com.mes.material.mapper.FinishedGoodsReceiptItemMapper;
import com.mes.material.service.IFinishedGoodsReceiptService;
import com.mes.material.service.IStorageInventoryService;
import com.mes.material.domain.entity.StorageInventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 完工入库单 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinishedGoodsReceiptServiceImpl extends ServiceImpl<FinishedGoodsReceiptMapper, FinishedGoodsReceipt>
        implements IFinishedGoodsReceiptService {

    private final FinishedGoodsReceiptItemMapper itemMapper;
    private final IStorageInventoryService storageInventoryService;

    @Override
    public PageResult<FinishedGoodsReceiptVO> page(FinishedGoodsReceiptQuery query) {
        LambdaQueryWrapper<FinishedGoodsReceipt> wrapper = new LambdaQueryWrapper<FinishedGoodsReceipt>()
                .like(StringUtils.hasText(query.getReceiptNo()),
                        FinishedGoodsReceipt::getReceiptNo, query.getReceiptNo())
                .eq(StringUtils.hasText(query.getReceiptType()),
                        FinishedGoodsReceipt::getReceiptType, query.getReceiptType())
                .eq(StringUtils.hasText(query.getStatus()),
                        FinishedGoodsReceipt::getStatus, query.getStatus())
                .orderByDesc(FinishedGoodsReceipt::getCreatedTime);

        Page<FinishedGoodsReceipt> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<FinishedGoodsReceiptVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public FinishedGoodsReceiptVO getDetail(Long id) {
        FinishedGoodsReceipt entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        
        FinishedGoodsReceiptVO vo = toVO(entity);
        
        // 查询明细
        List<FinishedGoodsReceiptItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<FinishedGoodsReceiptItem>()
                        .eq(FinishedGoodsReceiptItem::getReceiptId, id)
                        .orderByAsc(FinishedGoodsReceiptItem::getId));
        vo.setItems(items.stream().map(this::toItemVO).toList());
        
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FinishedGoodsReceiptDTO dto) {
        FinishedGoodsReceipt entity = new FinishedGoodsReceipt();
        BeanUtils.copyProperties(dto, entity);
        
        // 自动生成入库单号
        if (!StringUtils.hasText(entity.getReceiptNo())) {
            entity.setReceiptNo(NumberGenerator.generate("RK"));
        }
        
        entity.setStatus(ReceiptStatus.CREATED.getCode());
        save(entity);
        
        Long receiptId = entity.getId();
        
        // 保存明细
        if (!CollectionUtils.isEmpty(dto.getItems())) {
            for (FinishedGoodsReceiptItemDTO itemDTO : dto.getItems()) {
                FinishedGoodsReceiptItem item = new FinishedGoodsReceiptItem();
                BeanUtils.copyProperties(itemDTO, item);
                item.setReceiptId(receiptId);
                itemMapper.insert(item);
            }
        }
        
        // 库存联动：入库增加库存
        if (!CollectionUtils.isEmpty(dto.getItems())) {
            for (FinishedGoodsReceiptItemDTO itemDTO : dto.getItems()) {
                if (itemDTO.getMaterialId() != null && itemDTO.getReceiptQty() != null) {
                    StorageInventory inventory = storageInventoryService.getOne(
                            new LambdaQueryWrapper<StorageInventory>()
                                    .eq(StorageInventory::getMaterialId, itemDTO.getMaterialId())
                                    .last("LIMIT 1"));
                    if (inventory != null) {
                        storageInventoryService.addStock(inventory.getId(), itemDTO.getReceiptQty());
                        log.info("入库增加库存: materialId={}, qty={}", itemDTO.getMaterialId(), itemDTO.getReceiptQty());
                    }
                }
            }
        }

        log.info("新增完工入库单: {}", entity.getReceiptNo());
        return receiptId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FinishedGoodsReceiptDTO dto) {
        FinishedGoodsReceipt existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
        
        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);
        
        // 删除旧明细
        itemMapper.delete(new LambdaQueryWrapper<FinishedGoodsReceiptItem>()
                .eq(FinishedGoodsReceiptItem::getReceiptId, id));
        
        // 保存新明细
        if (!CollectionUtils.isEmpty(dto.getItems())) {
            for (FinishedGoodsReceiptItemDTO itemDTO : dto.getItems()) {
                FinishedGoodsReceiptItem item = new FinishedGoodsReceiptItem();
                BeanUtils.copyProperties(itemDTO, item);
                item.setReceiptId(id);
                itemMapper.insert(item);
            }
        }
        
        log.info("修改完工入库单: {}", existing.getReceiptNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FinishedGoodsReceipt entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        List<FinishedGoodsReceiptItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<FinishedGoodsReceiptItem>()
                        .eq(FinishedGoodsReceiptItem::getReceiptId, id));

        for (FinishedGoodsReceiptItem item : items) {
            if (item.getMaterialId() != null && item.getReceiptQty() != null) {
                StorageInventory inventory = storageInventoryService.getOne(
                        new LambdaQueryWrapper<StorageInventory>()
                                .eq(StorageInventory::getMaterialId, item.getMaterialId())
                                .last("LIMIT 1"));
                if (inventory != null) {
                    storageInventoryService.deductStock(inventory.getId(), item.getReceiptQty());
                    log.info("入库删除回扣库存: materialId={}, qty={}", item.getMaterialId(), item.getReceiptQty());
                }
            }
        }

        itemMapper.delete(new LambdaQueryWrapper<FinishedGoodsReceiptItem>()
                .eq(FinishedGoodsReceiptItem::getReceiptId, id));
        removeById(id);

        log.info("删除完工入库单: {}", entity.getReceiptNo());
    }

    // ==================== 私有方法 ====================

    private FinishedGoodsReceiptVO toVO(FinishedGoodsReceipt entity) {
        FinishedGoodsReceiptVO vo = new FinishedGoodsReceiptVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private FinishedGoodsReceiptItemVO toItemVO(FinishedGoodsReceiptItem entity) {
        FinishedGoodsReceiptItemVO vo = new FinishedGoodsReceiptItemVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
