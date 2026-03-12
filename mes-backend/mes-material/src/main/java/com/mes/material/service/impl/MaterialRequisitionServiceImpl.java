package com.mes.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mes.common.core.PageResult;
import com.mes.common.result.ResultCode;
import com.mes.common.utils.AssertUtil;
import com.mes.common.utils.NumberGenerator;
import com.mes.material.domain.dto.MaterialRequisitionDTO;
import com.mes.material.domain.dto.MaterialRequisitionItemDTO;
import com.mes.material.domain.entity.MaterialRequisition;
import com.mes.material.domain.entity.MaterialRequisitionItem;
import com.mes.material.domain.query.MaterialRequisitionQuery;
import com.mes.material.domain.vo.MaterialRequisitionVO;
import com.mes.material.domain.vo.MaterialRequisitionItemVO;
import com.mes.material.enums.RequisitionStatus;
import com.mes.material.mapper.MaterialRequisitionMapper;
import com.mes.material.mapper.MaterialRequisitionItemMapper;
import com.mes.material.service.IMaterialRequisitionService;
import com.mes.material.service.IStorageInventoryService;
import com.mes.material.domain.entity.StorageInventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产领料申请 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialRequisitionServiceImpl extends ServiceImpl<MaterialRequisitionMapper, MaterialRequisition>
        implements IMaterialRequisitionService {

    private final MaterialRequisitionItemMapper itemMapper;
    private final IStorageInventoryService storageInventoryService;

    @Override
    public PageResult<MaterialRequisitionVO> page(MaterialRequisitionQuery query) {
        LambdaQueryWrapper<MaterialRequisition> wrapper = new LambdaQueryWrapper<MaterialRequisition>()
                .like(StringUtils.hasText(query.getRequisitionNo()),
                        MaterialRequisition::getRequisitionNo, query.getRequisitionNo())
                .like(StringUtils.hasText(query.getWorkOrderNo()),
                        MaterialRequisition::getWorkOrderNo, query.getWorkOrderNo())
                .eq(query.getWorkOrderId() != null,
                        MaterialRequisition::getWorkOrderId, query.getWorkOrderId())
                .like(StringUtils.hasText(query.getProductCode()),
                        MaterialRequisition::getProductCode, query.getProductCode())
                .eq(StringUtils.hasText(query.getStatus()),
                        MaterialRequisition::getStatus, query.getStatus())
                .orderByDesc(MaterialRequisition::getCreatedTime);

        Page<MaterialRequisition> page = page(
                new Page<>(query.getPageNum(), query.getPageSize()),
                wrapper
        );

        List<MaterialRequisitionVO> voList = page.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(voList, page.getTotal());
    }

    @Override
    public MaterialRequisitionVO getDetail(Long id) {
        MaterialRequisition entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);
        
        MaterialRequisitionVO vo = toVO(entity);
        
        // 查询明细
        List<MaterialRequisitionItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<MaterialRequisitionItem>()
                        .eq(MaterialRequisitionItem::getRequisitionId, id)
                        .orderByAsc(MaterialRequisitionItem::getId));
        vo.setItems(items.stream().map(this::toItemVO).toList());
        
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MaterialRequisitionDTO dto) {
        MaterialRequisition entity = new MaterialRequisition();
        BeanUtils.copyProperties(dto, entity);
        
        // 自动生成领料单号
        if (!StringUtils.hasText(entity.getRequisitionNo())) {
            entity.setRequisitionNo(NumberGenerator.generate("LL"));
        }
        
        entity.setStatus(RequisitionStatus.CREATED.getCode());
        save(entity);
        
        Long requisitionId = entity.getId();
        
        // 保存明细
        if (!CollectionUtils.isEmpty(dto.getItems())) {
            for (MaterialRequisitionItemDTO itemDTO : dto.getItems()) {
                MaterialRequisitionItem item = new MaterialRequisitionItem();
                BeanUtils.copyProperties(itemDTO, item);
                item.setRequisitionId(requisitionId);
                item.setWorkOrderId(dto.getWorkOrderId());
                item.setPendingQty(itemDTO.getDemandQty()); // 设置待领数量等于需求数量
                item.setCreatedTime(LocalDateTime.now());
                item.setUpdatedTime(LocalDateTime.now());
                itemMapper.insert(item);
            }
        }
        
        // 库存联动：领料扣减库存
        if (!CollectionUtils.isEmpty(dto.getItems())) {
            for (MaterialRequisitionItemDTO itemDTO : dto.getItems()) {
                if (itemDTO.getMaterialId() != null && itemDTO.getDemandQty() != null) {
                    // 按物料编码和仓库查找库存记录
                    StorageInventory inventory = storageInventoryService.getOne(
                            new LambdaQueryWrapper<StorageInventory>()
                                    .eq(StorageInventory::getMaterialId, itemDTO.getMaterialId())
                                    .last("LIMIT 1"));
                    if (inventory != null) {
                        storageInventoryService.deductStock(inventory.getId(), itemDTO.getDemandQty());
                        log.info("领料扣减库存: materialId={}, qty={}", itemDTO.getMaterialId(), itemDTO.getDemandQty());
                    }
                }
            }
        }

        log.info("新增生产领料申请: {}", entity.getRequisitionNo());
        return requisitionId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MaterialRequisitionDTO dto) {
        MaterialRequisition existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
        
        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);
        
        // 删除旧明细
        itemMapper.delete(new LambdaQueryWrapper<MaterialRequisitionItem>()
                .eq(MaterialRequisitionItem::getRequisitionId, id));
        
        // 保存新明细
        if (!CollectionUtils.isEmpty(dto.getItems())) {
            for (MaterialRequisitionItemDTO itemDTO : dto.getItems()) {
                MaterialRequisitionItem item = new MaterialRequisitionItem();
                BeanUtils.copyProperties(itemDTO, item);
                item.setRequisitionId(id);
                item.setWorkOrderId(dto.getWorkOrderId());
                item.setPendingQty(itemDTO.getDemandQty());
                item.setCreatedTime(LocalDateTime.now());
                item.setUpdatedTime(LocalDateTime.now());
                itemMapper.insert(item);
            }
        }
        
        log.info("修改生产领料申请: {}", existing.getRequisitionNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MaterialRequisition entity = getById(id);
        AssertUtil.notNull(entity, ResultCode.DATA_NOT_EXIST);

        List<MaterialRequisitionItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<MaterialRequisitionItem>()
                        .eq(MaterialRequisitionItem::getRequisitionId, id));

        for (MaterialRequisitionItem item : items) {
            if (item.getMaterialId() != null && item.getDemandQty() != null) {
                StorageInventory inventory = storageInventoryService.getOne(
                        new LambdaQueryWrapper<StorageInventory>()
                                .eq(StorageInventory::getMaterialId, item.getMaterialId())
                                .last("LIMIT 1"));
                if (inventory != null) {
                    storageInventoryService.addStock(inventory.getId(), item.getDemandQty());
                    log.info("领料删除回补库存: materialId={}, qty={}", item.getMaterialId(), item.getDemandQty());
                }
            }
        }

        itemMapper.delete(new LambdaQueryWrapper<MaterialRequisitionItem>()
                .eq(MaterialRequisitionItem::getRequisitionId, id));
        removeById(id);

        log.info("删除生产领料申请: {}", entity.getRequisitionNo());
    }

    // ==================== 私有方法 ====================

    private MaterialRequisitionVO toVO(MaterialRequisition entity) {
        MaterialRequisitionVO vo = new MaterialRequisitionVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    private MaterialRequisitionItemVO toItemVO(MaterialRequisitionItem entity) {
        MaterialRequisitionItemVO vo = new MaterialRequisitionItemVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
