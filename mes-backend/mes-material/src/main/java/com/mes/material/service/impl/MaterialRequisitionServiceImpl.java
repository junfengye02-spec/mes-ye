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
import com.mes.workorder.domain.entity.WorkOrder;
import com.mes.workorder.domain.entity.WorkOrderInputMaterial;
import com.mes.workorder.enums.WorkOrderStatus;
import com.mes.workorder.mapper.WorkOrderInputMaterialMapper;
import com.mes.workorder.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderInputMaterialMapper workOrderInputMaterialMapper;

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
        WorkOrder workOrder = validateWorkOrderAndItems(dto);

        MaterialRequisition entity = new MaterialRequisition();
        BeanUtils.copyProperties(dto, entity);
        
        // 自动生成领料单号
        if (!StringUtils.hasText(entity.getRequisitionNo())) {
            entity.setRequisitionNo(NumberGenerator.generate("LL"));
        }
        
        entity.setStatus(RequisitionStatus.CREATED.getCode());
        save(entity);
        
        Long requisitionId = entity.getId();
        saveItemsAndApplyState(requisitionId, workOrder.getId(), dto.getItems());

        log.info("新增生产领料申请: {}", entity.getRequisitionNo());
        return requisitionId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromMrp(MaterialRequisitionDTO dto) {
        AssertUtil.isFalse(CollectionUtils.isEmpty(dto.getItems()), "领料明细不能为空");

        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());

        MaterialRequisition entity = new MaterialRequisition();
        BeanUtils.copyProperties(dto, entity);

        if (!StringUtils.hasText(entity.getRequisitionNo())) {
            entity.setRequisitionNo(NumberGenerator.generate("LL"));
        }

        entity.setStatus(RequisitionStatus.CREATED.getCode());
        entity.setCreatedBy("APS");
        entity.setUpdatedBy("APS");
        entity.setCreatedTime(LocalDateTime.now());
        entity.setUpdatedTime(LocalDateTime.now());
        save(entity);

        Long requisitionId = entity.getId();
        saveMrpItems(requisitionId, workOrder.getId(), dto.getItems());

        log.info("APS MRP生成生产领料申请: {}", entity.getRequisitionNo());
        return requisitionId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, MaterialRequisitionDTO dto) {
        MaterialRequisition existing = getById(id);
        AssertUtil.notNull(existing, ResultCode.DATA_NOT_EXIST);
        WorkOrder workOrder = validateWorkOrderAndItems(dto);

        List<MaterialRequisitionItem> oldItems = itemMapper.selectList(
                new LambdaQueryWrapper<MaterialRequisitionItem>()
                        .eq(MaterialRequisitionItem::getRequisitionId, id));
        
        BeanUtils.copyProperties(dto, existing);
        existing.setId(id);
        updateById(existing);

        for (MaterialRequisitionItem oldItem : oldItems) {
            restoreItemState(oldItem);
        }
        
        // 删除旧明细
        itemMapper.delete(new LambdaQueryWrapper<MaterialRequisitionItem>()
                .eq(MaterialRequisitionItem::getRequisitionId, id));
        
        saveItemsAndApplyState(id, workOrder.getId(), dto.getItems());
        
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
            restoreItemState(item);
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

    private WorkOrder validateWorkOrderAndItems(MaterialRequisitionDTO dto) {
        AssertUtil.isFalse(CollectionUtils.isEmpty(dto.getItems()), "领料明细不能为空");

        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());
        AssertUtil.isTrue(
                WorkOrderStatus.RELEASED.getCode().equals(workOrder.getStatus())
                        || WorkOrderStatus.IN_PROGRESS.getCode().equals(workOrder.getStatus()),
                "仅已下达/执行中的工单可以领料");
        return workOrder;
    }

    private void saveItemsAndApplyState(Long requisitionId, Long workOrderId, List<MaterialRequisitionItemDTO> items) {
        for (MaterialRequisitionItemDTO itemDTO : items) {
            AssertUtil.notNull(itemDTO.getMaterialId(), "领料明细物料不能为空");

            BigDecimal issueQty = resolveIssueQty(itemDTO.getIssueQty(), itemDTO.getDemandQty());
            WorkOrderInputMaterial requirement = findRequirement(workOrderId, itemDTO.getMaterialId());
            BigDecimal currentIssuedQty = defaultQty(requirement.getIssuedQty());
            BigDecimal remainingQty = defaultQty(requirement.getRequiredQty()).subtract(currentIssuedQty);
            AssertUtil.isTrue(issueQty.compareTo(remainingQty) <= 0, "领料数量超过工单剩余需求");

            MaterialRequisitionItem item = new MaterialRequisitionItem();
            BeanUtils.copyProperties(itemDTO, item);
            item.setRequisitionId(requisitionId);
            item.setWorkOrderId(workOrderId);
            item.setIssueQty(issueQty);
            item.setPendingQty(calculatePendingQty(itemDTO.getDemandQty(), issueQty));
            item.setCreatedTime(LocalDateTime.now());
            item.setUpdatedTime(LocalDateTime.now());
            itemMapper.insert(item);

            requirement.setIssuedQty(currentIssuedQty.add(issueQty));
            workOrderInputMaterialMapper.updateById(requirement);

            StorageInventory inventory = findInventory(itemDTO.getMaterialId(), itemDTO.getIssueLocation());
            if (inventory != null) {
                storageInventoryService.deductStock(inventory.getId(), issueQty);
                log.info("领料扣减库存: materialId={}, qty={}", itemDTO.getMaterialId(), issueQty);
            }
        }
    }

    private void saveMrpItems(Long requisitionId, Long workOrderId, List<MaterialRequisitionItemDTO> items) {
        for (MaterialRequisitionItemDTO itemDTO : items) {
            AssertUtil.notNull(itemDTO.getMaterialId(), "领料明细物料不能为空");

            BigDecimal demandQty = requireDemandQty(itemDTO.getDemandQty());

            MaterialRequisitionItem item = new MaterialRequisitionItem();
            BeanUtils.copyProperties(itemDTO, item);
            item.setRequisitionId(requisitionId);
            item.setWorkOrderId(workOrderId);
            item.setDemandQty(demandQty);
            item.setPendingQty(demandQty);
            item.setIssueQty(BigDecimal.ZERO);
            item.setCreatedTime(LocalDateTime.now());
            item.setUpdatedTime(LocalDateTime.now());
            itemMapper.insert(item);
        }
    }

    private void restoreItemState(MaterialRequisitionItem item) {
        BigDecimal issueQty = resolveIssueQty(item.getIssueQty(), item.getDemandQty());
        if (item.getMaterialId() != null) {
            StorageInventory inventory = findInventory(item.getMaterialId(), item.getIssueLocation());
            if (inventory != null) {
                storageInventoryService.addStock(inventory.getId(), issueQty);
                log.info("领料回补库存: materialId={}, qty={}", item.getMaterialId(), issueQty);
            }
        }

        if (item.getWorkOrderId() == null || item.getMaterialId() == null) {
            return;
        }

        WorkOrderInputMaterial requirement = workOrderInputMaterialMapper.selectOne(
                new LambdaQueryWrapper<WorkOrderInputMaterial>()
                        .eq(WorkOrderInputMaterial::getWorkOrderId, item.getWorkOrderId())
                        .eq(WorkOrderInputMaterial::getMaterialId, item.getMaterialId())
                        .last("LIMIT 1"));
        if (requirement == null) {
            return;
        }

        BigDecimal currentIssuedQty = defaultQty(requirement.getIssuedQty());
        BigDecimal revertedIssuedQty = currentIssuedQty.subtract(issueQty);
        if (revertedIssuedQty.compareTo(BigDecimal.ZERO) < 0) {
            revertedIssuedQty = BigDecimal.ZERO;
        }
        requirement.setIssuedQty(revertedIssuedQty);
        workOrderInputMaterialMapper.updateById(requirement);
    }

    private WorkOrderInputMaterial findRequirement(Long workOrderId, Long materialId) {
        WorkOrderInputMaterial requirement = workOrderInputMaterialMapper.selectOne(
                new LambdaQueryWrapper<WorkOrderInputMaterial>()
                        .eq(WorkOrderInputMaterial::getWorkOrderId, workOrderId)
                        .eq(WorkOrderInputMaterial::getMaterialId, materialId)
                        .last("LIMIT 1"));
        AssertUtil.notNull(requirement, "工单未配置该物料需求");
        return requirement;
    }

    private WorkOrder requireWorkOrder(Long workOrderId) {
        AssertUtil.notNull(workOrderId, "关联工单不存在");
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        AssertUtil.notNull(workOrder, "关联工单不存在");
        return workOrder;
    }

    private StorageInventory findInventory(Long materialId, String preferredLocation) {
        List<StorageInventory> inventories = storageInventoryService.list(
                new LambdaQueryWrapper<StorageInventory>()
                        .eq(StorageInventory::getMaterialId, materialId));
        if (CollectionUtils.isEmpty(inventories)) {
            return null;
        }

        return inventories.stream()
                .filter(Objects::nonNull)
                .max(Comparator
                        .comparing((StorageInventory inventory) ->
                                        locationMatched(preferredLocation, inventory.getStorageLocation()))
                                .thenComparing(inventory -> defaultQty(inventory.getUnrestrictedStock()))
                                .thenComparing(StorageInventory::getId, Comparator.nullsLast(Long::compareTo)))
                .orElse(null);
    }

    private boolean locationMatched(String preferredLocation, String actualLocation) {
        return StringUtils.hasText(preferredLocation) && preferredLocation.equals(actualLocation);
    }

    private BigDecimal resolveIssueQty(BigDecimal issueQty, BigDecimal demandQty) {
        BigDecimal requestedQty = issueQty != null ? issueQty : demandQty;
        AssertUtil.notNull(requestedQty, "领料数量不能为空");
        AssertUtil.isTrue(requestedQty.compareTo(BigDecimal.ZERO) > 0, "领料数量必须大于0");
        return requestedQty;
    }

    private BigDecimal calculatePendingQty(BigDecimal demandQty, BigDecimal issueQty) {
        if (demandQty == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal pendingQty = demandQty.subtract(issueQty);
        return pendingQty.compareTo(BigDecimal.ZERO) > 0 ? pendingQty : BigDecimal.ZERO;
    }

    private BigDecimal requireDemandQty(BigDecimal demandQty) {
        AssertUtil.notNull(demandQty, "需求数量不能为空");
        AssertUtil.isTrue(demandQty.compareTo(BigDecimal.ZERO) > 0, "需求数量必须大于0");
        return demandQty;
    }

    private BigDecimal defaultQty(BigDecimal qty) {
        return qty != null ? qty : BigDecimal.ZERO;
    }
}
